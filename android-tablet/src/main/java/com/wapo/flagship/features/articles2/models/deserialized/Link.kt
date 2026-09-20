package com.wapo.flagship.features.articles2.models.deserialized

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.Item

@JsonClass(generateAdapter = true)
open class Link(
    @Json(name = "subtype")
    open val subtype: String?,
    @Json(name = "type")
    override val type: String?,
    @Json(name = "id")
    open val id: String? = null,
) : Item(type = type)
