package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.AirshipConfig

@JsonClass(generateAdapter = true)
data class RawAirshipConfig(
    @Json(name = "openURLPatterns") val openURLPatterns: List<String>? = null,
) {
    fun mapToDomain(): AirshipConfig {
        return AirshipConfig(
            openURLPatterns = openURLPatterns.orEmpty(),
        )
    }
}