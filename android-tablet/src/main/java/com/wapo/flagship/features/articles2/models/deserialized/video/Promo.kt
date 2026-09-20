package com.wapo.flagship.features.articles2.models.deserialized.video

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Promo(
    @Json(name = "isLooping")
    val isLooping: Boolean?,
    @Json(name = "url")
    val url: String?,
)
