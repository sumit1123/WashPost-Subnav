package com.wapo.flagship.features.video.repo

import android.content.Context
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.features.video.service.VerticalVideoService
import com.wapo.flagship.features.wpvideos.data.WatchVideosApi
import com.wapo.flagship.network.retrofit.network.APIResult
import com.washingtonpost.android.config.domain.models.config.NextVideoConfig
import javax.inject.Inject

class VerticalVideoRepository @Inject constructor(
    val context: Context,
    private val service: VerticalVideoService,
    private val nextVideoConfig: NextVideoConfig,
) {

    suspend fun getNextVideo(offset: Int): WatchVideosApi? {
        val response = service.getNextVideo(offset = offset, size = nextVideoConfig.loadCount)

        return when (response) {
            is APIResult.Failure -> {
                EventLog.Builder().apply {
                    setMessage("Next video failed")
                    setModule(LogModules.NEXT_VIDEO)
                    setErrorMessage(response.getMessage())
                }.run {
                    RemoteLog.e(context, build())
                }
                null
            }

            is APIResult.NetworkError -> {
                null
            }

            is APIResult.Success -> {
                response.data
            }
        }
    }

}