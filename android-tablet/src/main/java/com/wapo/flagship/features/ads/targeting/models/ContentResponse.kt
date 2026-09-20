// Copyright (c) 2025 The Washington Post. All rights reserved.
package com.wapo.flagship.features.ads.targeting.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class ContentResponse (
    val items: List<ContentItem?>? = null
)

@JsonClass(generateAdapter = true)
class ContentItem (
    val id: String? = null,
    @Json(name = "adcall")
    val adCall: Any? = null,
    @Json(name = "permutive")
    val permutive: Any? = null,
)
