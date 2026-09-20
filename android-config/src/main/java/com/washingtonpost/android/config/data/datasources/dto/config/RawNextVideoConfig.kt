package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.NextVideoConfig

@JsonClass(generateAdapter = true)
data class RawNextVideoConfig(
    @Json(name = "url") val url: String? = null,
    @Json(name = "loadCount") val loadCount: Int? = 1,
    @Json(name = "percentageLoad") val percentageLoad: Float? = 0.10f,
) {
    fun mapToDomain(): NextVideoConfig {
        return NextVideoConfig(
            url = url ?: "https://www.washingtonpost.com/arcio/video-tab/",
            loadCount = loadCount ?: 1,
            percentageLoad = percentageLoad ?: 0.10f,
        )
    }
}