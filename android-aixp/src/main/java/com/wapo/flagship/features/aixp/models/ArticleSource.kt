package com.wapo.flagship.features.aixp.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ArticleSource(
    @Json(name = "source_id")
    val citationId: String?,
    @Json(name = "headline")
    val headline: String?,
    @Json(name = "canonical_url")
    val canonicalUrl: String?,
    @Json(name = "publish_date")
    val publishDate: String?,
    @Json(name = "text")
    val text: String?,
    @Json(name = "image")
    val imageUrl: String?
)
