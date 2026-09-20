/* Copyright (c) 2022 The Washington Post. All rights reserved. */

package com.washingtonpost.android.recirculation.carousel.models

import com.wapo.flagship.features.utils.PersonalizedPodcastHelper

/**
 * Item class for Audio Items
 */
open class CarouselAudioViewItem(
    val carouseId: String?,
    val audioMediaConfigId: String?,
    val headline: String,
    val artUrl: String?,
    val artAspectRatio: Float?,
    val duration: Long?,
    val articleUrl: String,
    val audioArticleDisplayDate: String?,
    val audioArticleDisplayLabel: String?,
    val audioArticleDisplayTransparency: String?,
    val audioArticleTitlePrefix: String?,
    val audioArticleTitle: String?,
    val mediaUrl: String?,
    val pageName: String?,
    val articleLinkIsWebType: Boolean,
    val overlayText: String?,
    val overlayPrefixImageUrl: String?,
    val shouldCardify: Boolean?,
    override val carouselItemType: String?,
    ) : CarouselViewItem(
    imageUrl = artUrl,
    contentUrl = articleUrl,
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
    title = headline,
    displayDate = if (PersonalizedPodcastHelper.isPersonalizedPodcastItem(carouselItemType)) audioArticleDisplayDate else null,
    cardify = shouldCardify,
    carouselItemType = carouselItemType
)