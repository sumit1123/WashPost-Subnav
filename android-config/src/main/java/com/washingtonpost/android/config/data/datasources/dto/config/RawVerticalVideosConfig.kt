package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.VerticalVideosConfig

/**
 * Data Config to hold Vertical Videos config values
 */
@JsonClass(generateAdapter = true)
data class RawVerticalVideosConfig(
    @Json(name = "enabled") val enabled: Boolean? = null,
    @Json(name = "adItemInterval") val adItemInterval: Int? = null,
    @Json(name = "firstItemDelay") val firstItemDelay: Int? = null,
    @Json(name = "requestsUiTimeoutMillis") val requestsUiTimeoutMillis: Long? = null,
) {
    fun mapToDomain(): VerticalVideosConfig {
        return VerticalVideosConfig(
            enabled = enabled ?: false,
            adItemInterval = adItemInterval ?: -1,
            firstItemDelay = firstItemDelay ?: -1,
            requestsUiTimeoutMillis = requestsUiTimeoutMillis ?: -1,
        )
    }
}
