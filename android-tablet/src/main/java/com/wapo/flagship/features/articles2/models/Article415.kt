package com.wapo.flagship.features.articles2.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Article415(
    @Json(name = "contenturl")
    val contentUrl: String? = null,
    @Json(name = "reason")
    val message: String? = null,
)
