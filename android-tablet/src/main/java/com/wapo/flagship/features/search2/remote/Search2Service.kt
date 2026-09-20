package com.wapo.flagship.features.search2.remote

import com.wapo.flagship.features.search2.model.SearchResultResponse
import com.wapo.flagship.network.retrofit.network.APIResult
import retrofit2.http.GET
import retrofit2.http.QueryMap

interface Search2Service {
    @GET("native-search.json")
    suspend fun searchFor(
        @QueryMap(encoded = true) queryParamsMap: Map<String, String>,
    ): APIResult<SearchResultResponse>
}
