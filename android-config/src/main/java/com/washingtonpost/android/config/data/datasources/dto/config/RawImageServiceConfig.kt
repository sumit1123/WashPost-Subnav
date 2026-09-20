package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.ImageServiceConfig

@JsonClass(generateAdapter = true)
data class RawImageServiceConfig(
    @Json(name = "imgHeight") val imgHeight: Int? = null,
    @Json(name = "imgWidth") val imgWidth: Int? = null,
) {
    fun mapToDomain(): ImageServiceConfig {
        return ImageServiceConfig(
            imgHeight = imgHeight ?: 0,
            imgWidth = imgWidth ?: 0,
        )
    }
}