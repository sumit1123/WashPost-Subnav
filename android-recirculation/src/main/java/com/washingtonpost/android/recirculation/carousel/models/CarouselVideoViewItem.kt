/* Copyright (c) 2022 The Washington Post. All rights reserved. */

package com.washingtonpost.android.recirculation.carousel.models

/**
 * Item class for Video Items
 */
open class CarouselVideoViewItem(
    val mContentUrl: String,
    val mImageUrl: String?,
    val duration: String?,
    val headline: String?,
    val altText: String?
) : CarouselViewItem(
        imageUrl = mImageUrl,
        contentUrl = mContentUrl,
        becauseYouRead = null,
        shouldShowTitle = false,
        sectionName = "",
        storyType = "",
        id = Int.hashCode(),
        byline = "",
        trackingString = null,
        headlinePrefix = null,
        label = "",
        kicker = null,
        title = headline ?: "",
        displayDate = null
)