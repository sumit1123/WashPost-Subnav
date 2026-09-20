package com.wapo.flagship.features.articles2.models.deserialized.tweet

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Sizes(
    @Json(name = "large")
    val large: Large?,
    @Json(name = "medium")
    val medium: Medium?,
    @Json(name = "small")
    val small: Small?,
    @Json(name = "thumb")
    val thumb: Thumb?,
)
