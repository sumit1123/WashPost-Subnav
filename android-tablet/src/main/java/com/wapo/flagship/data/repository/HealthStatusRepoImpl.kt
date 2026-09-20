// Copyright (c) 2026 The Washington Post. All rights reserved.
package com.wapo.flagship.data.repository

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.retrofit.DefaultHeadersInterceptor.Companion.headers
import com.wapo.android.commons.util.Logger
import com.wapo.android.domain.repository.RemoteLogRepo
import com.wapo.flagship.domain.repository.HealthStatusRepo
import com.wapo.flagship.features.backendhealth.models.BackendHealthStatus
import com.wapo.flagship.features.backendhealth.models.FailoverApiResponse
import kotlinx.coroutines.flow.first
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import javax.inject.Inject

class HealthStatusRepoImpl @Inject constructor(
    private val remoteLogRepo: RemoteLogRepo,
) : HealthStatusRepo {

    override suspend fun fetchHealthStatus(
        healthMonitorURL: String?,
        fallbackURL: String?,
        fallbackStaticURL: String?
    ): BackendHealthStatus {

        var httpConnection: HttpURLConnection? = null
        var isHealthy = true
        var finalFallbackURL = fallbackURL
        var finalFallbackStaticURL: String? = fallbackStaticURL
        try {
            if (!healthMonitorURL.isNullOrEmpty()) {
                val url = URL(healthMonitorURL)
                httpConnection =
                    (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = HTTP_TIMEOUT
                        readTimeout = HTTP_TIMEOUT
                        for (header in headers) {
                            setRequestProperty(header.key, header.value)
                        }
                        connect()

                        val responseCodeValue = responseCode

                        Logger.d(TAG, "checkBackendHealth, response=$responseCodeValue")
                        if (responseCodeValue == FALLBACK_RESPONSE_CODE) {
                            finalFallbackURL = getCustomHeaderField(
                                FALLBACK_URL_HEADER,
                                fallbackURL
                            )
                            finalFallbackStaticURL =
                                getFailoverApiResponse()?.endpoint ?: fallbackStaticURL
                            isHealthy = false
                        }
                    }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "health checker exception ", e)
            remoteLogRepo.e(
                EventLog
                    .Builder()
                    .setMessage("Failed to check for TopStories failover")
                    .setModule(LogModules.BACKEND_HEALTH)
                    .setErrorMessage(e.message)
                    .build()
            )
        } finally {
            try {
                httpConnection?.disconnect()
            } catch (e: Exception) {
                Logger.e("BHealthCheckerImpl", "health checker exception disconnect ", e)
            }
        }

        return BackendHealthStatus(
            isHealthy,
            finalFallbackURL,
            finalFallbackStaticURL,
            isLoading = false,
        )
    }

    private fun HttpURLConnection.getCustomHeaderField(
        key: String,
        fallback: String? = null
    ): String? {
        val value = getHeaderField(key)
        if (!value.isNullOrEmpty()) return value

        val valueLower = getHeaderField(key.lowercase(Locale.getDefault()))
        if (!valueLower.isNullOrEmpty()) return valueLower

        return fallback
    }

    private fun HttpURLConnection.getFailoverApiResponse(): FailoverApiResponse? {
        return try {
            val json = inputStream.use { it.readBytes() }.toString(Charsets.UTF_8)
            Gson().fromJson(json, FailoverApiResponse::class.java)
        } catch (e: JsonSyntaxException) {
            Logger.e(TAG, "Error parsing failover response: ${e.message}", e)
            null
        }
    }

    override suspend fun isFailoverActive(): Boolean {
        return com.wapo.flagship.util.isFailoverActive.first()
    }

    override suspend fun setFailoverActive(value: Boolean) {
        com.wapo.flagship.util.setFailoverActive(value)
    }

    companion object {
        private const val TAG = "BackendHealth"
        private const val FALLBACK_RESPONSE_CODE = 205
        private const val HTTP_TIMEOUT = 2500
        private const val FALLBACK_URL_HEADER = "X-Failover-Location"
    }
}
