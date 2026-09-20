package com.wapo.flagship.features.articles2.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Question(
    @Json(name = "type")
    val type: String? = null,
    @Json(name = "label")
    val label: String? = null,
    @Json(name = "text")
    val text: String? = null,
)
