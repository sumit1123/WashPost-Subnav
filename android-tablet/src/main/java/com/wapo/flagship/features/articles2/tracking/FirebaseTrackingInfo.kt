package com.wapo.flagship.features.articles2.tracking

import com.wapo.flagship.features.articles2.models.OmnitureX
import com.wapo.flagship.features.articles2.models.Targeting

/**
 * This data class is used to hold all of the tracking information required to track the rendering of an article.
 * [omnitureX] is copied over directly as is from the [Article2] response.
 * [blogName], [contentUrl] and [firstPublishedDate] are additional properties that are required for tracking also fetched from the [Article2] response.
 */
data class FirebaseTrackingInfo(
    val title: String?,
    val omnitureX: OmnitureX?,
    val blogName: String?,
    val contentUrl: String,
    val firstPublishedDate: Long?,
    val lastModifiedTime: Long?,
    val position: Int,
    val contentWeight: Float?,
    val inlinePushToggleFlag: Boolean,
    val targeting: Targeting?,
    val commercialNode: String?,
)
