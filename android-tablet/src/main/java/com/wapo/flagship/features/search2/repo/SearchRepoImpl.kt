package com.wapo.flagship.features.search2.repo

import android.content.Context
import com.wapo.android.commons.util.Logger
import androidx.appsearch.app.*
import androidx.appsearch.localstorage.LocalStorage
import androidx.concurrent.futures.await
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.DeviceUtils
import com.wapo.android.commons.util.LiveEvent
import com.wapo.android.domain.repository.RemoteLogRepo
import com.wapo.flagship.AppContext
import com.wapo.flagship.common.getMenuSections
import com.wapo.flagship.domain.repository.SearchRepo
import com.wapo.flagship.features.aixp.models.ArticleSource
import com.wapo.flagship.features.search2.events.SseEvent
import com.wapo.flagship.features.search2.model.*
import com.wapo.flagship.features.search2.remote.OkHttpSseService
import com.wapo.flagship.features.search2.remote.PostAnswersService
import com.wapo.flagship.features.search2.remote.Search2Service
import com.wapo.flagship.features.search2.remote.SearchRecipeService
import com.wapo.flagship.json.MenuSection
import com.wapo.flagship.network.retrofit.network.APIResult
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.R
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Repository responsible for fetching the following
 * - Article search results from endpoint
 * - Sections search results (potentially from endpoint)
 * - Recent searches stored in client DB (using App Search)
 * - Top Stories
 */

private const val TAG = "SearchRepository"

class SearchRepoImpl
@Inject
constructor(
    @ApplicationContext val context: Context,
    private val searchService: Search2Service,
    private val searchRecipeService: SearchRecipeService,
    private val postAnswersService: PostAnswersService,
    private val okHttpSseService: OkHttpSseService,
    private val remoteLogRepo: RemoteLogRepo
) : SearchRepo {
    /**
     * Session used to connect to DB for storing and retrieving recent searches
     */
    private lateinit var appSearchSession: AppSearchSession

    /**
     * Flow for tracking if search session has been initialized
     */
    private val isInitialized: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /**
     * LiveEvent for tracking result of search request.
     */
    override val requestSearchResultsStatus = LiveEvent<SearchResultApiStatus>()

    val articlesList = mutableListOf<Document>()

    var totalCount = 0

    /**
     * LiveEvent for tracking result of search request.
     */
    override val updateRecentStatus = LiveEvent<RecentLocalStatus>()

    private var searchRecentResults: SearchResults? = null

    /**
     * Cached list
     */
    override val searchQueryList = mutableListOf<SearchQuery>()

    // Coroutine scope for handling SSE event collection
    private val sseCoroutineScope = CoroutineScope(Dispatchers.IO + Job())

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _sseEventState = MutableSharedFlow<SseEvent>(
        replay = 0,
        extraBufferCapacity = 5000,
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    override val sseEventState: SharedFlow<SseEvent?> = _sseEventState

    init {
        observeSseEvents()
    }

    /**
     * Search for [query] and post results on [SearchResultApiStatus] live data.
     * Returns both a list of sections and articles matching query
     */
    override fun searchFor(queryFilter: QueryFilter) {
        coroutineScope.launch {
            var menuItems = getMenuSections()?.toMutableList()
            menuItems = filterOutDistinctMenuApplySearchQuery(menuItems, queryFilter.query)
            val result = searchService.searchFor(queryFilter.toQueryParamsMap())
            processSearchResult(queryFilter, result, menuItems)
            logErrorIfNeeded(queryFilter, result, false)
        }
    }

    override fun searchRecipe(queryFilter: QueryFilter) {
        coroutineScope.launch {
            val result =
                searchRecipeService.searchFor(
                    fullPath = getRecipeUrl(),
                    queryFilter.toRecipeQueryParamsMap(),
                )
            when (result) {
                is APIResult.Failure -> {
                    Logger.d(TAG, "searchRecipe: ERROR ${result.rawResponse}")
                }

                is APIResult.NetworkError -> {
                    requestSearchResultsStatus.postValue(
                        SearchResultApiStatus.Failure,
                    )
                    Logger.d(TAG, "searchRecipe: NETWORK ERROR: ${result.error}")
                }

                is APIResult.Success -> {
                    result.data?.results?.total?.let {
                        totalCount = it
                    }
                    requestSearchResultsStatus.postValue(
                        result.data?.results?.let { resultData ->
                            articlesList.clear()
                            articlesList.addAll(resultData.documents)
                            SearchResultApiStatus.Success(
                                articlesResult = resultData,
                                sections = emptyList(),
                                filters = null,
                            )
                        } ?: SearchResultApiStatus.Failure,
                    )
                }
            }
            logErrorIfNeeded(queryFilter, result, true)
        }
    }

    override suspend fun getPostAnswer(
        query: String,
        results: Results?,
        appName: String?,
    ): APIResult<PostAnswerResponse> {
        val urls =
            if (results?.documents?.size != null && results.documents.size > 10) {
                results.documents.subList(0, 10).mapNotNull { it.contenturl }
            } else {
                results?.documents?.mapNotNull { it.contenturl }
            }
        val requestBody = PostAnswerRequest(
            query = query,
            urls = urls,
            appName = appName,
            supportId = DeviceUtils.getUniqueDeviceId(context)
        )
        return postAnswersService.getPostAnswer(POST_ANSWERS_CALL_TIMEOUT, requestBody)
    }

    override suspend fun loadMoreSearches(
        queryFilter: QueryFilter,
        isRecipe: Boolean,
    ) {
        if (articlesList.count() >= totalCount) {
            requestSearchResultsStatus.postValue(SearchResultApiStatus.LoadMore(articlesList))
            return
        }

        queryFilter.offset = articlesList.count()
        val result =
            when {
                isRecipe ->
                    searchRecipeService.searchFor(
                        fullPath = getRecipeUrl(),
                        queryFilter.toRecipeQueryParamsMap(),
                    )

                else -> searchService.searchFor(queryFilter.toQueryParamsMap())
            }
        when (result) {
            is APIResult.Success -> {
                result.data?.results?.documents?.let {
                    articlesList.addAll(it)
                }
                requestSearchResultsStatus.postValue(SearchResultApiStatus.LoadMore(articlesList))
            }

            is APIResult.Failure -> {
                if (AppContextUtils.isConnectingOrConnected()) {
                    val builder: EventLog.Builder = EventLog.Builder()
                        .setModule(if (isRecipe) LogModules.SEARCH_RECIPE else LogModules.SEARCH)
                        .setModule(LogModules.SEARCH)
                        .setErrorMessage(result.getMessage())
                        .set("status", result.statusCode)
                    remoteLogRepo.e(builder.build())
                }
            }

            else -> {}
        }
        logErrorIfNeeded(queryFilter, result, isRecipe)
    }

    private fun filterOutDistinctMenuApplySearchQuery(
        sections: MutableList<MenuSection>?,
        queryStr: String,
    ): MutableList<MenuSection> {
        val filtered = mutableListOf<MenuSection>()
        if (sections == null) {
            return filtered
        }
        for (section in sections) {
            var subSections: Array<MenuSection>? = section.sectionInfo
            if (subSections.isNullOrEmpty()) subSections = arrayOf(section)
            if (subSections.isNotEmpty()) {
                filtered.addAll(
                    subSections
                        .filter {
                            it.displayName.lowercase().contains(queryStr.lowercase())
                        }.toTypedArray()
                        .map {
                            MenuSection(
                                it.title,
                                it.displayName,
                                it.type,
                                it.databaseId,
                                if (MenuSection.COMICS_TYPE == it.type) {
                                    MenuSection.COMICS_TYPE
                                } else {
                                    it.bundleName
                                },
                                null,
                                3,
                                it.aliases,
                                false,
                            )
                        },
                )
            }
        }
        return filtered.distinctBy { it.databaseId }.toMutableList()
    }

    override fun parseArticleSources(jsonString: String): List<ArticleSource> {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val listType = Types.newParameterizedType(List::class.java, ArticleSource::class.java)

        val adapter = moshi.adapter<List<ArticleSource>>(listType)

        return adapter.fromJson(jsonString) ?: emptyList()
    }

    override fun startLiveConversation(
        query: String,
        private: Boolean,
        voiceResponse: String?,
        timestamp: Float?,
        transcriptUrl: String?,
        entryPoint: String?
    ) {
        val askThePostRequest = AskThePostRequest(
            message = query,
            uuid = if (private) null else AppContext.getAirshipNamedUserId(context),
            anonymous = private,
            voiceResponse = voiceResponse,
            timestamp = timestamp,
            transcriptUrl = transcriptUrl
        )
        val sseRequest = okHttpSseService.buildSseRequest(
            endpointPath = context.getString(R.string.converse),
            surfaceName = entryPoint,
            askThePostRequest = askThePostRequest
        )
        okHttpSseService.connect(sseRequest)
    }

    override fun observeSseEvents() {
        // Start collecting SSE events and updating the StateFlow
        sseCoroutineScope.launch {
            withContext(Dispatchers.Main) {
                okHttpSseService.sseEvents.collect { event ->
                    _sseEventState.emit(event)
                }
            }
        }
    }

    override fun continueLiveConversation(
        conversationId: String?,
        query: String,
        private: Boolean,
        voiceResponse: String?,
        entryPoint: String?,
        shareId: String?
    ) {
        val askThePostRequest = AskThePostRequest(
            message = query,
            uuid = if (private) null else AppContext.getAirshipNamedUserId(context),
            anonymous = private,
            voiceResponse = voiceResponse,
            conversationId = conversationId,
            shareId = shareId
        )
        val sseRequest = okHttpSseService.buildSseRequest(
            endpointPath = context.getString(R.string.converse),
            surfaceName = entryPoint,
            askThePostRequest = askThePostRequest
        )

        okHttpSseService.connect(sseRequest)
    }

    override suspend fun endLiveConversation() {
        okHttpSseService.disconnect()
        _sseEventState.emit(SseEvent.ForceStop)
    }


    /**
     * Process search response for both articles and sections
     * and map to [SearchResultApiStatus] live data.
     */
    private fun processSearchResult(
        queryFilter: QueryFilter,
        result: APIResult<SearchResultResponse>,
        sections: MutableList<MenuSection>,
    ): SearchResultResponse? {
        when (result) {
            is APIResult.Failure, is APIResult.NetworkError -> {
                requestSearchResultsStatus.postValue(
                    SearchResultApiStatus.Failure,
                )
            }

            is APIResult.Success -> {
                result.data?.results?.total?.let {
                    totalCount = it
                }
                requestSearchResultsStatus.postValue(
                    result.data?.results?.let { resultData ->
                        articlesList.clear()
                        articlesList.addAll(resultData.documents)
                        SearchResultApiStatus.Success(
                            resultData,
                            sections.map {
                                Section(
                                    it.databaseId,
                                    it.displayName,
                                    it.type,
                                    it.bundleName,
                                )
                            },
                            result.data.filters,
                        )
                    } ?: SearchResultApiStatus.Failure,
                )

                return result.data
            }

            else -> {}
        }

        return null
    }

    /**
     * Start session for App Search.
     */
    override suspend fun startAppSearchSession() {
        // Creates a [AppSearchSession], for S+ devices uses PlatformStorage, for R- devices uses
        // LocalStorage session.
        appSearchSession =
            LocalStorage
                .createSearchSessionAsync(
                    LocalStorage.SearchContext.Builder(context, DATABASE_NAME).build(),
                ).await()

        try {
            // Sets the schema for the AppSearch database by registering the [Note] document class as a
            // schema type in the overall database schema.
            val setSchemaRequest =
                SetSchemaRequest.Builder().addDocumentClasses(SearchQuery::class.java).build()
            appSearchSession.setSchemaAsync(setSchemaRequest).await()

            // Set the [NoteAppSearchManager] instance as initialized to allow AppSearch operations to
            // be called.
            isInitialized.value = true

            awaitCancellation()
        } finally {
            appSearchSession.close()
        }
    }

    /**
     * Adds a [SearchQuery] document to the AppSearch database.
     */
    override suspend fun addSearched(searchQuery: SearchQuery) {
        awaitInitialization()

        val request = PutDocumentsRequest.Builder().addDocuments(searchQuery).build()
        val result = appSearchSession.putAsync(request).await()

        when {
            result.isSuccess -> updateRecentStatus.postValue(RecentLocalStatus.AddSuccess)
            else ->
                updateRecentStatus.postValue(
                    RecentLocalStatus.Failure("Failed to add ${searchQuery.id}"),
                )
        }
    }

    override suspend fun searchElection(queryFilter: QueryFilter) {
        val result = searchService.searchFor(queryFilter.toElectionQueryParamsMap())
        processElectionSearchResult(result)
    }

    private fun processElectionSearchResult(result: APIResult<SearchResultResponse>) {
        when (result) {
            is APIResult.Failure -> {
                requestSearchResultsStatus.postValue(
                    SearchResultApiStatus.Failure,
                )

                if (AppContextUtils.isConnectingOrConnected()) {
                    val eventLog = EventLog.Builder()
                        .setMessage("Election Search Failed")
                        .setModule(LogModules.SEARCH)
                        .setErrorMessage(result.getMessage())
                        .set("status", result.statusCode)
                        .build()
                    remoteLogRepo.e(eventLog)
                }
            }
            is APIResult.NetworkError ->
                requestSearchResultsStatus.postValue(
                    SearchResultApiStatus.Failure,
                )
            is APIResult.Success -> {
                result.data?.results?.total?.let {
                    totalCount = it
                }
                requestSearchResultsStatus.postValue(
                    result.data?.results?.let { resultData ->
                        articlesList.clear()
                        articlesList.addAll(resultData.documents)
                        SearchResultApiStatus.Success(
                            resultData,
                            sections = emptyList(),
                            filters = null,
                        )
                    } ?: SearchResultApiStatus.Failure,
                )
            }

            else -> {}
        }
    }

    /**
     * Queries the AppSearch database for matching [SearchQuery] documents.
     *
     * @return a list of [SearchResult] objects. This returns SearchResults in the order
     * they were created (with most recent first). This returns a maximum of 10
     * SearchResults that match the query, per AppSearch default page size.
     * Snippets are returned for the first 10 results.
     */
    override suspend fun getRecentSearches(query: String) {
        awaitInitialization()

        if (searchRecentResults == null) {
            val searchSpec =
                SearchSpec
                    .Builder()
                    .setRankingStrategy(SearchSpec.RANKING_STRATEGY_CREATION_TIMESTAMP)
                    .setSnippetCount(10)
                    .build()

            searchRecentResults = appSearchSession.search(query, searchSpec)
        }

        searchRecentResults
            ?.nextPageAsync
            ?.await()
            ?.map {
                it.genericDocument.toDocumentClass(SearchQuery::class.java)
            }?.let {
                searchQueryList.addAll(it)
                updateRecentStatus.postValue(RecentLocalStatus.FetchNextSuccess(searchQueryList))
                return
            }

        updateRecentStatus.postValue(RecentLocalStatus.FetchNextSuccess(listOf()))
    }

    override fun resetRecentSearchResults() {
        searchQueryList.clear()
        articlesList.clear()
        searchRecentResults = null
        totalCount = 0
    }

    /**
     * Removes [SearchQuery] document from the AppSearch database. If query is empty,
     * all items will be removed
     */
    override suspend fun removeRecentSearch(id: String) {
        awaitInitialization()

        val request = RemoveByDocumentIdRequest.Builder(NAME_SPACE).addIds(id).build()
        val result = appSearchSession.removeAsync(request).await()

        when {
            result.isSuccess -> {
                updateRecentStatus.postValue(RecentLocalStatus.RemoveSuccess)
            }

            else ->
                updateRecentStatus.postValue(
                    RecentLocalStatus.Failure("Failed to remove queries"),
                )
        }
    }

    override suspend fun removeAllRecentSearch() {
        awaitInitialization()

        val searchSpec =
            SearchSpec
                .Builder()
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_CREATION_TIMESTAMP)
                .setSnippetCount(10)
                .build()

        appSearchSession.removeAsync("", searchSpec).await()

        updateRecentStatus.postValue(RecentLocalStatus.RemoveAllSuccess)
    }

    /**
     * Awaits [isInitialized] being set to ```true```.
     */
    private suspend fun awaitInitialization() {
        if (!isInitialized.value) {
            isInitialized.first { it }
        }
    }

    private fun logErrorIfNeeded(
        queryFilter: QueryFilter,
        result: APIResult<SearchResultResponse>,
        isRecipe: Boolean,
    ) {
        when (result) {
            is APIResult.Failure -> {
                logError(
                    result.statusCode,
                    result.rawResponse,
                    "Search Error",
                    queryFilter,
                    isRecipe,
                    null,
                )
            }

            is APIResult.Success -> {
                if (result.data?.results == null) {
                    logError(
                        -1,
                        null,
                        "Search Parse Error",
                        queryFilter,
                        isRecipe,
                        null,
                    )
                }
            }

            is APIResult.NetworkError -> Unit
        }
    }

    private fun logError(
        errorCode: Int?,
        errorMessage: String?,
        message: String?,
        queryFilter: QueryFilter?,
        isRecipe: Boolean = false,
        t: Throwable?,
    ) {
        EventLog
            .Builder()
            .apply {
                setErrorCode(errorCode)
                setErrorMessage(errorMessage)
                setMessage(message)
                setModule(if (isRecipe) LogModules.SEARCH_RECIPE else LogModules.SEARCH)
                queryFilter?.toQueryParamsMap()?.forEach { entry ->
                    set(entry.key, entry.value)
                }
            }.run {
                remoteLogRepo.e( build())
            }
    }

    private fun getRecipeUrl(): String = ConfigManager.getInstance().config.recipesConfig.baseUrl

    companion object {
        private const val DATABASE_NAME = "queryDatabase"
        private const val LOAD_COUNT = 20
        private const val POST_ANSWERS_CALL_TIMEOUT = 12000
    }
}
