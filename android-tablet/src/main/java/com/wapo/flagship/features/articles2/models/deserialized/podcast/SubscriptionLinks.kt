package com.wapo.flagship.features.articles2.models.deserialized.podcast

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SubscriptionLinks(
    @Json(name = "alexa") val alexa: String?,
    @Json(name = "amazonMusic") val amazonMusic: String?,
    @Json(name = "applePodcasts") val applePodcasts: String?,
    @Json(name = "googlePlay") val googlePlay: String?,
    @Json(name = "iheartRadio") val iheartRadio: String?,
    @Json(name = "radioPublic") val radioPublic: String?,
    @Json(name = "rss") val rss: String?,
    @Json(name = "spotify") val spotify: String?,
    @Json(name = "stitcher") val stitcher: String?,
    @Json(name = "tuneIn") val tuneIn: String?
)
