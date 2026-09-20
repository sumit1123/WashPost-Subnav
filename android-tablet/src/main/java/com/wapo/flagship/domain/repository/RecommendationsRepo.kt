package com.wapo.flagship.domain.repository

import com.wapo.flagship.data.model.RecommendationsRepoResult

interface RecommendationsRepo {

    suspend fun fetchData(
        surface: String,
        fetchFromCache: Boolean,
        limit: Int? = null,
        currentUrl: String? = null,
        getMore: Boolean = false
    ): RecommendationsRepoResult
}
