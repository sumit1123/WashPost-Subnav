package com.wapo.flagship.data.model

import com.washingtonpost.foryou.data.ForYouResponse
import com.washingtonpost.foryou.data.RecommendationsItem

sealed class RecommendationsRepoResult {

    data class FeedData(
        val response: ForYouResponse? = null
    ): RecommendationsRepoResult()

    data class MapData(
        val item: RecommendationsItem? = null,
        val getMore: Boolean = false
    ): RecommendationsRepoResult()

    data class Error(
        val message: String
    ): RecommendationsRepoResult()
}
