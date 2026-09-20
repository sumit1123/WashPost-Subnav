package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.WPVideosConfig

@JsonClass(generateAdapter = true)
data class RawWPVideosConfig(
    @Json(name = "baseUrl") val baseUrl: String? = null,
    @Json(name = "loadCount") val loadCount: Int? = 100,
) {
    fun mapToDomain(): WPVideosConfig {
        return WPVideosConfig(
            baseUrl = baseUrl ?: "https://www.washingtonpost.com/arcio/video-tab/",
            loadCount = loadCount ?: 100,
        )
    }
}