package com.wapo.flagship.features.articles2.models.deserialized

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.Item

@JsonClass(generateAdapter = true)
data class Date(
    @Json(name = "content")
    val content: Long?,
    @Json(name = "mime")
    val mime: String?,
    @Json(name = "subtype")
    val subtype: String?,
    @Json(name = "recency_threshold")
    val recencyThreshold: Long?,
    @Json(name = "type")
    override val type: String?,
) : Item(type = type) {
    enum class SubType(
        val value: String,
    ) {
        LIVE_UPDATE("live-update"),
        LIVE_REPORTER_INSIGHT("live-reporter-insight"),
        EXPANDED_BYLINE("expanded-byline"),
    }
}
