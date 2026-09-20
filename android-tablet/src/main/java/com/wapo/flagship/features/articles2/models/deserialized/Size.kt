package com.wapo.flagship.features.articles2.models.deserialized

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Size(
    @Json(name = "width")
    val width: Int?,
    @Json(name = "height")
    val height: Int?,
)
