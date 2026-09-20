package com.wapo.flagship.features.articles2.models.deserialized.instagram

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class InstagramContent(
    @Json(name = "author_name")
    val authorName: String?,
    @Json(name = "title")
    val title: String?,
)
