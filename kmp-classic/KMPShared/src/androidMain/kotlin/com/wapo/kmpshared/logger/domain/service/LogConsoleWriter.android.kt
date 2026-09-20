package com.wapo.kmpshared.logger.domain.service

import android.util.Log
import com.wapo.kmpshared.logger.domain.model.LogLevel

private class AndroidConsoleWriter : LogConsoleWriter {
    override fun log(
        severity: LogLevel,
        tag: String,
        message: String,
    ) {
        when (severity) {
            LogLevel.Verbose -> Log.v(TAG, message)
            LogLevel.Debug -> Log.d(TAG, message)
            LogLevel.Info -> Log.i(TAG, message)
            LogLevel.Warn -> Log.w(TAG, message)
            LogLevel.Error -> Log.e(TAG, message)
        }
    }

    companion object {
        private const val TAG = "LogConsoleWriter"
    }
}

actual fun logConsoleWriter(): LogConsoleWriter = AndroidConsoleWriter()
