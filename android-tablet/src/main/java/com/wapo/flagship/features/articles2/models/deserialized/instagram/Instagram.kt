package com.wapo.flagship.features.articles2.models.deserialized.instagram

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.Item

@JsonClass(generateAdapter = true)
data class Instagram(
    @Json(name = "content")
    val instagramContent: InstagramContent?,
    @Json(name = "type")
    override val type: String?,
    @Json(name = "imageURL")
    val imageURL: String?,
) : Item(type = type)
