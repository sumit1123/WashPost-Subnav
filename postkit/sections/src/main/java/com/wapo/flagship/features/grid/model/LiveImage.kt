/* Copyright (c) 2020 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.grid.model

data class LiveImage(
        val tabs: MutableList<LiveImageTab>,
        val type: LiveImageType,
        val cta: String?
)

data class LiveImageTab(
        val images: MutableList<LiveImageInfo>,
        val text: String?
)

data class LiveImageInfo(
        val url: String,
        val darkModeUrl: String?,
        val aspectRatio: Float = 1.5f,
        val alternateText: String?
)

enum class LiveImageType {
    TAB,
    SEGMENT,
    CAROUSEL,
}