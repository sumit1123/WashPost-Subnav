package com.washingtonpost.android.paywall.bottomsheet.viewmodel

import android.os.Bundle
import androidx.lifecycle.*
import com.wapo.android.commons.iterable.AttributionInfo
import com.wapo.android.commons.iterable.toBundle
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.LiveEvent
import com.wapo.android.commons.util.Utils
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.paywallconf.Blocker
import com.washingtonpost.android.config.domain.models.config.paywallconf.Component
import com.washingtonpost.android.config.domain.models.config.paywallconf.Product
import com.washingtonpost.android.paywall.PaywallOmniture
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.bottomsheet.*
import com.washingtonpost.android.paywall.helper.componentTextToString
import com.washingtonpost.android.paywall.metering.MeteringPrefs
import com.washingtonpost.android.paywall.models.PaywallMessageItem
import com.washingtonpost.android.paywall.newdata.model.IAPSubItem
import com.washingtonpost.android.paywall.util.PaywallConstants
import com.washingtonpost.android.paywall.util.PaywallUtil
import com.washingtonpost.android.paywall.util.PaywallUtil.getFormattedOfferForProductId
import com.washingtonpost.android.paywall.util.PaywallUtil.getTilesFormattedPricePerPeriod
import com.washingtonpost.android.paywall.util.combineWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PaywallSheet2ViewModel : ViewModel() {

    val periodSelectedLiveData = MutableLiveData<GroupType?>(null)

    var wallName: String? = null
    var category: String? = null
    var analyticsWallName: String? = null
    var defaultInterval: Int? = null
    var promoId: String? = null
    var trialType: String? = null
    var attributionInfo: AttributionInfo? = null


    val isRegwallCategory
        get() = category == PaywallConstants.WallCategory.REGWALL.name.lowercase()

    val isSoftwallCategory
        get() = category == PaywallConstants.WallCategory.SOFTWALL.name.lowercase()

    private val isIapTerminated
        get() = PaywallService.getInstance()?.isIapTerminated ?: false

    private val isValidFreeArticleUser
        get() = PaywallService.getInstance() != null && PaywallService.getInstance().isFreeArticlesUser && MeteringPrefs.getFreeArticlesRemaining() > 0
    
    /**
     * LiveEvent to observe users [SubState] - No Sub, Terminated Sub, Active Sub.
     */
    private val _subStateLiveEvent = LiveEvent<SubState>()
    val subStateLiveEvent: LiveEvent<SubState> = _subStateLiveEvent

    /**
     * LiveEvent to observe users [SignInState] - Signed In, Signed Out
     */
    private val _signInStateLiveEvent = LiveEvent<SignInState>()
    val signInStateLiveEvent: LiveEvent<SignInState> = _signInStateLiveEvent

    /**
     * LiveEvent to observe paywall entry point - Global Subscribe, Metered, Bottom Cta, etc.
     */
    private val _paywallType = LiveEvent<PaywallConstants.WallType>()
    val paywallType: LiveEvent<PaywallConstants.WallType> = _paywallType

    /**
     * LiveEvent to manage user UI clicks - Subscribe, Sign In, Privacy Policy, etc.
     */
    private val _userEvent = LiveEvent<UserEvent>()
    val userEvent: LiveEvent<UserEvent> = _userEvent

    val isRegwall
        get() = paywallType.value == PaywallConstants.WallType.REGWALL || paywallType.value == PaywallConstants.WallType.REGWALL_TILE

    val isSignedIn
        get() = signInStateLiveEvent.value == SignInState.SignedIn

    /**
     * Update Subscription and Sign In state - Used to update on each resume call of the paywall.
     */
    fun update() {
        _subStateLiveEvent.value = getSubState()
        _signInStateLiveEvent.value =
            if (PaywallService.getInstance().isWpUserLoggedIn) SignInState.SignedIn else SignInState.SignedOut
    }

    /**
     * Get subscription state from [PaywallService] and map it to [SubState]
     */
    private fun getSubState(): SubState {
        return PaywallService.getInstance()?.subState ?: SubState.NoSub
    }

    /**
     * Get specific [Blocker] from messages or PaywallConfig model if [wallName] is provided.
     * Otherwise get the first blocker in the list of blockers from the model.
     */
    fun getBlocker(wallName: String?): Blocker? {
        // Return blocker from messages if there is one otherwise find it in the configs.
        PaywallService.getConnector().getBlockerFromMessages(wallName)?.run {
            return this
        }
        val blockers = ConfigManager.getInstance().config.paywallConf.blockers
        return blockers?.firstOrNull { it.name == wallName }
            ?: blockers?.firstOrNull { it.name == PaywallService.getConnector().blocker }
            ?: if (Utils.isAmazonBuild()) {
                blockers?.firstOrNull { it.name == PaywallConstants.WALL_NAME_AMAZON_MAIN }
            } else {
                blockers?.firstOrNull { it.name == PaywallConstants.WALL_NAME_MAIN }
            }
    }

    fun getComponentList(): List<Component>? {
        val blocker = getBlocker(wallName)
        category = blocker?.category
        promoId = blocker?.promoId
        trialType = blocker?.trialType
        // Ask connector which version to use (handles backward compatibility)
        return PaywallService.getConnector().getComponents(blocker , category)
    }

    /**
     * Get appropriate text based on [PaywallType] and [SubState].
     */
    fun getAppropriateText(component: Component): LiveData<String?> {
        return paywallType.combineWith(subStateLiveEvent) { paywallType, subState ->
            when {
                paywallType == PaywallConstants.WallType.GLOBAL_SUBSCRIBE_BUTTON_PAYWALL -> component.text.componentTextToString()
                paywallType == PaywallConstants.WallType.REGWALL -> component.text.componentTextToString()
                paywallType == PaywallConstants.WallType.SAVE_REGWALL -> component.text.componentTextToString()
                paywallType == PaywallConstants.WallType.PAUSEWALL -> component.text.componentTextToString()
                paywallType == PaywallConstants.WallType.GIFT_EXPIRED_PAYWALL -> component.giftExpired
                paywallType == PaywallConstants.WallType.GIFT_INVALID_PAYWALL -> component.giftInvalid
                subState == SubState.NoSub || (subState == SubState.FreeTrialSub && !isIapTerminated) ->
                    PaywallUtil.buildDynamicTextForPaywall(component.noneContentDynamic) ?: component.noneContent ?: component.text.componentTextToString()
                subState == SubState.TerminatedSub || (subState == SubState.FreeTrialSub && isIapTerminated) ->
                    PaywallUtil.buildDynamicTextForPaywall(component.endedContentDynamic) ?: component.endedContent ?: component.text.componentTextToString()
                else -> null
            }
        }
    }

    /**
     * Get appropriate font size based on Component variant
     */
    fun getComponentFontSize(variation: String?): Float? {
        return when (variation) {
            PaywallConstants.H1 -> 28f
            PaywallConstants.H2 -> 20f
            PaywallConstants.H3 -> 18f
            PaywallConstants.H4 -> 16f
            PaywallConstants.H5 -> 14f
            else -> null
        }
    }

    /**
     * Replaces placeholder values in dynamic name. Falls back to static name.
     */
    fun getAppropriateProductName(product: Product): String? {
        return when(product) {
            is Product.IapProduct -> PaywallUtil.buildDynamicTextForPaywall(product.nameDynamic) ?: product.name
            is Product.ExternalProduct -> product.tileLabel
            is Product.Registration -> product.tileLabel
        }
    }

    /**
     * Replaces placeholder values in dynamic product text list.
     * Falls back to static text list.
     */
    fun getAppropriateProductTextList(product: Product): List<String>? {
        return if (product.textDynamic == null) {
            product.text
        } else {
            val list = mutableListOf<String>()
            product.textDynamic?.forEach { item ->
                val dynamicText = PaywallUtil.buildDynamicTextForPaywall(item)
                if (dynamicText == null) { // fall back to non-dynamic text if anything goes wrong
                    return product.text
                } else {
                    list.add(dynamicText)
                }
            }
            list
        }
    }

    /**
     * Get filtered list of products based on [GroupType].
     * Products can be grouped by [intervals] (Monthly/Yearly)
     * or they can be grouped by [tiers] (Core/Premium)
     */
    fun getProducts(wallName: String?): LiveData<List<Product>> {
        return periodSelectedLiveData.map { groupType ->
            val blocker = getBlocker(wallName)
            val products = PaywallService.getConnector().getProducts(blocker, category)

            val filteredProducts = when (groupType) {
                GroupType.Monthly -> products.filter {
                    (it as? Product.IapProduct)?.id?.endsWith("annual", true) != true ||
                            (it as? Product.IapProduct)?.id?.endsWith("sub", true) == true
                }
                GroupType.Yearly -> products.filter { (it as? Product.IapProduct)?.id?.endsWith("annual", true) == true }
                GroupType.Core -> products.filter { (it as? Product.IapProduct)?.id?.contains("basic") == true }
                GroupType.Premium -> products.filter { (it as? Product.IapProduct)?.id?.contains("basic") != true }
                null -> products
            }.run {
                if (Utils.isAmazonBuild()) {
                    PaywallUtil.mapProductIdsToAmazonSkusIds(this)
                } else {
                    this
                }
            }.reorderByItemIds(getPaywallMessageItems())
                .applyItemFieldOverrides(getPaywallMessageItems())
            filteredProducts
        }
    }

    /**
     * Reorder the products list so that whenever an Iterable item's ID
     * (PaywallMessageItem.name, from Item.<n>.ID) matches a Product ID from the
     * raw JSON, that product is placed at the item's index. Products that don't
     * match any item keep their original relative order in the remaining slots.
     *
     * Matching is case-sensitive. IapProduct, ExternalProduct, and
     * Registration all expose an id that can be matched against Item.<n>.ID.
     */
    private fun List<Product>.reorderByItemIds(
        items: List<PaywallMessageItem?>?
    ): List<Product> {
        if (items.isNullOrEmpty() || this.isEmpty()) return this

        fun productId(product: Product): String? = when (product) {
            is Product.IapProduct -> product.id
            is Product.ExternalProduct -> product.id
            is Product.Registration -> product.id
        }

        val remaining = this.toMutableList()
        val slots = arrayOfNulls<Product>(this.size)

        // First pass: place products at the index of the matching item.
        items.forEachIndexed { index, item ->
            if (index !in slots.indices) return@forEachIndexed
            val itemId = item?.name?.takeIf { it.isNotEmpty() } ?: return@forEachIndexed
            val match = remaining.firstOrNull { productId(it) == itemId } ?: return@forEachIndexed
            slots[index] = match
            remaining.remove(match)
        }

        // Second pass: fill remaining empty slots with the leftover products,
        // preserving their original relative order.
        val leftover = remaining.iterator()
        for (i in slots.indices) {
            if (slots[i] == null && leftover.hasNext()) {
                slots[i] = leftover.next()
            }
        }

        return slots.filterNotNull()
    }

    /**
     * Overlay Iterable custom-text-field values (Item.<n>.Label / Title / Caption /
     * Action / URL) onto the corresponding tile Product. Applied here so the initial
     * default-selected tile's CTA and click target reflect the Iterable overrides
     * without waiting for the user to tap a tile.
     */
    private fun List<Product>.applyItemFieldOverrides(
        items: List<PaywallMessageItem?>?
    ): List<Product> {
        if (items.isNullOrEmpty()) return this
        return mapIndexed { index, product ->
            val item = items.getOrNull(index) ?: return@mapIndexed product
            val labelOverride = item.label?.takeIf { it.isNotEmpty() }
            val titleOverride = item.title?.takeIf { it.isNotEmpty() }
            val captionOverride = item.caption?.takeIf { it.isNotEmpty() }
            val actionOverride = item.action?.takeIf { it.isNotEmpty() }
            val urlOverride = item.url?.takeIf { it.isNotEmpty() }
            when (product) {
                is Product.ExternalProduct -> product.copy(
                    tileLabel = labelOverride ?: product.tileLabel,
                    tileTitle = titleOverride ?: product.tileTitle,
                    tileCaption = captionOverride ?: product.tileCaption,
                    action = actionOverride ?: product.action,
                    url = urlOverride ?: product.url
                )
                is Product.Registration -> product.copy(
                    tileLabel = labelOverride ?: product.tileLabel,
                    tileTitle = titleOverride ?: product.tileTitle,
                    tileCaption = captionOverride ?: product.tileCaption,
                    action = actionOverride ?: product.action,
                    authorize = urlOverride ?: product.authorize
                )
                else -> product
            }
        }
    }

    fun getProductButtonText(product: Product): String {
        val productId = (product as? Product.IapProduct)?.id
        val mappedProductId = productId?.let { PaywallUtil.mapProductNameToProductId(it) } ?: productId
        val iapSubItem =
            if (PaywallService.getConnector() != null && mappedProductId != null) {
                PaywallService.getConnector().iapSubItems.getItem(mappedProductId)
            } else {
                null
            }
        return PaywallUtil.getActionTextForProduct(
            product = product,
            ctaType = PaywallConstants.CTA_WALL,
            iapSubItem = iapSubItem,
            subState = subStateLiveEvent.value,
            isIapTerminated = isIapTerminated,
            wallName = wallName
        )
    }

    fun setPaywallType(paywallType: PaywallConstants.WallType?) {
        paywallType?.let { _paywallType.postValue(it) }
    }


    /**
     * Only use this for Amazon Intro Offers since they do not come through
     * from AmazonIAPListener.
     */
    private fun getConfigIntroOffer(introPriceText: String?): String? {
        if (Utils.isAmazonBuild()) {
            introPriceText?.let {
                return it
            }
        }
        return null
    }


    /**
     * Get analytics manager.
     */
    fun getAnalytic(): PaywallOmniture {
        return PaywallService.getOmniture()
    }

    /**
     * Log out user and than call onComplete.
     * Common use case will be Signing in as a different user.
     */
    fun logOutUser(onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            PaywallService.getInstance().logOutCurrentUser()
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    /**
     * All components are visible by default and
     * this method validates when component has values for visibility field.
     */
    fun isComponentVisible(component: Component): Boolean {
        component.visibility?.takeIf { it.isNotEmpty() }?.let { visibility ->
            var visible = false
            visibility.split(" ").forEach {
                visible = visible || when (it.trim()) {
                    ComponentVisibility.ENDED_CONTENT.id -> {
                        eligibleForEndedContent(subStateLiveEvent.value)
                    }

                    ComponentVisibility.NONE_CONTENT.id -> {
                        eligibleForNoneContent(subStateLiveEvent.value)
                    }

                    ComponentVisibility.GIFT_EXPIRED.id -> {
                        eligibleForGiftExpired(paywallType.value)
                    }

                    ComponentVisibility.V2.id -> {
                        true
                    }

                    else -> {
                        visible
                    }
                }
            }
            return visible
        }
        return true
    }

    /**
     * Returns the display price for a product tile.
     * - Tries IAP cache first (via connector.iapSubItems) to get current pricing.
     * - Prefers an active offer price (getFormattedOfferForProductId) when available.
     * - Falls back to a formatted price-per-period using basePrice + subscriptionPeriod.
     */

    fun getProductPrice(product: Product): String? {
        return when (product) {
            is Product.IapProduct -> {
                val iapSubItem = getIAPSubItem(product)
                iapSubItem?.apply {
                    val offerPrice = getFormattedOfferForProductId(productId ?: "", wallName)
                    return offerPrice ?: getTilesFormattedPricePerPeriod(
                        basePrice,
                        subscriptionPeriod,
                        productId
                    )
                }
                "\$X"
            }
            is Product.ExternalProduct -> product.tileTitle
            is Product.Registration -> product.tileTitle
        }
    }


    /**
     * Returns the CTA button text for a product tile.
     */
    fun getProductTileButtonText(product: Product): String? {
        return when (product) {
            is Product.IapProduct -> {
                val iapSubItem = getIAPSubItem(product)
                iapSubItem?.let { PaywallUtil.getCtaTextForTiles(it, product, wallName) }
            }
            is Product.ExternalProduct -> product.tileCaption
            is Product.Registration -> null
        }
    }


    private fun getIAPSubItem(product: Product): IAPSubItem? {
        val productId = (product as? Product.IapProduct)?.id
        val mappedProductId = productId?.let { PaywallUtil.mapProductNameToProductId(it) } ?: productId
        return if (PaywallService.getConnector() != null && mappedProductId != null) {
            PaywallService.getConnector().iapSubItems.getItem(mappedProductId)
        } else {
            null
        }
    }


    private fun eligibleForEndedContent(subState: SubState?): Boolean =
        subState == SubState.TerminatedSub || (subState == SubState.FreeTrialSub && isIapTerminated)

    private fun eligibleForNoneContent(subState: SubState?): Boolean =
        subState == SubState.NoSub || (subState == SubState.FreeTrialSub && !isIapTerminated)

    private fun eligibleForGiftExpired(paywallType: PaywallConstants.WallType?): Boolean =
        paywallType == PaywallConstants.WallType.GIFT_EXPIRED_PAYWALL

    fun trackMessageEvent(
        paywallAnalytics: PaywallOmniture?,
        eventType: EventType,
        productId: String? = null,
        offerId: String? = null,
        url: String? = null
    ) {
        viewModelScope.launch {
            // Added delay before startImpression call to maintain gap between
            // startSession and startImpression calls as both are firing same time.
            if (eventType == EventType.START_IMPRESSION_EVENT) {
                delay(100)
            }
            val message = PaywallService.getConnector().getBlockerPaywallMessage(wallName)
            val info = attributionInfo ?: message?.attributionInfo
            paywallAnalytics?.trackMessageEvent(
                eventType, info, productId, offerId, null, url
            )
        }
    }

    fun getPurchaseExtras(): Bundle? {
        val message = PaywallService.getConnector().getBlockerPaywallMessage(wallName)
        val info = attributionInfo ?: message?.attributionInfo
        return info?.toBundle()
    }

    fun logMessageSource() {
        val message = PaywallService.getConnector().getBlockerPaywallMessage(wallName)
        val info = attributionInfo ?: message?.attributionInfo
        EventLog.Builder().apply {
            setMessage("Iterable Blocker Load")
            set("wall_name", wallName)
            if (info != null) {
                set("campaign", info.campaignId)
                set("messageId", info.messageId)
                set("placement", info.placementId)
                set("name", message?.blocker?.name)
                set("result", "success")
            } else {
                set("name", getBlocker(wallName)?.name)
                set("result", "failure")
            }
            setModule(LogModules.ITERABLE)
            setForceUpload()
            PaywallService.getConnector().logD(this)
        }
    }

    // Item fields from Blocker Paywall Message. These are them item fields (from Iterable) from the blocker config.
    fun getPaywallMessageItems(): List<PaywallMessageItem?>? {
        val message = PaywallService.getConnector().getBlockerPaywallMessage(wallName)
        return message?.items
    }

    companion object {
        @JvmStatic
        val REASON_KEY = "reason"
        private val DYNAMIC_TEXT_REGEX = "(?<=\\{)([^\\}]+)(?=\\})".toRegex()
    }

    enum class ComponentVisibility(val id: String) {
        ENDED_CONTENT("ended-content"),
        NONE_CONTENT("none-content"),
        GIFT_EXPIRED("gift-expired"),
        V2("v2");
    }

    /**
     * Tracking event types for Placements. Paywall Analytics from App module handles the events.
     */
    enum class EventType {
        START_IMPRESSION_EVENT,
        PAUSE_IMPRESSION_EVENT,
        PURCHASE_EVENT,
        PRODUCT_PURCHASE_EVENT,
        OFFER_PURCHASE_EVENT,
        URL_EVENT,
        SIGN_IN_EVENT,
        PURCHASE_COMPLETE_EVENT,
        DISMISS_EVENT
    }
}
