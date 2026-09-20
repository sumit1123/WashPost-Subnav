package com.wapo.flagship.features.articles2.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.deserialized.Image

@JsonClass(generateAdapter = true)
data class LiveOutcome(
    @Json(name = "headline")
    val headline: Headline?,
    @Json(name = "image")
    val image: Image?,
    @Json(name = "subHeadline")
    val subHeadline: SubHeadline?,
    @Json(name = "type")
    override val type: String?,
) : Item(type = type),
    ElementGroupItem

@JsonClass(generateAdapter = true)
data class Headline(
    val content: String?,
)

@JsonClass(generateAdapter = true)
data class SubHeadline(
    val content: String?,
    val subtype: String?,
)
