package com.wapo.flagship.features.articles2.models.deserialized.video

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AdSetConfig(
    @Json(name = "adSetUrls")
    val adSetUrls: AdSetUrls?,
    @Json(name = "adSetZone")
    val adSetZone: String?,
)
