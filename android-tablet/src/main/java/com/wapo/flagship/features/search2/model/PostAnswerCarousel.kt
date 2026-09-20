package com.wapo.flagship.features.search2.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PostAnswerCarousel(
    @Json(name = "type")
    override val type: String?,
    @Json(name = "subtype")
    override val subtype: String?,
    @Json(name = "items")
    val items: List<PostAnswerCarouselItem>?,
) : PostAnswerItem(
        type = type,
        subtype = subtype,
    )

@JsonClass(generateAdapter = true)
data class PostAnswerCarouselItem(
    val id: Int = Int.hashCode(),
    @Json(name = "url")
    val url: String?,
    @Json(name = "publish_date")
    val publishDate: Long?,
    @Json(name = "content")
    val content: String?,
    @Json(name = "image")
    val image: String?,
    @Json(name = "passages")
    val passages: List<String>?,
)
