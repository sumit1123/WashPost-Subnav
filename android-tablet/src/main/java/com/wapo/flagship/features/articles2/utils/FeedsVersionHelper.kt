// Copyright (c) 2023 The Washington Post. All rights reserved.
package com.wapo.flagship.features.articles2.utils

import com.wapo.flagship.features.articles2.models.Article2

/**
 * "version" will be there in feeds starting from ads and inline alert toggle features support in feeds.
 * "version" number will be updated by the feeds and it helps to take decisions in the app.
 */
class FeedsVersionHelper(
    val article: Article2,
) {
    private val version = article.version ?: -1

    // Feeds has support for inline ads and alert toggle from version 1
    val hasInlineAds: Boolean = version >= 1
    val hasInlineAlertToggle: Boolean = version >= 1

    // Feeds has support for elevated_byline from version 2
    val supportsElevatedByline = 2

    val supportsBlockQuote = 3
}
