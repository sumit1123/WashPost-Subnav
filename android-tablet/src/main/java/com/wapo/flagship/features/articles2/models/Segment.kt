package com.wapo.flagship.features.articles2.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Segment(
    @Json(name = "key")
    val key: String? = null,
    @Json(name = "values")
    val values: Any? = null,
)
