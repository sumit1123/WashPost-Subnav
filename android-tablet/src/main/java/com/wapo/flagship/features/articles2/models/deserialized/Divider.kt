package com.wapo.flagship.features.articles2.models.deserialized

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.Item

@JsonClass(generateAdapter = true)
data class Divider(
    @Json(name = "subtype")
    val subtype: String?,
    @Json(name = "type")
    override val type: String?,
) : Item(type)
