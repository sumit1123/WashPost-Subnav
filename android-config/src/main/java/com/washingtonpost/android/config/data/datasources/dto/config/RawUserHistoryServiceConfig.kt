package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.UserHistoryServiceConfig

@JsonClass(generateAdapter = true)
data class RawUserHistoryServiceConfig(
    @Json(name = "url") val url: String? = null,
) {
    fun mapToDomain(): UserHistoryServiceConfig {
        return UserHistoryServiceConfig(
            url = url ?: "https://rte.washingtonpost.com/events/api/v1/",
        )
    }
}
