/* Copyright (c) 2021 The Washington Post. All rights reserved. */

package com.washingtonpost.android.recirculation.carousel.models

class CarouselBrightViewItem (val brightUrl: String, val storyUrl: String, val brightDescription: String?, var excerpt : String?) : CarouselViewItem(imageUrl = brightUrl, contentUrl = storyUrl, becauseYouRead = null, shouldShowTitle = false, sectionName = "", storyType = "", id= Int.hashCode(), byline = "", trackingString = null, headlinePrefix = null, label = "", kicker = null, title = "", displayDate = null)