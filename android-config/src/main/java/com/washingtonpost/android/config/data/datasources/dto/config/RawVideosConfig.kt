package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.VideosConfig

@JsonClass(generateAdapter = true)
data class RawVideosConfig(
    @Json(name = "mobileMaxBitRate") val mobileMaxBitRate: Int? = null,
    @Json(name = "tabletMaxBitRate") val tabletMaxBitRate: Int? = null,
    @Json(name = "maxConcurrentAutoplays") val maxConcurrentAutoplays: Int? = null
) {
    fun mapToDomain(): VideosConfig {
        return VideosConfig(
            mobileMaxBitRate = mobileMaxBitRate ?: 600,
            tabletMaxBitRate = tabletMaxBitRate ?: 1100,
            maxConcurrentAutoplays = maxConcurrentAutoplays ?: 4
        )
    }
}