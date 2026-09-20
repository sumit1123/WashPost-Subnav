package com.wapo.flagship.features.articles2.models.deserialized

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.Item

@JsonClass(generateAdapter = true)
data class Toggle(
    @Json(name = "type")
    override val type: String?,
    @Json(name = "subtype")
    val subtype: String?,
    @Json(name = "mime")
    val mime: String?,
    @Json(name = "key")
    val key: String?,
) : Item(type = type)
