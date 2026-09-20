/* Copyright (c) 2023 The Washington Post. All rights reserved. */

package com.washingtonpost.android.paywall.billing.playstore

import com.wapo.android.commons.util.Logger
import com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.newdata.model.IAPOfferItem
import com.washingtonpost.android.paywall.newdata.model.IAPSubItem

class IAPPlayStoreSubItem(
    playStoreProductId: String?,
    playStoreTitle: String?,
    playStoreSubscriptionOfferDetails: List<SubscriptionOfferDetails>
): IAPSubItem() {

    private var offerId: String?
    private var eligibleSubscriptionOfferDetails: List<SubscriptionOfferDetails>
    private val userVariant = PaywallService.getOmniture().getPricingTestVariant() ?: CONTROL_VARIANT

    init {
        productId = playStoreProductId
        title = playStoreTitle
        eligibleSubscriptionOfferDetails = getOffersForPricingTestVariant(playStoreSubscriptionOfferDetails)

        offers = getAllActiveOffersFromConfig()

        // Get highest weighted offer from the config to display in paywalls
        offerId = getOfferIdToDisplayOnPaywall(
            productId,
            eligibleSubscriptionOfferDetails
        )

        basePlanId = getBasePlanId(offerId)
        basePrice = getOfferPrice()
        currencyCode = getCurrencyCode()
        subscriptionPeriod = getOfferPeriod()
        offerId?.let {
            offerPrice = getOfferPrice(it)
            offerPeriod = getOfferPeriod(it)
            offerPriceCycles = getOfferCycles(it)
        }
        Logger.d("Pricing Test Info", "User Variant: $userVariant, productId: $productId, basePlanId: $basePlanId, offerId: $offerId")
    }

    override fun getIntroOfferIfInConfig(): IAPOfferItem? {
        return offers?.firstOrNull { it.offerId == INTRO_PRICE }
    }

    /**
     * Filters [subscriptionOfferDetails] to only include offers whose offer tags either match
     * the user's test variant, or are null (signifying a globally available offer)
     */
    private fun getOffersForPricingTestVariant(
        subscriptionOfferDetails: List<SubscriptionOfferDetails>
    ): List<SubscriptionOfferDetails> {
        return subscriptionOfferDetails.filter { it.offerTags.contains(userVariant) }
    }

    /** Get all offers from allActiveOffers in the config */
    private fun getAllActiveOffersFromConfig(): MutableList<IAPOfferItem> {
        val offers = mutableListOf<IAPOfferItem>()

        ConfigManager.getInstance().config.paywallConf.allActiveOffers?.get(productId)?.let { offer ->
            offer.forEach { offerId ->
                getValidOffer(offerId)?.let {
                    offers.add(it)
                }
            }
        }

        return offers
    }

    /**
     * @return the [SubscriptionOfferDetails] object for the given [offerId]
     * A null [offerId] signifies the base plan
     */
    private fun getOffer(offerId: String?): SubscriptionOfferDetails? {
        return eligibleSubscriptionOfferDetails.firstOrNull { sub -> sub.offerId == offerId }
    }

    /**
     * Gets the formatted price for a specific [offerId] and [offerPhase].
     * If [offerId] is not provided, the default is null, indicating the base plan.
     * If [offerPhase] is not provided, it is assumed that it is an offer with only one phase.
     */
    private fun getOfferPrice(offerId: String? = null, offerPhase: Int = 0): String? {
        return getOffer(offerId)?.pricingPhases?.pricingPhaseList?.get(offerPhase)?.formattedPrice
    }

    /**
     * Gets the billing period for a specific [offerId] and [offerPhase].
     * If [offerId] is not provided, the default is null, indicating the base plan.
     * If [offerPhase] is not provided, it is assumed that it is an offer with only one phase.
     */
    private fun getOfferPeriod(offerId: String? = null, offerPhase: Int = 0): String? {
        return getOffer(offerId)?.pricingPhases?.pricingPhaseList?.get(offerPhase)?.billingPeriod
    }

    /**
     * Gets the billing cycle count for a specific [offerId] and [offerPhase].
     * If [offerId] is not provided, the default is null, indicating the base plan.
     * If [offerPhase] is not provided, it is assumed that it is an offer with only one phase.
     */
    private fun getOfferCycles(offerId: String?, offerPhase: Int = 0): Int? {
        return eligibleSubscriptionOfferDetails.firstOrNull {
            sub -> sub.offerId == offerId
        }?.pricingPhases?.pricingPhaseList?.get(offerPhase)?.billingCycleCount
    }

    /**
     * Gets the currency code for a specific [offerId] and [offerPhase].
     * If [offerId] is not provided, the default is null, indicating the base plan.
     * If [offerPhase] is not provided, it is assumed that it is an offer with only one phase.
     */
    private fun getCurrencyCode(offerId: String? = null, offerPhase: Int = 0): String {
        return getOffer(offerId)?.pricingPhases?.pricingPhaseList?.get(offerPhase)?.priceCurrencyCode ?: ""
    }

    /**
     * @return the basePlanId for [offerId]
     */
    private fun getBasePlanId(offerId: String?): String? {
        return getOffer(offerId)?.basePlanId
    }

    /**
     * Gets the offerToken for the [offerId]
     * When [offerId] is null, the offerToken for the base plan is returned
     */
    fun getOfferToken(offerId: String?): String? {
        return getOffer(offerId ?: this.offerId)?.offerToken
    }

    /**
     * Determine if the given offer id is an eligible offer from Play Store.
     */
    private fun getValidOffer(offerId: String): IAPOfferItem? {
        val eligibleOffer = getOffer(offerId)

        if (eligibleOffer != null) {
            return IAPOfferItem().apply {
                this.offerId = offerId
                offerPrice = getOfferPrice(offerId)
                offerPeriod = getOfferPeriod(offerId)
                offerPriceCycles = getOfferCycles(offerId)
            }
        }

        return null
    }

    companion object {
        const val CONTROL_VARIANT = "control"
        const val INTRO_PRICE = "introprice"

        /** @return The highest weight offerId from paywallOffers that the user is eligible for */
        fun getOfferIdToDisplayOnPaywall(productId: String?, eligibleSubscriptionOfferDetails: List<SubscriptionOfferDetails?>): String? {
            val paywallOffersForProduct = ConfigManager.getInstance().config.paywallConf.paywallOffers?.get(productId)
            val paywallOffersSorted = paywallOffersForProduct?.sortedByDescending { it.weight ?: -1 }

            val eligibleOfferIdsFromPlayStore = eligibleSubscriptionOfferDetails.map { it?.offerId }
            val offerToDisplay = paywallOffersSorted?.firstOrNull {
                eligibleOfferIdsFromPlayStore.contains(it.offerId)
            }

            return offerToDisplay?.offerId
        }
    }
}
