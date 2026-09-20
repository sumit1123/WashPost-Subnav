package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.data.datasources.utils.MapConfigParams
import com.washingtonpost.android.config.domain.models.config.PersonalizedPodcastConfig

@JsonClass(generateAdapter = true)
data class RawPersonalizedPodcastConfig(
    @Json(name = "baseUrl") val baseUrl: String? = null,
) {
    fun mapToDomain(params: MapConfigParams): PersonalizedPodcastConfig {
        return PersonalizedPodcastConfig(
            baseUrl = baseUrl ?: "https://data-ai.washingtonpost.com/"
        )
    }
}
