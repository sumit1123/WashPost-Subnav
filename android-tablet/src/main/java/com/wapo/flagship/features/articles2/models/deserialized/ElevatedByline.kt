package com.wapo.flagship.features.articles2.models.deserialized

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.Item

@JsonClass(generateAdapter = true)
data class ElevatedByline(
    @Json(name = "byline")
    val byLine: ByLine?,
    @Json(name = "kicker")
    val kicker: Kicker?,
    @Json(name = "showVersion")
    override val showVersion: Int?,
    @Json(name = "hideVersion")
    override val hideVersion: Int?,
    @Json(name = "type")
    override val type: String?,
) : Item(type = type)
