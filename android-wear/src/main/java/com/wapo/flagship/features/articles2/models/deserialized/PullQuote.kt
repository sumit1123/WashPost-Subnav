/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.models.deserialized


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.Item

@JsonClass(generateAdapter = true)
data class PullQuote(
    @Json(name = "attribution")
    val attribution: String?,
    @Json(name = "content")
    val content: String?,
    @Json(name = "type")
    override val type: String?
) : Item(type = type)