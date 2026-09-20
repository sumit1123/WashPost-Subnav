package com.washingtonpost.android.paywall.bottomsheet

import com.washingtonpost.android.config.domain.models.config.paywallconf.ProductSkuEntry
import com.washingtonpost.android.paywall.util.PaywallUtil

/**
 * These are various click events triggered by the user related to
 * Paywall and Sign-In
 */
sealed class UserEvent {
    /**
     * Close Message (Paywall, Acquisition, Reminder, etc.) Dialog
     */
    object Close : UserEvent()

    /**
     * Launch store purchase flow (Play Billing or Amazon IAP)
     */
    class Subscribe(val skuEntry: ProductSkuEntry) : UserEvent() {
        val productId: String get() = skuEntry.compositeKey ?: skuEntry.resolvedSku ?: ""

        constructor(productId: String) : this(
            PaywallUtil.resolveProduct(productId) ?: ProductSkuEntry(playstore = productId, amazon = productId)
        )
    }


    /**
     * Launch store resume flow (Play Billing or Amazon IAP)
     */
    object Resume : UserEvent()

    /**
     * Launch WPAA registration for a specific promotion
     */
    class Register(val promoId: String? = null, val trialType: String? = null) : UserEvent()

    /**
     * Launch sign-in page WPAA (Webview)
     */
    object SignIn : UserEvent()

    /**
     * Show privacy policy page (Webview)
     */
    object PrivacyPolicy : UserEvent()

    /**
     * Show terms of service page (Webview)
     */
    object TermsOfService : UserEvent()

    /**
     * Open Playstore Subscription page to fix payment
     */
    object UpdatePaymentDetails : UserEvent()

    /**
     * Show contact us page (Webview)
     */
    object ContactUs : UserEvent()

    /**
     * Show paywall dialog
     */
    object Paywall : UserEvent()

    class OpenUrl(val url: String) : UserEvent()

    /**
     * Launch store subscription update flow (e.g. Core → Premium or Premium -> Core)
     */
    class UpdateSub(val targetProductId: String) : UserEvent()
}

/**
 * Various user subscription states
 */
sealed class SubState {
    /**
     * User never owned a subscription.
     */
    object NoSub : SubState()

    /**
     * User's previously owned sub is expired.
     */
    object TerminatedSub : SubState()

    /**
     * User has an active sub
     */
    object ActiveSub : SubState()

    /**
     * User has a free trial sub
     */
    object FreeTrialSub : SubState()

    /**
     * User has paused their sub
     */
    object PausedSub : SubState()
}

/**
 * Various sign-in states for user
 */
sealed class SignInState {
    /**
     * User is signed in
     */
    object SignedIn : SignInState()

    /**
     * User is signed out
     */
    object SignedOut : SignInState()
}

/**
 * Various groupings for products
 */
sealed class GroupType {
    /**
     * All monthly products
     */
    object Monthly : GroupType()

    /**
     * All yearly products
     */
    object Yearly : GroupType()

    /**
     * All Core Tier products
     */
    object Core : GroupType()

    /**
     * All Premium Tier products
     */
    object Premium : GroupType()
}