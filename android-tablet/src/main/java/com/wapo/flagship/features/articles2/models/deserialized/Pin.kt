package com.wapo.flagship.features.articles2.models.deserialized

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.Item

@JsonClass(generateAdapter = true)
data class Pin(
    @Json(name = "type")
    override val type: String?,
    @Json(name = "content")
    val content: String?,
) : Item(type = type)
