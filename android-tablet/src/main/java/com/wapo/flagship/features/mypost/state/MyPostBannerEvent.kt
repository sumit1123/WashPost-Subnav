package com.wapo.flagship.features.mypost.state

import com.wapo.android.commons.iterable.AttributionInfo

sealed class MyPostBannerEvent {
    class OpenUrl(
        val attributionInfo: AttributionInfo?,
        val url: String
    ) : MyPostBannerEvent()

    /**
     * Update Subscription event -> Signals user tapped upgrade/update subscription CTA
     */
    class UpdateSubscription(
        val attributionInfo: AttributionInfo?,
        val productId: String? = null,
        val offerId: String? = null,
        val actionResults: Map<String, Boolean> = emptyMap(),
        val url: String? = null,
        val blocker: String? = null
    ) : MyPostBannerEvent()
}