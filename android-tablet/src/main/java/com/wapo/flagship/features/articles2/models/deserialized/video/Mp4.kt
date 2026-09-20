package com.wapo.flagship.features.articles2.models.deserialized.video

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Mp4(
    @Json(name = "bitrate")
    val bitrate: Int?,
    @Json(name = "url")
    val url: String?,
)
