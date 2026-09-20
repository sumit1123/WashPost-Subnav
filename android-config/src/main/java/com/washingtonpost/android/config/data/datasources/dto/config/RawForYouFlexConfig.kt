package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.ForYouFlexConfig

@JsonClass(generateAdapter = true)
data class RawForYouFlexConfig(
    @Json(name = "url") val url: String? = null,
    @Json(name = "ttl") val ttl: Long? = null,
    @Json(name = "checkReadList") val checkReadList: Boolean? = null,
    @Json(name = "maxSize") val maxSize: Int? = null,
) {
    fun mapToDomain(): ForYouFlexConfig {
        return ForYouFlexConfig(
            url = url ?: "https://foryou-flex.washingtonpost.com/",
            ttl = ttl ?: 300,
            checkReadList = checkReadList ?: true,
            maxSize = maxSize ?: 100,
        )
    }
}
