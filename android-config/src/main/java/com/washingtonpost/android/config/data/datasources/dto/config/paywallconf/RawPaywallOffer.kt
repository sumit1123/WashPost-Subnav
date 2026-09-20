package com.washingtonpost.android.config.data.datasources.dto.config.paywallconf

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.paywallconf.PaywallOffer

/**
 * The higher the weight, the higher the priority of the offer.
 */
@JsonClass(generateAdapter = true)
data class RawPaywallOffer(
    @Json(name = "offerId") val offerId: String? = null,
    @Json(name = "weight") val weight: Int? = null,
) {
    fun mapToDomain(): PaywallOffer {
        return PaywallOffer(
            offerId = offerId,
            weight = weight,
        )
    }
}