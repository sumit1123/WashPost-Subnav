package com.wapo.flagship.features.articles2.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.deserialized.Size

@JsonClass(generateAdapter = true)
data class SubItem(
    @Json(name = "type")
    val type: String?,
    @Json(name = "subtype")
    val subtype: String?,
    @Json(name = "url")
    val url: String?,
    @Json(name = "sizes")
    val sizes: List<Size>?,
    @Json(name = "widthFactor")
    val widthFactor: String?,
)
