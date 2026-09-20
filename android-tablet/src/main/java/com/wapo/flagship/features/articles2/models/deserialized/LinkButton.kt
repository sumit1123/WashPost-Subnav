package com.wapo.flagship.features.articles2.models.deserialized

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LinkButton(
    @Json(name = "subtype")
    override val subtype: String?,
    @Json(name = "type")
    override val type: String?,
    @Json(name = "label")
    val label: String,
    @Json(name = "url")
    val url: String,
    @Json(name = "showArrow")
    val showArrow: Boolean = false,
) : Link(subtype, type) {
    enum class SubType(
        val value: String,
    ) {
        BUTTON_OUTCOME("button-outcome"),
    }
}
