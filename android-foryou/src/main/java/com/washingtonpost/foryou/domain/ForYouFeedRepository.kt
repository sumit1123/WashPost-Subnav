package com.washingtonpost.foryou.domain

import com.washingtonpost.foryou.data.ForYouContentType
import com.washingtonpost.foryou.data.ForYouResponse
import com.washingtonpost.foryou.data.RecommendationsItem
import com.washingtonpost.foryou.network.APIResult
import com.washingtonpost.foryou.repo.ForYouFeedRepositoryImpl.Companion.SURFACE_FEED
import com.washingtonpost.foryou.repo.ForYouMetaData

interface ForYouFeedRepository {

    suspend fun getForYouRecommendations(
        skipReadingFromCache: Boolean = false,
        excludeList: List<String> = emptyList(),
        surface: String,
        limit: Int? = null,
        currentUrl: String? = null,
        contentType: List<String>? = listOf(ForYouContentType.ARTICLE.type)
    ): APIResult<ForYouResponse>

    suspend fun refresh(): APIResult<ForYouResponse>

    suspend fun getMoreRecommendations(
        surface: String = SURFACE_FEED,
        limit: Int? = null,
        currentUrl: String? = null,
        contentType: List<String>? = listOf(ForYouContentType.ARTICLE.type)
    ): APIResult<ForYouResponse>

    suspend fun getCache(surface: String): APIResult<ForYouResponse>

    fun maxLimit(): Int

    fun getCachedArticlesPage(page: Int = 0): List<RecommendationsItem>

    suspend fun makeWidgetCall(forYouMeta: ForYouMetaData? = null)
}
