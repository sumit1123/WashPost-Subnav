package com.wapo.kmpshared.logger.domain

import co.touchlab.stately.concurrency.AtomicInt
import co.touchlab.stately.concurrency.value
import com.wapo.kmpshared.core.config.AppConfig
import com.wapo.kmpshared.core.config.KMPEnv
import com.wapo.kmpshared.core.config.LoggerConfig
import com.wapo.kmpshared.core.lifecycle.AppActivationState
import com.wapo.kmpshared.core.lifecycle.AppEnvironment
import com.wapo.kmpshared.core.lifecycle.AppLifecycle
import com.wapo.kmpshared.core.network.PlatformNetworkProvider
import com.wapo.kmpshared.logger.data.LogIO
import com.wapo.kmpshared.logger.domain.model.LogLevel
import com.wapo.kmpshared.logger.domain.model.LogMessage
import com.wapo.kmpshared.logger.domain.model.LogMetadata
import com.wapo.kmpshared.logger.domain.model.LogOrigin
import com.wapo.kmpshared.logger.domain.service.LogConsoleWriter
import com.wapo.kmpshared.logger.domain.service.LogFormatter
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.Single
import kotlin.math.abs

@Single([LoggerProvider::class])
class LoggerRepository internal constructor(
    appConfig: AppConfig,
    private val appLifecycle: AppLifecycle,
    private val config: LoggerConfig,
    private val formatter: LogFormatter,
    network: PlatformNetworkProvider,
    private val consoleWriter: LogConsoleWriter,
) : LoggerProvider {
    //region Initializers
    private val appConfig = AtomicInt(appConfig.version)
    private val appEnv: AppEnvironment = appLifecycle.env
    private val level: LogLevel
    private val cohort: Double
    private val logIO: LogIO
    private val writeDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val uploadDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val repositoryJob = SupervisorJob()
    private val exceptionHandler = CoroutineExceptionHandler { _, _ -> } // swallow
    private val writeScope = CoroutineScope(repositoryJob + writeDispatcher + exceptionHandler)
    private val uploadScope = CoroutineScope(repositoryJob + uploadDispatcher + exceptionHandler)

    init {
        level = config.minLogLevel ?: when (appEnv.env) {
            KMPEnv.Debug -> LogLevel.Verbose
            KMPEnv.Test -> LogLevel.Info
            KMPEnv.Prod -> LogLevel.Warn
        }
        cohort = (abs(appEnv.supportID.hashCode().toLong()) % 100L).toDouble() / 100.0

        logIO = runBlocking(writeDispatcher) { LogIO(config, network, uploadScope) }

        writeScope.launch {
            appLifecycle.reachability
                .map { it.isReachable }
                .distinctUntilChanged()
                .filter { it }
                .collect {
                    logIO.compressAndUpload()
                }
        }

        writeScope.launch {
            appLifecycle.activationState
                .filter { it == AppActivationState.Background }
                .collect {
                    logIO.flush()
                }
        }
    }
    //endregion

    //region Logger API
    override fun log(
        severity: LogLevel,
        message: String,
        origin: LogOrigin,
        sampling: String?,
        extras: Map<Any, Any?>?,
    ) {
        if (severity < level) {
            return
        }

        val threshold = sampling?.let { config.sampling[it]?.coerceIn(0.0F, 1.0F) } ?: 1.0F
        if (cohort >= threshold) {
            return
        }

        val meta = LogMetadata(appConfig.value, appLifecycle)
        writeScope.launch {
            val log = LogMessage(appEnv, meta, severity, origin, message, extras)

            if (appEnv.env == KMPEnv.Debug) {
                formatter.formatToConsole(log).let {
                    consoleWriter.log(severity = it.first, tag = it.second, message = it.third)
                }
            }

            formatter.formatToFile(log).let {
                logIO.write(it)
            }
        }
    }

    override fun upload() {
        writeScope.launch {
            logIO.rotateAndUpload()
        }
    }

    internal fun updateAppConfig(config: AppConfig) {
        appConfig.set(config.version)
    }

    fun clear() {
        runBlocking(writeDispatcher) {
            repositoryJob.cancelAndJoin()
            logIO.clear()
        }
    }
    //endregion
}
