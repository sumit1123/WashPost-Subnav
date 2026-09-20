package com.wapo.flagship.features.wpvideos.repo

import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.Utils.isConnectedOrConnecting
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.features.wpvideos.data.WatchVideosApi
import com.wapo.flagship.network.request.WatchApiService
import com.washingtonpost.android.config.domain.models.config.WPVideosConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WatchVideosRepository @Inject constructor(
    private val watchApiService: WatchApiService,
    private val wpVideosConfig: WPVideosConfig
) {
    suspend fun getWpVideosFeed(offset: Int): WatchVideosApi = withContext(Dispatchers.IO) {
        try {
            val response = watchApiService.getWpVideos(
                wpVideosConfig.baseUrl,
                offset,
                wpVideosConfig.loadCount
            ).execute()

            if (response.isSuccessful) {
                response.body() ?: throw Exception("Response body is null for URL: $wpVideosConfig.baseUrl")
            } else {
                throw Exception("API call failed with code ${response.code()} and message: ${response.message()}")
            }
        } catch (e: Exception) {
            // log non n/w errors
            val isNetworkError = !AppContextUtils.isConnectingOrConnected()
            // Log non-network exceptions
            val errorMessage = e.message ?: "Unknown error"
            if (!isNetworkError) {
                val eventLog = EventLog.Builder()
                    .setMessage("WP VIDEOS FEED FAILURE")
                    .set("url", wpVideosConfig.baseUrl)
                    .set("exception", e.javaClass.simpleName)
                    .set("error_message", errorMessage)
                    .setModule(LogModules.WATCH_VIDEO)
                    .build()

                RemoteLog.e(AppContextUtils.appContext, eventLog)
            }

            throw e
        }
    }
}
