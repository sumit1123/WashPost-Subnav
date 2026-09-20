package com.wapo.flagship.features.articles2.models.deserialized

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ArticleAudioAdConfig(
    @Json(name = "adBreaks") val adBreaks: List<ArticleAudioAdBreak>? = null,
    @Json(name = "adSetUrl") val adSetUrl: String? = null,
    @Json(name = "primarySectionID") val primarySectionId: String? = null,
)

@JsonClass(generateAdapter = true)
data class ArticleAudioAdBreak(
    @Json(name = "type") val type: ArticleAudioAdBreakType? = null,
    @Json(name = "maxAds") val maxAds: Int? = null,
    @Json(name = "time") val time: Double? = null,
)

enum class ArticleAudioAdBreakType {
    @Json(name = "preroll") PREROLL,
    @Json(name = "midroll") MIDROLL,
    @Json(name = "postroll") POSTROLL,
}