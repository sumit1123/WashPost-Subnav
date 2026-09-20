package com.wapo.flagship.features.articles2.models.deserialized

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.Item

@JsonClass(generateAdapter = true)
open class ExpandCollapseCard(
    @Json(name = "type")
    override val type: String?,
    @Json(name = "truncatedLabel")
    val truncatedLabel: String?,
    @Json(name = "expandedLabel")
    val expandedLabel: String?,
) : Item(type = type)
