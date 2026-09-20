package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.ReadingHistoryServiceConfig

@JsonClass(generateAdapter = true)
data class RawReadingHistoryServiceConfig(
    @Json(name = "url") val url: String? = null,
) {
    fun mapToDomain(): ReadingHistoryServiceConfig {
        return ReadingHistoryServiceConfig(
            url = url ?: "https://subscribe.washingtonpost.com/",
        )
    }
}
