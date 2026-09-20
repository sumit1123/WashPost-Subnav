/* Copyright (c) 2025 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.aixp.services

import com.wapo.flagship.features.aixp.models.ArticleSummaryFeedbackRequest
import com.wapo.flagship.features.aixp.models.ArticleSummaryFeedbackResponse
import com.wapo.flagship.features.aixp.network.APIResult
import com.wapo.flagship.features.aixp.repo.ArticleSummaryRepository
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

interface ArticleFeedbackService {

    @Headers("Accept: application/json")
    @POST
    suspend fun submitFeedback(
        @Url endpoint: String,
        @Header(ArticleSummaryRepository.WP_TIMEOUT) timeoutMs: Int,
        @Body request: ArticleSummaryFeedbackRequest
    ): APIResult<ArticleSummaryFeedbackResponse>
}