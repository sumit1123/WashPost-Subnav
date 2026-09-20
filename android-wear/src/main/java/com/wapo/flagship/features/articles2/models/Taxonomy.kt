/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.models


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Taxonomy(
    @Json(name = "auxiliaries")
    val auxiliaries: List<Auxiliary>? = null,
    @Json(name = "topics")
    val topics: List<Topic>? = null
)