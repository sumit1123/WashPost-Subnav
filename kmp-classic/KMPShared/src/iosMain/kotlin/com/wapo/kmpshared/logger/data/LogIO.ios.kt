package com.wapo.kmpshared.logger.data

import com.wapo.kmpshared.core.config.LoggerConfig
import com.wapo.kmpshared.core.network.PlatformSession
import com.wapo.kmpshared.util.KMPUUID
import com.wapo.kmpshared.util.div
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import platform.Foundation.HTTPMethod
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.Foundation.allHTTPHeaderFields
import platform.zlib.Z_DEFAULT_COMPRESSION
import platform.zlib.Z_DEFAULT_STRATEGY
import platform.zlib.Z_DEFLATED
import platform.zlib.Z_FINISH
import platform.zlib.Z_OK
import platform.zlib.Z_STREAM_END
import platform.zlib.Z_SYNC_FLUSH
import platform.zlib.deflate
import platform.zlib.deflateEnd
import platform.zlib.deflateInit2
import platform.zlib.z_stream

// region Gzip
private const val INPUT_BUF_SIZE = 2 * 1024 // 2 KB
private const val OUTPUT_BUF_SIZE = 1 * 1024 // 1 KB
private const val GZIP_WINDOW_BITS = 15 + 16 // gzip wrapper (RFC 1952)
private const val MEM_LEVEL = 8

@OptIn(ExperimentalForeignApi::class)
internal actual fun gzipFile(
    source: Path,
    target: Path,
) {
    val fileSystem = SystemFileSystem
    val parent = target.parent ?: error("Gzip: target has no parent: $target")
    val tempTarget = parent / "temp-${target.name}"

    // Match WP: pre-clean stale temp
    runCatching { fileSystem.delete(tempTarget) }

    try {
        fileSystem.source(source).buffered().use { src ->
            fileSystem.sink(tempTarget).buffered().use { sink ->
                memScoped {
                    val strm =
                        alloc<z_stream>().apply {
                            zalloc = null
                            zfree = null
                            opaque = null
                            total_out = 0u
                        }

                    val initRc =
                        deflateInit2(
                            strm.ptr,
                            Z_DEFAULT_COMPRESSION,
                            Z_DEFLATED,
                            GZIP_WINDOW_BITS,
                            MEM_LEVEL,
                            Z_DEFAULT_STRATEGY,
                        )
                    if (initRc != Z_OK) error("Gzip: deflateInit2 failed (rc=$initRc)")

                    try {
                        compressLoop(src, sink, strm.ptr)
                    } finally {
                        deflateEnd(strm.ptr)
                    }
                }
            }
        }

        // Delete target if it already exists, then move temp -> target
        runCatching { fileSystem.delete(target) }
        fileSystem.atomicMove(tempTarget, target)
    } catch (t: Throwable) {
        runCatching { fileSystem.delete(tempTarget) }
        throw t
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun compressLoop(
    src: Source,
    sink: Sink,
    strmPtr: CPointer<z_stream>,
) {
    val inputBuf = ByteArray(INPUT_BUF_SIZE)
    var inputDataSize = 0 // valid bytes in inputBuf carrying over from prior iterations
    var done = false

    inputBuf.usePinned { pinned ->
        val inputBase = pinned.addressOf(0) // CPointer<ByteVar>

        memScoped {
            val outputBuf = allocArray<ByteVar>(OUTPUT_BUF_SIZE)

            while (!done) {
                // Read into inputBuf at offset inputDataSize
                val readCapacity = INPUT_BUF_SIZE - inputDataSize
                val strm = strmPtr.pointed

                // kotlinx-io uses readAtMostTo with absolute endIndex boundaries
                val read =
                    if (readCapacity > 0) {
                        src.readAtMostTo(inputBuf, inputDataSize, inputDataSize + readCapacity)
                    } else {
                        0
                    }
                val readCount = if (read == -1) 0 else read
                if (read == -1 && inputDataSize == 0 && strm.total_out == 0u.toULong()) {
                    // Empty source: still need one Z_FINISH pass to emit valid empty gzip.
                }
                inputDataSize += readCount

                // Run deflate
                val moreToCome = (read != -1) // matches WP's hasBytesAvailable
                val flush = if (moreToCome) Z_SYNC_FLUSH else Z_FINISH

                strm.next_in = inputBase.reinterpret<UByteVar>()
                strm.avail_in = inputDataSize.toUInt()
                strm.next_out = outputBuf.reinterpret<UByteVar>()
                strm.avail_out = OUTPUT_BUF_SIZE.toUInt()

                val prevTotalIn = strm.total_in
                val prevTotalOut = strm.total_out

                val rc = deflate(strmPtr, flush)
                if (rc != Z_OK && rc != Z_STREAM_END) error("Gzip: deflate failed (rc=$rc)")

                val inputProcessed = (strm.total_in - prevTotalIn).toInt()
                val outputProcessed = (strm.total_out - prevTotalOut).toInt()

                // Write output
                if (outputProcessed > 0) {
                    sink.write(outputBuf.readBytes(outputProcessed))
                }

                // Keep leftover input bytes at the front of the buffer
                val inputRemaining = inputDataSize - inputProcessed
                if (inputRemaining > 0 && inputProcessed > 0) {
                    inputBuf.copyInto(
                        destination = inputBuf,
                        destinationOffset = 0,
                        startIndex = inputProcessed,
                        endIndex = inputProcessed + inputRemaining,
                    )
                }
                inputDataSize = inputRemaining

                done = (flush == Z_FINISH && inputDataSize == 0 && rc == Z_STREAM_END)

                // Defensive: if deflate filled the output but didn't finish, loop again to drain.
                if (!done && strm.avail_out == 0u) continue
            }
        }
    }
}
//endregion

//region Uploader
internal actual fun enqueueLogUploadAndDelete(
    config: LoggerConfig,
    session: PlatformSession,
    channel: KMPUUID,
    file: Path,
) {
    val fileURL = NSURL.fileURLWithPath(file.toString())
    val request =
        NSMutableURLRequest.requestWithURL(config.uploaderURL).apply {
            HTTPMethod = "POST"
            allHTTPHeaderFields =
                mapOf(
                    "Content-Encoding" to "gzip",
                    "Authorization" to "Splunk ${config.uploaderToken}",
                    "X-Splunk-Request-Channel" to channel.UUIDString,
                    "X-Logger-FileName" to file.name,
                )
        }
    session.uploadTaskWithRequest(request, fromFile = fileURL).resume()
    // iOS background session moves the file to system location so delete here is safe
    runCatching { SystemFileSystem.delete(file) }
}
//endregion
