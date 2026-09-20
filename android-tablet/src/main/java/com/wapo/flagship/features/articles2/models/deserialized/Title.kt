package com.wapo.flagship.features.articles2.models.deserialized

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.ElementGroupItem
import com.wapo.flagship.features.articles2.models.Item

@JsonClass(generateAdapter = true)
data class Title(
    @Json(name = "content")
    val content: String?,
    @Json(name = "liveContent")
    val liveContent: String?,
    @Json(name = "mime")
    val mime: String?,
    @Json(name = "subtype")
    val subtype: String?,
    @Json(name = "prefix")
    val prefix: String?,
    @Json(name = "separator")
    val separator: String?,
    @Json(name = "type")
    override val type: String?,
    @Json(name = "style")
    val style: String?,
) : Item(type = type), ElementGroupItem {
    enum class SubType(
        val value: String,
    ) {
        H1("h1"),
        H2("h2"),
        H3("h3"),
        H4("h4"),
        H5("h5"),
        H6("h6"),
        LIVE_REPORTER_INSIGHTS("live-reporter-insight"),
    }

    enum class Style(
        val value: String,
    ) {
        STYLE("style"),
    }
}
