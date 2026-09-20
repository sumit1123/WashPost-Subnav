package com.washingtonpost.foryou.remote


import com.washingtonpost.foryou.data.ForYouRequestBody
import com.washingtonpost.foryou.data.ForYouResponse
import com.washingtonpost.foryou.data.HabitTilesRequestBody
import com.washingtonpost.foryou.data.HabitTilesResponse
import com.washingtonpost.foryou.network.APIResult
import com.washingtonpost.foryou.repo.ForYouFeedRepositoryImpl.Companion.CLIENT_ID
import com.washingtonpost.foryou.repo.ForYouFeedRepositoryImpl.Companion.WP_TIMEOUT
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Interface for accessing for you feed
 */
interface ForYouService {
    @POST("recommendations/v1/medley/")
    suspend fun getFeed(
        @Header(WP_TIMEOUT) timeoutMs: Int,
        @Header(CLIENT_ID) clientId: String?,
        @Body param: ForYouRequestBody,
    ) : APIResult<ForYouResponse>

    @POST("recommendations/v1/habittiles/")
    suspend fun getHabitTiles(
        @Header(WP_TIMEOUT) timeoutMs: Int,
        @Header(CLIENT_ID) clientId: String?,
        @Body param: HabitTilesRequestBody
    ) : APIResult<HabitTilesResponse>
}

