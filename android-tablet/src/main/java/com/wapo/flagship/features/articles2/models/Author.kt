package com.wapo.flagship.features.articles2.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Author(
    @Json(name = "id")
    val id: String? = null,
    @Json(name = "url")
    val url: String? = null,
    @Json(name = "name")
    val name: String? = null,
    @Json(name = "bio")
    val bio: String? = null,
    @Json(name = "expertise")
    val expertise: String? = null,
    @Json(name = "image")
    val image: String? = null,
)
