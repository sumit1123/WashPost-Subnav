package com.wapo.kmpshared.logger.data

import com.wapo.kmpshared.core.config.LoggerConfig
import com.wapo.kmpshared.core.network.PlatformSession
import com.wapo.kmpshared.util.KMPUUID
import com.wapo.kmpshared.util.div
import kotlinx.io.asInputStream
import kotlinx.io.asOutputStream
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.GZIPOutputStream

//region Gzip
internal actual fun gzipFile(
    source: Path,
    target: Path,
) {
    val fileSystem = SystemFileSystem
    val parent = target.parent ?: error("Gzip: target has no parent: $target")
    val tempTarget = parent / "temp-${target.name}"

    runCatching { fileSystem.delete(tempTarget) }

    try {
        // Manually open and buffer streams, utilizing `.use` to guarantee closure
        fileSystem.source(source).buffered().use { src ->
            fileSystem.sink(tempTarget).buffered().use { sink ->

                // Wrap using `asOutputStream()` and `asInputStream()`
                GZIPOutputStream(sink.asOutputStream()).use { gzipOut ->
                    src.asInputStream().copyTo(gzipOut)
                }
            }
        }

        runCatching { fileSystem.delete(target) }
        fileSystem.atomicMove(tempTarget, target)
    } catch (t: Throwable) {
        runCatching { fileSystem.delete(tempTarget) }
        throw t
    }
}
//endregion

//region Uploader
private val inFlightUploads: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())

internal actual fun enqueueLogUploadAndDelete(
    config: LoggerConfig,
    session: PlatformSession,
    channel: KMPUUID,
    file: Path,
) {
    val key = file.toString()
    if (!inFlightUploads.add(key)) return

    try {
        val requestBody = File(file.toString()).asRequestBody("application/gzip".toMediaType())
        val request =
            Request
                .Builder()
                .url(config.uploaderURL.toString())
                .post(requestBody)
                .header("Content-Encoding", "gzip")
                .header("Authorization", "Splunk ${config.uploaderToken}")
                .header("X-Splunk-Request-Channel", channel.toString())
                .build()

        session.newCall(request).enqueue(
            object : Callback {
                override fun onResponse( call: Call, response: okhttp3.Response ) {
                    try {
                        response.use {
                            if (it.isSuccessful) {
                                runCatching { SystemFileSystem.delete(file) }
                            }
                        }
                    } finally {
                        inFlightUploads.remove(key)
                    }
                }

                override fun onFailure( call: Call, e: IOException ) {
                    inFlightUploads.remove(key)
                }
            },
        )
    } catch (throwable: Throwable) {
        inFlightUploads.remove(key)
        throw throwable
    }
}
//endregion
