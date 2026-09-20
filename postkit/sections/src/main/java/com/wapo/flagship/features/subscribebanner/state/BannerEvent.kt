package com.wapo.flagship.features.subscribebanner.state

import com.wapo.flagship.features.grid.model.GlobalBannerMessage
import com.wapo.flagship.features.grid.model.SectionInlineMessage
import com.washingtonpost.android.paywall.models.BannerPaywallMessage

sealed class BannerEvent {
    data class BannerClicked(val message: BannerPaywallMessage?, val iamMessageType: String? = null) : BannerEvent()
    data class GlobalBannerClicked(val message: GlobalBannerMessage) : BannerEvent()
    data class SectionInLineClicked(val message: SectionInlineMessage) : BannerEvent()
    data class BannerDismissed(val message: BannerPaywallMessage) : BannerEvent()
    data class ImpressionEvent(val event: BannerLifecycleEvent) : BannerEvent()
}