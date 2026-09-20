package com.washingtonpost.android.paywall.bottomsheet.model

import com.washingtonpost.android.paywall.util.PaywallConstants

/**
 * Actions when user taps on either one of these two types of bottom CTAs
 * These actions should be classes since each click event is separate and we want to propagate all of them.
 */
sealed class BottomCtaToPaywallAction(val paywallType: PaywallConstants.WallType){
    /**
     * default bottom CTA click action
     * Currently only difference in the paywall shown is in tracking event that we send.
     * For acquisition_paywall event we set acq_entrance_type =>> article_subscribe_button.
     */
    class DefaultBottomCtaToPaywallAction : BottomCtaToPaywallAction(PaywallConstants.WallType.BOTTOM_CTA_PAYWALL)

    /**
     * Gift CTA is shown when user clicked on the gifted article deeplink in place of the default bottom cta.
     * Currently only difference in the paywall shown is in tracking event that we send.
     * For acquisition_paywall event we set acq_entrance_type =>> gift_article
     */
    class GiftBottomCtaToPaywallAction: BottomCtaToPaywallAction(PaywallConstants.WallType.BOTTOM_CTA_GIFT_PAYWALL)
}
