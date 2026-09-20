package com.wapo.flagship.features.deeplinks

import com.wapo.android.commons.iterable.AttributionInfo
import com.wapo.flagship.sdk.iterable.models.MessageTracking

/**
 * data class for In app message analytics
 */
data class InAppMessageData(
    val attributionInfo: AttributionInfo?,
    val scheduleId: String,
    val title: String?,
    val headline: String?,
    val eventLabel: String?,
    val messageTracking: MessageTracking? = null
) {
    var miscellany: String? = null
}
