package com.washingtonpost.android.recirculation.carousel.models

data class CarouselEndCardViewItem(
    val url: String,
    val ctaText: String,
    val shouldCardify: Boolean?
) : CarouselViewItem(
    imageUrl = null,
    contentUrl = url,
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
    title = "",
    displayDate = null,
    cardify = shouldCardify
)
