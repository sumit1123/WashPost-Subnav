package com.wapo.kmpshared.logger.domain.model

import com.wapo.kmpshared.core.lifecycle.AppActivationState
import com.wapo.kmpshared.core.lifecycle.AppEnvironment
import com.wapo.kmpshared.core.lifecycle.AppLifecycle
import com.wapo.kmpshared.core.lifecycle.ReachabilityStatus
import com.wapo.kmpshared.util.KMPDate
import kotlinx.serialization.Serializable

@Serializable
enum class LogLevel(
    val label: String,
) {
    Verbose("VERBOSE"),
    Debug("DEBUG"),
    Info("INFO"),
    Warn("WARNING"),
    Error("ERROR"),
}

sealed interface LogOrigin {
    data class Module(
        val module: String,
    ) : LogOrigin

    data class Source(
        val file: String,
        val line: UInt,
        val function: String,
    ) : LogOrigin
}

internal data class LogMetadata(
    val appConfigVersion: Int,
    val appState: AppActivationState,
    val network: ReachabilityStatus,
    val date: KMPDate,
) {
    companion object {
        operator fun invoke(
            config: Int,
            lifecycle: AppLifecycle,
        ): LogMetadata =
            LogMetadata(
                appConfigVersion = config,
                appState = lifecycle.activationState.value,
                network = lifecycle.reachability.value,
                date = KMPDate(),
            )
    }
}

internal data class LogMessage(
    val env: AppEnvironment,
    val meta: LogMetadata,
    val severity: LogLevel,
    val origin: LogOrigin,
    val message: String,
    val extras: Map<String, String>?,
) {
    companion object {
        operator fun invoke(
            env: AppEnvironment,
            meta: LogMetadata,
            severity: LogLevel,
            origin: LogOrigin,
            message: String,
            extras: Map<Any, Any?>?,
        ): LogMessage {
            val moduleOrSource =
                when (origin) {
                    is LogOrigin.Module -> {
                        origin
                    }

                    is LogOrigin.Source -> {
                        origin.copy(file = origin.file.substringAfterLast("/"))
                    }
                }

            return LogMessage(
                env = env,
                meta = meta,
                severity = severity,
                origin = moduleOrSource,
                message = message,
                extras = extras?.mapKeys { it.key.toString() }?.mapValues { it.value?.toString() ?: "<null>" },
            )
        }
    }
}
