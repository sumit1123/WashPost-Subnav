// Copyright (c) 2025 The Washington Post. All rights reserved.
package com.wapo.flagship.sdk.iterable

import com.iterable.iterableapi.CommerceItem
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableAttributionInfo
import com.iterable.iterableapi.IterableEmbeddedMessage
import com.iterable.iterableapi.IterableInAppCloseAction
import com.iterable.iterableapi.IterableInAppLocation
import com.iterable.iterableapi.IterableInAppMessage
import com.wapo.android.commons.iterable.AttributionInfo
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.deeplinks.AirshipAnalytics
import com.wapo.flagship.features.deeplinks.InAppMessageData
import com.wapo.flagship.sdk.iterable.IterableAppUserFields.Companion.BASE_PRICE_FIELD
import com.wapo.flagship.sdk.iterable.IterableAppUserFields.Companion.CAMPAIGN_NAME_FIELD
import com.wapo.flagship.sdk.iterable.IterableAppUserFields.Companion.EVENT_CURRENCY_CODE_FIELD
import com.wapo.flagship.sdk.iterable.IterableAppUserFields.Companion.OFFER_TYPE_FIELD
import com.wapo.flagship.sdk.iterable.IterableAppUserFields.Companion.PLAN_STRUCTURE_FIELD
import com.wapo.flagship.sdk.iterable.IterableAppUserFields.Companion.STORE_COUNTRY_CODE_FIELD
import com.wapo.flagship.sdk.iterable.handlers.IterableAppInAppHandler
import com.wapo.flagship.sdk.iterable.models.CONSUME_ACTION
import com.wapo.flagship.util.tracking.Events
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.paywall.PaywallService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject

class IterableAnalyticsHandler(
    private val coroutineScope: CoroutineScope,
    private val iterableApi: IterableApi,
    private val airshipAnalytics: AirshipAnalytics,
    private val inAppHandler: IterableAppInAppHandler,
) {

    private val embeddedSessionManager = iterableApi.embeddedManager.getEmbeddedSessionManager()
    private var currentScreen: AirshipAnalytics.Screen? = null
    private val activeImpressions = mutableListOf<AttributionInfo>()
    private val allowedScreensMap = listOf(
        AirshipAnalytics.Screen.SECTION_FRONTS_SCREEN,
        AirshipAnalytics.Screen.ARTICLES_SCREEN,
        AirshipAnalytics.Screen.PAYWALL_SHEET_2_SCREEN,
        AirshipAnalytics.Screen.MY_POST_SCREEN,
        AirshipAnalytics.Screen.SETTINGS_SCREEN,
        AirshipAnalytics.Screen.ASK_THE_POST_SCREEN
    )
    private lateinit var job: Job

    init {
        collectActiveScreensState()
    }

    private fun collectActiveScreensState() {
        Logger.d(TAG, "Iterable, Analytics, collectActiveScreensState()")
        job = coroutineScope.launch {
            airshipAnalytics.activeScreensState.collect { screens ->
                Logger.d(TAG, "Iterable, Analytics, collectActiveScreensState(), screens=$screens")
                screens.lastOrNull { allowedScreensMap.contains(it) }.let { screen ->
                    // Call endSession for the previous screen and then startSession for the new screen.
                    // Also call pauseImpression for the active messages before ending previous screen.
                    if (screen != currentScreen) {
                        if (currentScreen != null) {
                            // pause impressions before ending session
                            // creating a list with toList() to avoid ConcurrentModification exception
                            activeImpressions.toList().forEach {
                                pauseImpression(it)
                            }
                            endSession()
                        }
                        currentScreen = screen
                        if (screen != null) {
                            startSession()
                        }
                    }
                }
            }
        }
    }

    private fun startSession() {
        Logger.i(TAG, "Iterable, Analytics, startSession(), currentScreen=$currentScreen")
        embeddedSessionManager.startSession()
    }

    private fun endSession() {
        Logger.i(TAG, "Iterable, Analytics, endSession(), currentScreen=$currentScreen")
        embeddedSessionManager.endSession()
    }

    fun startImpression(attributionInfo: AttributionInfo) {
        currentScreen ?: return
        val placementId = attributionInfo.placementId
        val messageId = attributionInfo.messageId
        if (activeImpressions.map { it.messageId }.contains(attributionInfo.messageId)) return
        Logger.i(TAG, "Iterable, Analytics, startImpression(), currentScreen=$currentScreen, messageId=$messageId, placementId=$placementId")
        activeImpressions.add(attributionInfo)
        val iamMessage = getIamMessage(attributionInfo)
        if (iamMessage?.analyticsData?.attributionInfo?.messageId == messageId) {
            // send inAppOpen event
            trackInAppOpen(iamMessage.inAppMessage)
        } else {
            // send embedded message start impression
            embeddedSessionManager.startImpression(messageId, placementId)
        }
    }

    fun pauseImpression(attributionInfo: AttributionInfo) {
        currentScreen ?: return
        val messageId = attributionInfo.messageId
        if (!activeImpressions.map { it.messageId }.contains(attributionInfo.messageId)) return
        Logger.i(TAG, "Iterable, Analytics, pauseImpression(), currentScreen=$currentScreen, messageId=$messageId")
        activeImpressions.remove(attributionInfo)
        val iamMessage = getIamMessage(attributionInfo)
        if (iamMessage?.analyticsData?.attributionInfo?.messageId == messageId) {
            // send inAppClose event
                trackInAppClose(message = iamMessage.inAppMessage, null)
        } else {
            embeddedSessionManager.pauseImpression(messageId)
        }
    }

    fun trackClick(attributionInfo: AttributionInfo, url: String?) {
        val placementId = attributionInfo.placementId
        val messageId = attributionInfo.messageId
        val iamMessage = getIamMessage(attributionInfo)
        if (iamMessage?.analyticsData?.attributionInfo?.messageId == messageId) {
            trackInAppClick(iamMessage, url)
        } else {
            iterableApi.embeddedManager.getMessages(placementId)
                ?.filter { it.metadata.messageId == messageId }?.forEach {
                    trackClick(it, null, url)
                }
        }
    }

    private fun trackClick(message: IterableEmbeddedMessage, buttonId: String?, clickedUrl: String?) {
        iterableApi.trackEmbeddedClick(message, buttonId, clickedUrl)
    }

    /**
     * Sends Purchase Event to Iterable
     * ProductId and ProductName Mapping is from [PaywallOmniture.PRODUCT_IDS_TO_PRODUCT_NAMES_MAP]
     */
    fun trackPurchase(
        attributionInfo: AttributionInfo,
        productId: String?,
        productName: String?,
        sku: String?,
        productBasePrice: String?,
        productOfferPrice: String?,
        currencyCode: String?,
        productOfferText: String?,
        campaignName: String?,
        offerType: String?
    ) {
        val campaignId = attributionInfo.campaignId
        val messageId = attributionInfo.messageId
        val itemId = productId ?: "" // basic-monthly...
        val itemName = productName ?: "" // wp.classic.basic...
        // Remove currency symbol before converting to a double value
        val basePrice = if (productBasePrice?.isNotEmpty() == true)
            productBasePrice.substring(1).toDoubleOrNull() ?: 0.0 else 0.0
        val totalPrice = if (productOfferPrice?.isNotEmpty() == true)
            productOfferPrice.substring(1).toDoubleOrNull() ?: basePrice else basePrice
        val itemQuantity = 1

        val commercialItem = CommerceItem(
            itemId,
            itemName,
            totalPrice,
            itemQuantity,
            sku,
            null,
            null,
            null,
            null,
            null,
        )

        iterableApi.trackPurchase(
            totalPrice,
            listOf(commercialItem),
            JSONObject().apply {
                put(STORE_COUNTRY_CODE_FIELD, PaywallService.getConnector().billingCountryCode)
                put(EVENT_CURRENCY_CODE_FIELD, currencyCode)
                put(PLAN_STRUCTURE_FIELD, productOfferText)
                put(BASE_PRICE_FIELD, basePrice)
                campaignName?.takeIf(String::isNotBlank)?.let {
                    put(CAMPAIGN_NAME_FIELD, it)
                }
                offerType?.takeIf(String::isNotBlank)?.let {
                    put(OFFER_TYPE_FIELD, it)
                }
            },
            IterableAttributionInfo(campaignId, -1, messageId)
        )
    }

    private fun trackInAppOpen(message: IterableInAppMessage) {
        iterableApi.trackInAppOpen(message, IterableInAppLocation.IN_APP)
    }

    fun trackInAppDismissClickEvent(attributionInfo: AttributionInfo) {
        val iamMessage = getIamMessage(attributionInfo)
        if (iamMessage?.analyticsData?.attributionInfo?.messageId == attributionInfo.messageId) {
            trackInAppClick(iamMessage, IN_APP_MESSAGE_DISMISS_URL)
            inAppHandler.removeInAppMessage(attributionInfo.messageId)
        }
    }

    private fun trackInAppClick(iamMessage: IterableAppInAppHandler.IamMessage, clickedUrl: String?) {
        val attributionInfo = iamMessage.jsonOnlyInAppMessage?.attributionInfo
        val inAppMessage = iamMessage.inAppMessage

        clickedUrl?.let {
            iterableApi.trackInAppClick(inAppMessage, it, IterableInAppLocation.IN_APP)
        }
        // Send click events to GA as well from in-app message.
        InAppMessageData(
            attributionInfo,
            inAppMessage.campaignId?.toString() ?: inAppMessage.messageId,
            null,
            iamMessage.jsonOnlyInAppMessage?.banner?.title,
            null,
            iamMessage.jsonOnlyInAppMessage?.banner?.messageTracking
        ).run {
            miscellany = clickedUrl
            Measurement.isInAppMessageOriginated = true
        }
    }

    private fun trackInAppClose(message: IterableInAppMessage, clickedUrl: String?) {
        iterableApi.trackInAppClose(
            message,
            clickedUrl,
            IterableInAppCloseAction.OTHER,
            IterableInAppLocation.IN_APP
        )
    }

    private fun getIamMessage(attributionInfo: AttributionInfo): IterableAppInAppHandler.IamMessage? {
        return inAppHandler.inAppMessagesFlow.values.firstNotNullOfOrNull { flow ->
            flow.value.firstOrNull { message ->
                message.jsonOnlyInAppMessage?.attributionInfo?.messageId == attributionInfo.messageId
            }
        }
    }

    companion object {
        private const val TAG = "IterableAnalyticsHandler"
        private const val IN_APP_MESSAGE_DISMISS_URL = "iterable://dismiss"
        const val ATP_CTA_EVENT_KEY = "atp_banner_action"
        const val BANNER_DISMISS_EVENT_KEY = "app_banner_dismiss"
        const val APP_CONVERSION_ACTION_EVENT_KEY = "app_conversion_action"
        const val ACTION_EVENT_KEY = "action"
        const val ACTION_DISMISS = "dismiss"
        const val ATP_CTA_ACTION_QUESTION = "question"
    }
}