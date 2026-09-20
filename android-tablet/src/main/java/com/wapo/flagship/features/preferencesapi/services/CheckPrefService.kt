/* Copyright (c) 2024 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.preferencesapi.services

import com.wapo.flagship.features.preferencesapi.models.CheckPrefResponse
import com.wapo.flagship.network.retrofit.network.APIResult
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.Path

interface CheckPrefService {
    @GET("user/{userId}")
    suspend fun checkPref(
        @HeaderMap headers: HashMap<String, String>,
        @Path("userId") userId: String
    ): APIResult<CheckPrefResponse>
}
