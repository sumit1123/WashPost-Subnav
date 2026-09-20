package com.washingtonpost.android.recirculation.carousel.models

/**
 * Item class for Recipe Items
 */
open class CarouselRecipeViewItem(
    val headline: String,
    val artUrl: String?,
    val artAspectRatio: Float?,
    val articleUrl: String,
    val mediaUrl: String?,
    val articleLinkIsWebType: Boolean,
    var duration: Long?,
    var course: String?,
    var rating: Double?,
    var reviews: Int?,
    val shouldCardify: Boolean?
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
    headlinePrefix = "",
    label = "",
    kicker = "",
    title = headline,
    displayDate = null,
    cardify = shouldCardify,
)