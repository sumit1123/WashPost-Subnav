package com.wapo.flagship.di.app.modules.features.readinghistory

import com.washingtonpost.userhistory.network.APIResult
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.Query

interface ReadingHistoryService {
    @GET("foryouapi/v3/history/get")
    suspend fun getReadingHistory(
        @HeaderMap headers: HashMap<String, String>,
        @Query("content_type") contentTypes: String
    ): APIResult<ReadingHistoryServiceGetResponse>
}