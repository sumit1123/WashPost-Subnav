package com.washingtonpost.android.config.domain.models.config.paywall

import com.wapo.android.commons.util.Utils

data class OnboardingReminderModel(
    val newSubscriber: SubscriberDataModel,
    val terminatedSubscriber: SubscriberDataModel,
    val productId: String,
    val ctaDestination: String,
)