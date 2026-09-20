package com.wapo.flagship.features.video.service

import com.wapo.flagship.features.wpvideos.data.WatchVideosApi
import com.wapo.flagship.network.retrofit.network.APIResult
import retrofit2.http.GET
import retrofit2.http.Query

interface VerticalVideoService {

    @GET(".")
    suspend fun getNextVideo(
        @Query("offset") offset: Int,
        @Query("size") size: Int,
    ): APIResult<WatchVideosApi>
}