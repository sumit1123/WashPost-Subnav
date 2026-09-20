// Copyright (c) 2026 The Washington Post. All rights reserved.

package com.wapo.kmpshared.features.feedback.domain

import com.wapo.kmpshared.core.KMPResult
import com.wapo.kmpshared.features.feedback.data.FeedbackService
import com.wapo.kmpshared.features.feedback.data.mapper.toRequestDto
import com.wapo.kmpshared.features.feedback.domain.model.Feedback
import org.koin.core.annotation.Single

@Single
class FeedbackRepository(
    private val service: FeedbackService,
) {
    suspend fun submitFeedback(feedback: Feedback): KMPResult<Unit> = service.submitFeedback(feedback.toRequestDto(), feedback.surface)
}
