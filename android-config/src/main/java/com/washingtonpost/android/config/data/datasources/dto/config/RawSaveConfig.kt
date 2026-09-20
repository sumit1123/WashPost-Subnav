package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.SaveConfig

@JsonClass(generateAdapter = true)
data class RawSaveConfig(
    @Json(name = "preferenceBaseUrlV2") val preferenceBaseUrl: String? = null,
    @Json(name = "metadataServiceBaseUrl") val metadataServiceBaseUrl: String? = null,
) {
    fun mapToDomain(): SaveConfig {
        return SaveConfig(
            preferenceBaseUrl = preferenceBaseUrl
                ?: "https://subscribe.washingtonpost.com/preferenceapi/v1/saved/story/",
            metadataServiceBaseUrl = metadataServiceBaseUrl
                ?: "https://api.washingtonpost.com/metadata-service/v1/metadata/canonical/"
        )
    }
}