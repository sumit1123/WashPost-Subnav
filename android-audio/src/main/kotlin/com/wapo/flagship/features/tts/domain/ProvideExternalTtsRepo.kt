/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.tts.domain

import com.wapo.flagship.features.tts.model.ExternalTts

/**
 * Provides a way to get external text-to-speech (TTS) source material,
 * for example, by fetching and parsing an article from a URL.
 */
interface ProvideExternalTtsRepo {
    /**
     * Fetches and processes an external source to provide a list of strings suitable for TTS.
     *
     * @param source The identifier for the external source (e.g., a URL).
     * @return A ExternalTts object.
     * @throws Exception if the source cannot be fetched or parsed.
     */
    suspend fun getExternalTts(source: String): ExternalTts
}
