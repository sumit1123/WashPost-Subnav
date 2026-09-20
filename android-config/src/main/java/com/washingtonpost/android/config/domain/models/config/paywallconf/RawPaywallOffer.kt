package com.washingtonpost.android.config.domain.models.config.paywallconf

/**
 * The higher the weight, the higher the priority of the offer.
 */
data class PaywallOffer(
    val offerId: String?,
    val weight: Int?
)