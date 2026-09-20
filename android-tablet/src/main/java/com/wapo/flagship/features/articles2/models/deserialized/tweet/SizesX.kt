package com.wapo.flagship.features.articles2.models.deserialized.tweet

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SizesX(
    @Json(name = "large")
    val large: LargeX?,
    @Json(name = "medium")
    val medium: MediumX?,
    @Json(name = "small")
    val small: SmallX?,
    @Json(name = "thumb")
    val thumb: ThumbX?,
)
