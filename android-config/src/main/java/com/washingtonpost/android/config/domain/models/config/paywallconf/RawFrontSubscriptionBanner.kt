package com.washingtonpost.android.config.domain.models.config.paywallconf

/**
 * Show the subscribe banner if enabled is true.
 * If the value is missing from the config we want to default to true.
 */
data class FrontSubscriptionBanner(
    val enabled: Boolean? = true
)