/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.models


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * This is a response model for the request made to download the list pof available voices [voices]
 */
@JsonClass(generateAdapter = true)
data class Voices(
    @Json(name = "voices")
    val voices: List<Voice>?
)