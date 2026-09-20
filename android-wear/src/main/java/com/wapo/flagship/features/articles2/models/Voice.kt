/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.models


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * This is an individual voice that is included in the list of available voices.
 */
@JsonClass(generateAdapter = true)
data class Voice(
    @Json(name = "adsUrl")
    val adsUrl: String?,
    @Json(name = "id")
    val id: String?,
    @Json(name = "label")
    val label: String?,
    @Json(name = "marksUrl")
    val marksUrl: String?,
    @Json(name = "rawUrl")
    val rawUrl: String?,
    @Json(name = "s3Key")
    val s3Key: String?
)