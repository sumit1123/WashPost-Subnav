// Copyright (c) 2025 The Washington Post. All rights reserved.
package com.wapo.flagship.features.ads.targeting.repo

import android.content.Context
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.Logger
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.features.ads.targeting.models.ContentResponse
import com.wapo.flagship.features.ads.targeting.services.ContentService
import com.wapo.flagship.features.aixp.BuildConfig
import com.wapo.flagship.features.aixp.network.APIResult
import com.washingtonpost.android.config.domain.models.config.ContextualTargetingContent
import javax.inject.Inject

private const val TAG = "ContentRepository"

class ContentRepository @Inject constructor(
    private val context: Context,
    private val service: ContentService,
    private val config: ContextualTargetingContent?,
) {
    private val timeout = if (BuildConfig.DEBUG) 5_000 else 2_500

    suspend fun getContent(id: String): APIResult<ContentResponse> {
        if (config?.enabled == false)
            return APIResult.Failure(-1, "Content feature is disabled")
        val url = config?.url
            ?: return APIResult.Failure(-1, "Config url is empty")

        val sendResponseAtMillis = System.currentTimeMillis()
        val apiResult = service.getContent(endpoint = url, timeoutMs = timeout, id = id)
        val responseTimeMillis = System.currentTimeMillis() - sendResponseAtMillis

        Logger.d(TAG, "CTContent: Result $apiResult")

        when (apiResult) {
            is APIResult.Success -> {
                remoteLog(
                    "CTContent Success",
                    null,
                    responseTimeMillis,
                    id,
                    (apiResult.data?.items?.firstOrNull()?.adCall as? Map<*, *>)?.size ?: -1
                )
            }

            is APIResult.Failure -> {
                remoteLog(
                    "CTContent Failure",
                    apiResult.rawResponse ?: "Failure",
                    responseTimeMillis,
                    id
                )
                return APIResult.Failure(apiResult.statusCode, apiResult.getMessage())
            }

            is APIResult.NetworkError -> {
                return APIResult.NetworkError(apiResult.error)
            }
        }

        return apiResult
    }

    private fun remoteLog(
        message: String,
        errorMessage: String?,
        responseTimeMs: Long?,
        id: String,
        dataSize: Int? = null
    ) {
        EventLog.Builder().apply {
            setMessage(message)
            setModule(LogModules.VIDEO_ADS)
            set("response_time", responseTimeMs)
            set("video_id", id)
            setForceUpload()
        }.run {
            errorMessage?.let {
                setErrorMessage(errorMessage)
            }
            dataSize?.let {
                set("data_size", dataSize)
            }
            RemoteLog.e(context, build())
        }
    }
}