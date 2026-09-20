/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.models.deserialized


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AdditionalProperties(
    @Json(name = "link_box_byline")
    val linkBoxByline: Any?,
    @Json(name = "link_box_display_date")
    val linkBoxDisplayDate: Long?,
    @Json(name = "link_box_subheadline")
    val linkBoxSubheadline: String?,
    @Json(name = "link_box_title")
    val linkBoxTitle: String?,
    @Json(name = "kicker")
    val kicker: String?
)