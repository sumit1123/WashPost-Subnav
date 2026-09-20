// Copyright (c) 2026 The Washington Post. All rights reserved.
package com.wapo.flagship.features.grid.model

import com.washingtonpost.android.config.domain.models.config.Config

/**
 * Class to delegate config values to mappers and view holders
 */
data class PageConfig(
    val videoMaxBitRateMobile: Int,
    val videoMaxBitRateTablet: Int,
) {
    companion object {
        fun build(config: Config): PageConfig {
            return PageConfig(
                videoMaxBitRateMobile = config.videosConfig.mobileMaxBitRate,
                videoMaxBitRateTablet = config.videosConfig.tabletMaxBitRate
            )
        }
    }
}
