package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.ProxyApiConfig


@JsonClass(generateAdapter = true)
data class RawProxyApiConfig(
    @Json(name = "enabled") val enabled: Boolean? = null,
    @Json(name = "feedsApiHitCount") val feedsApiHitCount: Int? = null,
    @Json(name = "baseUrl") val baseUrl: String? = null,
) {
    fun mapToDomain(): ProxyApiConfig {
        return ProxyApiConfig(
            enabled = enabled ?: false,
            apiHitCount = feedsApiHitCount ?: 0,
            baseUrl = baseUrl.orEmpty(),
        )
    }
}