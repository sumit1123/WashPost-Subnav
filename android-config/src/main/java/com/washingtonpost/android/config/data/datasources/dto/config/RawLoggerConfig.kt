package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.android.commons.extensions.toUri
import com.washingtonpost.android.config.domain.models.StoreType
import com.washingtonpost.android.config.domain.models.config.LoggerConfig
import com.washingtonpost.android.config.data.datasources.utils.MapConfigParams

@JsonClass(generateAdapter = true)
data class RawLoggerConfig(
    @Json(name = "active") val isRemoteLoggingActive: Boolean? = null,
    @Json(name = "errorActive") val isErrorLoggingActive: Boolean? = null,
    @Json(name = "paywallActive") val isPaywallLoggingActive: Boolean? = null,
    @Json(name = "debugActive") val isDebugLoggingActive: Boolean? = null,
    @Json(name = "verboseActive") val isVerboseLoggingActive: Boolean? = null,
    @Json(name = "metricsActive") val isMetricsLoggingActive: Boolean? = null,
    @Json(name = "metricsSamplingActive") val isMetricsSamplingActive: Boolean? = null,
    @Json(name = "metricsSamplingPercentageRate") val metricsSamplingRate: Int? = null,
    @Json(name = "appName") val appName: String? = null,
    @Json(name = "fileSizeThreshold") val fileSizeThreshold: Int? = null,
    @Json(name = "splunkHttpURL") val splunkHttpURL: String? = null,
) {
    fun mapToDomain(params: MapConfigParams): LoggerConfig {
        val storeType = params.configProvider.storeType
        val isDebug = params.configProvider.isDebugBuild
        val source = when (storeType) {
            StoreType.AMAZON -> if (isDebug) "rainbow_android_dev" else "rainbow_android_prod"
            else -> if (isDebug) "classic_android_dev" else "classic_android_prod"
        }
        val newSplunkHttpUrl = splunkHttpURL?.toUri()?.let { uri ->
            uri.buildUpon()
                .clearQuery()
                .apply {
                    uri.queryParameterNames.forEach { key ->
                        val value = if (key.equals("source") || key == "sourcetype") {
                            source
                        } else {
                            uri.getQueryParameter(key)
                        }
                        appendQueryParameter(key, value)
                    }
                }
                .toString()
        }


        val isMetricsSamplingActive = when (storeType) {
            StoreType.AMAZON -> !isDebug
            else -> isMetricsSamplingActive ?: false
        }
        val metricsSamplingRate = when (storeType) {
            StoreType.AMAZON -> if (isDebug) 100 else 10
            else -> metricsSamplingRate ?: 100
        }

        val metricsSamplingSegment = params.deviceSerialId.let {
            (if (it.hashCode() < 0) it.hashCode() * -1 else it.hashCode()) % 99
        }

        return LoggerConfig(
            isRemoteLoggingActive = isRemoteLoggingActive ?: false,
            isErrorLoggingActive = isErrorLoggingActive ?: true,
            isPaywallLoggingActive = isPaywallLoggingActive ?: true,
            isDebugLoggingActive = isDebugLoggingActive ?: true,
            isVerboseLoggingActive = isVerboseLoggingActive ?: true,
            isMetricsLoggingActive = isMetricsLoggingActive ?: true,
            isMetricsSamplingActive = isMetricsSamplingActive,
            metricsSamplingRate = metricsSamplingRate,
            appName = appName ?: "WashPost",
            fileSizeThreshold = fileSizeThreshold ?: 20000,
            splunkHttpURL = newSplunkHttpUrl.orEmpty(),
            metricsSamplingSegment = metricsSamplingSegment,
            filesDirectory = params.filesDirectoryAbsolutePath,
            archivesDirectory = params.archiveDirectory,
            canStoreRemoteLogs = params.canStoreRemoteLogs,
        )
    }
}