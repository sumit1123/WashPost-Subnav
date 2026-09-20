package com.wapo.flagship.sdk.iterable.handlers

import android.content.Context
import android.os.Bundle
import com.wapo.android.commons.iterable.AttributionInfo
import com.wapo.android.commons.iterable.toBundle
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.Logger
import com.wapo.android.commons.util.URLParser
import com.wapo.flagship.Utils
import com.wapo.flagship.features.articles2.utils.appendTrackingParams
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor
import com.wapo.flagship.features.shared.activities.DefaultNativePaywallResultCallbacks
import com.wapo.flagship.features.subscribebanner.state.BannerLifecycleEvent
import com.wapo.flagship.sdk.iterable.IterableAnalyticsHandler.Companion.ACTION_DISMISS
import com.wapo.flagship.sdk.iterable.IterableAnalyticsHandler.Companion.ACTION_EVENT_KEY
import com.wapo.flagship.sdk.iterable.IterableAnalyticsHandler.Companion.ATP_CTA_EVENT_KEY
import com.wapo.flagship.sdk.iterable.IterableAnalyticsHandler.Companion.BANNER_DISMISS_EVENT_KEY
import com.wapo.flagship.sdk.iterable.models.IamMessageType
import com.wapo.flagship.sdk.iterable.viewmodels.IterableActivityViewModel
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.util.tracking.states.AcquisitionEntranceTypeBuilder
import com.wapo.flagship.util.tracking.states.NavigationBehavior
import com.wapo.flagship.wapomain.MainConstants
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.paywallconf.Product
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.models.PromoAction
import com.washingtonpost.android.paywall.billing.NativePaywallListenerActivity
import com.washingtonpost.android.paywall.bottomsheet.viewmodel.PaywallSheet2ViewModel
import com.washingtonpost.android.paywall.helper.PaywallSheetHelper
import com.washingtonpost.android.paywall.models.BannerPaywallMessage
import com.washingtonpost.android.paywall.util.PaywallConstants
import com.washingtonpost.android.paywall.util.PaywallConstants.WallType
import com.washingtonpost.android.paywall.util.PaywallUtil

class BannerActionHandler(
    private val paywallSheetHelper: PaywallSheetHelper? = null,
    private val iterableActivityViewModel: IterableActivityViewModel
) {
    private val iterableConfig = ConfigManager.getInstance().config.iterableConfig

    /**
     * Execute the action carried by [banner].
     *
     * Evaluates the [PromoAction]'s action requirements via guardrails first.
     * If all requirements pass, the [BannerPaywallMessage.promoAction] is executed.
     * If any requirement fails, the [BannerPaywallMessage.fallbackAction] is executed instead.
     *
     * @param banner  The message whose [PromoAction] will be dispatched.
     * @param context Android [Context] used to start activities.
     * @param front   Section this banner appears on (Analytics).
     */
    fun execute(
        banner: BannerPaywallMessage?,
        context: Context,
        front: String?,
        onBannerClicked: () -> Unit = {},
        onShowArticleSoftwall: ((String) -> Unit)? = null,
        onShowStandardPaywall: ((wallName: String?, wallType: WallType, reason: Int) -> Unit)? = null
    ) {
        if (banner == null) {
            Logger.w(TAG, "execute(): banner is null – aborting")
            return
        }
        // analytics for GA banner click events
        trackOnPageTap(banner)
        // currently utilized to send general analytics for banner clicks
        onBannerClicked()

        // determine constructed placement type for analytics tracking, based on the banner's attribution info
        val placementType =
            iterableActivityViewModel.findResolvedBanner(banner.attributionInfo.messageId)?.first?.type

        placementType?.let {
            Measurement.setNavigationBehaviorInDefaultMap(getPurchaseNavigationBehavior(it))
        }

        val entranceType = placementType?.let {
            AcquisitionEntranceTypeBuilder.build(
                it,
                banner.messageTracking?.campaignName,
                banner.attributionInfo.campaignId,
                false,
            )
        }

        when (val action = resolveBannerAction(banner)) {
            is PromoAction.Blocker -> {
                var wallReason = PaywallConstants.GLOBAL_SUBSCRIBE_BUTTON
                var wallType = WallType.GLOBAL_SUBSCRIBE_BUTTON_PAYWALL

                when (banner.attributionInfo.placementId) {
                    iterableConfig.article -> {
                        Measurement.setNavigationBehavior(NavigationBehavior.ARTICLE_INLINE_OFFER)
                    }

                    iterableConfig.section -> {
                        Measurement.setNavigationBehavior(NavigationBehavior.SECTION_INLINE_OFFER)

                        wallReason = PaywallConstants.SECTION_INLINE_OFFER
                        wallType = WallType.SECTION_INLINE_OFFER_PAYWALL
                    }
                }

                val hasSub = PaywallService.getInstance()
                    .isPremiumUser() || PaywallService.getInstance().isSubActive

                when {
                    onShowStandardPaywall != null && !hasSub -> {
                        onShowStandardPaywall.invoke(action.name, wallType, wallReason)
                    }

                    onShowArticleSoftwall != null && !hasSub -> {
                        onShowArticleSoftwall.invoke(action.name ?: "")
                    }

                    else -> {
                        paywallSheetHelper?.showWall(
                            wallName = action.name,
                            wallType = wallType,
                            wallReason = wallReason,
                            preventAutoDismiss = true,
                            attributionInfo = banner?.attributionInfo
                        )
                    }
                }

                // retrieve blocker product for analytics purposes
                val blockerProductId = when {
                    action.name == PaywallConstants.WALL_NAME_AD_FREE_LEGAL ->
                        PaywallService.getInstance()?.adFreeProductId

                    else ->
                        ConfigManager.getInstance().config.paywallConf
                            ?.blockers
                            ?.firstOrNull { it.name == action.name }
                            ?.items
                            ?.filterIsInstance<Product.IapProduct>()
                            ?.firstOrNull()
                            ?.id
                            ?.let { PaywallUtil.mapProductNameToProductId(it) }
                }

                trackImpressionEvent(
                    attributionInfo = banner.attributionInfo,
                    eventType = PaywallSheet2ViewModel.EventType.PURCHASE_EVENT,
                    productId = blockerProductId
                )
            }

            is PromoAction.Code -> {
                val offerId = if (!action.code.isNullOrBlank()) {
                    action.code
                } else {
                    null
                }
                launchNativePurchaseFlow(
                    context,
                    action.product,
                    offerId,
                    banner,
                    entranceType
                )
            }

            is PromoAction.Open -> {
                var url = action.url
                if (url == null) {
                    Logger.w(TAG, "execute(): url is null – aborting")
                    return
                }

                when (banner.attributionInfo.placementId) {
                    iterableConfig.article -> {
                        Measurement.setNavigationBehavior(NavigationBehavior.ARTICLE_INLINE_OFFER)
                        Measurement.trackInlineOfferArticleClick(
                            appendTrackingParams(
                                url,
                                Measurement.INLINE_ARTICLE_EXTRA_ACCOUNT,
                                ""
                            )
                        )
                    }

                    iterableConfig.section -> {
                        Measurement.setNavigationBehavior(NavigationBehavior.SECTION_INLINE_OFFER)
                        Measurement.setContentUrl(
                            appendTrackingParams(
                                url,
                                Measurement.INLINE_HOMEPAGE_EXTRA_ACCOUNT,
                                front
                            )
                        )
                    }

                    iterableConfig.askThePost, iterableConfig.askThePostBanner -> {
                        Measurement.trackBannerClicked(url)
                    }

                    iterableConfig.frontHomeScroll -> {
                        val isSubActive = PaywallService.getInstance().isPremiumUser
                        Measurement.setSignInPromptNavigationBehavior(isSubActive, front)
                    }

                    else -> {
                    }
                }

                if (!DeepLinksProcessor.isExternalUrl(URLParser(url)) && !DeepLinksProcessor.isDeepLinkSupported(
                        URLParser(url)
                    )
                ) {
                    Utils.startWebActivity(url, context, false, true)
                } else {
                    DeepLinksProcessor.processAsync(
                        link = url,
                        bundle = (banner.attributionInfo.toBundle() ?: Bundle()).apply {
                            putString(MainConstants.EXTRA_CAMPAIGN_ENTRANCE_TYPE, entranceType)
                        },
                        activityContext = context,
                        sourceType = DeepLinksProcessor.SourceType.ITERABLE,
                    )
                }

                val isFallback =
                    banner.promoAction is PromoAction.Blocker || banner.promoAction is PromoAction.Code
                if (isFallback) {
                    EventLog.Builder().apply {
                        setMessage("Offer fallback to mweb")
                        setModule(LogModules.PAYWALL)
                        setContentUrl(url)
                    }.run {
                        PaywallService.getConnector().logD(this)
                    }
                }

                trackImpressionEvent(
                    attributionInfo = banner.attributionInfo,
                    eventType = PaywallSheet2ViewModel.EventType.URL_EVENT,
                    url = url
                )
            }

            else -> {
                return
            }
        }

        if (banner.consume == "action") {
            dismissBanner(banner, front)
        }
    }

    fun dismissBanner(
        banner: BannerPaywallMessage?,
        sectionFront: String?
    ) {
        if (banner == null) {
            Logger.w(TAG, "dismissBanner(): banner is null – aborting dismiss")
            return
        }

        val placementType =
            iterableActivityViewModel.findResolvedBanner(banner.attributionInfo.messageId)?.first?.type

        val campaignEntranceType = AcquisitionEntranceTypeBuilder.build(
            placementType ?: "unknown",
            banner.messageTracking?.campaignName,
            banner.attributionInfo.campaignId,
            false
        )
        handleLifecycleEvent(BannerLifecycleEvent.Dismiss(banner.attributionInfo))
        iterableActivityViewModel.clearInAppMessage(banner.attributionInfo.messageId)
        when (placementType) {
            IamMessageType.FRONT_HOME_SCROLL.type -> {
                val eventKey = BANNER_DISMISS_EVENT_KEY
                val dataFields: Map<String, Any> =
                    mapOf(eventKey to IamMessageType.FRONT_HOME_SCROLL.name)
                sendIterableCustomEvent(eventKey, dataFields)
                val isSubActive = PaywallService.getInstance().isPremiumUser
                Measurement.trackSignInPromptDismissed(
                    isSubActive,
                    campaignEntranceType,
                    sectionFront
                )
            }

            IamMessageType.ASK_THE_POST_BANNER.type -> {
                Measurement.trackBannerDismissed(banner.url)
                val eventKey = ATP_CTA_EVENT_KEY
                val actionKey = ACTION_EVENT_KEY
                val map = mapOf(actionKey to ACTION_DISMISS)
                sendIterableCustomEvent(eventKey, map)
            }
        }
    }

    private fun trackOnPageTap(banner: BannerPaywallMessage) {
        iterableActivityViewModel.findResolvedBanner(banner.attributionInfo.messageId)
            ?.let { (placement, banner) ->
                iterableActivityViewModel.trackBannerClicked(
                    placementType = placement.type,
                    contentUrl = banner.url,
                    kind = banner.messageTracking?.kind,
                    campaignName = banner.messageTracking?.campaignName,
                    campaignId = banner.attributionInfo.campaignId,
                    navigationBehavior = getPurchaseNavigationBehavior(placement.type),
                )
            }
    }

    private fun trackAppsWall(banner: BannerPaywallMessage) {
        iterableActivityViewModel.findResolvedBanner(banner.attributionInfo.messageId)
            ?.let { (placement, banner) ->
                iterableActivityViewModel.trackWallDisplayed(
                    placementType = placement.type,
                    contentUrl = banner.url,
                    kind = banner.messageTracking?.kind,
                    campaignName = banner.messageTracking?.campaignName,
                    campaignId = banner.attributionInfo.campaignId,
                    navigationBehavior = getPurchaseNavigationBehavior(placement.type),
                )
            }
    }

    private fun launchNativePurchaseFlow(
        context: Context,
        productName: String?,
        offerId: String?,
        banner: BannerPaywallMessage,
        entranceType: String?
    ) {
        val productId = PaywallUtil.mapProductNameToProductId(productName)
        if (productId.isNullOrEmpty()) {
            Logger.w(TAG, "launchNativePurchaseFlow(): unknown product '$productName' – aborting")
            return
        }

        var fallbackReason = PaywallConstants.GLOBAL_SUBSCRIBE_BUTTON
        var fallbackOrdinal = WallType.GLOBAL_SUBSCRIBE_BUTTON_PAYWALL.ordinal
        val attributionInfo = banner.attributionInfo

        when (attributionInfo.placementId) {
            iterableConfig.article -> {
                Measurement.setNavigationBehavior(NavigationBehavior.ARTICLE_INLINE_OFFER)
                fallbackReason = PaywallConstants.ARTICLE_INLINE_OFFER
                fallbackOrdinal = WallType.ARTICLE_INLINE_OFFER_PAYWALL.ordinal
            }

            iterableConfig.section -> {
                Measurement.setNavigationBehavior(NavigationBehavior.SECTION_INLINE_OFFER)
                fallbackReason = PaywallConstants.SECTION_INLINE_OFFER
                fallbackOrdinal = WallType.SECTION_INLINE_OFFER_PAYWALL.ordinal
            }
        }

        val kind = banner.messageTracking?.kind

        val purchaseBundle = (attributionInfo.toBundle() ?: Bundle()).apply {
            putString(NativePaywallListenerActivity.MESSAGE_TRACKING_KIND, kind)
            banner.messageTracking?.campaignName?.let {
                putString(NativePaywallListenerActivity.MESSAGE_TRACKING_CAMPAIGN_NAME, it)
            }
            banner.messageTracking?.offerType?.let {
                putString(NativePaywallListenerActivity.MESSAGE_TRACKING_OFFER_TYPE, it)
            }
            entranceType?.let {
                putString(
                    NativePaywallListenerActivity.SUBSCRIPTION_UPGRADE_LOCATION,
                    it
                )
            }
        }

        val adFreeIterableProducts =
            ConfigManager.getInstance().config.paywallConf?.adFreeIterableProducts ?: emptyList()
        val isAdFree = !productName.isNullOrEmpty() && adFreeIterableProducts.contains(productName)

        val service = PaywallService.getInstance()
        val baseProductId = service?.inAppSubProductId
        val adFreeProductId = service?.adFreeProductId

        if (isAdFree && !baseProductId.isNullOrEmpty() && !adFreeProductId.isNullOrEmpty()) {
            context.startActivity(
                NativePaywallListenerActivity.getPurchaseWithAddOnsIntent(
                    context,
                    baseProductId,
                    arrayListOf(adFreeProductId),
                    purchaseBundle,
                )
            )
        } else if (!isAdFree && !baseProductId.isNullOrEmpty() && (
                    NativePaywallListenerActivity.isUpgrade(
                        baseProductId, productId
                    ) || NativePaywallListenerActivity.isDowngrade(baseProductId, productId))
        ) {
            context.startActivity(
                NativePaywallListenerActivity.getSubscriptionUpdateIntent(
                    context,
                    baseProductId,
                    productId,
                    purchaseBundle,
                )
            )
        } else {
            NativePaywallListenerActivity.launch(
                context,
                productId,
                offerId,
                entranceType,
                purchaseBundle,
                DefaultNativePaywallResultCallbacks(
                    fallbackPaywallReason = fallbackReason,
                    fallbackPaywallTypeOrdinal = fallbackOrdinal,
                ),
            )
        }

        if (offerId != null) {
            trackImpressionEvent(
                attributionInfo = attributionInfo,
                eventType = PaywallSheet2ViewModel.EventType.OFFER_PURCHASE_EVENT,
                productId = productId,
                offerId = offerId
            )
        } else {
            trackImpressionEvent(
                attributionInfo = attributionInfo,
                eventType = PaywallSheet2ViewModel.EventType.PURCHASE_EVENT,
                productId = productId,
            )
        }
    }

    private fun getPurchaseNavigationBehavior(placementType: String): String =
        when (placementType) {
            IamMessageType.MY_POST_BANNER.type -> Measurement.PATH_TO_VIEW_MY_POST_BANNER
            IamMessageType.BANNER.type -> NavigationBehavior.GLOBAL_SUBSCRIBE_BUTTON.value
            IamMessageType.ARTICLE.type -> NavigationBehavior.ARTICLE_INLINE_OFFER.value
            IamMessageType.SECTION.type -> NavigationBehavior.SECTION_INLINE_OFFER.value
            else -> placementType
        }

    // resolves the banner's action based on guardrail requirements, returning either the promoAction or fallbackAction (open url)
    private fun resolveBannerAction(banner: BannerPaywallMessage?): PromoAction? {
        if (banner == null) {
            return null
        }
        // evaluate the CTA's guardrail requirements
        val requirementsPassed =
            iterableActivityViewModel.evaluateActionRequirements(
                when (val promo = banner.promoAction) {
                    is PromoAction.Blocker -> promo.actionRequirements
                    is PromoAction.Code -> promo.actionRequirements
                    else -> null
                }
            )

        // allow promoAction if requirements passed, otherwise fallback to fallbackAction
        return if (requirementsPassed) {
            banner.promoAction
        } else {
            banner.fallbackAction
        }
    }

    /**
     * Handle a non-CTA lifecycle event (impressions, dismiss).
     * These events are analytics-only and don't require a [BannerPaywallMessage].
     */
    fun handleLifecycleEvent(
        event: BannerLifecycleEvent,
        message: BannerPaywallMessage? = null,
        attributionInfo: AttributionInfo? = null
    ) {
        when (event) {
            is BannerLifecycleEvent.StartImpression -> {
                val info = event.attributionInfo ?: message?.attributionInfo ?: attributionInfo
                info?.let {
                    trackImpressionEvent(
                        attributionInfo = info,
                        eventType = PaywallSheet2ViewModel.EventType.START_IMPRESSION_EVENT
                    )
                    // fire banner displayed event for GA analytics
                    iterableActivityViewModel.findResolvedBanner(it.messageId)
                        ?.let { (placement, banner) ->
                            iterableActivityViewModel.trackBannerDisplayed(
                                placementType = placement.type,
                                contentUrl = banner.url,
                                kind = banner.messageTracking?.kind,
                                campaignName = banner.messageTracking?.campaignName,
                                campaignId = banner.attributionInfo.campaignId
                            )
                        }
                }
            }

            is BannerLifecycleEvent.EndImpression -> {
                val info = event.attributionInfo ?: message?.attributionInfo ?: attributionInfo
                info?.let {
                    trackImpressionEvent(
                        attributionInfo = info,
                        eventType = PaywallSheet2ViewModel.EventType.PAUSE_IMPRESSION_EVENT
                    )
                }
            }

            is BannerLifecycleEvent.Dismiss -> {
                val info = event.attributionInfo ?: message?.attributionInfo ?: attributionInfo
                info?.let {
                    trackImpressionEvent(
                        attributionInfo = info,
                        eventType = PaywallSheet2ViewModel.EventType.DISMISS_EVENT
                    )
                }
            }
        }
    }

    private fun trackImpressionEvent(
        attributionInfo: AttributionInfo,
        eventType: PaywallSheet2ViewModel.EventType,
        productId: String? = null,
        offerId: String? = null,
        url: String? = null
    ) {
        iterableActivityViewModel.trackEvent(
            eventType = eventType,
            attributionInfo = attributionInfo,
            productId = productId,
            offerId = offerId,
            url = url
        )
    }

    private fun sendIterableCustomEvent(eventKey: String, dataFields: Map<String, Any>) {
        iterableActivityViewModel.sendIterableCustomEvent(eventKey, dataFields)
    }

    companion object {
        private val TAG = BannerActionHandler::class.java.simpleName
    }
}
