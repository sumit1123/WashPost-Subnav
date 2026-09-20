/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.models.deserialized


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.Item

@JsonClass(generateAdapter = true)
data class Audio(
    @Json(name = "adsUrl")
    val adsUrl: String?,
    @Json(name = "rawUrl")
    val rawUrl: String?,
    @Json(name = "subtype")
    val subtype: String?,
    @Json(name = "manifestUrl")
    val manifestUrl: String?,
    @Json(name = "type")
    override val type: String?
) : Item(type = type)