// Copyright (c) 2025 The Washington Post. All rights reserved.
package com.wapo.flagship.sdk.iterable.handlers

import com.iterable.iterableapi.IterableInAppDeleteActionType
import com.iterable.iterableapi.IterableInAppHandler
import com.iterable.iterableapi.IterableInAppHandler.InAppResponse
import com.iterable.iterableapi.IterableInAppLocation
import com.iterable.iterableapi.IterableInAppManager
import com.iterable.iterableapi.IterableInAppMessage
import com.wapo.android.commons.iterable.AttributionInfo
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.deeplinks.AirshipAnalytics
import com.wapo.flagship.features.deeplinks.InAppMessageData
import com.wapo.flagship.sdk.iterable.models.DEFAULT_CAMPAIGN_ID
import com.wapo.flagship.sdk.iterable.models.IamMessageType
import com.wapo.flagship.sdk.iterable.models.JsonOnlyInAppMessage
import com.wapo.flagship.sdk.iterable.models.MessageTracking
import com.wapo.flagship.sdk.iterable.models.toJsonOnlyInAppMessage
import com.wapo.flagship.util.tracking.Events
import com.wapo.flagship.util.tracking.Measurement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class IterableAppInAppHandler(
    private val coroutineScope: CoroutineScope,
    private val iterableInAppManager: () -> IterableInAppManager,
    private val airshipAnalytics: AirshipAnalytics,
) : IterableInAppHandler {

    // Store in-app messages for each message type when they are received in onNewInApp or during syncInAppMessages.
    val inAppMessagesFlow = IamMessageType.entries.associateWith {
        MutableStateFlow<List<IamMessage>>(emptyList())
    }

    private var currentScreen: AirshipAnalytics.Screen? = null
    private lateinit var job: Job

    init {
        collectActiveScreensState()
    }

    override fun onNewInApp(message: IterableInAppMessage): InAppResponse {
        Logger.d(TAG, "Iterable, InAppHandler, onNewInApp, message=${message}")
        processInAppMessage(message)
        return if (message.isJsonOnly) {
            InAppResponse.SKIP
        } else {
            InAppResponse.SHOW
        }
    }

    private fun collectActiveScreensState() {
        Logger.d(TAG, "Iterable, InAppAnalytics, collectActiveScreensState()")
        job = coroutineScope.launch(Dispatchers.IO) {
            airshipAnalytics.activeScreensState.collect { screens ->
                Logger.d(
                    TAG,
                    "Iterable, InAppAnalytics, collectActiveScreensState(), screens=$screens"
                )
                if (screens.contains(AirshipAnalytics.Screen.ITERABLE_MESSAGE_SCREEN)) {
                    if (currentScreen == null) {
                        // Delay for other events to be fired first when resume the app.
                        delay(100)
                        onMessageDisplayed()
                        currentScreen = AirshipAnalytics.Screen.ITERABLE_MESSAGE_SCREEN
                    }
                } else {
                    if (currentScreen != null) {
                        // Close button action handles the `onMessageFinished` event.
                        currentScreen = null
                    }
                }
            }
        }
    }

    private fun prepareAnalyticsData(message: IterableInAppMessage): InAppMessageData {
        // No `title` and `eventLabel` from Iterable messages.
        // Airship can still have those.
        // So data class is same but nulls for Iterable message.
        val campaignId = message.campaignId
        val messageId = message.messageId
        // Parse messageTracking from customPayload
        val messageTracking = (message.customPayload?.opt("tracking") as? JSONObject)?.let {
            MessageTracking(
                kind = it.optString("kind", "wall"),
                campaignName = it.optString("campaignName", "unknown"),
                offerType = it.optString("offerType").takeIf(String::isNotBlank)
            )
        }
        return InAppMessageData(
            attributionInfo = AttributionInfo(
                campaignId = campaignId?.toInt() ?: DEFAULT_CAMPAIGN_ID,
                messageId = messageId
            ),
            campaignId?.toString() ?: messageId,
            null,
            message.run {
                customPayload?.optString(
                    MSG_LABEL_KEY, campaignId?.toString() ?: MSG_UNKNOWN_VALUE
                )
            },
            null,
            messageTracking
        )
    }

    private fun onMessageDisplayed() {
        val analyticsData = inAppMessagesFlow[IamMessageType.REGULAR_IN_APP_MESSAGE]?.value?.firstOrNull()?.analyticsData ?: return
        Measurement.trackInAppMessage(Events.EVENT_IN_APP_MESSAGE_DISPLAYED, analyticsData)
    }

    fun onMessageFinished(url: String? = null) {
        val analyticsData = inAppMessagesFlow[IamMessageType.REGULAR_IN_APP_MESSAGE]?.value?.firstOrNull()?.analyticsData ?: return
        analyticsData.run {
            miscellany = url
            Measurement.trackInAppMessage(Events.EVENT_IN_APP_MESSAGE_FINISHED, this)
        }
        clearInAppMessageData()
    }

    fun onMessageButtonClicked(url: String? = null) {
        val analyticsData = inAppMessagesFlow[IamMessageType.REGULAR_IN_APP_MESSAGE]?.value?.firstOrNull()?.analyticsData ?: return
        analyticsData.run {
            miscellany = url
            Measurement.trackInAppMessage(Events.EVENT_IN_APP_MESSAGE_CTA, this)
        }
        Measurement.isInAppMessageOriginated = true
        clearInAppMessageData()
    }

    fun isInAppMessageDisplayed(): Boolean {
        return currentScreen == AirshipAnalytics.Screen.ITERABLE_MESSAGE_SCREEN
    }

    private fun clearInAppMessageData() {
        val flow = inAppMessagesFlow[IamMessageType.REGULAR_IN_APP_MESSAGE]
        flow?.value = flow?.value?.drop(1) ?: emptyList()
    }

    fun getAttributionInfo(): AttributionInfo? {
        return inAppMessagesFlow[IamMessageType.REGULAR_IN_APP_MESSAGE]?.value?.firstOrNull()?.analyticsData?.attributionInfo
    }

    fun removeInAppMessage(messageId: String?) {
        Logger.d(TAG, "Iterable, InAppHandler, removeInAppMessage, messageId=${messageId}")
        for (entry in inAppMessagesFlow) {
            val currentQueue = entry.value.value
            val messageToRemove = currentQueue.find { it.inAppMessage.messageId == messageId }

            if (messageToRemove != null) {
                iterableInAppManager.invoke().removeMessage(
                    messageToRemove.inAppMessage,
                    IterableInAppDeleteActionType.OTHER,
                    IterableInAppLocation.IN_APP
                )
                entry.value.value = currentQueue.filter { it.inAppMessage.messageId != messageId }
            }
        }
    }

    fun syncInAppMessages() {
        iterableInAppManager.invoke().messages.forEach {
            Logger.d(
                TAG,
                "Iterable, InAppHandler, syncInAppMessages, isJsonOnly = ${it.isJsonOnly}, id = ${it.messageId}, message=${it.customPayload}"
            )
            processInAppMessage(it)
        }
    }

    private fun processInAppMessage(message: IterableInAppMessage) {
        val placement = message.customPayload?.opt(MSG_PLACEMENT_KEY) as? String
        val messageType =
            IamMessageType.entries.firstOrNull { messageType -> messageType.type == placement }
        if (messageType != null) {
            processJsonOnlyInAppMessage(message, messageType)
        } else {
            processRegularInAppMessage(message)
        }
    }

    private fun processJsonOnlyInAppMessage(
        message: IterableInAppMessage,
        messageType: IamMessageType
    ) {
        val iamMessage = IamMessage(
            inAppMessage = message,
            analyticsData = prepareAnalyticsData(message),
            jsonOnlyInAppMessage = message.toJsonOnlyInAppMessage()
        )
        val flow = inAppMessagesFlow[messageType]
        flow?.value = flow?.value.orEmpty() + iamMessage
    }

    fun processRegularInAppMessage(message: IterableInAppMessage) {
        val flow = inAppMessagesFlow[IamMessageType.REGULAR_IN_APP_MESSAGE]
        val newMessage = IamMessage(
            inAppMessage = message,
            analyticsData = prepareAnalyticsData(message),
        )
        flow?.value = flow?.value.orEmpty() + newMessage
    }

    data class IamMessage(
        val inAppMessage: IterableInAppMessage,
        val analyticsData: InAppMessageData,
        val jsonOnlyInAppMessage: JsonOnlyInAppMessage? = null,
    )

    companion object {
        private const val TAG = "IterableAppInAppHandler"
        private const val MSG_UNKNOWN_VALUE = "unknown"
        private const val MSG_LABEL_KEY = "label"
        private const val MSG_PLACEMENT_KEY = "placement"
    }
}