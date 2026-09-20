/* Copyright (c) 2025 The Washington Post. All rights reserved. */
package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.IterableConfig

@JsonClass(generateAdapter = true)
data class RawIterableConfig(
    @Json(name = "banner")
    val banner: Long? = null,
    @Json(name = "article")
    val article: Long? = null,
    @Json(name = "section")
    val section: Long? = null,
    @Json(name = "myPostBanner")
    val myPostBanner: Long? = null,
    @Json(name = "askThePostBanner")
    val askThePostBanner: Long? = null,
    @Json(name = "myPost")
    val myPost: Long? = null,
    @Json(name = "askThePost")
    val askThePost: Long? = null,
    @Json(name = "settingsTop")
    val settingsTop: Long? = null,
    @Json(name = "frontHomeScroll")
    val frontHomeScroll: Long? = null,
    @Json(name = "settingsPlan")
    val settingsPlan: Long? = null,
    @Json(name = "enableDebugLogs")
    val enableDebugLogs: Boolean? = null,
    @Json(name = "enableAmazonIntegration")
    val enableAmazonIntegration: Boolean? = null
) {
    fun mapToDomain(): IterableConfig {
        return IterableConfig(
            banner = banner,
            article = article,
            section = section,
            myPostBanner = myPostBanner,
            askThePostBanner = askThePostBanner,
            myPost = myPost,
            askThePost = askThePost,
            settingsTop = settingsTop,
            frontHomeScroll = frontHomeScroll,
            settingsPlan = settingsPlan,
            enableDebugLogs = enableDebugLogs ?: false,
            enableAmazonIntegration = enableAmazonIntegration ?: false,
        )
    }
}
