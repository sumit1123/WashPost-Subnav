package com.wapo.flagship.features.ask.auto

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.Logger
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.auto.ask.AskSpeechRecognizer
import com.wapo.flagship.auto.ask.AskSpeechResult
import com.wapo.flagship.features.ask.speechRecognizerErrorMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

internal class CarAskSpeechRecognizer
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : AskSpeechRecognizer {
        private val recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createRecognitionListener())
        }
        private val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        private val _results = MutableStateFlow<AskSpeechResult?>(null)
        override val results: Flow<AskSpeechResult> = _results.filterNotNull()

        private var silenceJob: Job? = null
        private var initialSilenceJob: Job? = null
        private var silenceTimeoutMillis: Long? = null
        private var latestPartialResult: String? = null
        private var resultDelivered = false
        private var stoppedListening = false
        private var currentAudioSource: ParcelFileDescriptor? = null

        private var sessionToken = 0

        override fun startListening(
            silenceTimeoutMillis: Long,
            initialSilenceTimeoutMillis: Long,
            audioSource: ParcelFileDescriptor?,
        ) {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                audioSource?.close()
                Logger.w(TAG, "Recognition not available")
                _results.value = AskSpeechResult.Failure
                return
            }

            cancelTimers()
            closeAudioSource()
            val session = ++sessionToken
            this.silenceTimeoutMillis = silenceTimeoutMillis
            latestPartialResult = null
            resultDelivered = false
            stoppedListening = false
            currentAudioSource = audioSource

            recognizer.startListening(
                Intent(recognizerIntent).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && audioSource != null) {
                        putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, audioSource)
                        putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, CAR_AUDIO_CHANNEL_COUNT)
                        putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
                        putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, CAR_AUDIO_SAMPLE_RATE_HZ)
                    }
                },
            )

            initialSilenceJob = coroutineScope.launch {
                delay(initialSilenceTimeoutMillis.milliseconds)
                if (session != sessionToken) return@launch
                if (resultDelivered || stoppedListening || latestPartialResult != null) return@launch
                finishWith(AskSpeechResult.Failure)
            }
        }

        override fun stopListening() {
            cancelTimers()
            stoppedListening = true
            recognizer.stopListening()
            closeAudioSource()
        }

        override fun clearResult() {
            _results.value = null
        }

        override fun destroy() {
            cancelTimers()
            closeAudioSource()
            coroutineScope.cancel()
            recognizer.destroy()
        }

        private fun createRecognitionListener(): RecognitionListener =
            object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Logger.d(TAG, "onReadyForSpeech: $params")

                override fun onBeginningOfSpeech() = Logger.d(TAG, "onBeginningOfSpeech")

                override fun onRmsChanged(rmsdB: Float) = Unit

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() = Logger.d(TAG, "onEndOfSpeech")

                override fun onError(error: Int) {
                    cancelTimers()
                    closeAudioSource()
                    if (stoppedListening) {
                        stoppedListening = false
                        Logger.d(TAG, "Cancelled, skipping error handling")
                        return
                    }
                    logError(error)
                    if (resultDelivered) return
                    resultDelivered = true
                    _results.value = AskSpeechResult.Failure
                }

                override fun onResults(results: Bundle) {
                    cancelTimers()
                    closeAudioSource()
                    if (resultDelivered) return
                    resultDelivered = true
                    val data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    Logger.d(TAG, "onResults: $data")
                    _results.value = data?.firstOrNull()?.takeIf { it.isNotBlank() }
                        ?.let { AskSpeechResult.Success(it) }
                        ?: AskSpeechResult.Failure
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val data = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    Logger.d(TAG, "onPartialResults: $data")
                    latestPartialResult = data?.firstOrNull()?.takeIf { it.isNotBlank() } ?: return
                    val timeoutMillis = silenceTimeoutMillis ?: return
                    val session = sessionToken

                    initialSilenceJob?.cancel()
                    silenceJob?.cancel()
                    silenceJob = coroutineScope.launch {
                        delay(timeoutMillis.milliseconds)
                        if (session != sessionToken || resultDelivered) return@launch
                        val recognitionText = latestPartialResult ?: return@launch
                        finishWith(AskSpeechResult.Success(recognitionText))
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Logger.d(TAG, "onEvent: $eventType, $params")
            }

        private fun finishWith(result: AskSpeechResult) {
            resultDelivered = true
            stoppedListening = true
            recognizer.stopListening()
            closeAudioSource()
            _results.value = result
        }

        private fun cancelTimers() {
            silenceJob?.cancel()
            silenceJob = null
            initialSilenceJob?.cancel()
            initialSilenceJob = null
        }

        private fun closeAudioSource() {
            runCatching { currentAudioSource?.close() }
            currentAudioSource = null
        }

        private fun logError(error: Int) {
            val errorMessage = speechRecognizerErrorMessage(error)
            Logger.e(TAG, "onError: $errorMessage")
            EventLog.Builder().apply {
                setMessage("Ask Sam Android Auto speech recognition error")
                setModule(LogModules.ASK_THE_POST)
                setErrorMessage(errorMessage)
            }.run {
                RemoteLog.e(context, build())
            }
        }

        private companion object {
            val TAG = CarAskSpeechRecognizer::class.simpleName
            const val CAR_AUDIO_SAMPLE_RATE_HZ = 16_000
            const val CAR_AUDIO_CHANNEL_COUNT = 1
        }
    }
