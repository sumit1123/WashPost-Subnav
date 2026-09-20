package com.wapo.flagship.features.articles2.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AudioTracking(
    @Json(name = "id")
    val id: String? = null,
    @Json(name = "name")
    val name: String? = null
)
