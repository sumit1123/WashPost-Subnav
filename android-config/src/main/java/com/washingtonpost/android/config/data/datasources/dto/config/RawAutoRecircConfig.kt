package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.AutoRecircConfig

@JsonClass(generateAdapter = true)
data class RawAutoRecircConfig(
    @Json(name = "baseUrl") val baseUrl: String? = null,
) {
    fun mapToDomain(): AutoRecircConfig {
        return AutoRecircConfig(
            baseUrl = baseUrl ?: "https://data-ai.washingtonpost.com/collections/api/v1/"
        )
    }
}