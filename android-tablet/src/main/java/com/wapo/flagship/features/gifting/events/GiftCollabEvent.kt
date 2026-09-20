package com.wapo.flagship.features.gifting.events

/**
 * Click Events that user can trigger on Dialog
 */
sealed class GiftCollabEvent {
    /**
     * Launch sign-in page WPAA
     */
    object SignIn : GiftCollabEvent()

    /**
     * Show paywall dialog
     */
    object Paywall : GiftCollabEvent()

    /**
     * Show Delayed paywall dialog
     */
    object ShowDelayPaywall : GiftCollabEvent()

    /**
     * Dismiss paywall dialog
     */
    object DismissDelayedPaywall : GiftCollabEvent()
}
