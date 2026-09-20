/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.models.deserialized


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.Author
import com.wapo.flagship.features.articles2.models.Item

@JsonClass(generateAdapter = true)
data class ByLine(
    @Json(name = "authors")
    val authors: List<Author>?,
    @Json(name = "content")
    val content: String?,
    @Json(name = "mime")
    val mime: String?,
    @Json(name = "subtype")
    val subtype: String?,
    @Json(name = "subtext")
    val subtext: String?,
    @Json(name = "type")
    override val type: String?
) : Item(type = type) {

    enum class SubType(val value: String) {
        LIVE_UPDATE("live-update"), OPINION("opinion");
    }
}