package com.washingtonpost.android.config.domain.models.config.paywall

data class AcquisitionReminderModel(
    val newSubscriber: SubscriberDataModel,
    val terminatedSubscriber: SubscriberDataModel,
    val productId: String,
    val ctaDestination: AcquisitionReminderCtaDestination,
    val enabled: Boolean,
    val frequency: Long,
)

enum class AcquisitionReminderCtaDestination {
    PURCHASE,
    PAYWALL,
}