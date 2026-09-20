package com.wapo.flagship.features.articles2.interfaces

import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.articles2.models.recirculation.AutoRecircResponse
import com.wapo.flagship.network.retrofit.network.APIResult

interface ArticleRecirculationRepository {
    suspend fun getAutoRecirculation(
        article: Article2,
    ): APIResult<AutoRecircResponse>
}