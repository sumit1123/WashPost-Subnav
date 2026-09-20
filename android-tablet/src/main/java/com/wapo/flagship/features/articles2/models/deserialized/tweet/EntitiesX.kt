package com.wapo.flagship.features.articles2.models.deserialized.tweet

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EntitiesX(
    @Json(name = "description")
    val description: Description?,
    @Json(name = "url")
    val url: UrlX?,
)
