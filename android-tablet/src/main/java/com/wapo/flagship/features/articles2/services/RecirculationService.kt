package com.wapo.flagship.features.articles2.services

import com.wapo.flagship.features.articles2.models.recirculation.ArticleCollectionsRequestBody
import com.wapo.flagship.features.articles2.models.recirculation.AutoRecircResponse
import com.wapo.flagship.network.retrofit.network.APIResult
import retrofit2.http.Body
import retrofit2.http.POST

interface RecirculationService {
    @POST("articles/")
    suspend fun getAutoRecirculationArticles(
        @Body param: ArticleCollectionsRequestBody,
    ): APIResult<AutoRecircResponse>
}