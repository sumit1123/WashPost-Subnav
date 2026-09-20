package com.wapo.flagship.features.articles2.models.deserialized

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Truncate(
    @Json(name = "truncatedLabel")
    val truncatedLabel: String?,
    @Json(name = "expandedLabel")
    val expandedLabel: String?,
    @Json(name = "itemsCount")
    val itemsCount: Int?,
)
