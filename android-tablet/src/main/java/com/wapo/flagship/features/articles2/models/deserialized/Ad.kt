// Copyright (c) 2023 The Washington Post. All rights reserved.
package com.wapo.flagship.features.articles2.models.deserialized

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.Item

@JsonClass(generateAdapter = true)
data class Ad(
    @Json(name = "type")
    override val type: String?,
    @Json(name = "kind")
    val kind: String?,
    @Json(name = "size")
    val size: String?,
    @Json(name = "adPath")
    val adPath: String?,
    @Json(name = "style")
    val style: String?,
    @Json(name = "primarySectionID")
    val primarySectionId: String?,
    @Json(name = "position")
    val position: Position?
) : Item(type = type) {
    enum class Style(
        val value: String,
    ) {
        LUF("luf"),
    }
}
