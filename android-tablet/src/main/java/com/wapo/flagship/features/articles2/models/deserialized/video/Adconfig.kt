package com.wapo.flagship.features.articles2.models.deserialized.video

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Adconfig(
    @Json(name = "adSetConfig")
    val adSetConfig: AdSetConfig?,
    @Json(name = "adSetUrls")
    val adSetUrls: AdSetUrlsX?,
    @Json(name = "allowPrerollOnDomain")
    val allowPrerollOnDomain: Boolean?,
    @Json(name = "autoPlayPreroll")
    val autoPlayPreroll: Boolean?,
    @Json(name = "commercialAdNode")
    val commercialAdNode: String?,
    @Json(name = "enableAdInsertion")
    val enableAdInsertion: Boolean?,
    @Json(name = "enableAutoPreview")
    val enableAutoPreview: Boolean?,
    @Json(name = "enableServerSideFallback")
    val enableServerSideFallback: Boolean?,
    @Json(name = "forceAd")
    val forceAd: Boolean?,
    @Json(name = "playAds")
    val playAds: Boolean?,
    @Json(name = "playVideoAds")
    val playVideoAds: Boolean?,
    @Json(name = "videoAdZone")
    val videoAdZone: String?,
    @Json(name = "primarySectionID")
    val primarySectionId: String?
)
