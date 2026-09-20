package com.wapo.kmpshared.logger.domain

import com.wapo.kmpshared.logger.domain.model.LogLevel
import com.wapo.kmpshared.logger.domain.model.LogOrigin

interface LoggerProvider {
    fun log(
        severity: LogLevel,
        message: String,
        origin: LogOrigin,
        sampling: String?,
        extras: Map<Any, Any?>?,
    )

    fun upload()
}

object NoOpLoggerProvider : LoggerProvider {
    override fun log(
        severity: LogLevel,
        message: String,
        origin: LogOrigin,
        sampling: String?,
        extras: Map<Any, Any?>?,
    ) {
        // No operation
    }

    override fun upload() {
        // No operation
    }
}
