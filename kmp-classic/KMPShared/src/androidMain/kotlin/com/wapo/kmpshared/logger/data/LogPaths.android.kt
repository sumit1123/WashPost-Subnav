package com.wapo.kmpshared.logger.data

import android.content.Context
import kotlinx.io.files.Path

object KMPLoggerRuntime {
    lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val isInitialized: Boolean
        get() = ::appContext.isInitialized
}

internal actual fun platformLogRoot(): Path {
    if (!KMPLoggerRuntime.isInitialized) {
        error("Logger Path initialized via init(Context): Path not initialized")
    }
    return Path(
        KMPLoggerRuntime.appContext
            .cacheDir
            .resolve("wp-remote-logger")
            .absolutePath,
    )
}
