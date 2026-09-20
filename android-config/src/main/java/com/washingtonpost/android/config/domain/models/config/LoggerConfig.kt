package com.washingtonpost.android.config.domain.models.config

data class LoggerConfig(
    val isRemoteLoggingActive: Boolean,
    val isErrorLoggingActive: Boolean,
    val isPaywallLoggingActive: Boolean,
    val isDebugLoggingActive: Boolean,
    val isVerboseLoggingActive: Boolean,
    val isMetricsLoggingActive: Boolean,
    val isMetricsSamplingActive: Boolean,
    val metricsSamplingRate: Int,
    val appName: String,
    val fileSizeThreshold: Int,
    val splunkHttpURL: String,
    val metricsSamplingSegment: Int,
    val filesDirectory: String,
    val archivesDirectory: String,
    val canStoreRemoteLogs: Boolean,
) {
    fun isSampledForMetrics(): Boolean {
        if (isMetricsSamplingActive) {
            return metricsSamplingSegment < metricsSamplingRate
        }
        return true
    }

    /**
     * Checks to see whether or not remote logging active
     */
    fun isLoggable(): Boolean {
        if (!isRemoteLoggingActive || !canStoreRemoteLogs) {
            return false
        }
        return true
    }
}