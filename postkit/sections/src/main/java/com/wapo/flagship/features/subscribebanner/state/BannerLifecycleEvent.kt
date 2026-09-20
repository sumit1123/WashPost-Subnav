package com.wapo.flagship.features.subscribebanner.state

import com.wapo.android.commons.iterable.AttributionInfo

sealed class BannerLifecycleEvent {
    data class StartImpression(val attributionInfo: AttributionInfo?) : BannerLifecycleEvent()
    data class EndImpression(val attributionInfo: AttributionInfo?) : BannerLifecycleEvent()
    data class Dismiss(val attributionInfo: AttributionInfo?) : BannerLifecycleEvent()
}