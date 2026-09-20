package com.wapo.flagship.features.ask

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.Logger
import com.wapo.android.remotelog.logger.RemoteLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

class TalkToThePostSpeechRecManager @Inject constructor(@ApplicationContext private val context: Context) {
    private val recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
        setRecognitionListener(createRecognitionListener())
    }
    private val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
    }

    private val _voiceResult: MutableStateFlow<TalkToThePostVoiceResult?> = MutableStateFlow(null)
    val voiceResult: MutableStateFlow<TalkToThePostVoiceResult?> = _voiceResult

    private val _inputRms = MutableStateFlow(Float.MIN_VALUE)
    val inputRms = _inputRms

    private var stoppedListening = false
    fun startListening() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            recognizer.startListening(recognizerIntent)
        } else {
            Logger.w(TAG, "Recognition not available")
        }
    }

    fun stopListening() {
        stoppedListening = true
        recognizer.stopListening()
    }

    fun destroyRecognizer() {
        recognizer.destroy()
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(p0: Bundle?) {
                Logger.d(TAG, "onReadyForSpeech: $p0")
            }

            override fun onBeginningOfSpeech() {
                Logger.d(TAG, "onBeginningOfSpeech")
            }

            /**
             * RMS stands for Root Mean Square, and is a way to measure changes in input volume.
             */
            override fun onRmsChanged(p0: Float) {
                _inputRms.value = p0
            }

            override fun onBufferReceived(p0: ByteArray?) {
                Logger.d(TAG, "onBufferReceived: $p0")
            }

            override fun onEndOfSpeech() {
                Logger.d(TAG, "onEndOfSpeech")
            }

            override fun onError(p0: Int) {
                if (stoppedListening) {
                    stoppedListening = false
                    Logger.d(TAG, "Cancelled, skipping error handling")
                    return
                }
                val errorMessage = speechRecognizerErrorMessage(p0)

                logError(errorMessage)
                _voiceResult.value = TalkToThePostVoiceResult.Failure(p0)
            }

            override fun onResults(results: Bundle) {
                val data: ArrayList<String>? = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                data?.firstOrNull()?.let { recognitionText ->
                    _voiceResult.value = TalkToThePostVoiceResult.Success(recognitionText)
                } ?: run {
                    _voiceResult.value = TalkToThePostVoiceResult.Failure(SpeechRecognizer.ERROR_NO_MATCH)
                }
                Logger.d(TAG, "onResults: $data")
            }

            override fun onPartialResults(p0: Bundle?) {
                val data: ArrayList<String>? = p0?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                Logger.d(TAG, "onPartialResults: $data")
            }

            override fun onEvent(p0: Int, p1: Bundle?) {
                Logger.d(TAG, "onEvent: $p0, $p1")
            }
        }
    }

    private fun logError(errorMessage: String) {
        Logger.e(TAG, "onError: $errorMessage")
        EventLog.Builder().apply {
            setMessage("Ask Sam speech recognition error")
            setModule(LogModules.ASK_THE_POST)
            setErrorMessage(errorMessage)
        }.run {
            RemoteLog.e(context, build())
        }
    }

    fun clearQuestion() {
        _voiceResult.value = null
    }

    companion object {
        val TAG = TalkToThePostSpeechRecManager::class.simpleName
        const val MAX_ERROR_COUNT = 3
    }
}

/**
 * Maps a [SpeechRecognizer] error code to a human readable message for logging.
 *
 * Shared by the phone flow and the Android Auto flow so the two stay in sync.
 *
 * Error code documentation:
 * https://developer.android.com/reference/android/speech/SpeechRecognizer#summary
 */
internal fun speechRecognizerErrorMessage(error: Int): String = when (error) {
    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
    SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT -> "The service does not allow to check for support."
    SpeechRecognizer.ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS -> "The service does not support listening to model downloads events."
    SpeechRecognizer.ERROR_CLIENT -> "Other client side errors."
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Requested language is not available to be used with the current recognizer."
    SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Requested language is supported, but not available currently (e.g. not downloaded yet)."
    SpeechRecognizer.ERROR_NETWORK -> "Other network related errors."
    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network operation timed out."
    SpeechRecognizer.ERROR_NO_MATCH -> "No recognition result matched."
    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy."
    SpeechRecognizer.ERROR_SERVER -> "Server sends error status."
    SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "Server has been disconnected, e.g. because the app has crashed."
    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
    SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Too many requests from the same client."
    else -> "Unknown error"
}