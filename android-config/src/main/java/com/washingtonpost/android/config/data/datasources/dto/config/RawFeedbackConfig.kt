package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.FeedbackConfig

@JsonClass(generateAdapter = true)
data class RawFeedbackConfig (
    @Json(name = "baseUrl") val baseUrl: String? = null,
) {
    fun mapToDomain(): FeedbackConfig {
        return FeedbackConfig(
            baseUrl = baseUrl
                ?: "https://data-ai.washingtonpost.com/feedback-service/api/v1/",
        )
    }
}
