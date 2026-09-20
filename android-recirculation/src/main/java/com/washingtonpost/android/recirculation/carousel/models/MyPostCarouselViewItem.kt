package com.washingtonpost.android.recirculation.carousel.models

import com.wapo.android.commons.util.ContentType

data class MyPostCarouselViewItem(
    val mContentUrl: String = "",
    val headline: String? = null,
    val mHeadlinePrefix: String? = null,
    val mKicker: String? = null,
    val transparency: String? = null,
    val mImageUrl: String? = null,
    val mByline: String? = null,
    val displayDateMillis: Long? = null,
    val recencyThresholdMinutes: Long? = null,
    var contentType: ContentType? = null,
    val deepestScrollId: String? = null,
    var listenDepthSec: Long? = null,
    var percentConsumed: Float? = null,
    var mediaId: String? = null,
    var streamUrl: String? = null,
) : CarouselViewItem(
    imageUrl = mImageUrl,
    contentUrl = mContentUrl,
    becauseYouRead = null,
    shouldShowTitle = false,
    sectionName = "",
    storyType = "",
    id = Int.hashCode(),
    byline = mByline ?: "",
    trackingString = null,
    headlinePrefix = mHeadlinePrefix,
    label = transparency ?: "",
    kicker = mKicker,
    title = "",
    displayDate = null
)
