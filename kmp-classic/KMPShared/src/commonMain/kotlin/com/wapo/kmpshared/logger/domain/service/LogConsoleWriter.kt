package com.wapo.kmpshared.logger.domain.service

import com.wapo.kmpshared.logger.domain.model.LogLevel

interface LogConsoleWriter {
    fun log(
        severity: LogLevel,
        tag: String,
        message: String,
    )
}

expect fun logConsoleWriter(): LogConsoleWriter
