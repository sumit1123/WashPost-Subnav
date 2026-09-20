package com.wapo.flagship.features.articles2.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Represents a list of titles/anchor links for live-update articles
 */
@JsonClass(generateAdapter = true)
data class TableOfContents(
    @Json(name = "liveText")
    val liveText: String? = null,
    @Json(name = "children")
    val children: List<LiveEntry>? = null,
)

@JsonClass(generateAdapter = true)
data class LiveEntry(
    @Json(name = "title")
    val title: String? = null,
    @Json(name = "arcId")
    val arcId: String? = null,
    @Json(name = "anchor")
    val anchor: String? = null,
    @Json(name = "displayDate")
    val displayDate: Long? = null,
)
