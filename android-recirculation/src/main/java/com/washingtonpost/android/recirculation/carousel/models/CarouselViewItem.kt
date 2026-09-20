/*
 * Copyright (c) 2018. The Washington Post
 */
package com.washingtonpost.android.recirculation.carousel.models

/**
 * Created by adkinsj on 7/9/18.
 */
open class CarouselViewItem(
        val id: Int,
        val contentUrl: String,
        val kicker: String?,
        val storyType: String?,
        val label: String,
        val title: String,
        val byline: String,
        val imageUrl: String?,
        val sectionName: String,
        val shouldShowTitle: Boolean,
        val becauseYouRead: String?,
        val displayDate: String?,
        val trackingString: String?,
        val headlinePrefix: String?,
        val cardify: Boolean? = true,
        val refreshInterval: Long? = -1,
        val lmt: Long? = 0,
        val isLive: Boolean? = false,
        val style: StyleEntity? = null,
        val secondaryLabel: String? = null,
        open val carouselItemType: String? = null
)

enum class StyleEntity(value: String?) {
    OPINIONS("opinions"),
    WP_INTELLIGENCE("wp-intelligence"),
    THE_SEVEN_LIVE("the-7-live")
}