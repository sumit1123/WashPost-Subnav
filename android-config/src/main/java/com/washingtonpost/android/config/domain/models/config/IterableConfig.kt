/* Copyright (c) 2025 The Washington Post. All rights reserved. */
package com.washingtonpost.android.config.domain.models.config

data class IterableConfig(
    val banner: Long?,
    val article: Long?,
    val section: Long?,
    val myPostBanner: Long?,
    val askThePostBanner: Long?,
    val myPost: Long?,
    val askThePost: Long?,
    val settingsTop: Long?,
    val frontHomeScroll: Long?,
    val settingsPlan: Long?,
    val enableDebugLogs: Boolean?,
    val enableAmazonIntegration: Boolean?
)
