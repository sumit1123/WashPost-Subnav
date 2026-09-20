package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.LowDataModeConfig

/**
 * Data class that holds the information for a low data mode config
 */
@JsonClass(generateAdapter = true)
data class RawLowDataModeConfig(
    @Json(name = "enable") val enable: Boolean? = null,
    @Json(name = "liteUrlPath") val liteUrlPath: String? = null
) {
    fun mapToDomain(): LowDataModeConfig {
        return LowDataModeConfig(
            enable = enable ?: false,
            liteUrlPath = liteUrlPath ?: "homepage.lite",
        )
    }
}