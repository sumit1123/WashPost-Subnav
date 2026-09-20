package com.wapo.android.commons.util

import android.util.Log
import com.wapo.android.commons.logger.BuildConfig

/**
 * Utility class for logging messages across the application.
 * It checks if logging is enabled based on the build configuration and a forceDebug flag.
 */
object Logger {

    private val DEBUG = BuildConfig.DEBUG
    var forceDebug: Boolean = false

    private val isLoggable: Boolean
        get() = DEBUG || forceDebug

    /**
     * Log a verbose message
     */
    @JvmStatic
    fun v(tag: String?, msg: String) {
        if (isLoggable) {
            Log.v(tag, msg)
        }
    }

    /**
     * Log a debug message
     */
    @JvmStatic
    fun d(tag: String?, msg: String) {
        if (isLoggable) {
            Log.d(tag, msg)
        }
    }

    /**
     * Log a debug message with an exception
     */
    @JvmStatic
    fun d(tag: String?, msg: String?, ex: Throwable?) {
        if (isLoggable) {
            Log.d(tag, msg, ex)
        }
    }

    /**
     * Log a info message
     */
    @JvmStatic
    fun i(tag: String?, msg: String) {
        if (isLoggable) {
            Log.i(tag, msg)
        }
    }

    /**
     * Log a warning message
     */
    @JvmStatic
    fun w(tag: String?, msg: String) {
        if (isLoggable) {
            Log.w(tag, msg)
        }
    }

    /**
     * Log a warning message with an exception
     */
    @JvmStatic
    fun w(tag: String?, msg: String?, ex: Throwable?) {
        if (isLoggable) {
            Log.w(tag, msg, ex)
        }
    }

    /**
     * Log what a terrible failure message
     */
    @JvmStatic
    fun wtf(tag: String?, msg: String) {
        if (isLoggable) {
            Log.wtf(tag, msg)
        }
    }

    /**
     * Log what a terrible failure message with an exception
     */
    @JvmStatic
    fun wtf(tag: String?, msg: String?, ex: Throwable?) {
        if (isLoggable) {
            Log.wtf(tag, msg, ex)
        }
    }

    /**
     * Log an error message
     */
    @JvmStatic
    fun e(tag: String?, msg: String) {
        if (isLoggable) {
            Log.e(tag, msg)
        }
    }

    /**
     * Log an error message with an exception
     */
    @JvmStatic
    fun e(tag: String?, msg: String?, ex: Throwable?) {
        if (isLoggable) {
            Log.e(tag, msg, ex)
        }
    }

    /**
     * Log a message
     */
    @JvmStatic
    fun log(level: Int, tag: String, message: String) {
        when (level.coerceIn(Log.VERBOSE, Log.ERROR)) {
            Log.VERBOSE -> v(tag, message)
            Log.DEBUG -> d(tag, message)
            Log.INFO -> i(tag, message)
            Log.WARN -> w(tag, message)
            Log.ERROR -> e(tag, message)
            Log.ASSERT -> wtf(tag, message)
        }
    }
}
