/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.models.deserialized


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.Item

@JsonClass(generateAdapter = true)
data class SanitizedHtml(
    @Json(name = "content")
    val content: String?,
    @Json(name = "mime")
    val mime: String?,
    @Json(name = "subtype")
    val subtype: String?,
    @Json(name = "type")
    override val type: String?,
    @Json(name = "subhead_level")
    val subheadLevel: Int?,
    @Json(name = "style")
    val style: String?,
) : Item(type = type)