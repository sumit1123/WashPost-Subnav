/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.tts.domain

import javax.inject.Inject

class SpeakTextUseCase @Inject constructor(
    private val ttsRepository: TtsRepository
) {
    suspend operator fun invoke(textToSpeak: List<String>, title: String) {
        ttsRepository.speak(textToSpeak, title)
    }
}
