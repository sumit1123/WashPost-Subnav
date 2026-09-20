// Copyright (c) 2025 The Washington Post. All rights reserved.
package com.wapo.flagship.sdk.iterable.handlers

import com.iterable.iterableapi.IterableEmbeddedMessage
import com.iterable.iterableapi.IterableEmbeddedUpdateHandler
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.sdk.iterable.IterableSdk
import com.wapo.flagship.sdk.iterable.getWallPlacementIds
import com.washingtonpost.android.config.domain.models.config.IterableConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class IterableAppEmbeddedUpdateHandler(
    private val iterableConfig: IterableConfig?,
    private val iterableSdk: IterableSdk,
    private val contextUtils: AppContextUtils,
    private val currentSupportedBlockerConfigVersion: () -> Int
) : IterableEmbeddedUpdateHandler {

    private val allowedMessagesPlacementIds: List<Long> =
        iterableConfig?.run { listOfNotNull(banner, article, section, myPostBanner, askThePostBanner, myPost, askThePost, settingsTop, settingsPlan) } ?: emptyList()

    private val allowedBlockerMessagesPlacementIds: List<Long> =
        getWallPlacementIds(currentSupportedBlockerConfigVersion.invoke())

    private val finalAllowedPlacementIds: List<Long> =
        allowedMessagesPlacementIds + allowedBlockerMessagesPlacementIds

    private val _messagesMapState: MutableMap<Long, MutableStateFlow<List<IterableEmbeddedMessage>>> =
        mutableMapOf()
    val messagesMapState: Map<Long, StateFlow<List<IterableEmbeddedMessage>>> =
        _messagesMapState

    init {
        // Initialize _messagesMapState with allowed placement ids
        finalAllowedPlacementIds.forEach { placementId ->
            _messagesMapState[placementId] = MutableStateFlow(listOf())
        }
    }

    override fun onEmbeddedMessagingDisabled() {
        Logger.d(TAG, "Iterable, onEmbeddedMessagingDisabled")
    }

    override fun onMessagesUpdated() {
        // Note The SDK does not always call onMessagesUpdated on the main thread.
        Logger.d(TAG, "Iterable, onMessagesUpdated")
        updateMessagesMap()
    }

    fun updateMessagesMap() {
        finalAllowedPlacementIds.forEach { allowedPlacementId ->
            iterableSdk.getEmbeddedManager().getMessages(allowedPlacementId).let { messages ->
                if (messages == null) {
                    _messagesMapState[allowedPlacementId]?.value = emptyList()
                    Logger.d(
                        TAG,
                        "Iterable, updateMessagesMap, clearing $allowedPlacementId, list ($finalAllowedPlacementIds)"
                    )
                } else {
                    val filteredMessages = if (allowBothTemplatesAndCampaigns())
                        messages
                    else
                        messages.filter { it.metadata.campaignId != 0 }
                    _messagesMapState[allowedPlacementId]?.value = filteredMessages
                    Logger.d(
                        TAG,
                        "Iterable, updateMessagesMap, allowedIds=$finalAllowedPlacementIds, currentId=${allowedPlacementId}, campaignIds=${filteredMessages.map { it.metadata.campaignId }}, messages=${filteredMessages}"
                    )
                }
            }
        }
    }

    private fun allowBothTemplatesAndCampaigns(): Boolean {
        return contextUtils.isDebuggableBuild()
    }

    companion object {
        private const val TAG = "IterableAppEmbeddedUpdateHandler"
    }
}
