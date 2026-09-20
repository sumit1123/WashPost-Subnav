package com.wapo.flagship.features.articles2.models.deserialized.video

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AdSetUrlsX(
    @Json(name = "apps")
    val apps: String?,
    @Json(name = "desktop")
    val desktop: String?,
    @Json(name = "mweb")
    val mweb: String?,
    @Json(name = "tablet")
    val tablet: String?,
)
