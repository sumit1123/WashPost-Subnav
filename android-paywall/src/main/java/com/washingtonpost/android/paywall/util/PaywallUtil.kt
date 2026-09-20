// Copyright (c) 2019 The Washington Post. All rights reserved.

package com.washingtonpost.android.paywall.util

import android.content.Context
import android.os.Build
import android.text.SpannableStringBuilder
import com.wapo.android.commons.util.Utils
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.paywall.OAuthConfigStub
import com.washingtonpost.android.config.domain.models.config.paywallconf.Product
import com.washingtonpost.android.config.domain.models.config.paywallconf.ProductSkuEntry
import com.washingtonpost.android.paywall.PaywallReactive
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.R
import com.washingtonpost.android.paywall.bottomsheet.SubState
import com.washingtonpost.android.paywall.newdata.model.IAPOfferItem
import com.washingtonpost.android.paywall.newdata.model.IAPSubItem
import com.washingtonpost.android.paywall.newdata.model.Subscription
import com.washingtonpost.android.paywall.newdata.response.SubItem
import com.washingtonpost.android.paywall.reminder.acquisition.AcquisitionReminderViewModel
import com.washingtonpost.android.paywall.util.PaywallConstants.PW_FREE_PERIOD_PLACEHOLDER
import com.washingtonpost.android.paywall.util.PaywallConstants.PW_INTRO_PERIOD_PLACEHOLDER
import com.washingtonpost.android.paywall.util.PaywallConstants.PW_INTRO_PRICE_PLACEHOLDER
import com.washingtonpost.android.paywall.util.PaywallConstants.PW_INTRO_TEXT_PLACEHOLDER
import com.washingtonpost.android.paywall.util.PaywallConstants.PW_INTRO_UNIT_PLACEHOLDER
import com.washingtonpost.android.paywall.util.PaywallConstants.PW_OFFER_PERIOD_PLACEHOLDER
import com.washingtonpost.android.paywall.util.PaywallConstants.PW_OFFER_PRICE_PLACEHOLDER
import com.washingtonpost.android.paywall.util.PaywallConstants.PW_OFFER_TEXT_PLACEHOLDER
import com.washingtonpost.android.paywall.util.PaywallConstants.PW_OFFER_UNIT_PLACEHOLDER
import com.washingtonpost.android.paywall.util.PaywallConstants.PW_PERIOD_PLACEHOLDER
import com.washingtonpost.android.paywall.util.PaywallConstants.PW_PRICE_PLACEHOLDER
import com.washingtonpost.android.paywall.util.PaywallConstants.PW_REGULAR_PERIOD_PLACEHOLDER
import com.washingtonpost.android.paywall.util.PaywallConstants.PW_REGULAR_PRICE_PLACEHOLDER
import com.washingtonpost.android.paywall.util.PaywallConstants.PW_REGULAR_TEXT_PLACEHOLDER
import com.washingtonpost.android.paywall.util.PaywallConstants.PW_REGULAR_UNIT_PLACEHOLDER
import com.washingtonpost.android.paywall.util.PaywallConstants.PW_TEXT_PLACEHOLDER
import com.washingtonpost.android.paywall.util.PaywallConstants.PW_UNIT_PLACEHOLDER
import org.json.JSONObject
import org.threeten.bp.Period
import java.util.Locale
import kotlin.time.Duration.Companion.parseIsoStringOrNull
import kotlin.time.DurationUnit

object PaywallUtil {
    const val UNKNOWN = "unknown"
    private const val AD_FREE_DYNAMIC_TOKEN = "ad_free"
    private val DYNAMIC_TEXT_REGEX = "(?<=\\{)([^\\}]+)(?=\\})".toRegex()

    /**
     * Returns the CTA/button text for a Product tile.
     * - IapProduct: dynamic text derived from IAP data via [getCTAText].
     * - ExternalProduct / Registration: the static [action] string from config.
     */
    fun getActionTextForProduct(
        product: Product,
        ctaType: String = PaywallConstants.CTA_WALL,
        iapSubItem: IAPSubItem? = null,
        subState: SubState? = null,
        isIapTerminated: Boolean = false,
        wallName: String?
    ): String {
        return when (product) {
            is Product.ExternalProduct -> product.action.orEmpty()
            is Product.Registration -> product.action.orEmpty()
            is Product.IapProduct -> {
                getCTAText(
                    ctaType = ctaType,
                    iapSubItem = iapSubItem,
                    subState = subState,
                    isIapTerminated = isIapTerminated,
                    introOfferText = product.introOfferText,
                    wallName = wallName
                )
            }
        }
    }



    /**
     * Get subscribe button text based on [SubState]
     * - No sub -> Should show Intro Offer [Try X month free] if it exists otherwise Subscribe
     * - Terminated -> Should show Resubscribe for $XX.XX
     */
    fun getCTAText(
        ctaType: String,
        iapSubItem: IAPSubItem?,
        subState: SubState?,
        isIapTerminated: Boolean,
        isDollarOneActive: Boolean = false,
        introOfferText: String? = null,
        iapOfferItem: IAPOfferItem? = null,
        wallName: String?
    ): String {
        var text = textFallback(ctaType, iapSubItem?.productId)

        if (subState != null) {
            iapSubItem?.apply {
                if (!productId.isNullOrEmpty() && !basePrice.isNullOrEmpty()) {
                    // Playstore IAP Library will give intro offer details response
                    // Amazon IAP Library will not give intro price in response. This is why we get it from the config [product.introPriceText]

                    val selectedOffer = iapOfferItem ?: getEligibleOffer(iapSubItem, wallName)
                    val selectedOfferText =
                        getFormattedOffer(
                            selectedOffer?.offerPrice,
                            selectedOffer?.offerPeriod,
                            selectedOffer?.offerPriceCycles
                        )
                    val normalOfferText =
                        getFormattedPricePerPeriod(basePrice, subscriptionPeriod, productId)
                    text =
                        when (subState) {
                            SubState.NoSub -> textNoSub(ctaType, selectedOfferText, normalOfferText, iapSubItem.productId)
                            SubState.TerminatedSub -> {
                                if (PaywallService.getPaywallPrefHelper().purchaseHistory.contains(iapSubItem.productId)) {
                                    textTerminatedSub(ctaType, selectedOfferText, normalOfferText, iapSubItem.productId)
                                } else {
                                    textNoSub(ctaType, selectedOfferText, normalOfferText, iapSubItem.productId)
                                }
                            }
                            SubState.FreeTrialSub -> textFreeTrialSub(ctaType, normalOfferText, isIapTerminated, iapSubItem.productId)
                            SubState.ActiveSub, SubState.PausedSub ->
                                textActiveSub(
                                    ctaType,
                                    getAmazonIntroOffer(introOfferText),
                                    normalOfferText,
                                    isDollarOneActive,
                                    isPremiumProduct(productId)
                                )
                        }
                }
            }
        }
        return text
    }

    /**
     * Acquisition Reminder text gets processed as HTML so \n must be replaced with <br/>.
     */
    private fun textNoSub(
        ctaType: String,
        introOffer: String?,
        normalOffer: String?,
        productId: String?
    ): String =
        if (introOffer != null) {
            when (ctaType) {
                PaywallConstants.CTA_WALL -> "<b>$introOffer</b>\nthen $normalOffer"
                PaywallConstants.CTA_ACQUISITION_REMINDER,
                PaywallConstants.CTA_BANNER -> "<b>$introOffer</b><br/>then $normalOffer"
                else -> introOffer
            }
        } else {
            when (ctaType) {
                PaywallConstants.CTA_WALL -> {
                    if (isOneDayPassProduct(productId)) {
                        "<b>Purchase One-Day Pass</b>\nfor $normalOffer"
                    } else {
                        "<b>Subscribe</b>\nfor $normalOffer"
                    }
                }
                PaywallConstants.CTA_ACQUISITION_REMINDER,
                PaywallConstants.CTA_BANNER -> "<b>Subscribe</b><br/>for $normalOffer"
                else -> "Subscribe for $normalOffer"
            }
        }

    /**
     * Acquisition Reminder text gets processed as HTML so \n must be replaced with <br/>.
     */
    private fun textTerminatedSub(
        ctaType: String,
        introOffer: String?,
        normalOffer: String?,
        productId: String?,
    ): String =
        if (introOffer != null) {
            when (ctaType) {
                PaywallConstants.CTA_WALL -> "<b>$introOffer</b>\nthen $normalOffer"
                PaywallConstants.CTA_ACQUISITION_REMINDER,
                PaywallConstants.CTA_BANNER -> "<b>$introOffer</b><br/>then $normalOffer"
                else -> introOffer
            }
        } else {
            when (ctaType) {
                PaywallConstants.CTA_WALL -> {
                    if (isOneDayPassProduct(productId)) {
                        "<b>Purchase One-Day Pass</b>\nfor $normalOffer"
                    } else {
                        "<b>Resubscribe</b>\nfor $normalOffer"
                    }
                }
                PaywallConstants.CTA_ACQUISITION_REMINDER,
                PaywallConstants.CTA_BANNER -> "<b>Resubscribe</b><br/>for $normalOffer"
                else -> "Resubscribe for $normalOffer"
            }
        }

    private fun textFreeTrialSub(
        ctaType: String,
        normalOffer: String?,
        isIapTerminated: Boolean,
        productId: String?
    ): String =
        if (isIapTerminated) {
            when (ctaType) {
                PaywallConstants.CTA_WALL -> {
                    if (isOneDayPassProduct(productId)) {
                        "<b>Purchase One-Day Pass</b>\nfor $normalOffer"
                    } else {
                        "<b>Resubscribe</b>\nfor $normalOffer"
                    }
                }
                PaywallConstants.CTA_ACQUISITION_REMINDER,
                PaywallConstants.CTA_BANNER -> "<b>Resubscribe</b><br/>for $normalOffer"
                else -> "Resubscribe for $normalOffer"
            }
        } else {
            when (ctaType) {
                PaywallConstants.CTA_WALL -> {
                    if (isOneDayPassProduct(productId)) {
                        "<b>Purchase One-Day Pass</b>\nfor $normalOffer"
                    } else {
                        "<b>Subscribe</b>\nfor $normalOffer"
                    }
                }
                PaywallConstants.CTA_ACQUISITION_REMINDER,
                PaywallConstants.CTA_BANNER -> "<b>Subscribe</b><br/>for $normalOffer"
                else -> "Subscribe for $normalOffer"
            }
        }

    private fun textActiveSub(
        ctaType: String,
        introOffer: String,
        normalOffer: String?,
        isDollarOneActive: Boolean,
        isPremiumOffer: Boolean,
    ): String {
        // This case will be hit when there are 30 days or less left in Amazon 6 month free trial
        return if (isDollarOneActive) {
            when (ctaType) {
                PaywallConstants.CTA_WALL -> "<b>$introOffer</b>\nthen $normalOffer"
                PaywallConstants.CTA_ACQUISITION_REMINDER -> "$introOffer, then $normalOffer"
                else -> introOffer
            }
        } else {
            // change CTA text for upgrade/downgrade cases
            if (
                ctaType != PaywallConstants.CTA_ACQUISITION_REMINDER &&
                PaywallService.getInstance()?.isBasicTierUser == true &&
                isPremiumOffer
            ) {
                return "<b>Upgrade to Premium</b>\nfor $normalOffer"
            } else if (
                ctaType != PaywallConstants.CTA_ACQUISITION_REMINDER &&
                PaywallService.getInstance()?.isPremiumTierUser == true &&
                !isPremiumOffer
            ) {
                return "<b>Downgrade to Core</b>\nfor $normalOffer"
            }
            when (ctaType) {
                PaywallConstants.CTA_WALL -> "Already Subscribed"
                else -> {
                    PaywallService
                        .getConnector()
                        .logHandledException(Exception(AcquisitionReminderViewModel.ACTIVE_SUB_ERROR))
                    "User already has subscription"
                }
            }
        }
    }

    private fun textFallback(ctaType: String, productId: String?): String =
        when (ctaType) {
            PaywallConstants.CTA_WALL -> {
                if (isOneDayPassProduct(productId)) {
                    "<b>Purchase One-Day Pass</b>"
                }
                else {
                    "<b>Subscribe</b>"
                }
            }
            else -> "Subscribe"
        }

    /**
     * [introPriceText] - This is the text coming from config since Amazon IAP response does not provide it.
     */
    private fun getAmazonIntroOffer(introPriceText: String?): String {
        introPriceText?.let {
            return it
        }
        return "Get 6 months for $1"
    }

    fun getFormattedPricePerPeriod(
        price: String?,
        subscriptionPeriod: String?,
        productId: String?,
    ): String? {
        price ?: return null
        productId ?: return null
        val period = if (!subscriptionPeriod.isNullOrEmpty()) getFormattedPeriod(
            subscriptionPeriod,
            false
        ) else UNKNOWN
        return when {
            isNonRenewableProduct(productId) -> "$price"
            period != UNKNOWN -> "$price/$period"
            productId.contains("annual") -> "$price/year"
            // Since there are only annual and monthly subs, we can assume this will be monthly.
            else -> "$price/month"
        }
    }

    fun getBannerOfferText(productId: String?, offerId: String?): String? {
        val paywall = PaywallService.getInstance() ?: return null
        if (!productId.isNullOrEmpty()) {
            val iapSubItem = PaywallService
                .getConnector()
                .iapSubItems
                .getItem(productId)
            val iapOfferItem = iapSubItem?.offers
                ?.firstOrNull {
                    offerId == it.offerId
                }
            return getCTAText(
                ctaType = PaywallConstants.CTA_BANNER,
                iapSubItem = iapSubItem,
                subState = paywall.getSubState(),
                isIapTerminated = paywall.isIapTerminated,
                iapOfferItem = iapOfferItem,
                wallName = null
            )
        }
        return null
    }

    fun getFormattedOfferForProductId(productId: String, wallName: String?): String? {
        val iapSubItems = PaywallService.getConnector().iapSubItems
        iapSubItems.getItem(productId)?.let {
            val selectedOffer = getEligibleOffer(it, wallName)
            return selectedOffer?.offerPrice
        }

        return null
    }

    fun getOfferId(productId: String?, wallName: String?): String? {
        // Retrieve message based on wallName if provided, otherwise use the default blocker paywall message.
        // If wallName is not provided (for example for banners, acquisition reminder etc.,), we will use the default blocker paywall message to get the offer ID.
        val message = wallName?.let {
            PaywallService.getConnector().getBlockerPaywallMessage(it)
        } ?: PaywallService.getConnector().blockerPaywallMessage

        return message?.items?.firstOrNull { item ->
            mapProductNameToProductId(item?.name) == productId
        }?.code?.takeIf { it.isNotEmpty() }
    }

    /**
     * Retrieves the eligible offer for an iapSubItem.
     * If there is an offer, use that.
     * If there is no offer, return null.
     */
    fun getEligibleOffer(iapSubItem: IAPSubItem, wallName: String?): IAPOfferItem? {
        // Retrieve message based on wallName if provided, otherwise use the default blocker paywall message.
        // If wallName is not provided (for example for banners, acquisition reminder etc.,), we will use the default blocker paywall message to get the offer ID.
        val message = wallName?.let {
            PaywallService.getConnector().getBlockerPaywallMessage(it)
        } ?: PaywallService.getConnector().blockerPaywallMessage

        // find an offer if it exists for the user
        val offer = message?.items?.firstOrNull { item ->
                    mapProductNameToProductId(item?.name) == iapSubItem.productId
                }?.code

        val eligibleOffer: IAPOfferItem? = iapSubItem.offers?.find { it.offerId == offer }

        return eligibleOffer
            ?: if (iapSubItem.offerPrice == null) {
                null
            } else {
                IAPOfferItem().apply {
                    offerPrice = iapSubItem.offerPrice
                    offerPeriod = iapSubItem.offerPeriod
                    offerPriceCycles = iapSubItem.offerPriceCycles
                }
            }
    }

    fun getFormattedOffer(
        introductoryPrice: String?,
        offerPeriod: String?,
        numCycles: Int?,
    ): String? {
        offerPeriod?.let {
            if (offerPeriod.isNotEmpty() && !introductoryPrice.isNullOrEmpty() && numCycles != null) {
                return if (introductoryPrice == PaywallConstants.OFFER_PRICE_FREE) {
                    getFormattedPeriod(it, true) + " free"
                } else if (numCycles == 1) {
                    "$introductoryPrice for " + getFormattedPeriod(it, true)
                } else {
                    // if number of cycles is greater than 1, then the offer period is the billing period, so we need to use the number of cycles to format the value
                    val parsedPeriod = Period.parse(offerPeriod)
                    val billingPeriod =
                        when {
                            parsedPeriod.years > 0 -> if (numCycles == 1) "year" else "years"
                            parsedPeriod.months > 0 -> if (numCycles == 1) "month" else "months"
                            else -> if (numCycles == 1) "day" else "days"
                        }
                    "$numCycles $billingPeriod for $introductoryPrice/" + getFormattedPeriod(
                        it,
                        false
                    )
                }
            }
        }

        return null
    }

    fun getFormattedPeriod(
        period: String,
        shouldCountOne: Boolean,
    ): String =
        try {
            val parsedPeriod = Period.parse(period)
            when {
                parsedPeriod.years > 0 ->
                    if (parsedPeriod.years == 1 &&
                        shouldCountOne
                    ) {
                        "1 year"
                    } else if (parsedPeriod.years == 1 && !shouldCountOne) {
                        "year"
                    } else {
                        "${parsedPeriod.years} years"
                    }

                parsedPeriod.months > 0 || parsedPeriod.days >= 30 ->
                    if ((parsedPeriod.months == 1 || parsedPeriod.months == 0) &&
                        shouldCountOne
                    ) {
                        "1 month"
                    } else if ((parsedPeriod.months == 1 || parsedPeriod.months == 0) &&
                        !shouldCountOne
                    ) {
                        "month"
                    } else {
                        "${parsedPeriod.months} months"
                    }

                else ->
                    if (parsedPeriod.days == 1 &&
                        shouldCountOne
                    ) {
                        "1 day"
                    } else if (parsedPeriod.days == 1 && !shouldCountOne) {
                        "day"
                    } else {
                        "${parsedPeriod.days} days"
                    }
            }
        } catch (e: Exception) {
            PaywallService.getConnector().breadcrumb("ThreeTen Library failed to parse $period")
            PaywallService.getConnector().logHandledException(e)
            UNKNOWN
        }

    // parseIsoString handles the "P1D" ISO 8601 format specifically
    fun convertISOtoMs(iso: String): Long? {
        return parseIsoStringOrNull(iso)?.toLong(DurationUnit.MILLISECONDS)
    }

    /**
     * Parse offer period and number of cycles of that offer
     */
    fun getFormattedCycles(
        offerPeriod: String?,
        numCycles: Int?,
    ): String? {
        return try {
            offerPeriod?.let {
                if (offerPeriod.isNotEmpty() && numCycles != null) {
                    val parsedPeriod = Period.parse(offerPeriod)
                    when {
                        parsedPeriod.years > 0 -> if (numCycles == 1) "1 year" else "$numCycles years"
                        parsedPeriod.months > 0 -> if (numCycles == 1) "1 month" else "$numCycles months"
                        else -> if (numCycles == 1) "1 day" else "$numCycles days"
                    }
                } else {
                    return null
                }
            }
        } catch (e: Exception) {
            PaywallService.getConnector()
                .breadcrumb("ThreeTen Library failed to parse $offerPeriod")
            PaywallService.getConnector().logHandledException(e)
            UNKNOWN
        }
    }

    /**
     * Returns true if the given product has type: non-renewable
     */
    fun isNonRenewableProduct(productId: String?): Boolean {
        productId ?: return false
        val map = ConfigManager.getInstance().config.paywallConf.productToSkuMap ?: return false
        val entry = map.values.find { it.playstore.equals(productId, ignoreCase = true) }
        return entry?.isNonRenewable ?: false
    }

    /**
     * Returns true if the given product is a one-day pass subscription
     */
    fun isOneDayPassProduct(productId: String?): Boolean {
        productId ?: return false
        val map = ConfigManager.getInstance().config.paywallConf.productToSkuMap ?: return false
        val entry = map.values.find { it.playstore.equals(productId, ignoreCase = true) }
        return entry?.isOneDayPass ?: false
    }

    /**
     * Determines if the given product is a premium-tier product by reverse-looking up
     * the product name in productToSkuMap config
     */
    @JvmStatic
    fun isPremiumProduct(productId: String?): Boolean {
        productId ?: return false
        // remove base plan ID if present
        val cleanProductId = productId.substringBefore(":")
        val productToSkuMap = ConfigManager.getInstance().config.paywallConf.productToSkuMap
            ?: return false

        for ((productName, entry) in productToSkuMap) {
            val entrySku = if (Utils.isAmazonBuild()) entry.amazon else entry.playstore
            if (entrySku.equals(cleanProductId, ignoreCase = true)) {
                return productName.startsWith("premium", ignoreCase = true)
            }
        }
        return false
    }

    /**
     * Resolves a product name to its [ProductSkuEntry] from productToSkuMap config.
     * Returns null if the product name is not found in the config.
     */
    fun resolveProduct(productName: String?): ProductSkuEntry? {
        productName ?: return null
        val productNameToProductIdMap =
            ConfigManager.getInstance().config.paywallConf.productToSkuMap
        return productNameToProductIdMap?.get(productName)
    }

    fun mapProductNameToProductId(productName: String?): String? {
        // Ad-free products are stored in a separate config map (adFreeProductToSkuMap),
        // resolved via PaywallService based on the user's current tier/term.
        if (PaywallReactive.AD_FREE_PRODUCT.equals(productName, ignoreCase = true)) {
            return PaywallService.getInstance()?.adFreeProductId
        }
        if (PaywallReactive.PREMIUM_PRODUCT.equals(productName, ignoreCase = true)
            || PaywallReactive.BASIC_PRODUCT.equals(productName, ignoreCase = true)) {
            return PaywallService.getInstance()?.getTargetSubscriptionProductId(productName)
        }
        val entry = resolveProduct(productName) ?: return null
        val sku = if (Utils.isAmazonBuild() && entry.amazon != null) {
            entry.amazon
        } else if (!Utils.isAmazonBuild() && entry.playstore != null) {
            entry.playstore
        } else {
            null
        }
        sku ?: return null
        return if (entry.basePlanId != null) "$sku:${entry.basePlanId}" else sku
    }

    /**
     * Resolves server-side product names from a list of Play Store / Amazon SKUs
     * by reverse-looking up the productToSkuMap and adFreeSKUs configs.
     *
     * e.g. ["wp.classic.basic", "wp.classic.ad.free.monthly"] -> ["BASIC", "AD_FREE"]
     *
     * Product name keys like "basic-monthly" are normalized to just the tier ("BASIC", "PREMIUM")
     * since Iterable requirements use tier-level product names.
     */
    @JvmStatic
    fun resolveProductNamesFromSkus(skuList: List<String>?): List<String> {
        if (skuList.isNullOrEmpty()) return emptyList()

        val products = mutableSetOf<String>()
        val adFreeSKUs = PaywallService.getInstance()?.adFreeSKUs ?: emptySet()
        val productToSkuMap = ConfigManager.getInstance().config.paywallConf.productToSkuMap

        for (sku in skuList) {
            // Check if it's an ad-free SKU
            if (adFreeSKUs.contains(sku)) {
                products.add(PaywallReactive.AD_FREE_PRODUCT)
                continue
            }

            // Reverse lookup from productToSkuMap: find which product name maps to this SKU
            if (productToSkuMap != null) {
                for ((productName, entry) in productToSkuMap) {
                    val entrySku = if (Utils.isAmazonBuild()) entry.amazon else entry.playstore
                    if (entrySku == sku) {
                        // Normalize "basic-monthly" / "basic-annual" -> "basic"
                        val tier = productName.substringBefore("-")
                        products.add(tier.uppercase(Locale.ROOT))
                        break
                    }
                }
            }
        }

        return products.toList()
    }

    /**
     * Replaces all placeholders in Dynamic text with appropriate price or period text.
     * Note that buildDynamicTextForPaywall is used for paywalls, while this method is used
     * for other places in the app where a specific product/offer must be displayed (e.g.) airship IAM
     * @param rawText text with placeholders e.g. {offerPrice} {offerUnit}
     * @param productAndOffer pair of product name and offer name. e.g. (basic-monthly, winback)
     * note that offer is optional
     */
    fun buildDynamicTextForProductAndOffer(
        rawText: String?,
        productAndOffer: Pair<String, String?>,
    ): String {
        var modifiedText = rawText ?: ""
        val skuItem = getIAPSubItem(productAndOffer.first)
        skuItem?.let {
            val selectedOffer =
                if (productAndOffer.second != null) skuItem.offers?.firstOrNull { it.offerId == productAndOffer.second } else null
            val introOffer = skuItem.getIntroOfferIfInConfig()
            DYNAMIC_TEXT_REGEX.findAll(modifiedText).forEach { dynamicText ->
                dynamicText.value.apply {
                    val value = replacePlaceholder(this, skuItem, selectedOffer, introOffer)
                    if (!value.isNullOrEmpty()) {
                        modifiedText = modifiedText.replace("{$this}", value)
                    }
                }
            }
        }
        return modifiedText
    }

    /**
     * Replaces all placeholders in Dynamic text in paywalls with appropriate price or period text.
     * e.g. "none-content-dynamic": "Subscribe to get the full experience for just {premium-monthly:introPrice} per {premium-monthly:introPeriod}."
     * Note the key has to be a productName and not a productId.
     * In Pause case, finds last active productId.
     */
    fun buildDynamicTextForPaywall(rawText: String?): String? {
        var modifiedText = rawText ?: return null
        DYNAMIC_TEXT_REGEX.findAll(modifiedText).forEach { dynamicText ->
            dynamicText.value.apply {
                val pair =
                    this.split(":").let {
                        Pair(it[0], it.getOrNull(1) ?: "")
                    }
                // "ad_free" is a special token that resolves to the user's ad-free SKU
                // based on their current base subscription (monthly or annual).
                // getAdFreeProductId() returns the SKU directly, so look it up from IAPSubItems.
                val skuItem = if (pair.first == AD_FREE_DYNAMIC_TOKEN) {
                    val adFreeSku = PaywallService.getInstance()?.adFreeProductId
                    val item = adFreeSku?.let { PaywallService.getConnector().iapSubItems.getItem(it) }
                    item
                } else {
                    getIAPSubItem(pair.first)
                }

                val selectedOffer = skuItem?.let { getEligibleOffer(it, null) }
                val introOffer = skuItem?.getIntroOfferIfInConfig()
                var value = replacePlaceholder(pair.second, skuItem, selectedOffer, introOffer)
                if (pair.first == AD_FREE_DYNAMIC_TOKEN) {
                    value = value?.removeSuffix(".00")
                }
                if (!value.isNullOrEmpty()) {
                    modifiedText = modifiedText.replace("{$this}", value)
                }
            }
        }
        return modifiedText
    }

    fun getCtaTextForTiles(iapSubItem: IAPSubItem, product: Product?, wallName: String?): String {
        val selectedOffer = getEligibleOffer(iapSubItem, wallName)

        // 1. Prefer CTA from selected offer if available
        selectedOffer?.let {
            return getFormattedOfferSubTitle(it.offerPrice, it.offerPeriod, it.offerPriceCycles)
                ?: ""
        }

        // 2. If no offer, infer from the sub item's subscription period
        iapSubItem.subscriptionPeriod?.let { period ->
            try {
                val parsed = Period.parse(period)
                return when {
                    parsed.years > 0 || parsed.months >= 12 || parsed.days >= 365 -> "per year"
                    parsed.months > 0 || parsed.days >= 28 -> "per month"
                    parsed.days == 1 -> "one-time charge"
                    else -> ""
                }
            } catch (e: Exception) {
                PaywallService.getConnector().logHandledException(e)
            }
        }

        // Optional fallback to product id naming convention if period is unavailable
        val productId = (product as? Product.IapProduct)?.id
        productId?.let {
            return if (it.contains("annual", ignoreCase = true)) "per year" else "per month"
        }

        // 3. Fallback if neither is available
        return ""
    }

    fun getFormattedOfferSubTitle(
        introductoryPrice: String?,
        offerPeriod: String?,
        numCycles: Int?,
    ): String? {
        offerPeriod?.let {
            if (offerPeriod.isNotEmpty() && !introductoryPrice.isNullOrEmpty() && numCycles != null) {
                return if (introductoryPrice == PaywallConstants.OFFER_PRICE_FREE) {
                    getFormattedPeriod(it, true) + " free"
                } else if (numCycles == 1) {
                    "for " + getFormattedPeriod(it, true)
                } else {
                    // if number of cycles is greater than 1, then the offer period is the billing period, so we need to use the number of cycles to format the value
                    val parsedPeriod = Period.parse(offerPeriod)
                    val billingPeriod =
                        when {
                            parsedPeriod.years > 0 -> if (numCycles == 1) "year" else "years"
                            parsedPeriod.months > 0 -> if (numCycles == 1) "month" else "months"
                            else -> if (numCycles == 1) "day" else "days"
                        }
                    " for $numCycles $billingPeriod"
                }
            }
        }
        return null
    }


    fun getTilesFormattedPricePerPeriod(
        price: String?,
        subscriptionPeriod: String?,
        productId: String?,
    ): String? {
        if (price.isNullOrEmpty() || productId.isNullOrEmpty()) return null

        val period = subscriptionPeriod?.takeIf { it.isNotEmpty() }
            ?.let { getFormattedPeriod(it, false) }
            ?: UNKNOWN

        return when {
            period != UNKNOWN -> "$price"
            productId.contains("annual", ignoreCase = true) -> "$price"
            else -> "$price"
        }
    }



    /**
     * Replace placeholder text with actual value
     * @param placeholder - placeholder text e.g. offerPrice
     * @param skuItem - product info
     * @param offerItem - offer info
     * @param introOfferItem intro offer info
     */
    private fun replacePlaceholder(
        placeholder: String,
        skuItem: IAPSubItem?,
        offerItem: IAPOfferItem?,
        introOfferItem: IAPOfferItem?,
    ): String? {
        skuItem ?: return null
        return when (placeholder) {
            PW_PRICE_PLACEHOLDER, PW_REGULAR_PRICE_PLACEHOLDER -> skuItem.basePrice
            PW_INTRO_PERIOD_PLACEHOLDER ->
                introOfferItem?.offerPeriod?.let {
                    getFormattedPeriod(
                        it,
                        false,
                    )
                }

            PW_OFFER_PERIOD_PLACEHOLDER, PW_FREE_PERIOD_PLACEHOLDER ->
                offerItem?.offerPeriod?.let {
                    getFormattedPeriod(
                        it,
                        false,
                    )
                }

            PW_PERIOD_PLACEHOLDER, PW_REGULAR_PERIOD_PLACEHOLDER ->
                skuItem.subscriptionPeriod?.let {
                    getFormattedPeriod(
                        it,
                        true,
                    )
                }

            PW_UNIT_PLACEHOLDER, PW_REGULAR_UNIT_PLACEHOLDER ->
                skuItem.subscriptionPeriod?.let {
                    getFormattedPeriod(
                        it,
                        false,
                    )
                }

            PW_OFFER_UNIT_PLACEHOLDER -> {
                offerItem?.let {
                    getFormattedCycles(it.offerPeriod, it.offerPriceCycles)
                }
            }

            PW_INTRO_UNIT_PLACEHOLDER ->
                introOfferItem?.let {
                    getFormattedCycles(
                        it.offerPeriod,
                        it.offerPriceCycles,
                    )
                }

            PW_TEXT_PLACEHOLDER, PW_REGULAR_TEXT_PLACEHOLDER ->
                getFormattedPricePerPeriod(
                    skuItem.basePrice,
                    skuItem.subscriptionPeriod,
                    skuItem.productId,
                )

            PW_OFFER_TEXT_PLACEHOLDER ->
                getFormattedPricePerPeriod(
                    offerItem?.offerPrice,
                    offerItem?.offerPeriod,
                    skuItem.productId,
                )

            PW_INTRO_TEXT_PLACEHOLDER ->
                introOfferItem?.let {
                    getFormattedPricePerPeriod(it.offerPrice, it.offerPeriod, skuItem.productId)
                }

            PW_OFFER_PRICE_PLACEHOLDER -> offerItem?.offerPrice
            PW_INTRO_PRICE_PLACEHOLDER -> introOfferItem?.offerPrice

            else -> {
                return null
            }
        }
    }

    /**
     * Helper function to map product name to product id and then get that IAPSubItem, if it exists.
     */
    private fun getIAPSubItem(productName: String?): IAPSubItem? {
        productName ?: return null

        val sku =
            when (productName) {
                PaywallConstants.PW_PAUSED_SKU_PLACEHOLDER -> {
                    val lastSubscription: Subscription? =
                        PaywallService.getBillingHelper().lastActiveSubscription
                    lastSubscription?.storeProductId
                        ?: PaywallService.getPaywallPrefHelper()?.prefLastSubProductId
                }

                else -> mapProductNameToProductId(productName)
            }

        return sku?.let {
            PaywallService.getConnector().iapSubItems.getItem(it)
        }
    }

    fun getProductOfferJson(
        productName: String,
        offerName: String?,
    ): String? {
        val skuItem = getIAPSubItem(productName)
        skuItem?.let {
            val selectedOffer =
                if (offerName != null) skuItem.offers?.firstOrNull { it.offerId == offerName } else null
            val introOffer = skuItem.getIntroOfferIfInConfig()
            val productJson =
                JSONObject().apply {
                    put(
                        PW_REGULAR_PRICE_PLACEHOLDER,
                        replacePlaceholder(
                            PW_REGULAR_PRICE_PLACEHOLDER,
                            skuItem,
                            selectedOffer,
                            introOffer
                        )
                    )
                    put(
                        PW_REGULAR_UNIT_PLACEHOLDER,
                        replacePlaceholder(
                            PW_REGULAR_UNIT_PLACEHOLDER,
                            skuItem,
                            selectedOffer,
                            introOffer
                        )
                    )
                    put(
                        PW_REGULAR_PERIOD_PLACEHOLDER,
                        replacePlaceholder(
                            PW_REGULAR_PERIOD_PLACEHOLDER,
                            skuItem,
                            selectedOffer,
                            introOffer
                        ),
                    )
                    put(
                        PW_REGULAR_TEXT_PLACEHOLDER,
                        replacePlaceholder(
                            PW_REGULAR_TEXT_PLACEHOLDER,
                            skuItem,
                            selectedOffer,
                            introOffer
                        )
                    )
                    introOffer?.let {
                        put(
                            PW_INTRO_PRICE_PLACEHOLDER,
                            replacePlaceholder(
                                PW_REGULAR_PERIOD_PLACEHOLDER,
                                skuItem,
                                selectedOffer,
                                introOffer
                            ),
                        )
                    }
                    introOffer?.let {
                        put(
                            PW_INTRO_UNIT_PLACEHOLDER,
                            replacePlaceholder(
                                PW_INTRO_UNIT_PLACEHOLDER,
                                skuItem,
                                selectedOffer,
                                introOffer
                            ),
                        )
                    }
                    introOffer?.let {
                        put(
                            PW_INTRO_PERIOD_PLACEHOLDER,
                            replacePlaceholder(
                                PW_INTRO_PERIOD_PLACEHOLDER,
                                skuItem,
                                selectedOffer,
                                introOffer
                            ),
                        )
                    }
                    introOffer?.let {
                        put(
                            PW_INTRO_TEXT_PLACEHOLDER,
                            replacePlaceholder(
                                PW_INTRO_TEXT_PLACEHOLDER,
                                skuItem,
                                selectedOffer,
                                introOffer
                            ),
                        )
                    }
                    selectedOffer?.let {
                        put(
                            PW_OFFER_PRICE_PLACEHOLDER,
                            replacePlaceholder(
                                PW_OFFER_PRICE_PLACEHOLDER,
                                skuItem,
                                selectedOffer,
                                introOffer
                            ),
                        )
                    }
                    selectedOffer?.let {
                        put(
                            PW_OFFER_UNIT_PLACEHOLDER,
                            replacePlaceholder(
                                PW_OFFER_UNIT_PLACEHOLDER,
                                skuItem,
                                selectedOffer,
                                introOffer
                            ),
                        )
                    }
                    selectedOffer?.let {
                        put(
                            PW_OFFER_PERIOD_PLACEHOLDER,
                            replacePlaceholder(
                                PW_OFFER_PERIOD_PLACEHOLDER,
                                skuItem,
                                selectedOffer,
                                introOffer
                            ),
                        )
                    }
                    selectedOffer?.let {
                        put(
                            PW_OFFER_TEXT_PLACEHOLDER,
                            replacePlaceholder(
                                PW_OFFER_TEXT_PLACEHOLDER,
                                skuItem,
                                selectedOffer,
                                introOffer
                            ),
                        )
                    }
                }
            return productJson.toString().replace("\\\\", "")
        }
        return null
    }

    fun getPaywallServiceConfigExtra(): OAuthConfigStub.PaywallServiceConfigExtra {
        return OAuthConfigStub.PaywallServiceConfigExtra(
            PaywallService.getInstance().isPremiumUser(),
            PaywallService.getInstance().hasBeenPaywalled(),
            PaywallService.getConnector().getDeviceId(),
            PaywallService.getConnector().getAppVersion(),
            PaywallService.getConnector().isTablet(),
            Build.VERSION.RELEASE,
            PaywallService.getOmniture().getGenesisLocation(),
            PaywallService.getOmniture().getTestGroup(),
            PaywallService.getOmniture().getArcId(),
            PaywallService.getOmniture().getGenesisExperience()
        )
    }

    /**
     * Helper method to replace products `id` values with the amazon sku values from productToSkuMap.
     * Playstore and Amazon Walls are common other than the main walls. Walls were configured
     * with the products and products have playstore sku ids. Amazon can have its own wall configuration
     * if required. Otherwise products ids should be mapped correctly to Amazon to make them to work.
     */
    fun mapProductIdsToAmazonSkusIds(products: List<Product>): List<Product> {
        if (!Utils.isAmazonBuild()) return products

        val productNameToProductIdMap =
            ConfigManager.getInstance().config.paywallConf.productToSkuMap
        val amazonProducts = mutableListOf<Product>()

        products.forEach { product ->
            var amazonProduct: Product? = null
            val productId = (product as? Product.IapProduct)?.id
            // find an entry in the map that has product.id and then take corresponding amazon
            // sku id to override the product.id
            productNameToProductIdMap?.forEach { mapEntry ->
                val entry = mapEntry.value
                if (entry.playstore == productId || entry.amazon == productId) {
                    if (entry.amazon != null) {
                        amazonProduct = when (product) {
                            is Product.IapProduct -> product.copy(id = entry.amazon)
                            is Product.ExternalProduct -> product // No id to update
                            is Product.Registration -> product // No id to update
                        }
                    }
                }
            }
            amazonProduct?.let {
                amazonProducts.add(it)
            }
        }

        return amazonProducts
    }

    /**
     * Derives ad-free status from a list of SubItems (from verify/profile response)
     * and updates [PaywallReactive] accordingly using the specified source.
     *
     * This updates only the given source. The combined ad-free status is the OR
     * of all sources (billing, verify, and profile).
     */
    @JvmStatic
    fun updateAdFreeStatusFromSubscriptions(
        subscriptions: List<SubItem>?,
        source: AdFreeSource
    ) {
        if (subscriptions == null) return
        var hasAdFree = false
        var isAnyAdFreeRenewable = false
        for (subItem in subscriptions) {
            if (!PaywallReactive.isAdFreeProduct(subItem)) continue
            if (!PaywallConstants.ACTIVE.equals(subItem.subStatus, ignoreCase = true)) continue
            hasAdFree = true
            if (!PaywallConstants.SUB_STATE_CANCELLED.equals(subItem.subState, ignoreCase = true)) {
                isAnyAdFreeRenewable = true
            }
        }
        PaywallReactive.updateAdFreeRenewableStatus(isAnyAdFreeRenewable)
        when (source) {
            AdFreeSource.VERIFY -> PaywallReactive.updateAdFreeFromVerify(hasAdFree)
            AdFreeSource.PROFILE -> PaywallReactive.updateAdFreeFromProfile(hasAdFree)
        }
        if (hasAdFree) {
            PaywallReactive.updateAdFreeRenewableStatus(isAnyAdFreeRenewable)
        }
    }

    /**
     * Returns true if the user has an active ad-free subscription purchased via the web (DIGITAL source).
     */
    @JvmStatic
    fun hasAdFreeFromDigital(subscriptions: List<SubItem>?): Boolean {
        if (subscriptions == null) return false
        return subscriptions.any { subItem ->
            PaywallReactive.isAdFreeProduct(subItem) &&
                    PaywallConstants.DIGITAL_SOURCE.equals(subItem.source, ignoreCase = true)
        }
    }

    /**
     * Returns the active base subscription from the subscriptions list.
     *
     * There will only ever be 1 active base subscription in the list.
     * A "base" subscription is one whose product matches a value in [baseSubscriptionProducts]
     * from config (e.g., ["PREMIUM", "BASIC"]).
     *
     * @param subscriptions the list of SubItems from a verify/profile response
     * @param baseSubscriptionProducts list of base product names from config
     * @return the active base SubItem, or null if none found
     */
    @JvmStatic
    fun getBestAvailableBaseSubscription(
        subscriptions: List<SubItem>?,
        parentSubscriptionId: String?
    ): SubItem? {
        if (subscriptions.isNullOrEmpty()) return null

        // 1. Filter for items that are explicitly CORE and currently active
        val activeCoreSubs = subscriptions.filter { subItem ->
            "CORE".equals(subItem.productCategoryId, ignoreCase = true) && subItem.subStatus == PaywallConstants.ACTIVE
        }
        if (activeCoreSubs.isEmpty()) return null
        if (activeCoreSubs.size == 1) return activeCoreSubs.first()

        // Match against the root parent subscription ID if provided
        if (!parentSubscriptionId.isNullOrEmpty()) {
            val matchingParent = activeCoreSubs.find { it.subscriptionId == parentSubscriptionId }
            if (matchingParent != null) return matchingParent
        }

        // Fallback: Return the first active item
        return activeCoreSubs.first()
    }

    /**
     * Persists add-on subscriptions on the cached subscription.
     * Filters out the item whose subscriptionId matches the root subscription_id
     * (i.e. the base subscription), keeping only add-ons.
     */
    @JvmStatic
    fun persistAddonSubscriptions(
        subscriptions: List<com.washingtonpost.android.paywall.newdata.response.SubItem>?,
        rootSubscriptionId: String?
    ) {
        val cachedSub = PaywallService.getBillingHelper()?.cachedSubscription() ?: return
        if (subscriptions.isNullOrEmpty()) {
            cachedSub.addonSubscriptions = null
        } else {
            val addons = subscriptions.filter { subItem ->
                subItem.subscriptionId != rootSubscriptionId
            }
            cachedSub.addonSubscriptions = addons.ifEmpty { null }
        }
        PaywallService.getBillingHelper()?.updateSubscriptionDetails(cachedSub)
    }

    enum class AdFreeSource {
        VERIFY, PROFILE
    }
}