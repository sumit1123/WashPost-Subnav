package com.washingtonpost.android.recirculation.carousel.models

class CarouselArticleItem(
    val arcId: String,
    contentUrl: String,
    kicker: String?,
    storyType: String?,
    label: String,
    title: String,
    byline: String,
    imageUrl: String?,
    sectionName: String,
    displayDate: String?,
) : CarouselViewItem(
    id = arcId.hashCode(),
    contentUrl = contentUrl,
    kicker = kicker,
    storyType = storyType,
    label = label,
    title = title,
    byline = byline,
    imageUrl = imageUrl,
    sectionName = sectionName,
    shouldShowTitle = true,
    becauseYouRead = null,
    displayDate = displayDate,
    trackingString = null,
    headlinePrefix = null,
)