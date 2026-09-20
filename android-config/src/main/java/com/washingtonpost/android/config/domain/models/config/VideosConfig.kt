// Copyright (c) 2026 The Washington Post. All rights reserved.
package com.washingtonpost.android.config.domain.models.config

data class VideosConfig(
    val mobileMaxBitRate: Int,
    val tabletMaxBitRate: Int,
    val maxConcurrentAutoplays: Int
)
