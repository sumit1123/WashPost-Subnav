/* Copyright (c) 2024 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.aixp.services

import com.wapo.flagship.features.aixp.models.PostAnswersFeedbackRequest
import com.wapo.flagship.features.aixp.models.PostAnswersFeedbackResponse
import com.wapo.flagship.features.aixp.network.APIResult
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

interface PostAnswersFeedbackService {
    @Headers("Accept: application/json")
    @POST
    suspend fun submitFeedback(
        @Url endpoint: String,
        @Body request: PostAnswersFeedbackRequest
    ): APIResult<PostAnswersFeedbackResponse>
}
