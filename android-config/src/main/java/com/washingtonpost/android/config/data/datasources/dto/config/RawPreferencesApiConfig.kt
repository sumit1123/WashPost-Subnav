package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.PreferencesApiConfig

@JsonClass(generateAdapter = true)
data class RawPreferencesApiConfig(
    @Json(name = "preferenceBaseUrl") val preferenceBaseUrl: String? = null,
    @Json(name = "checkPrefBaseUrl") val checkPrefBaseUrl: String? = null
) {
    fun mapToDomain(): PreferencesApiConfig {
        return PreferencesApiConfig(
            preferenceBaseUrl = preferenceBaseUrl ?: "https://subscribe.washingtonpost.com/",
            checkPrefBaseUrl = checkPrefBaseUrl ?: "https://checkpref.washingtonpost.com/",
        )
    }
}
