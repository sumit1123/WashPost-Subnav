package com.wapo.kmpshared.logger.data

import com.wapo.kmpshared.core.config.LoggerConfig
import com.wapo.kmpshared.core.network.PlatformNetworkProvider
import com.wapo.kmpshared.core.network.PlatformSession
import com.wapo.kmpshared.util.KMPUUID
import com.wapo.kmpshared.util.createKMPUUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.io.Sink
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString

internal class LogIO(
    private val config: LoggerConfig,
    network: PlatformNetworkProvider,
    private val uploadScope: CoroutineScope,
) {
    private val fileSystem = SystemFileSystem
    private val paths = LogPaths()
    private val session: PlatformSession by lazy { network.getSessions().logger }
    private val channel: KMPUUID = createKMPUUID()

    private var activeFile: Path? = null
    private var activeSink: Sink? = null

    init {
        activeFile = paths.listCurrent().lastOrNull() ?: paths.new()
    }

    //region API
    fun write(message: String) {
        runCatching {
            val sink = ensureActiveSink()
            sink.writeString(message)
            sink.writeString("\n")
            sink.emit()

            if (shouldRotate()) rotateAndUpload()
        }
    }

    fun flush() {
        runCatching {
            activeSink?.flush()
        }
    }

    fun compressAndUpload() {
        activeFile?.let { active ->
            val filesToCompress = paths.listCurrent().filter { it.name != active.name }

            uploadScope.launch {
                compress(filesToCompress)
                upload()
                cleanup()
            }
        }
    }

    fun rotateAndUpload() {
        rotate()
        compressAndUpload()
    }

    fun clear() {
        closeActiveSink()
    }
    //endregion

    //region Rotation pipeline
    private fun rotate() {
        closeActiveSink()
        activeFile = paths.new()
    }

    private fun compress(files: List<Path>) {
        files.forEach { compressFile(it) }
    }

    private fun compressFile(source: Path) {
        runCatching {
            gzipFile(source, paths.archived(source))
            fileSystem.delete(source)
        }
    }

    private fun upload() =
        paths
            .listArchived()
            .forEach { file -> enqueueLogUploadAndDelete(config, session, channel, file) }

    private fun cleanup() {
        runCatching {
            paths.list().dropLast(config.maxLogFiles).forEach { fileSystem.delete(it) }
        }
    }
    //endregion

    //region Active file management
    private fun ensureActiveSink(): Sink {
        activeSink?.let { return it }
        val file = activeFile ?: paths.new().also { activeFile = it }
        runCatching { fileSystem.createDirectories(paths.root) }
        return fileSystem.sink(file, append = true).buffered().also { activeSink = it }
    }

    private fun closeActiveSink() {
        runCatching { activeSink?.flush() }
        runCatching { activeSink?.close() }
        activeSink = null
        activeFile = null
    }
    //endregion

    //region Rotation policy
    private fun shouldRotate(): Boolean =
        activeFile
            ?.let { shouldRotate(it) } ?: false

    private fun shouldRotate(file: Path): Boolean {
        val ts = paths.timestamp(file) ?: return true
        val size = fileSystem.metadataOrNull(file)?.size ?: return true
        return size >= config.maxLogSize ||
            isTooOld((config.maxLogAge * 1000).toLong(), ts)
    }

    private fun isTooOld(
        maxAgeMillis: Long,
        createdAtMillis: Long,
    ): Boolean =
        createdAtMillis.takeIf { it > 0 }?.let { createdAt ->
            (Clock.System.now().toEpochMilliseconds() - createdAt) >= maxAgeMillis
        } ?: false
    //endregion
}

internal expect fun enqueueLogUploadAndDelete(
    config: LoggerConfig,
    session: PlatformSession,
    channel: KMPUUID,
    file: Path,
)

internal expect fun gzipFile(
    source: Path,
    target: Path,
)
