/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.tts.domain

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * A use case that provides a stream of [TtsState] updates from the repository.
 */
class ObserveTtsStateUseCase @Inject constructor(
    private val ttsRepository: TtsRepository
) {
    /**
     * Returns a Flow that emits the current [TtsState].
     */
    operator fun invoke(): Flow<TtsState> {
        return ttsRepository.observeTtsState()
    }
}
