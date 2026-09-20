package com.washingtonpost.android.config.data.datasources.dto.config.paywallconf

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.paywallconf.FrontSubscriptionBanner

/**
 * Show the subscribe banner if enabled is true.
 * If the value is missing from the config we want to default to true.
 */
@JsonClass(generateAdapter = true)
data class RawFrontSubscriptionBanner(
    @Json(name = "enabled") val enabled: Boolean? = null,
) {
    fun mapToDomain(): FrontSubscriptionBanner {
        return FrontSubscriptionBanner(
            enabled = enabled ?: true,
        )
    }
}