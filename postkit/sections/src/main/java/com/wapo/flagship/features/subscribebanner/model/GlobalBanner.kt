package com.wapo.flagship.features.grid.model

import android.text.SpannableStringBuilder
import com.wapo.android.commons.iterable.AttributionInfo
import com.washingtonpost.android.paywall.models.MessageRequirements
import com.washingtonpost.android.paywall.models.PromoAction

/**
 * Model item used by GridAdapter. This model will be used by
 * GlobalBannerViewHolder in order to get appropriate GlobalBannerState
 */
data class GlobalBanner(
    val globalBannerState: GlobalBannerState,
) : Item()

/**
 * State of Banner determines which banner to show and
 * text that will be displayed
 */
sealed class GlobalBannerState {
    /**
     * Provides default Banner data for users who have no subscription
     */
    class NoSub(
        val isSignedIn: Boolean,
        val isTerminated: Boolean,
        val offer: GlobalBannerMessage?,
    ) : GlobalBannerState()

    /**
     * In this state, show only promotional banners
     */
    class Sub(val offer: GlobalBannerMessage?) : GlobalBannerState()

    /**
     * Temporary state derived from the config to make all of our articles free
     */
    object ConfigHideBanner : GlobalBannerState()

    /**
     * This will be the initial state until, app recieves appropriate sub state.
     * In this state, no banner is shown
     */
    object Unknown : GlobalBannerState()
}

/**
 * Provides data to display a single offer.
 */
data class GlobalBannerMessage(
    val attributionInfo: AttributionInfo?,
    val productId: String?,
    val offerId: String?,
    val offerTitle: String?,
    val offerSubtitle: String?,
    val offerDetail: String?,
    val offerUrl: String?,
    val ctaText: SpannableStringBuilder?,
    val wallName: String?,
    val dismissible: Boolean? = false,
    val productName: String? = null,
    val secondaryProductId: String? = null,
    val action: String? = null,
    val isAdFreeProduct: Boolean = false,
    val passedIterableGuardrails: Boolean? = null,
    val promoAction: PromoAction? = null,
    val fallbackAction: PromoAction? = null,
    val messageRequirements: List<MessageRequirements>?
) {
    val isLocalAmazonAdFreeBanner: Boolean
        get() = isAdFreeProduct
}
