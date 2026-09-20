package com.wapo.flagship.features.articles2.models.deserialized.tweet

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Small(
    @Json(name = "h")
    val h: Int?,
    @Json(name = "resize")
    val resize: String?,
    @Json(name = "w")
    val w: Int?,
)
