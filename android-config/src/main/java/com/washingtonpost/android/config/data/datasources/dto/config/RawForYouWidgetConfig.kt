package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.ForYouWidgetConfig

@JsonClass(generateAdapter = true)
data class RawForYouWidgetConfig(
    @Json(name = "ttls") val ttls: Long? = null,
    @Json(name = "maxSize") val maxSize: Int? = null,
    @Json(name = "pageSize") val pageSize: Int? = null
) {
    fun mapToDomain(): ForYouWidgetConfig {
        return ForYouWidgetConfig(
            ttls = ttls ?: 3600L,
            maxSize = maxSize ?: 24,
            pageSize = pageSize ?: 8,
        )
    }
}
