package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.WebArticlesConfig

@JsonClass(generateAdapter = true)
class RawWebArticlesConfig(
    @Json(name = "isWebTypeEnabled") val isWebTypeEnabled: Boolean? = null,
    @Json(name = "minSdk") val minSdk: Int? = null,
) {
    fun mapToDomain(): WebArticlesConfig {
        return WebArticlesConfig(
            isWebTypeEnabled = isWebTypeEnabled ?: false,
            minSdk = minSdk ?: 0,
        )
    }
}