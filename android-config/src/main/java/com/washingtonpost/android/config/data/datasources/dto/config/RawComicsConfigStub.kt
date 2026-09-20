package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.ComicsConfigStub

@JsonClass(generateAdapter = true)
data class RawComicsConfigStub(
    @Json(name = "url") val endpointURL: String? = null,
) {
    fun mapToDomain(): ComicsConfigStub {
        return ComicsConfigStub(
            endpointURL = endpointURL ?: "https://comics-api.wpdigital.net"
        )
    }
}