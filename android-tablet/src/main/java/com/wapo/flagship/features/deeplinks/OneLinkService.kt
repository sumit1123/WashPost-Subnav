package com.wapo.flagship.features.deeplinks

import com.wapo.flagship.features.deeplinks.models.OneLinkResponse
import com.washingtonpost.userhistory.network.APIResult
import retrofit2.http.GET
import retrofit2.http.Query

interface OneLinkService {
    @GET("create/link")
    suspend fun generateOneLink(
        @Query("payload") body: String,
    ): APIResult<OneLinkResponse>
}