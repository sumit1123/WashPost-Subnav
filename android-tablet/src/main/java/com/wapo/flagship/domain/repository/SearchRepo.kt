package com.wapo.flagship.domain.repository

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.wapo.android.commons.util.LiveEvent
import com.wapo.flagship.features.aixp.models.ArticleSource
import com.wapo.flagship.features.search2.events.SseEvent
import com.wapo.flagship.features.search2.model.PostAnswerResponse
import com.wapo.flagship.features.search2.model.QueryFilter
import com.wapo.flagship.features.search2.model.RecentLocalStatus
import com.wapo.flagship.features.search2.model.Results
import com.wapo.flagship.features.search2.model.SearchQuery
import com.wapo.flagship.features.search2.model.SearchResultApiStatus
import com.wapo.flagship.network.retrofit.network.APIResult
import kotlinx.coroutines.flow.SharedFlow

interface SearchRepo {

    val sseEventState: SharedFlow<SseEvent?>

    val searchQueryList: MutableList<SearchQuery>

    val requestSearchResultsStatus: LiveEvent<SearchResultApiStatus>

    val updateRecentStatus: LiveEvent<RecentLocalStatus>


    fun searchFor(queryFilter: QueryFilter)
    fun searchRecipe(queryFilter: QueryFilter)

    suspend fun getPostAnswer(
        query: String,
        results: Results?,
        appName: String? = null,
    ): APIResult<PostAnswerResponse>

    suspend fun loadMoreSearches(
        queryFilter: QueryFilter,
        isRecipe: Boolean = false,
    )

    fun parseArticleSources(jsonString: String): List<ArticleSource> {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val listType = Types.newParameterizedType(List::class.java, ArticleSource::class.java)

        val adapter = moshi.adapter<List<ArticleSource>>(listType)

        return adapter.fromJson(jsonString) ?: emptyList()
    }

    fun startLiveConversation(
        query: String,
        private: Boolean = false,
        voiceResponse: String? = null,
        timestamp: Float? = null,
        transcriptUrl: String? = null,
        entryPoint: String? = null
    )

    fun observeSseEvents()

    fun continueLiveConversation(
        conversationId: String?,
        query: String,
        private: Boolean = false,
        voiceResponse: String? = null,
        entryPoint: String? = null,
        shareId: String? = null
    )

    suspend fun endLiveConversation()

    suspend fun startAppSearchSession()

    suspend fun addSearched(searchQuery: SearchQuery)

    suspend fun searchElection(queryFilter: QueryFilter)

    suspend fun getRecentSearches(query: String = "")

    fun resetRecentSearchResults()

    suspend fun removeRecentSearch(id: String)

    suspend fun removeAllRecentSearch()

}