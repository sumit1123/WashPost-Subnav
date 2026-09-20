package com.wapo.flagship.features.articles2.models.deserialized

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Omniture(
    @Json(name = "channel")
    val channel: String?,
    @Json(name = "contentId")
    val contentId: String?,
    @Json(name = "contentSubsection")
    val contentSubsection: String?,
    @Json(name = "contentType")
    val contentType: String?,
    @Json(name = "pageName")
    val pageName: String?,
    @Json(name = "source")
    val source: String?,
)
