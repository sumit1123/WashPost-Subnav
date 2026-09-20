// Copyright (c) 2025 The Washington Post. All rights reserved.
package com.wapo.flagship.features.ads.targeting.services

import com.wapo.flagship.features.ads.targeting.models.ContentResponse
import com.wapo.flagship.features.aixp.network.APIResult
import com.wapo.flagship.util.network.TimeoutInterceptor
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Interface for accessing Content response
 */
interface ContentService {
    @Headers("X-Client-Video: true")
    @GET
    suspend fun getContent(
        @Url endpoint: String,
        @Header(TimeoutInterceptor.WP_TIMEOUT) timeoutMs: Int,
        @Query("id") id: String,
    ): APIResult<ContentResponse>
}
