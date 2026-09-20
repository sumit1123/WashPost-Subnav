package com.washingtonpost.android.recirculation.carousel.models

import androidx.annotation.DrawableRes

class EmptyCardItem(
    val text1: String,
    val text2: String,
    @DrawableRes val iconRes: Int
) : CarouselViewItem(
    id = 0,
    contentUrl = "",
    kicker = null,
    storyType = null,
    label = "",
    title = "",
    byline = "",
    imageUrl = "",
    sectionName = "",
    shouldShowTitle = false,
    becauseYouRead = null,
    displayDate = null,
    trackingString = null,
    headlinePrefix = null
)