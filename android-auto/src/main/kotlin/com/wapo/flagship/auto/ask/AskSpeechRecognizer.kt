package com.wapo.flagship.auto.ask

import android.os.ParcelFileDescriptor
import kotlinx.coroutines.flow.Flow

interface AskSpeechRecognizer {
    val results: Flow<AskSpeechResult>

    fun startListening(
        silenceTimeoutMillis: Long,
        initialSilenceTimeoutMillis: Long,
        audioSource: ParcelFileDescriptor?,
    )

    fun stopListening()

    fun clearResult()

    fun destroy()
}

sealed interface AskSpeechResult {
    data class Success(
        val text: String,
    ) : AskSpeechResult

    data object Failure : AskSpeechResult
}
