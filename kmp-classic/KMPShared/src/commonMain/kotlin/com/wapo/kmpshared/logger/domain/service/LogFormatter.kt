package com.wapo.kmpshared.logger.domain.service

import com.wapo.kmpshared.core.lifecycle.getNetworkType
import com.wapo.kmpshared.logger.domain.model.LogLevel
import com.wapo.kmpshared.logger.domain.model.LogMessage
import com.wapo.kmpshared.logger.domain.model.LogOrigin
import com.wapo.kmpshared.util.KMPDate
import org.koin.core.annotation.Single

@Single
internal class LogFormatter {
    private val formatter = LogDateFormatter()

    fun formatToConsole(log: LogMessage): Triple<LogLevel, String, String> {
        val sb = StringBuilder()

        sb.append(formatter.format(log.meta.date)).append(" ")

        val severityLetter =
            when (log.severity) {
                LogLevel.Verbose -> "V"
                LogLevel.Debug -> "D"
                LogLevel.Info -> "I"
                LogLevel.Warn -> "W"
                LogLevel.Error -> "E"
            }
        sb.append("[$severityLetter] ")

        val tag =
            when (val origin = log.origin) {
                is LogOrigin.Module -> {
                    sb.append(origin.module)
                    origin.module
                }

                is LogOrigin.Source -> {
                    sb
                        .append(origin.file)
                        .append("#")
                        .append(origin.function)
                        .append("(")
                        .append(origin.line)
                        .append(")")
                    origin.file
                }
            }
        sb.append(" ")

        log.appendKeyValueString(sb)

        return Triple(log.severity, tag, sb.toString())
    }

    fun formatToFile(log: LogMessage): String {
        val sb = StringBuilder()

        // 1. Header Format: [Timestamp]
        sb.append("[").append(formatter.format(log.meta.date)).append("] ")

        // 2. Main Identity Block
        sb.append("level=\"").append(log.severity.label).append("\" ")

        val s = log.env
        sb.append("device=\"").apply { s.device?.let { appendSanitized(it) } ?: append("unknown") }.append("\" ")
        sb.append("os_version=\"").appendSanitized(s.osVersion).append("\" ")
        sb.append("app_version=\"").apply { s.appVersion?.let { appendSanitized(it) } ?: append("(null)") }.append("\" ")
        sb.append("app_build=\"").apply { s.appBuild?.let { appendSanitized(it) } ?: append("(null)") }.append("\" ")
        sb.append("support_id=\"").appendSanitized(s.supportID).append("\" ")

        val d = log.meta
        sb.append("app_config=\"").append(d.appConfigVersion).append("\" ")
        sb.append("network=\"").append(d.network.getNetworkType()).append("\" ")
        sb.append("app_state=\"").append(d.appState.label).append("\" ")

        when (val origin = log.origin) {
            is LogOrigin.Module -> {
                sb.append("module=\"").appendSanitized(origin.module).append("\" ")
            }

            is LogOrigin.Source -> {
                sb.append("file=\"").appendSanitized(origin.file).append("\" ")
                sb.append("function=\"").appendSanitized(origin.function).append("\" ")
                sb.append("line=\"").append(origin.line).append("\" ")
            }
        }

        // 3. Metadata and Message (Sanitized)
        log.appendKeyValueString(sb)

        return sb.toString()
    }

    private fun LogMessage.appendKeyValueString(sb: StringBuilder) {
        extras?.entries?.sortedBy { it.key }?.forEach { (key, value) ->
            if (sb.isNotEmpty() && sb.last() != ' ') sb.append(" ")
            sb.append(key).append("=\"")
            sb.appendSanitized(value)
            sb.append("\"")
        }

        if (message.isNotEmpty()) {
            if (sb.isNotEmpty() && sb.last() != ' ') sb.append(" ")
            sb.append("message=\"")
            sb.appendSanitized(message)
            sb.append("\"")
        }
    }

    private fun StringBuilder.appendSanitized(input: String): StringBuilder =
        apply {
            var spacePending = false
            var hasAppended = false

            for (c in input) {
                if (c.isWhitespace()) {
                    if (hasAppended) spacePending = true
                } else {
                    if (spacePending) {
                        append(' ')
                        spacePending = false
                    }
                    append(if (c == '"') '\'' else c)
                    hasAppended = true
                }
            }
        }
}

internal expect class LogDateFormatter() {
    fun format(date: KMPDate): String
}
