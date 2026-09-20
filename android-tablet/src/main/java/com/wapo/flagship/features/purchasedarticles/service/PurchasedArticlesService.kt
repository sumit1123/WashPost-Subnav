package com.wapo.flagship.features.purchasedarticles.service

import com.wapo.flagship.network.retrofit.network.APIResult
import com.wapo.flagship.features.purchasedarticles.model.PurchasedArticleResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.Query

interface PurchasedArticlesService {
    @GET("fulfillment/v1/purchased/articles/list")
     suspend fun getPurchasedArticles(
        @HeaderMap headerMap: HashMap<String, String>,
        @Query("allArticles") allArticles: String,
        @Query("expired") expired: String,
        @Query("page") page: String,
        @Query("pageSize") pageSize: String,
    ): APIResult<PurchasedArticleResponse>
}