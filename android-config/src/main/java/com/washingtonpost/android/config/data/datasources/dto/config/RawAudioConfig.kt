package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.AudioConfig

@JsonClass(generateAdapter = true)
data class RawAudioConfig(
    @Json(name = "audioAdInterval") val audioAdInterval: Long? = null,
    @Json(name = "fullWidth") val fullWidth: RawImageServiceConfig? = null,
    @Json(name = "thumbnail") val thumbnail: RawImageServiceConfig? = null,
    @Json(name = "audioApiBaseUrl") val audioApiBaseUrl: String? = null,
    @Json(name = "audioDisabledUrls") val audioDisabledUrls: List<String>? = null,
) {
    fun mapToDomain(): AudioConfig {
        return AudioConfig(
            audioAdInterval = audioAdInterval ?: 600000,
            fullWidth = (fullWidth ?: RawImageServiceConfig(1020, 1020)).mapToDomain(),
            thumbnail = (thumbnail ?: RawImageServiceConfig(144, 144)).mapToDomain(),
            audioApiBaseUrl = audioApiBaseUrl ?: "https://api.washingtonpost.com/audio-api/api/v1/",
            audioDisabledUrls = audioDisabledUrls ?: emptyList()
        )
    }
}
