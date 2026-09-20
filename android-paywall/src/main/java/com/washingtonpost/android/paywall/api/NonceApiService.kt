package com.washingtonpost.android.paywall.api

import com.washingtonpost.android.paywall.models.NonceResponse
import com.washingtonpost.android.paywall.network.retrofit.APIResult
import com.washingtonpost.android.paywall.util.PaywallConstants
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

interface NonceApiService {
    @Headers("Content-Type: application/x-www-form-urlencoded")
    @FormUrlEncoded
    @POST
    suspend fun fetchNonce(
        @Url url: String,
        @Header(PaywallConstants.AUTHORIZATION_HEADER) accessToken: String,
        @Field("client_id") clientId: String,
    ): APIResult<NonceResponse>
}