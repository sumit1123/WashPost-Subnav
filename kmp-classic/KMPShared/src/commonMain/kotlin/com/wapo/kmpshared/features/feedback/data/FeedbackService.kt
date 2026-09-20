// Copyright (c) 2026 The Washington Post. All rights reserved.

package com.wapo.kmpshared.features.feedback.data

import com.wapo.kmpshared.core.KMPResult
import com.wapo.kmpshared.features.feedback.data.remote.FeedbackRequestDto
import com.wapo.kmpshared.features.feedback.domain.model.FeedbackSurface

/**
 * Service contract for submitting feedback.
 */
interface FeedbackService {
    suspend fun submitFeedback(
        dto: FeedbackRequestDto,
        surface: FeedbackSurface,
    ): KMPResult<Unit>
}
