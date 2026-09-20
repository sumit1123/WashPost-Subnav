package com.wapo.flagship.features.articles2.models.deserialized.tweet

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Entities(
    @Json(name = "hashtags")
    val hashtags: List<Any>?,
    @Json(name = "media")
    val media: List<Media>?,
    @Json(name = "symbols")
    val symbols: List<Any>?,
    @Json(name = "urls")
    val urls: List<Url>?,
    @Json(name = "user_mentions")
    val userMentions: List<Any>?,
)
