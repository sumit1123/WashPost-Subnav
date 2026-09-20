// Copyright (c) 2025 The Washington Post. All rights reserved.
package com.wapo.flagship.features.aixp.models

import com.squareup.moshi.Json

data class ArticleRealtimeSummary(
    @Json(name = "url") val url: String?,
    @Json(name = "model_id") val modelId: String?,
    @Json(name = "headline") val headline: String? = null,
    @Json(name = "key_points_heading") val keyPointsHeading: String? = null,
    @Json(name = "key_points") val keyPoints: List<String?>? = null,
    @Json(name = "summary") val overview: String? = null,
    @Json(name = "disclaimer") val disclaimer: String? = null,
)
