package com.wapo.flagship.features.ask

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.AudioTrack.STATE_INITIALIZED
import android.media.AudioTrack.STATE_UNINITIALIZED
import android.media.MediaPlayer
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.ask.viewmodels.TalkToThePostViewModel.ErrorType
import com.wapo.flagship.features.settings.ASK_SAM_JUNIPER_ID
import com.wapo.flagship.util.PrefUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.sqrt

class TalkToThePostAudioManager @Inject constructor(@ApplicationContext private val context: Context) {

    val audioFocusListener = TalkToThePostAudioFocusListener(
        context = context,
        onAbandonFocus = { pauseAllAudio() }
    )

    fun requestAudioFocus() {
        audioFocusListener.requestAudioFocus()
    }

    fun abandonAudioFocus() {
        audioFocusListener.abandonAudioFocus()
    }

    private val _outputRms = MutableStateFlow(Float.MIN_VALUE)
    val outputRms = _outputRms

    private var minBufferSize = AudioTrack.getMinBufferSize(
        SAMPLE_RATE_24_KHZ,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ) * 2

    private fun getAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
    }

    private fun getAudioFormat(): AudioFormat {
        return AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(SAMPLE_RATE_24_KHZ)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
    }

    var answerAudioTrack = AudioTrack(
        getAudioAttributes(),
        getAudioFormat(),
        minBufferSize,
        AudioTrack.MODE_STREAM,
        0
    )

    fun playAnswerAudio() {
        requestAudioFocus()
        answerAudioTrack.play()
    }

    fun resetAnswerTrack() {
        if (answerAudioTrack.state == STATE_UNINITIALIZED) return
        answerAudioTrack.setPlaybackPositionUpdateListener(null)
        answerAudioTrack.pause()
        answerAudioTrack.flush()
        answerBytesWritten = 0
        answerAudioTrack.playbackHeadPosition = 0
        answerAudioTrack.notificationMarkerPosition = 0
    }

    var messageAudioTrack = AudioTrack(
        getAudioAttributes(),
        getAudioFormat(),
        minBufferSize,
        AudioTrack.MODE_STREAM,
        0
    )

    fun playMessageAudio() {
        requestAudioFocus()
        messageAudioTrack.play()
    }

    fun resetMessageTrack() {
        if (messageAudioTrack.state == STATE_UNINITIALIZED) return
        messageAudioTrack.setPlaybackPositionUpdateListener(null)
        messageAudioTrack.pause()
        messageAudioTrack.flush()
        messageBytesWritten = 0
        messageAudioTrack.notificationMarkerPosition = 0
    }

    private var answerBytesWritten = 0
    @OptIn(ExperimentalEncodingApi::class)
    fun writeAnswerChunk(encodedAudioData: String) {
        val audioData = Base64.decode(encodedAudioData)
        answerBytesWritten += answerAudioTrack.write(audioData, 0, audioData.size)
        Logger.d(TAG, "buffer size: ${minBufferSize}, answerBytesWritten: $answerBytesWritten")
        _outputRms.value = calculateRms(audioData)
    }

    private fun calculateRms(audioData: ByteArray): Float {
        var sum = 0f
        var sampleCount = 0
        var i = 0
        while (i < audioData.size - 1) {
            val sample = ((audioData[i + 1].toInt() shl 8) or (audioData[i].toInt() and 0xFF)).toShort()
            sum += (sample * sample)
            sampleCount++
            i += 2
        }
        val meanSquare = if (sampleCount > 0) sum / sampleCount else 0f
        return sqrt(meanSquare)
    }

    fun setAnswerNotificationMarkerPosition() {
        Logger.d(TAG, "marker position set to $answerBytesWritten")
        answerAudioTrack.notificationMarkerPosition = answerBytesWritten / 2
    }

    private var errorMessages = getNewErrorMessagesList()
    private fun getNewErrorMessagesList(): MutableList<String>? {
        return context.assets.list(getErrorMessagePoolDirectory())?.toMutableList()
    }
    var recognitionErrorCount = 0
    fun writeErrorMessageToAudioTrack(errorType: ErrorType) {
        when (errorType) {
            ErrorType.RECOGNITION_ERROR -> {
                val fileToPlay = errorMessages?.randomOrNull()
                fileToPlay ?: return
                errorMessages?.remove(fileToPlay)
                writeMessageToAudioTrack("${getErrorMessagePoolDirectory()}/$fileToPlay")
                recognitionErrorCount++
            }
            ErrorType.RECOGNITION_ERROR_MAX -> {
                writeMessageToAudioTrack(getErrorMaxFilePath())
            }
            ErrorType.API_ERROR -> {
                writeMessageToAudioTrack(getApiFailsFilePath())
            }
        }
    }

    fun writeOnboardingMessageToAudioTrack() {
        writeMessageToAudioTrack(getOnboardingFilePath())
    }

    private var messageBytesWritten = 0
    private fun writeMessageToAudioTrack(filePath: String) {
        val inputStream = context.assets.open(filePath)
        val bytes = inputStream.readBytes()
        inputStream.close()
        messageBytesWritten += messageAudioTrack.write(bytes, 0, bytes.size)
        messageAudioTrack.notificationMarkerPosition = messageBytesWritten / 2
    }

    private val mp3Player by lazy { MediaPlayer() }

    fun playVoiceSelectionMessage(voiceId: String) {
        var afd: android.content.res.AssetFileDescriptor? = null
        try {
            mp3Player.reset()
            afd = context.assets.openFd(getVoiceSelectionFilePath(voiceId))
            mp3Player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            mp3Player.prepare()
            mp3Player.start()
        } catch (e: Exception) {
            Logger.e(TAG, "Exception while playing voice selection message", e)
            mp3Player.reset()
        } finally {
            try {
                afd?.close()
            } catch (e: Exception) {
                Logger.e(TAG, "Exception while closing AssetFileDescriptor", e)
            }
        }
    }

    fun pauseAllAudio() {
        pauseIfInitialized(answerAudioTrack, "answer")
        pauseIfInitialized(messageAudioTrack, "message")
        try {
            if (mp3Player.isPlaying) {
                mp3Player.pause()
            }
        } catch (e: IllegalStateException) {
            Logger.e(TAG, "Exception while trying to pause mp3Player", e)
        }
    }

    private fun pauseIfInitialized(audioTrack: AudioTrack, trackName: String) {
        if (audioTrack.state != STATE_INITIALIZED) return
        try {
            audioTrack.pause()
        } catch (error: IllegalStateException) {
            Logger.e(TAG, "Unable to pause $trackName AudioTrack", error)
        }
    }

    fun releaseAudioPlayers() {
        if (answerAudioTrack.state == STATE_INITIALIZED) {
            answerAudioTrack.flush()
            answerAudioTrack.stop()
            answerAudioTrack.release()
        }

        if (messageAudioTrack.state == STATE_INITIALIZED) {
            messageAudioTrack.flush()
            messageAudioTrack.stop()
            messageAudioTrack.release()
        }

        try {
            mp3Player.release()
        } catch (e: Exception) {
            Logger.e(TAG, "Exception while releasing mp3Player", e)
        }
    }

    private fun getVoiceId(): String {
        return PrefUtils.getTalkToThePostVoiceSelection(context) ?: ASK_SAM_JUNIPER_ID
    }

    private fun getBasePathWithVoiceId(): String {
        return "$BASE_PATH/${getVoiceId()}"
    }

    private fun getOnboardingFilePath(): String {
        return "${getBasePathWithVoiceId()}/$ONBOARDING_PCM"
    }

    private fun getVoiceSelectionFilePath(voiceId: String): String {
        return "$BASE_PATH/$voiceId/$VOICE_SELECTION_MP3"
    }

    private fun getErrorMessagePoolDirectory(): String {
        return "${getBasePathWithVoiceId()}/$ERROR_MESSAGE_POOL"
    }

    private fun getErrorMaxFilePath(): String {
        return "${getBasePathWithVoiceId()}/$ERROR_MAX_PCM"
    }

    private fun getApiFailsFilePath(): String {
        return "${getBasePathWithVoiceId()}/$API_FAILS_PCM"
    }

    companion object {
        val TAG = TalkToThePostAudioManager::class.simpleName
        const val SAMPLE_RATE_24_KHZ = 24000
        const val BASE_PATH = "ask-sam-audio-messages"
        const val ONBOARDING_PCM = "onboarding.pcm"
        const val VOICE_SELECTION_MP3 = "voice_selection.mp3"
        const val ERROR_MESSAGE_POOL = "error-message-pool"
        const val ERROR_MAX_PCM = "error_max.pcm"
        const val API_FAILS_PCM = "api_fails.pcm"
    }
}
