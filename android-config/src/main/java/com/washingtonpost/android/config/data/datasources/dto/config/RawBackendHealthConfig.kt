package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.BackendHealthConfig

@JsonClass(generateAdapter = true)
data class RawBackendHealthConfig(
    @Json(name = "backendHealthMonitorURL") val backendHealthMonitorURL: String? = null,
    @Json(name = "fallbackURL") val fallbackURL: String? = null,
    @Json(name = "fallbackStaticURL") val fallbackStaticURL: String? = null,
) {
    fun mapToDomain(): BackendHealthConfig {
        return BackendHealthConfig(
            backendHealthMonitorURL = backendHealthMonitorURL ?: "",
            fallbackURL = fallbackURL ?: "https://www.washingtonpost.com",
            fallbackStaticURL = fallbackStaticURL.orEmpty(),
        )
    }
}