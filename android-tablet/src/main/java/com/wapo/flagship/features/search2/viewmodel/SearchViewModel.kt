package com.wapo.flagship.features.search2.viewmodel

import android.os.Build
import android.text.Html
import com.wapo.android.commons.util.Logger
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.LiveEvent
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.search2.events.UserEvent
import com.wapo.flagship.features.search2.model.ArticleItem
import com.wapo.flagship.features.search2.model.Document
import com.wapo.flagship.features.search2.model.ElectionItem
import com.wapo.flagship.features.search2.model.FilterHeaderItem
import com.wapo.flagship.features.search2.model.FilterRadioItem
import com.wapo.flagship.features.search2.model.Filters
import com.wapo.flagship.features.search2.model.PostAnswerCarousel
import com.wapo.flagship.features.search2.model.PostAnswerFeedback
import com.wapo.flagship.features.search2.model.PostAnswerResponse
import com.wapo.flagship.features.search2.model.PostAnswerText
import com.wapo.flagship.features.search2.model.QueryFilter
import com.wapo.flagship.features.search2.model.RecentLocalStatus
import com.wapo.flagship.features.search2.model.RecipeItem
import com.wapo.flagship.features.search2.model.Results
import com.wapo.flagship.features.search2.model.SearchItem
import com.wapo.flagship.features.search2.model.SearchQuery
import com.wapo.flagship.features.search2.model.SearchQueryItem
import com.wapo.flagship.features.search2.model.SearchResultApiStatus
import com.wapo.flagship.features.search2.model.Section
import com.wapo.flagship.features.search2.model.SectionItem
import com.wapo.flagship.features.search2.state.PostAnswersUIState
import com.wapo.flagship.features.search2.state.SearchUiState
import com.wapo.flagship.features.search2.ui.CarouselSubtype
import com.wapo.flagship.features.search2.ui.CarouselUIItem
import com.wapo.flagship.features.search2.ui.MimeType
import com.wapo.flagship.features.search2.ui.PostAnswerUIItem
import com.wapo.flagship.features.search2.ui.TextSubtype
import com.wapo.flagship.network.retrofit.network.APIResult
import com.wapo.flagship.util.coroutines.DispatcherProvider
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.R
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.LinksItems
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.text.style.URLSpan
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.AnnotatedString
import com.wapo.android.domain.repository.LoadRenderMetrics
import com.wapo.android.domain.repository.LoadRenderMetricsEvent
import com.wapo.flagship.domain.repository.SearchRepo
import com.wapo.flagship.features.search2.model.ParsedCitation
import com.wapo.flagship.features.search2.model.ParsedTextResult
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class SearchViewModel
@Inject
constructor(
    private val searchRepo: SearchRepo,
    val dispatcherProvider: DispatcherProvider,
    private val loadRenderMetrics: LoadRenderMetrics
) : ViewModel() {

    private val config get() = ConfigManager.getInstance().config

    /**
     * Ui state that directly dictates the content of the search screen
     */
    private val _searchUiState = MediatorLiveData<SearchUiState>()
    val searchUiState: LiveData<SearchUiState> = _searchUiState

    private val _postAnswersUIState =
        MutableStateFlow<PostAnswersUIState>(PostAnswersUIState.Loading)
    var postAnswersUIState: StateFlow<PostAnswersUIState> = _postAnswersUIState

    private val _initiatePostAnswersFeedbackEvent = LiveEvent<Triple<String, String, Int>>()
    val initiatePostAnswersFeedbackEvent: LiveEvent<Triple<String, String, Int>> =
        _initiatePostAnswersFeedbackEvent

    /**
     * User event that handles
     */
    private val _userEvent = LiveEvent<UserEvent>()
    val userEvent: LiveData<UserEvent> = _userEvent

    /**
     * hold state of expand/collapse of sections list
     */
    private val _loadMore = MediatorLiveData<Boolean>()
    val loadMore: LiveData<Boolean> = _loadMore

    /**
     * Holds query & searchType to send to analytics when search is queried
     */
    var searchQuery: QueryFilter? = null

    var searchType = ""
    var navigationBehavior: String? = null

    var articleWasOpened = false

    var electionSearchPageName = ""

    var recipeNavigationBehavior = Measurement.RECIPE_FINDER_NAVIGATION

    private var _searchMode = MutableLiveData<SearchMode>()
    val searchMode: LiveData<SearchMode> = _searchMode

    var currentItems: List<SearchItem>? = null

    init {
        _searchMode.value = SearchMode.Regular
        initRecentSearch()
        addSearchRemoteSource()
        addUpdateQuerySource()
    }

    val isFilterVisible
        // temporarily removed check on query response during Search AB Testb
        get() = _searchMode.value == SearchMode.Recipe // || searchQuery?.query?.isNotEmpty() == true

    /**
     * Initialize App Search storage to get recent queries
     */
    private fun initRecentSearch() {
        viewModelScope.launch(dispatcherProvider.io) {
            searchRepo.startAppSearchSession()
        }
    }

    /**
     * Post event for any items clicked in RV
     */
    fun itemClicked(event: UserEvent) {
        _userEvent.postValue(event)
    }

    /**
     *
     */
    fun loadMoreSections(load: Boolean) {
        if (_loadMore.value != load) {
            _loadMore.postValue(load)
        }
    }

    fun addEventType(type: String) {
        searchType = type
    }

    fun articleStatus() {
        if (articleWasOpened) {
            if (SearchMode.Recipe == searchMode.value) {
                Measurement.trackSearchRecipePageView(
                    "recipe_finder_result",
                    Measurement.PATH_TO_VIEW_BACK_TO_FRONT,
                )
            } else if (SearchMode.Election == searchMode.value) {
                Measurement.trackSearchElectionPageView(
                    electionSearchPageName,
                    searchQuery,
                    Measurement.PATH_TO_VIEW_BACK_TO_FRONT,
                )
            } else {
                Measurement.trackBackToSearchResults(searchQuery, navigationBehavior)
            }
            articleWasOpened = false
        }
    }

    /**
     * show landing page on initialization
     */
    fun showLandingPage() {
        when (_searchMode.value) {
            SearchMode.Regular -> {
                _searchUiState.postValue(SearchUiState.Loading)
                viewModelScope.launch(dispatcherProvider.io) {
                    searchRepo.getRecentSearches()
                }
                Measurement.trackMainSearchPage()
            }

            SearchMode.Recipe -> {
                _searchUiState.postValue(SearchUiState.RecipeLanding)
            }

            SearchMode.Election -> {
                val links = config.electionConfig.links
                val electionItems = links.map { it.toElectionItem() }
                _searchUiState.postValue(SearchUiState.ElectionLanding(electionItems))
            }

            null -> {}
        }
    }

    fun LinksItems.toElectionItem(): ElectionItem =
        ElectionItem(
            name = this.name,
            type = this.type,
            path = this.path,
        )

    /**
     * Update landing page based on changes to query list
     */
    private fun updateLandingPage() {
        searchRepo.searchQueryList
            .map {
                SearchQueryItem(it.text)
            }.let {
                _searchUiState.postValue(SearchUiState.Landing(it))
            }
    }

    /**
     * Add Recent Query to the list.
     *  - Update cached list in repository
     *  - Update db list in repo
     */
    fun addRecentQuery(
        queryId: String,
        query: String,
    ) {
        val searchQuery = SearchQuery(id = queryId, text = query)
        viewModelScope.launch(dispatcherProvider.io) {
            searchRepo.addSearched(searchQuery)
        }
    }

    /**
     * Remove Recent Query to the list.
     *  - Update cached list in repository
     *  - Update db list in repo
     */
    fun removeRecentQuery(query: String) {
        val searchQuery =
            searchRepo.searchQueryList.find { it.id == query || it.text == query }
        viewModelScope.launch(dispatcherProvider.io) {
            searchQuery?.let {
                searchRepo.searchQueryList.remove(searchQuery)
                updateLandingPage()
                searchRepo.removeRecentSearch(query)
            }
        }
    }

    /**
     * Remove All Recent Queries
     *  - Update cached list in repository
     *  - Update db list in repo
     */
    fun clearAllRecentQueries() {
        viewModelScope.launch(dispatcherProvider.io) {
            searchRepo.searchQueryList.clear()
            updateLandingPage()
            searchRepo.removeAllRecentSearch()
        }
    }

    /**
     * Execute search for given query.
     * Hits repo and remote api to get results
     */
    fun searchFor(query: QueryFilter) {
        loadRenderMetrics.startLoadRenderMetrics(LoadRenderMetricsEvent.SearchResultRenderEvent)
        when (_searchMode.value) {
            SearchMode.Regular -> {
                // TODO: Prevent empty searches
                _searchUiState.postValue(SearchUiState.Loading)
                searchQuery = query
                viewModelScope.launch(dispatcherProvider.io) {
                    searchRepo.resetRecentSearchResults()
                    searchQuery?.let {
                        searchRepo.searchFor(it)
                    }
                }
            }

            SearchMode.Recipe -> {
                if (query.hasRecipeQueryOrFilters()) {
                    _searchUiState.postValue(SearchUiState.Loading)
                    searchQuery = query
                    viewModelScope.launch(dispatcherProvider.io) {
                        searchQuery?.let {
                            searchRepo.searchRecipe(it)
                        }
                    }
                } else {
                    _searchUiState.postValue(SearchUiState.RecipeLanding)
                }
            }

            SearchMode.Election -> {
                _searchUiState.postValue(SearchUiState.Loading)
                searchQuery = query
                viewModelScope.launch(dispatcherProvider.io) {
                    searchQuery?.let {
                        searchRepo.searchElection(it)
                    }
                }
            }

            else -> {
                // no-op
            }
        }
    }

    fun loadMoreSearches() {
        // check if we are in recipe search
        val isRecipe = _searchMode.value == SearchMode.Recipe
        _searchUiState.postValue(SearchUiState.NextPageLoading)
        viewModelScope.launch(dispatcherProvider.io) {
            searchQuery?.let {
                searchRepo.loadMoreSearches(it, isRecipe)
            }
        }
    }

    /**
     * Add remote source that holds search results to UI live data
     */
    private fun addSearchRemoteSource() {
        _searchUiState.addSource(searchRepo.requestSearchResultsStatus) { status ->
            loadRenderMetrics.stopLoadRenderMetrics(LoadRenderMetricsEvent.SearchResultRenderEvent)
            when (status) {
                SearchResultApiStatus.Failure -> _searchUiState.postValue(SearchUiState.Failure)
                is SearchResultApiStatus.LoadMore ->
                    _searchUiState.postValue(
                        SearchUiState.NextPageLoaded(
                            status.articles.toSearchItems(),
                        ),
                    )

                is SearchResultApiStatus.Success -> {
                    when {
                        status.articlesResult.total > 0 -> {
                            _searchUiState.postValue(
                                SearchUiState.Success(
                                    status.sections.toSectionItems(),
                                    status.articlesResult.documents.toSearchItems(),
                                    status.articlesResult.total,
                                    status.filters?.toFilterMap(),
                                ),
                            )
                            searchQuery?.query?.let {
                                if (SearchMode.Recipe == searchMode.value) {
                                    Measurement.trackSearchPageRecipeQuerySubmitted(
                                        searchQuery?.query,
                                        recipeNavigationBehavior,
                                    )
                                    resetRecipeNavigationBehavior() // reset Recipe navigation behavior after search to cover Deeplink case
                                } else if (SearchMode.Election == searchMode.value) {
                                    electionSearchPageName = "election_search_result"
                                    Measurement.trackSearchPageElectionQuerySubmitted(
                                        searchQuery?.query,
                                        "search_sn_elections_keyword",
                                    )
                                } else {
                                    Measurement.trackSearchResults(
                                        searchQuery?.queryId,
                                        it,
                                        searchType,
                                        navigationBehavior,
                                    )
                                }
                            }

                            getPostAnswer(status.articlesResult)
                        }

                        else -> {
                            _searchUiState.postValue(SearchUiState.NoMatches)
                            searchQuery?.query?.let {
                                Measurement.trackSearchResults(
                                    searchQuery?.queryId,
                                    it,
                                    searchType,
                                    Measurement.NAVIGATION_BEHAVIOR_SEARCH_NOT_FOUND,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getPostAnswer(articlesResult: Results) {
        if (!config.search2Config.postAnswersEnabled) return

        viewModelScope.launch(dispatcherProvider.io) {
            searchQuery?.let {
                _postAnswersUIState.value = PostAnswersUIState.Loading
                val startTime = System.currentTimeMillis()
                val response = searchRepo.getPostAnswer(it.query, articlesResult)
                val responseTimeMillis = System.currentTimeMillis() - startTime
                _postAnswersUIState.value =
                    processPostAnswerResult(
                        it.query,
                        responseTimeMillis,
                        response,
                    )
                Measurement.trackPostAnswersImpression(
                    it.queryId,
                    it.query,
                    searchType,
                    navigationBehavior,
                    _postAnswersUIState.value is PostAnswersUIState.Success,
                )
            }
        }
    }

    fun parseCitations(html: String): ParsedTextResult {
        val spanned = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        else
            Html.fromHtml(html)

        val builder = AnnotatedString.Builder()
        val citations = mutableListOf<ParsedCitation>()

        // get all CITATION_ spans and order them from start to end
        val spans = spanned.getSpans(0, spanned.length, URLSpan::class.java)
            .filter { it.url.startsWith(CITATION_PREFIX, ignoreCase = true) }
            .sortedBy { spanned.getSpanStart(it) }

        var current = 0

        for (span in spans) {
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)

            // add text before the citation
            if (current < start) {
                builder.append(spanned.subSequence(current, start))
            }

            // parse the citation number
            val num = span.url.removePrefix(CITATION_PREFIX).toIntOrNull() ?: continue
            val tag = "$CITATION_PREFIX$num"

            builder.appendInlineContent(tag, "$num")
            citations += ParsedCitation(num, tag)
            current = end
        }

        // add text after the last citation
        if (current < spanned.length) {
            builder.append(spanned.subSequence(current, spanned.length))
        }

        return ParsedTextResult(
            annotated = builder.toAnnotatedString(),
            citations = citations
        )
    }

    private fun processPostAnswerResult(
        query: String,
        responseTimeMillis: Long,
        postAnswerResponse: APIResult<PostAnswerResponse>,
    ): PostAnswersUIState =
        when (postAnswerResponse) {
            is APIResult.Success -> {
                val data = postAnswerResponse.data
                if (data == null || data.items.isNullOrEmpty()) {
                    logPostAnswerError(
                        "Null or Empty Data",
                        query,
                        responseTimeMillis,
                        postAnswerResponse.getMessage(),
                    )
                    PostAnswersUIState.Error
                } else {
                    Logger.d(TAG, "$POST_ANSWER_RESPONSE_LOG_PREFIX Success")
                    logATPMetrics(query, responseTimeMillis, "")
                    val uiItems: ArrayList<PostAnswerUIItem> = arrayListOf()
                    val citations = data.citations ?: emptyList()
                    postAnswerResponse.data.items?.forEach { postAnswerItemResponse ->
                        when (postAnswerItemResponse) {
                            is PostAnswerText -> {
                                uiItems.add(
                                    PostAnswerUIItem.TextItem(
                                        subtype = TextSubtype.entries.find { it.value == postAnswerItemResponse.subtype },
                                        content = postAnswerItemResponse.content.orEmpty(),
                                        mimeType = MimeType.entries.find { it.value == postAnswerItemResponse.mime },
                                        streamingUrl = postAnswerItemResponse.streamingUrl,
                                        icon = postAnswerItemResponse.icon,
                                        parsed = parseCitations(postAnswerItemResponse.content.orEmpty()),
                                        citations = citations
                                    ),
                                )
                            }

                            is PostAnswerCarousel -> {
                                val carouselItems =
                                    postAnswerItemResponse.items?.map { carouselItem ->
                                        CarouselUIItem(
                                            id = carouselItem.id,
                                            url = carouselItem.url.orEmpty(),
                                            publishDateMillis = carouselItem.publishDate ?: 0,
                                            content = carouselItem.content.orEmpty(),
                                            imageUrl = carouselItem.image.orEmpty(),
                                            passages = carouselItem.passages.orEmpty(),
                                        )
                                    }
                                uiItems.add(
                                    PostAnswerUIItem.Carousel(
                                        subtype = CarouselSubtype.entries.find { it.value == postAnswerItemResponse.subtype },
                                        items = carouselItems.orEmpty(),
                                    ),
                                )
                            }

                            is PostAnswerFeedback -> {
                                if (!postAnswerItemResponse.endpoint.isNullOrEmpty() &&
                                    !postAnswerItemResponse.responseId.isNullOrEmpty()
                                ) {
                                    uiItems.add(
                                        PostAnswerUIItem.Feedback(
                                            endPointUrl = postAnswerItemResponse.endpoint,
                                            responseId = postAnswerItemResponse.responseId,
                                        ),
                                    )
                                }
                            }

                            else -> { /*no-op*/
                            }
                        }
                    }

                    uiItems.let { PostAnswersUIState.Success(uiItems) }
                }
            }

            is APIResult.Failure -> {
                logPostAnswerError(
                    "Failure",
                    query,
                    responseTimeMillis,
                    postAnswerResponse.getMessage(),
                )
                PostAnswersUIState.Error
            }

            is APIResult.NetworkError -> {
                PostAnswersUIState.Error
            }
        }

    private fun logATPMetrics(query: String, responseTimeMillis: Long, entryPoint: String) {
        val responseTimeSeconds = responseTimeMillis / 1000f
        EventLog
            .Builder()
            .apply {
                setModule(LogModules.POST_ANSWERS)
                set(QUERY, query)
                set(RESPONSE_TIME_SECONDS, responseTimeSeconds)
                set(APP_NAME, entryPoint)
            }.run {
                RemoteLog.m(FlagshipApplication.getInstance(), build())
            }
    }


    private fun logPostAnswerError(
        errorType: String,
        query: String,
        responseTimeMillis: Long,
        responseMessage: String,
    ) {
        val responseTimeSeconds = responseTimeMillis / 1000f
        EventLog
            .Builder()
            .apply {
                setModule(LogModules.POST_ANSWERS)
                set(QUERY, query)
                set(RESPONSE_TIME_SECONDS, responseTimeSeconds)
                set(ERROR_TYPE, errorType)
                set(APP_NAME, "")
                setErrorMessage(responseMessage)
            }.run {
                RemoteLog.e(FlagshipApplication.getInstance(), build())
            }
        Logger.e(
            TAG,
            "$POST_ANSWER_RESPONSE_LOG_PREFIX $QUERY=\"$query\" $RESPONSE_TIME_SECONDS=\"$responseTimeSeconds\" $ERROR_TYPE=\"$errorType\" err_msg=\"$responseMessage\"",
        )
    }

    fun initiatePostAnswersFeedback(
        endpoint: String?,
        responseId: String?,
        reaction: PostAnswersFeedbackReaction,
    ) {
        endpoint ?: return
        responseId ?: return
        initiatePostAnswersFeedbackEvent.postValue(Triple(endpoint, responseId, reaction.ordinal))
    }

    private fun List<Document>.toSearchItems(): List<SearchItem> =
        when (_searchMode.value) {
            SearchMode.Regular -> toArticleItems()
            SearchMode.Recipe -> toRecipeItems()
            SearchMode.Election -> toArticleItems()
            null -> toArticleItems()
        }

    private fun List<Document>.toArticleItems(): List<ArticleItem> =
        this.map {
            ArticleItem(
                it.headline ?: "",
                it.byline ?: "",
                if (it.smallthumburl != NONE) it.smallthumburl else null,
                it.contenturl ?: "",
                it.displaydatetime?.toLong(),
            )
        }

    private fun List<Document>.toRecipeItems(): List<RecipeItem> =
        this.map {
            RecipeItem(
                headline = it.headline ?: "",
                imageUrl = if (it.smallthumburl != NONE) it.smallthumburl else null,
                contentUrl = it.contenturl ?: "",
                duration = it.recipeInfo?.totalTime,
                course =
                    it.recipeInfo
                        ?.courses
                        ?.getOrNull(0)
                        ?.description,
                rating = it.rating?.value ?: 0.0,
                reviews = it.rating?.count ?: 0,
            )
        }

    private fun List<Section>.toSectionItems(): List<SectionItem> =
        this.map {
            SectionItem(
                it.name,
                it.url,
                it.type,
                it.path,
            )
        }

    private fun Filters.toFilterMap(): Map<FilterHeaderItem, List<FilterRadioItem>> {
        val map = mutableMapOf<FilterHeaderItem, List<FilterRadioItem>>()
        if (this.authors?.isNotEmpty() == true) {
            this.authors
                .filter { it.name != null }
                .map { FilterRadioItem(it.name!!, it.name, FilterHeaderItem.AUTHORS) }
                .let {
                    val list =
                        mutableListOf(
                            FilterRadioItem(
                                "All authors",
                                null,
                                FilterHeaderItem.AUTHORS,
                            ),
                        )
                    list.addAll(it)
                    map.put(FilterHeaderItem.AUTHORS, list)
                }
        }

        if (this.sections?.isNotEmpty() == true) {
            this.sections
                .filter { it.name != null }
                .map { FilterRadioItem(it.name!!, it.name, FilterHeaderItem.SECTIONS) }
                .let {
                    val list =
                        mutableListOf(
                            FilterRadioItem(
                                "All sections",
                                null,
                                FilterHeaderItem.SECTIONS,
                            ),
                        )
                    list.addAll(it)
                    map.put(FilterHeaderItem.SECTIONS, list)
                }
        }
        return map
    }

    /**
     * Add db source that holds search queries to UI live data
     */
    private fun addUpdateQuerySource() {
        _searchUiState.addSource(searchRepo.updateRecentStatus) { status ->
            when (status) {
                is RecentLocalStatus.Failure -> {
                    Logger.d(TAG, status.message)
                }

                is RecentLocalStatus.FetchNextSuccess -> {
                    if (_searchMode.value == SearchMode.Regular) {
                        _searchUiState.postValue(
                            status.recents
                                .map {
                                    SearchQueryItem(it.text)
                                }.let {
                                    SearchUiState.Landing(it)
                                },
                        )
                    }
                }

                else -> {}
            }
        }
    }

    fun setSearchMode(searchMode: SearchMode) {
        _searchMode.value = searchMode
        if (_searchMode.value == SearchMode.Recipe &&
            searchQuery?.hasRecipeQueryOrFilters() == true ||
            // If in Recipe Mode and either query is NOT empty or filters are set, search for recipes
            _searchMode.value == SearchMode.Regular &&
            searchQuery?.query?.isNotEmpty() == true ||
            // If in regular search and search query is not empty, perform normal search
            _searchMode.value == SearchMode.Election &&
            searchQuery?.query?.isNotEmpty() == true
        ) {
            searchQuery?.let {
                searchFor(it)
            }
        } else {
            showLandingPage()
        }

        if (searchMode == SearchMode.Recipe) {
            Measurement.trackSearchRecipePageView(
                Measurement.PAGE_RECIPE_FINDER_LANDING,
                recipeNavigationBehavior,
            )
            if (recipeNavigationBehavior == Measurement.RECIPE_FINDER_INLINE_SEARCH_NAVIGATION) {
                resetRecipeNavigationBehavior() // reset after page_view in Inline case
            }
        } else {
            resetRecipeNavigationBehavior() // reset when leaving Recipe mode in all cases
        }
    }

    private fun resetRecipeNavigationBehavior() {
        recipeNavigationBehavior = Measurement.RECIPE_FINDER_NAVIGATION
    }

    fun onRecipeModeClicked() {
        when (_searchMode.value) {
            SearchMode.Regular -> setSearchMode(SearchMode.Recipe)
            SearchMode.Recipe -> setSearchMode(SearchMode.Regular)
            null -> {
                // noop
            }

            else -> {}
        }
    }

    companion object {
        private const val NONE = "None"
        private val TAG = SearchViewModel::class.simpleName
        private const val POST_ANSWER_RESPONSE_LOG_PREFIX = "PostAnswerResponse:"
        private const val ERROR_TYPE = "error_type"
        private const val RESPONSE_TIME_SECONDS = "response_time_seconds"
        private const val QUERY = "query"
        private const val APP_NAME = "app_name"
        const val CITATION_PREFIX = "CITATION_"
    }
}

enum class SearchMode {
    Regular,
    Recipe,
    Election,
}

enum class PostAnswersFeedbackReaction(
    val stringId: Int,
    val iconId: Int,
) {
    YES(com.wapo.flagship.features.aixp.R.string.feedback_yes, R.drawable.ic_thumbs_up_unfilled_16),
    NO(com.wapo.flagship.features.aixp.R.string.feedback_no, R.drawable.ic_thumbs_down_unfilled_16),
    SHARE(com.wapo.flagship.features.aixp.R.string.feedback_share, R.drawable.ic_atp_forward)
}
