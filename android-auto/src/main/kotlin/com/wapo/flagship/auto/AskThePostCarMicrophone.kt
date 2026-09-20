package com.wapo.flagship.auto

import android.annotation.SuppressLint
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.media.CarAudioRecord
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class AskThePostCarMicrophone(
    private val carContext: CarContext,
    private val onDismissed: () -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()

    private var recording: CarAudioRecord? = null
    private var pipeWriter: ParcelFileDescriptor? = null
    private var recordingJob: Job? = null

    @SuppressLint("MissingPermission")
    fun start(): ParcelFileDescriptor? {
        stop()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null

        val (reader, writer) = ParcelFileDescriptor.createPipe()
        val carRecording = CarAudioRecord.create(carContext)
        try {
            carRecording.startRecording()
        } catch (error: Throwable) {
            reader.close()
            writer.close()
            throw error
        }

        synchronized(lock) {
            recording = carRecording
            pipeWriter = writer
            recordingJob = scope.launch { stream(carRecording, writer) }
        }
        return reader
    }

    fun stop() {
        val activeRecording: CarAudioRecord?
        val activeWriter: ParcelFileDescriptor?
        val activeJob: Job?
        synchronized(lock) {
            activeRecording = recording
            activeWriter = pipeWriter
            activeJob = recordingJob
            recording = null
            pipeWriter = null
            recordingJob = null
        }
        runCatching { activeRecording?.stopRecording() }
            .onFailure { Log.w(TAG, "Unable to stop the car microphone", it) }
        runCatching { activeWriter?.close() }
        activeJob?.cancel()
    }

    fun release() {
        stop()
        scope.cancel()
    }

    private fun stream(
        carRecording: CarAudioRecord,
        writer: ParcelFileDescriptor,
    ) {
        var dismissedByHost = false
        try {
            FileOutputStream(writer.fileDescriptor).use { output ->
                val buffer = ByteArray(CarAudioRecord.AUDIO_CONTENT_BUFFER_SIZE)
                while (scope.isActive) {
                    val byteCount = carRecording.read(buffer, 0, buffer.size)
                    if (byteCount < 0) {
                        dismissedByHost = true
                        break
                    }
                    output.write(buffer, 0, byteCount)
                }
            }
        } catch (error: Exception) {
            Log.d(TAG, "Car microphone stream closed", error)
        } finally {
            val wasCurrentRecording = closeIfCurrent(carRecording, writer)
            if (dismissedByHost && wasCurrentRecording) onDismissed()
        }
    }

    private fun closeIfCurrent(
        carRecording: CarAudioRecord,
        writer: ParcelFileDescriptor,
    ): Boolean {
        synchronized(lock) {
            if (recording !== carRecording) return false
            recording = null
            pipeWriter = null
            recordingJob = null
        }
        runCatching { carRecording.stopRecording() }
        runCatching { writer.close() }
        return true
    }

    private companion object {
        const val TAG = "AskThePostCarMicrophone"
    }
}
