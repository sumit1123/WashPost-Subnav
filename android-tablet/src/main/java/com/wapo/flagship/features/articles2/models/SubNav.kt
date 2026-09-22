package com.wapo.flagship.features.articles2.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SubNav(
    @Json(name = "type")
    override val type: String?,
    @Json(name = "url")
    val url: String? = null,
    /**
     * The strip's own web embed. Only its `sizes` are read: they give the panel below the strip a
     * fixed viewport, without which a tab pointing at a full page (e.g. live updates) renders at
     * the page's whole height.
     */
    @Json(name = "item")
    val subItem: SubItem? = null,
) : Item(type = type),
    ElementGroupItem
