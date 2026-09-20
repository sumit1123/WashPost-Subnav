package com.wapo.flagship.features.gifting.events

/**
 * Click Events that user can trigger on Dialog
 */
sealed class UserEvent {
    /**
     * Launch sign-in page WPAA
     */
    object SignIn : UserEvent()

    /**
     * Show paywall dialog
     */
    object Paywall : UserEvent()

    /**
     * Learn More about Gift Articles Event
     */
    object LearnMore : UserEvent()

    /**
     * Gift Article Event
     */
    object Gift : UserEvent()

    /**
     * Contact Us Event
     */
    object ContactUs : UserEvent()

    /**
     * Contact Us Event
     */
    object Dismiss : UserEvent()
}
