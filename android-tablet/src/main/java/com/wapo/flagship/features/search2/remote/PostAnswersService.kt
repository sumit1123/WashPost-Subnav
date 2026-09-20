/* Copyright (c) 2024 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.search2.remote

import com.wapo.flagship.features.search2.model.PostAnswerRequest
import com.wapo.flagship.features.search2.model.PostAnswerResponse
import com.wapo.flagship.network.retrofit.network.APIResult
import com.wapo.flagship.util.network.TimeoutInterceptor
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface PostAnswersService {
    @Headers("Content-Type: application/json")
    @POST("post-answers.json")
    suspend fun getPostAnswer(
        @Header(TimeoutInterceptor.WP_TIMEOUT) timeoutMs: Int,
        @Body requestBody: PostAnswerRequest
    ): APIResult<PostAnswerResponse>
}
