package com.wapo.flagship.features.articles2.models.deserialized

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Anchor(
    @Json(name = "subtype")
    override val subtype: String?,
    @Json(name = "type")
    override val type: String?,
    @Json(name = "id")
    override val id: String?,
) : Link(subtype, type, id)
