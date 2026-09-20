/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.tts.domain

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * A use case that provides a stream of [OnTtsEvent] updates from the repository.
 */
class ObserveTtsEventsUseCase @Inject constructor(
    private val ttsRepository: TtsRepository
) {
    /**
     * Returns a Flow that emits the current [OnTtsEvent].
     */
    operator fun invoke(): Flow<OnTtsEvent> {
        return ttsRepository.observeTtsEvent()
    }
}
