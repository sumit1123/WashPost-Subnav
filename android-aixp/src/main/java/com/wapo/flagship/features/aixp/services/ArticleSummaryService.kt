/* Copyright (c) 2025 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.aixp.services

import com.wapo.flagship.features.aixp.models.ArticleRealtimeSummary
import com.wapo.flagship.features.aixp.network.APIResult
import com.wapo.flagship.features.aixp.repo.ArticleSummaryRepository
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Query
import retrofit2.http.Url

interface ArticleSummaryService {

    @Headers("Accept: application/json")
    @GET
    suspend fun getArticleSummary(
        @Url urlEndpoint: String,
        @Header(ArticleSummaryRepository.WP_TIMEOUT) timeoutMs: Int,
        @Query("content_id") contentId: String,
        @Query("url") url: String,
        @Query("platform") platform: String = "app",
    ): APIResult<ArticleRealtimeSummary>
}