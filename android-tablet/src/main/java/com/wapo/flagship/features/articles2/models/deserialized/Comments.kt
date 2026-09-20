package com.wapo.flagship.features.articles2.models.deserialized

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Comments(
    @Json(name = "subtype")
    override val subtype: String?,
    @Json(name = "type")
    override val type: String?,
) : Link(subtype, type)
