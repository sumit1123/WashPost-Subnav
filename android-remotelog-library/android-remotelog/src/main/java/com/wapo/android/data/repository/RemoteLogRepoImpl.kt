/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.android.data.repository

import android.content.Context
import android.os.Bundle
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.domain.repository.RemoteLogRepo
import com.wapo.android.remotelog.logger.Level
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.kmpshared.logger.domain.LoggerProvider
import com.wapo.kmpshared.logger.domain.model.LogLevel
import com.wapo.kmpshared.logger.domain.model.LogOrigin
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteLogRepoImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : RemoteLogRepo,
        LoggerProvider {

    override fun d(eventLog: EventLog?) = d(eventLog, null, null)
    override fun d(eventLog: EventLog?, process: String?) = d(eventLog, process, null)
    override fun d(eventLog: EventLog?, process: String?, paywallInfo: String?) {
        val config = RemoteLog.getConfig()
        if (eventLog == null || config == null || !config.isLoggable() || !config.isDebugLoggingActive) return
        RemoteLog.writeMessageToFile(context, Level.DEBUG, eventLog, process, paywallInfo)
    }

    override fun e(eventLog: EventLog?) = e(eventLog, null, null)
    override fun e(eventLog: EventLog?, process: String?) = e(eventLog, process, null)
    override fun e(eventLog: EventLog?, process: String?, paywallInfo: String?) {
        val config = RemoteLog.getConfig()
        if (eventLog == null || config == null || !config.isLoggable() || !config.isErrorLoggingActive) return
        RemoteLog.writeMessageToFile(context, Level.ERROR, eventLog, process, paywallInfo)
    }

    override fun w(eventLog: EventLog?) {
        val config = RemoteLog.getConfig()
        if (eventLog == null || config == null || !config.isLoggable()) return
        RemoteLog.writeMessageToFile(context, Level.WARNING, eventLog, null, null)
    }

    override fun p(eventLog: EventLog?) = p(eventLog, null, null)
    override fun p(eventLog: EventLog?, process: String?) = p(eventLog, process, null)
    override fun p(eventLog: EventLog?, process: String?, paywallInfo: String?) {
        val config = RemoteLog.getConfig()
        if (eventLog == null || config == null || !config.isLoggable() || !config.isPaywallLoggingActive) return
        RemoteLog.writeMessageToFile(context, Level.PAYWALL, eventLog, process, paywallInfo)
    }

    override fun v(eventLog: EventLog?) = v(eventLog, null, null)
    override fun v(eventLog: EventLog?, process: String?) = v(eventLog, process, null)
    override fun v(eventLog: EventLog?, process: String?, paywallInfo: String?) {
        val config = RemoteLog.getConfig()
        if (eventLog == null || config == null || !config.isLoggable() || !config.isVerboseLoggingActive) return
        RemoteLog.writeMessageToFile(context, Level.VERBOSE, eventLog, process, paywallInfo)
    }

    override fun m(eventLog: EventLog?) = m(eventLog, null, null)
    override fun m(eventLog: EventLog?, process: String?) = m(eventLog, process, null)
    override fun m(eventLog: EventLog?, process: String?, paywallInfo: String?) {
        val config = RemoteLog.getConfig()
        if (eventLog == null || config == null || !config.isLoggable() || !config.isMetricsLoggingActive || !config.isSampledForMetrics()) return
        RemoteLog.writeMessageToFile(context, Level.METRICS, eventLog, process, paywallInfo)
    }

    override fun uploadLogFiles(bundle: Bundle?) {
        RemoteLog.uploadLogFiles(context, bundle)
    }

    override fun log(
        severity: LogLevel,
        message: String,
        origin: LogOrigin,
        sampling: String?,
        extras: Map<Any, Any?>?,
    ) {
        val builder = EventLog.Builder().setMessage(message)
        extras?.forEach { (key, value) -> builder.set(key.toString(), value) }
        val eventLog = builder.build()
        val process = when (origin) {
            is LogOrigin.Module -> origin.module
            is LogOrigin.Source -> origin.function
        }
        when (severity) {
            LogLevel.Verbose -> v(eventLog, process)
            LogLevel.Debug -> d(eventLog, process)
            LogLevel.Info -> p(eventLog, process)
            LogLevel.Warn -> w(eventLog)
            LogLevel.Error -> e(eventLog, process)
        }
    }

    override fun upload() = uploadLogFiles(null)
}
