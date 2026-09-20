package com.washingtonpost.android.save.database.model

import com.wapo.android.commons.util.ContentType

class ReadingHistoryAndMetadata {
    var contentType: ContentType? = null
    // Article items
    var contentId: String? = null
    var percentConsumed: Float? = null
    var displayDate: String? = null
    var readingTimeMilli: Long? = null
    var listenDepthSec: Long? = null
    var deepestScrollId: String? = null
    var hidden: Boolean? = null

    var contentUrl: String? = null
    var headline: String? = null
    var byline: String? = null
    var blurb: String? = null
    var imageURL: String? = null
    var publishedTime: Long? = null
    var lastUpdated: Long? = null
    var canonicalURL: String? = null
    var secondaryText: String? = null
    var displayLabel: String? = null
    var displayTransparency: String? = null
    var trackingString: String? = null
    var headlinePrefix: String? = null
    var lmt: Long? = null
    var isListened: Boolean = false

    // Podcast Items
    var mediaId: String? = null
    var label: String? = null
    var streamUrl: String? = null
}