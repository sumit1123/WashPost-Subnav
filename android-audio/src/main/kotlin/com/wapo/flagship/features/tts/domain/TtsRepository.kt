/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.tts.domain

import kotlinx.coroutines.flow.Flow

/**
 * Represents the state of the Text-to-Speech engine.
 *
 * @param isSpeaking True if the engine is currently speaking an utterance.
 * @param isPause True if the engine is paused.
 * @param isInitializing True if the engine is being initialized.
 * @param progress The progress of the current utterance (0.0 to 1.0).
 * @param error An optional error message if something went wrong.
 * @param currentUtteranceId The ID of the text currently being spoken.
 */
data class TtsState(
    val isSpeaking: Boolean = false,
    val isPause: Boolean = false,
    val isInitializing: Boolean = true,
    val progress: Float = 0f,
    val error: String? = null,
    val currentUtteranceId: String? = null
)

sealed class OnTtsEvent {
    data object None : OnTtsEvent()
    data class OnTtsError(val errorId: Int) : OnTtsEvent()
}

/**
 * Defines the contract for a Text-to-Speech repository.
 */
interface TtsRepository {
    /**
     * Initializes the engine and starts speaking the given list of text chunks in a queue.
     *
     * @param textToSpeak The list of text chunks to speak.
     * @param title title for notification.
     */
    fun speak(textToSpeak: List<String>, title: String?)

    /**
     * Stops the current speech.
     */
    fun stop()

    fun pause()

    fun resume()

    fun error(error: String)

    /**
     * Observes the state of the TTS engine.
     */
    fun observeTtsState(): Flow<TtsState>

    /**
     * Observes the state of the TTS engine.
     */
    fun observeTtsEvent(): Flow<OnTtsEvent>

    /**
     * Shuts down the TTS engine and releases resources.
     */
    fun shutdown()
}
