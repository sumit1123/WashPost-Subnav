/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.tts.data

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

sealed class TtsEvent(open val utteranceId: String) {
    data class OnStart(override val utteranceId: String) : TtsEvent(utteranceId)
    data class OnDone(override val utteranceId: String) : TtsEvent(utteranceId)
    data class OnError(override val utteranceId: String, val error: String) : TtsEvent(utteranceId)
    data class OnProgress(override val utteranceId: String, val progress: Float) : TtsEvent(utteranceId)
}

class TtsEngineSource @Inject constructor(context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private var onInitListener: Listener? = null

    val ttsEvents: Flow<TtsEvent> = callbackFlow {
        val listener = object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                launch {
                    trySend(TtsEvent.OnStart(utteranceId))
                }
            }

            override fun onDone(utteranceId: String) {
                launch {
                    trySend(TtsEvent.OnDone(utteranceId))
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                launch {
                    trySend(TtsEvent.OnError(utteranceId ?: "", "Unknown TTS error"))
                }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                val errorMsg = "TTS Error ($utteranceId): code $errorCode"
                launch {
                    trySend(TtsEvent.OnError(utteranceId ?: "", errorMsg))
                }
            }

            override fun onRangeStart(utteranceId: String, start: Int, end: Int, frame: Int) {
                // We can calculate progress here if needed, but it's often unreliable.
                // A timer-based approach in the repository is more consistent.
            }
        }

        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                onInitListener?.onInitReady()
                tts?.language = Locale.US
                tts?.setOnUtteranceProgressListener(listener)
            } else {
                launch {
                    trySend(TtsEvent.OnError("", "TTS initialization failed"))
                }
            }
        }
        tts?.setSpeechRate(1.0f)

        awaitClose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    fun speak(text: String, utteranceId: String) {
        if (!isInitialized) {
            onInitListener = object : Listener {
                override fun onInitReady() {
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                    onInitListener = null
                }
            }
        } else {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    fun stop() {
        tts?.stop()
    }

    interface Listener {
        fun onInitReady()
    }
}
