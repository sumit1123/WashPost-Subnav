/* Copyright (c) 2022 The Washington Post. All rights reserved. */

package com.washingtonpost.android.recirculation.carousel.models

/**
 * Item class for Audio Items
 */
open class CarouselImmersionViewItem(
    val headline: String,
    val artUrl: String?,
    val artAspectRatio: Float?,
    val duration: Long?,
    val articleUrl: String,
    val mediaUrl: String?,
    val pageName: String?,
    val kickerName: String?,
    val byLine: String,
    val articleLinkIsWebType: Boolean,
    val prefix: String,
    val liveImage: Boolean?,
    val shouldCardify: Boolean?,
    val itemsRefreshInterval: Long?,
    val kickerStyle: StyleEntity?,
    val secondaryText: String?,
) : CarouselViewItem(
    imageUrl = artUrl,
    contentUrl = articleUrl,
    becauseYouRead = null,
    shouldShowTitle = false,
    sectionName = "",
    storyType = "",
    id = Int.hashCode(),
    byline = byLine,
    trackingString = null,
    headlinePrefix = prefix,
    label = "",
    kicker = kickerName,
    title = headline ?: "",
    displayDate = null,
    cardify = shouldCardify,
    refreshInterval = itemsRefreshInterval,
    style = kickerStyle,
    secondaryLabel = secondaryText
)