/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.tts.domain

import javax.inject.Inject

/**
 * A use case to stop the current Text-to-Speech playback.
 */
class StopTtsUseCase @Inject constructor(
    private val ttsRepository: TtsRepository
) {
    /**
     * Stops the TTS engine from speaking.
     */
    operator fun invoke() {
        ttsRepository.stop()
    }
}
