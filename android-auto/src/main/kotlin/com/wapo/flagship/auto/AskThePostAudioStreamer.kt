package com.wapo.flagship.auto

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Base64
import androidx.annotation.RequiresApi
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@RequiresApi(Build.VERSION_CODES.M)
internal class AskThePostAudioStreamer(
    private val onPlaybackStarted: () -> Unit,
    private val onPlaybackComplete: () -> Unit,
) {
    private val commands = Channel<Command>(Channel.UNLIMITED)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val shouldBePaused = AtomicBoolean(false)
    @Volatile
    private var activeStream: PlaybackStream? = null

    init {
        scope.launch { processCommands() }
    }

    fun beginAnswer() {
        shouldBePaused.set(false)
        commands.trySend(Command.Reset)
    }

    fun appendBase64Chunk(encodedAudio: String) {
        commands.trySend(Command.Chunk(encodedAudio))
    }

    fun appendPcmChunk(audio: ByteArray) {
        commands.trySend(Command.PcmChunk(audio))
    }

    fun finishAnswer() {
        commands.trySend(Command.Finish)
    }

    fun pause() {
        shouldBePaused.set(true)
        activeStream?.pause()
        commands.trySend(Command.Pause)
    }

    fun resume() {
        shouldBePaused.set(false)
        activeStream?.resume()
        commands.trySend(Command.Resume)
    }

    fun stop() {
        shouldBePaused.set(false)
        activeStream?.release()
        commands.trySend(Command.Reset)
    }

    fun release() {
        activeStream?.release()
        commands.trySend(Command.Release)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private suspend fun processCommands() {
        var stream = PlaybackStream(startPaused = shouldBePaused.get()).also { activeStream = it }

        for (command in commands) {
            when (command) {
                Command.Reset -> {
                    stream.release()
                    stream = PlaybackStream(startPaused = shouldBePaused.get()).also { activeStream = it }
                }

                is Command.Chunk -> {
                    val data =
                        runCatching { Base64.decode(command.encodedAudio, Base64.DEFAULT) }
                            .getOrNull() ?: continue
                    stream.append(data)
                }

                is Command.PcmChunk -> stream.append(command.audio)

                Command.Finish -> stream.finish()

                Command.Pause -> stream.pause()

                Command.Resume -> stream.resume()

                Command.Release -> {
                    stream.release()
                    activeStream = null
                    commands.close()
                    scope.cancel()
                    return
                }
            }
        }
    }

    private inner class PlaybackStream(
        startPaused: Boolean,
    ) {
        private val writerCommands = Channel<WriterCommand>(Channel.UNLIMITED)
        private val audioTrack = createAudioTrack()
        private val isPaused = AtomicBoolean(startPaused)
        private val hasStarted = AtomicBoolean(false)
        private val isReleased = AtomicBoolean(false)
        private val completionDelivered = AtomicBoolean(false)
        private val writerJob: Job = scope.launch { writeAudio() }
        private var completionMonitorJob: Job? = null

        fun append(data: ByteArray) {
            if (!isReleased.get()) writerCommands.trySend(WriterCommand.Chunk(data))
        }

        fun finish() {
            if (!isReleased.get()) writerCommands.trySend(WriterCommand.Finish)
        }

        fun pause() {
            if (isReleased.get()) return
            isPaused.set(true)
            if (hasStarted.get()) runCatching { audioTrack.pause() }
        }

        fun resume() {
            if (isReleased.get()) return
            isPaused.set(false)
            if (hasStarted.get()) runCatching { audioTrack.play() }
            writerCommands.trySend(WriterCommand.Resume)
        }

        fun release() {
            if (!isReleased.compareAndSet(false, true)) return
            completionMonitorJob?.cancel()
            writerCommands.close()
            audioTrack.releaseSafely()
            writerJob.cancel()
        }

        private suspend fun writeAudio() {
            val bufferedChunks = ArrayDeque<ByteArray>()
            var bufferedBytes = 0
            var bytesWritten = 0
            var streamFinished = false

            suspend fun startPlayback() {
                if (
                    hasStarted.get() ||
                    isPaused.get() ||
                    isReleased.get() ||
                    bufferedChunks.isEmpty()
                ) {
                    return
                }
                hasStarted.set(true)
                audioTrack.play()
                mainHandler.post {
                    if (!isReleased.get()) onPlaybackStarted()
                }
                // bufferedChunks stores the PCM data that has been received
                // wait until we have at least 1 second of audio before starting playback
                while (bufferedChunks.isNotEmpty() && !isReleased.get()) {
                    // bytesWritten is the total number of bytes written to the AudioTrack (total number of bytes played)
                    bytesWritten += writeFully(bufferedChunks.removeFirst())
                }
                // once all audio has been written, clear the buffer and reset the counter
                bufferedBytes = 0
            }

            fun armCompletionIfPlaying() {
                if (hasStarted.get() && bytesWritten > 0 && !isReleased.get()) {
                    armCompletion(bytesWritten / BYTES_PER_SAMPLE)
                }
            }

            for (command in writerCommands) {
                if (isReleased.get()) return
                when (command) {
                    is WriterCommand.Chunk -> {
                        if (hasStarted.get()) {
                            bytesWritten += writeFully(command.data)
                        } else {
                            // buffer the audio data until we have at least 1 second of audio, then start playback
                            bufferedChunks.addLast(command.data)
                            bufferedBytes += command.data.size
                            if (bufferedBytes >= ONE_SECOND_PCM_BYTES) startPlayback()
                        }
                    }

                    WriterCommand.Finish -> {
                        streamFinished = true
                        if (!hasStarted.get()) startPlayback()
                        armCompletionIfPlaying()
                    }

                    WriterCommand.Resume -> {
                        if (!hasStarted.get() && (bufferedBytes >= ONE_SECOND_PCM_BYTES || streamFinished)) {
                            startPlayback()
                        }
                        // the stream can finish while paused, in which case completion was never
                        // armed because playback had not started yet
                        if (streamFinished) armCompletionIfPlaying()
                    }
                }
            }
        }

        private fun armCompletion(completionFrame: Int) {
            // set marker to trigger completion when playback reaches the end of the audio data
            audioTrack.setPlaybackPositionUpdateListener(
                object : AudioTrack.OnPlaybackPositionUpdateListener {
                    override fun onMarkerReached(track: AudioTrack?) {
                        completePlayback(track ?: audioTrack)
                    }

                    override fun onPeriodicNotification(track: AudioTrack?) = Unit
                },
                mainHandler,
            )
            audioTrack.notificationMarkerPosition = completionFrame

            // fallback if marker fails to trigger, poll the playback head position to detect when audio is done playing
            completionMonitorJob?.cancel()
            completionMonitorJob =
                scope.launch {
                    while (!isReleased.get() && !completionDelivered.get()) {
                        if (
                            !isPaused.get() &&
                            hasStarted.get() &&
                            audioTrack.playbackHeadPosition >= completionFrame
                        ) {
                            completePlayback(audioTrack)
                        }
                        delay(COMPLETION_POLL_MILLIS.milliseconds)
                    }
                }
        }

        private fun completePlayback(track: AudioTrack) {
            if (isReleased.get() || !completionDelivered.compareAndSet(false, true)) return

            runCatching { track.stop() }
            hasStarted.set(false)
            mainHandler.post {
                if (!isReleased.get()) onPlaybackComplete()
            }
        }

        private suspend fun writeFully(data: ByteArray): Int {
            var offset = 0
            while (offset < data.size && !isReleased.get()) {
                while (isPaused.get() && !isReleased.get()) {
                    delay(PAUSE_POLL_MILLIS.milliseconds)
                }
                if (isReleased.get()) break

                val written =
                    audioTrack.write(
                        data,
                        offset,
                        data.size - offset,
                        AudioTrack.WRITE_NON_BLOCKING,
                    )
                when {
                    written > 0 -> offset += written
                    written == 0 || isPaused.get() -> delay(WRITE_RETRY_MILLIS.milliseconds)
                    else -> {
                        // A valid, initialized track should only return a negative value when it
                        // is being stopped/released. Retry transient state changes without losing
                        // the unwritten tail of the answer.
                        delay(WRITE_RETRY_MILLIS.milliseconds)
                    }
                }
            }
            return offset
        }
    }

    private fun createAudioTrack(): AudioTrack {
        val minBufferSize =
            AudioTrack.getMinBufferSize(
                SAMPLE_RATE_HZ,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
        return AudioTrack(
            AudioAttributes
                .Builder()
                .setUsage(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        AudioAttributes.USAGE_ASSISTANT
                    } else {
                        AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE
                    },
                )
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
            AudioFormat
                .Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE_HZ)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            maxOf(minBufferSize, ONE_SECOND_PCM_BYTES),
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE,
        )
    }

    private fun AudioTrack.releaseSafely() {
        runCatching { if (playState != AudioTrack.PLAYSTATE_STOPPED) stop() }
        runCatching { flush() }
        runCatching { release() }
    }

    private sealed interface Command {
        data object Reset : Command

        data class Chunk(
            val encodedAudio: String,
        ) : Command

        data class PcmChunk(
            val audio: ByteArray,
        ) : Command {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as PcmChunk

                return audio.contentEquals(other.audio)
            }

            override fun hashCode(): Int {
                return audio.contentHashCode()
            }
        }

        data object Finish : Command

        data object Pause : Command

        data object Resume : Command

        data object Release : Command
    }

    private sealed interface WriterCommand {
        data class Chunk(
            val data: ByteArray,
        ) : WriterCommand {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Chunk

                return data.contentEquals(other.data)
            }

            override fun hashCode(): Int {
                return data.contentHashCode()
            }
        }

        data object Finish : WriterCommand

        data object Resume : WriterCommand
    }

    private companion object {
        const val SAMPLE_RATE_HZ = 24_000
        const val BYTES_PER_SAMPLE = 2
        const val ONE_SECOND_PCM_BYTES = SAMPLE_RATE_HZ * BYTES_PER_SAMPLE
        const val PAUSE_POLL_MILLIS = 10L
        const val WRITE_RETRY_MILLIS = 5L
        const val COMPLETION_POLL_MILLIS = 25L
    }
}
