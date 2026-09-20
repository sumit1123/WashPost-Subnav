package com.wapo.kmpshared.logger.domain.service

import com.wapo.kmpshared.logger.domain.model.LogLevel
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ptr
import platform.darwin.OS_LOG_DEFAULT
import platform.darwin.OS_LOG_TYPE_DEBUG
import platform.darwin.OS_LOG_TYPE_DEFAULT
import platform.darwin.OS_LOG_TYPE_ERROR
import platform.darwin.OS_LOG_TYPE_INFO
import platform.darwin.__dso_handle
import platform.darwin._os_log_internal
import platform.darwin.os_log_type_t

private class IOSConsoleWriter : LogConsoleWriter {
    @OptIn(ExperimentalForeignApi::class)
    override fun log(
        severity: LogLevel,
        tag: String,
        message: String,
    ) {
        val type = severity.toOSLogType()

        // replication of Kermit's underlying implementation
        _os_log_internal(
            __dso_handle.ptr,
            OS_LOG_DEFAULT,
            type,
            "%s",
            message,
        )
    }

    private fun LogLevel.toOSLogType(): os_log_type_t =
        when (this) {
            LogLevel.Verbose, LogLevel.Debug -> OS_LOG_TYPE_DEBUG
            LogLevel.Info -> OS_LOG_TYPE_INFO
            LogLevel.Warn -> OS_LOG_TYPE_DEFAULT
            LogLevel.Error -> OS_LOG_TYPE_ERROR
        }
}

actual fun logConsoleWriter(): LogConsoleWriter = IOSConsoleWriter()
