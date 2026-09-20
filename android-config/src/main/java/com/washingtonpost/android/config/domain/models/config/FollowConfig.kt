package com.washingtonpost.android.config.domain.models.config

import com.squareup.moshi.Json

data class FollowConfig(
    @Json(name = "thumbnail") val thumbnail: ImageServiceConfig,
    @Json(name = "authorFollowUrl") val authorFollowUrl: String,
    @Json(name = "authorFollowBaseSyncUrl") val authorFollowBaseSyncUrl: String,
)