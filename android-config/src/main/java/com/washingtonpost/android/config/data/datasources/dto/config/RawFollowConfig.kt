package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.FollowConfig

@JsonClass(generateAdapter = true)
data class RawFollowConfig(
    @Json(name = "thumbnail") val thumbnail: RawImageServiceConfig? = null,
    @Json(name = "authorFollowUrl") val authorFollowUrl: String? = null,
    @Json(name = "authorFollowBaseSyncUrl") val authorFollowBaseSyncUrl: String? = null,
) {
    fun mapToDomain(): FollowConfig {
        return FollowConfig(
            thumbnail = (thumbnail ?: RawImageServiceConfig(100, 100)).mapToDomain(),
            authorFollowUrl = authorFollowUrl
                ?: "https://www.washingtonpost.com/arcio/classic-author/%s/?size=%d&from=%d",
            authorFollowBaseSyncUrl = authorFollowBaseSyncUrl
                ?: "https://subscribe.washingtonpost.com",
        )
    }
}
