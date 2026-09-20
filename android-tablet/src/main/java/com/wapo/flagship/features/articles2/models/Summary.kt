package com.wapo.flagship.features.articles2.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Summary(
    @Json(name = "url")
    val url: String? = null,
    @Json(name = "model_id")
    val modelId: String? = null,
    @Json(name = "headline")
    val headline: String? = null,
    @Json(name = "summary")
    val summary: String? = null,
    @Json(name = "key_points_heading")
    val keyPointsHeading: String? = null,
    @Json(name = "key_points")
    val keyPoints: List<String?>? = null,
    @Json(name = "disclaimer")
    val disclaimer: String? = null,
    @Json(name = "reviewed")
    val reviewed: Boolean? = null,
)
