package com.wapo.flagship.features.articles2.models.deserialized.tweet

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UrlX(
    @Json(name = "urls")
    val urls: List<UrlXX>?,
)
