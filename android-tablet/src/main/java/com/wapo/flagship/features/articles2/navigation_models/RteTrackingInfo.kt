package com.wapo.flagship.features.articles2.navigation_models

import com.wapo.flagship.features.articles2.models.Article2

/**
 * This model is used to track the tracking info for Real Time Event API for recording the page view events.
 * [omnitureArcId] retireved from [Article2.omniture]
 * [auxilaries] retrieved from [Article2.taxonomy]
 */
data class RteTrackingInfo(
    val omnitureArcId: String?,
    val eventType: String? = null,
    val contentType: String? = null,
)
