package com.wapo.flagship.features.feedback.domain

import com.wapo.kmpshared.features.feedback.domain.FeedbackRepository

interface FeedbackRepositoryProvider {
    val repository: FeedbackRepository?

    val isEnabled: Boolean
        get() = repository != null
}
