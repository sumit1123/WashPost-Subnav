package com.wapo.flagship.features.articles2.services

import com.wapo.flagship.features.articles2.models.DisclaimerInfo
import com.wapo.flagship.network.retrofit.network.APIResult
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Url

interface DisclaimerInfoService {
    @Headers("Accept: application/json")
    @GET
    suspend fun getDisclaimerInfo(
        @Url urlEndpoint: String,
    ): APIResult<DisclaimerInfo>
}