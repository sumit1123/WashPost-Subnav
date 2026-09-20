package com.wapo.kmpshared.logger.data

import com.wapo.kmpshared.util.div
import kotlinx.datetime.Clock
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

internal class LogPaths {
    val root: Path = platformLogRoot()

    internal fun new(): Path {
        val ts = Clock.System.now().toEpochMilliseconds()
        return root / "$LOG_PREFIX$ts$LOG_CURRENT_SUFFIX"
    }

    internal fun timestamp(file: Path): Long? {
        val name = file.name
        if (!name.startsWith(LOG_PREFIX)) return null

        return name
            .removePrefix(LOG_PREFIX)
            .removeSuffix(LOG_ARCHIVE_SUFFIX)
            .removeSuffix(LOG_CURRENT_SUFFIX)
            .toLongOrNull()
    }

    internal fun archived(source: Path): Path =
        root / "${source.name}$LOG_ARCHIVE_SUFFIX"

    internal fun list(): List<Path> =
        runCatching {
            SystemFileSystem
                .list(root)
                .filter { it.name.startsWith(LOG_PREFIX) }
                .sortedBy { it.name }
        }.getOrElse { emptyList() }

    internal fun listCurrent(): List<Path> =
        list().filter { it.name.endsWith(LOG_CURRENT_SUFFIX) }

    internal fun listArchived(): List<Path> =
        list().filter { it.name.endsWith(LOG_ARCHIVE_SUFFIX) }

    private companion object {
        const val LOG_PREFIX = "log-"
        const val LOG_CURRENT_SUFFIX = ".log"
        const val LOG_ARCHIVE_SUFFIX = ".gz"
    }
}

internal expect fun platformLogRoot(): Path
