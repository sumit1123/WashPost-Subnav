package com.wapo.flagship.features.articles2.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SubNav(
    @Json(name = "type")
    override val type: String?,
    @Json(name = "siteMap")
    val siteMap: String?,
    @Json(name = "parent")
    val parent: String?,
    @Json(name = "item")
    val subItem: SubItem?,
) : Item(type = type),
    ElementGroupItem
