package com.wapo.flagship.features.articles2.models.deserialized.tweet

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UrlXX(
    @Json(name = "display_url")
    val displayUrl: String?,
    @Json(name = "expanded_url")
    val expandedUrl: String?,
    @Json(name = "indices")
    val indices: List<Int>?,
    @Json(name = "url")
    val url: String?,
)
