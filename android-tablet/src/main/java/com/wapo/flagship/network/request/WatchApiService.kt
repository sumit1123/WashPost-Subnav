package com.wapo.flagship.network.request


import com.wapo.flagship.features.wpvideos.data.WatchVideosApi
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface WatchApiService {
    @GET
    fun getWpVideos(
        @Url url: String,
        @Query("offset") offset: Int,
        @Query("size") size: Int
    ): Call<WatchVideosApi>
}