package com.wapo.kmpshared.logger

import co.touchlab.stately.concurrency.AtomicReference
import co.touchlab.stately.concurrency.value
import com.wapo.kmpshared.logger.domain.LoggerProvider
import com.wapo.kmpshared.logger.domain.NoOpLoggerProvider
import com.wapo.kmpshared.logger.domain.model.LogLevel
import com.wapo.kmpshared.logger.domain.model.LogOrigin
import kotlin.coroutines.cancellation.CancellationException

/**
 * Unified entry point for logging across all platforms (KMP, Swift, Android).
 */
class WPLogger {
    private val provider = AtomicReference<LoggerProvider>(NoOpLoggerProvider)

    fun updateProvider(newProvider: LoggerProvider) {
        provider.set(newProvider)
    }

    fun verbose(
        message: String,
        module: String,
        sampling: String? = null,
        extras: Map<Any, Any>? = null,
    ) = log(LogLevel.Verbose, message, LogOrigin.Module(module), sampling, extras)

    fun debug(
        message: String,
        module: String,
        sampling: String? = null,
        extras: Map<Any, Any>? = null,
    ) = log(LogLevel.Debug, message, LogOrigin.Module(module), sampling, extras)

    fun info(
        message: String,
        module: String,
        sampling: String? = null,
        extras: Map<Any, Any>? = null,
    ) = log(LogLevel.Info, message, LogOrigin.Module(module), sampling, extras)

    fun warn(
        message: String,
        module: String,
        sampling: String? = null,
        extras: Map<Any, Any>? = null,
    ) = log(LogLevel.Warn, message, LogOrigin.Module(module), sampling, extras)

    fun error(
        message: String,
        module: String,
        sampling: String? = null,
        extras: Map<Any, Any>? = null,
    ) = log(LogLevel.Error, message, LogOrigin.Module(module), sampling, extras)

    fun upload() {
        try {
            provider.value.upload()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            // :( Fail silently. No recursive logs or print in prod
        }
    }

    // iOS and underlying systems use this directly to pass LogOrigin.Source
    fun log(
        severity: LogLevel,
        message: String,
        origin: LogOrigin,
        sampling: String?,
        extras: Map<Any, Any?>? = null,
    ) {
        try {
            provider.value.log(severity, message, origin, sampling, extras)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            // :( Fail silently. No recursive logs or print in prod
        }
    }
}
