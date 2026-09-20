// Copyright (c) 2025 The Washington Post. All rights reserved.
package com.wapo.flagship.sdk.iterable.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.android.commons.extensions.toSpannableBuilder
import com.wapo.android.commons.iterable.AttributionInfo
import com.wapo.android.commons.util.isDatePast
import com.wapo.flagship.features.deeplinks.AirshipAnalytics
import com.wapo.flagship.features.articles2.models.ArticleInlineMessage
import com.wapo.flagship.features.grid.model.GlobalBannerMessage
import com.wapo.flagship.features.grid.model.SectionInlineMessage
import com.wapo.flagship.sdk.iterable.IterableSdk
import com.wapo.flagship.sdk.iterable.getWallPlacementIds
import com.wapo.flagship.sdk.iterable.models.BannerMessage
import com.wapo.flagship.sdk.iterable.models.BlockerMessage
import com.wapo.flagship.sdk.iterable.models.IamMessageType
import com.wapo.flagship.sdk.iterable.models.MessageRequirement
import com.wapo.flagship.sdk.iterable.models.PaywallUser
import com.wapo.flagship.sdk.iterable.models.buildUserContext
import com.wapo.flagship.sdk.iterable.models.getRequirements
import com.wapo.flagship.sdk.iterable.models.mapToBannerMessage
import com.wapo.flagship.sdk.iterable.models.mapToBannerPaywallMessage
import com.wapo.flagship.sdk.iterable.models.mapToBlockerMessage
import com.wapo.flagship.sdk.iterable.models.mapToBlockerPaywallMessage
import com.wapo.flagship.sdk.iterable.models.toMessageRequirement
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.IterableConfig
import com.washingtonpost.android.paywall.PaywallReactive
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.bottomsheet.viewmodel.PaywallSheet2ViewModel
import com.washingtonpost.android.paywall.models.BannerPaywallMessage
import com.washingtonpost.android.paywall.models.BlockerPaywallMessage
import com.washingtonpost.android.paywall.models.MessageOperator
import com.washingtonpost.android.paywall.models.MessagePropertyType
import com.washingtonpost.android.paywall.models.MessageRequirements
import com.washingtonpost.android.paywall.util.PaywallUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IterableActivityViewModel @Inject constructor(
    private val configManager: ConfigManager?,
    private val iterableSdk: IterableSdk
) : ViewModel() {

    private val iterableConfig: IterableConfig? get() = configManager?.config?.iterableConfig

    private val connector by lazy { PaywallService.getConnector() }
    private val allowedBlockerPlacementIds: List<Long> =
        getWallPlacementIds(iterableSdk.getSupportedBlockerConfigVersion())

    @OptIn(ExperimentalCoroutinesApi::class)
    val blockerMessagesState: Flow<List<BlockerMessage>> =
        combine(
            combine(allowedBlockerPlacementIds.map { iterableSdk.getEmbeddedMessagesState(it) }) { it },
            PaywallReactive.updateIterablePlacement
        ) { states, _ -> states }
            .mapLatest { states ->
                val placementMessages = mutableListOf<BlockerMessage>()
                val paywallMessages = mutableListOf<BlockerPaywallMessage>()
                states.forEach {
                    val embeddedMessage = it.firstOrNull()
                    if (shouldShowBanner(embeddedMessage?.getRequirements())) {
                        (embeddedMessage?.mapToBlockerMessage() as? BlockerMessage)?.also { message ->
                            placementMessages.add(message)
                            paywallMessages.add(message.mapToBlockerPaywallMessage())
                        }
                    }
                }
                connector.setBlockerPaywallMessages(paywallMessages)
                placementMessages
            }

    fun getBannerFlowForPlacement(placement: IamMessageType): Flow<BannerPaywallMessage?> {
        return promoBannersFlow
            .map { bannerMap -> bannerMap[placement] }
            .distinctUntilChanged()
    }

    fun getBannerForPlacement(
        placement: IamMessageType
    ): BannerPaywallMessage? = promoBannersFlow.value[placement]

    fun findResolvedBanner(
        messageId: String
    ): Pair<IamMessageType, BannerPaywallMessage>? {
        return promoBannersFlow.value.entries.firstNotNullOfOrNull { (placement, message) ->
            message
                ?.takeIf { it.attributionInfo.messageId == messageId }
                ?.let { placement to it }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val promoBannersFlow: StateFlow<Map<IamMessageType, BannerPaywallMessage?>> =
        combine(
            IamMessageType.entries
                // filter out the non-banner type
                .filter { it != IamMessageType.REGULAR_IN_APP_MESSAGE }
                .map { placement ->
                    resolvedBannerFlow(placement).map { banner -> placement to banner }
                }
        ) { results ->
            // maps all placements into the map (can contain null values for placements with no eligible banner)
            results.toMap()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )


    fun getGlobalBannerMessageFromMessages(context: Context): GlobalBannerMessage? {
        val bannerMessage = promoBannersFlow.value[IamMessageType.BANNER] ?: return null
        val adFreeIterableProducts =
            ConfigManager.getInstance().config.paywallConf?.adFreeIterableProducts ?: emptyList()

        val isPremiumUser = PaywallService.getInstance().isPremiumUser
        val isTerminated = PaywallService.getInstance().isSubscriptionTerminated

        return bannerMessage.run {
            val productId = PaywallUtil.mapProductNameToProductId(productName)
            val offerDetail = PaywallUtil.getBannerOfferText(productId, code)
            GlobalBannerMessage(
                attributionInfo = attributionInfo,
                productId = productId,
                offerId = code?.takeIf { it.isNotEmpty() },
                offerTitle = title,
                offerSubtitle = body,
                offerDetail = offerDetail,
                offerUrl = url,
                ctaText = getAction(
                    context, blocker, productId, code, offerDetail, action,
                    isPremiumUser, isTerminated, url, productName
                )?.toSpannableBuilder(),
                wallName = blocker,
                dismissible = dismissible,
                productName = productName,
                isAdFreeProduct = adFreeIterableProducts.contains(productName),
                passedIterableGuardrails = messageRequirements
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { reqs -> shouldShowBanner(reqs.map { it.toMessageRequirement() }) },
                promoAction = promoAction,
                fallbackAction = fallbackAction,
                messageRequirements = messageRequirements
            )
        }
    }

    fun getSectionInlineMessageFromMessages(context: Context): SectionInlineMessage? {
        val sectionMessage = promoBannersFlow.value[IamMessageType.SECTION] ?: return null

        val isPremiumUser = PaywallService.getInstance().isPremiumUser
        val isTerminated = PaywallService.getInstance().isSubscriptionTerminated

        return sectionMessage.run {
            val blocker = this.blocker
            val productId = PaywallUtil.mapProductNameToProductId(productName)
            val code = this.code?.takeIf { it.isNotEmpty() }
            val url = this.url
            val action = this.action
            val offerDetail = PaywallUtil.getBannerOfferText(productId, code)
            SectionInlineMessage(
                attributionInfo = attributionInfo,
                title = title,
                body = body,
                url = url,
                action = getAction(
                    context, blocker, productId, code, offerDetail, action,
                    isPremiumUser, isTerminated, url, productName
                ),
                true
            )
        }
    }

    fun getArticleInlineMessageFromMessages(context: Context): ArticleInlineMessage? {
        val articleMessage = promoBannersFlow.value[IamMessageType.ARTICLE] ?: return null

        val isPremiumUser = PaywallService.getInstance().isPremiumUser
        val isTerminated = PaywallService.getInstance().isSubscriptionTerminated

        return articleMessage.run {
            val blocker = this.blocker
            val productId = PaywallUtil.mapProductNameToProductId(productName)
            val code = this.code?.takeIf { it.isNotEmpty() }
            val url = this.url
            val action = this.action
            val offerDetail = PaywallUtil.getBannerOfferText(productId, code)
            ArticleInlineMessage(
                attributionInfo = attributionInfo,
                title = title,
                body = body,
                url = url,
                action = getAction(
                    context, blocker, productId, code, offerDetail, action,
                    isPremiumUser, isTerminated, url, productName
                ),
                true
            )
        }
    }

    private fun getAction(
        context: Context,
        blocker: String?, productId: String?, code: String?, offerDetail: String?,
        action: String?, isPremiumUser: Boolean, isTerminated: Boolean, url: String?,
        productName: String?
    ): String? {
        return when {
            isPremiumUser && action?.isNotEmpty() == true -> {
                action
            }

            !isPremiumUser && blocker?.isNotEmpty() == true -> {
                if (action?.isNotEmpty() == true) {
                    action
                } else if (isTerminated) {
                    context.resources.getString(com.washingtonpost.android.sections.R.string.resubscribe)
                } else {
                    context.resources.getString(com.washingtonpost.android.sections.R.string.subscribe)
                }
            }

            !isPremiumUser && productId?.isNotEmpty() == true && code?.isNotEmpty() == true -> {
                if (!offerDetail.isNullOrEmpty()) {
                    offerDetail
                } else if (isTerminated) {
                    context.resources.getString(com.washingtonpost.android.sections.R.string.resubscribe)
                } else {
                    context.resources.getString(com.washingtonpost.android.sections.R.string.subscribe)
                }
            }

            url?.isNotEmpty() == true -> {
                if (action?.isNotEmpty() == true) {
                    action
                } else {
                    context.resources.getString(com.washingtonpost.android.sections.R.string.go)
                }
            }

            else -> {
                // Not sure what this message is.
                null
            }
        }
    }

    /**
     * Resolves a single [BannerPaywallMessage?] for [placement] by combining its embedded SDK flow and its IAM SDK flow.
     *
     *   - Embedded messages are checked first (priority).
     *   - IAM message is the fallback, guarded by its expiry date.
     *   - Both sources are filtered by [shouldShowBanner] before mapping.
     *
     * Returns `null` when no eligible banner exists for [placement].
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun resolvedBannerFlow(placement: IamMessageType): Flow<BannerPaywallMessage?> =
        combine(
            iterableSdk.getEmbeddedMessagesState(placement.embeddedPlacementId(iterableConfig)),
            iterableSdk.getInAppMessageState(placement),
            PaywallReactive.updateIterablePlacement
        ) { embeddedMessages, iamMessages, _ ->

            // checks against embedded message guardrails and then maps to BannerPaywallMessage
            val embeddedPaywallMessage: BannerPaywallMessage? = embeddedMessages
                .firstNotNullOfOrNull { message ->
                    if (shouldShowBanner(message.getRequirements())) {
                        val bannerMessage = message.mapToBannerMessage() as? BannerMessage
                        bannerMessage?.mapToBannerPaywallMessage()
                    } else {
                        null
                    }
                }

            val iamPaywallMessage: BannerPaywallMessage? = iamMessages
                .firstNotNullOfOrNull { message ->
                    val expiresAt = message.inAppMessage.expiresAt
                    val isNotExpired = expiresAt == null || !isDatePast(expiresAt)
                    val meetsRequirements =
                        shouldShowBanner(message.jsonOnlyInAppMessage?.banner?.messageRequirements)
                    if (isNotExpired && meetsRequirements) {
                        message.jsonOnlyInAppMessage?.mapToBannerPaywallMessage()
                    } else {
                        null
                    }
                }

            embeddedPaywallMessage ?: iamPaywallMessage
        }

    fun evaluateSingleRequirement(
        messageRequirement: MessageRequirement,
        user: PaywallUser
    ): Boolean {
        if (!messageRequirement.onlyOneOf.isNullOrEmpty()) {
            val count = messageRequirement.onlyOneOf.count { evaluateSingleRequirement(it, user) }
            return count == 1
        }

        val result = when (messageRequirement.property) {
            MessagePropertyType.User.id -> {
                when (messageRequirement.comparator) {
                    MessageOperator.IsSet.id -> {
                        user.loggedInUser != null
                    }

                    MessageOperator.IsNotSet.id -> {
                        user.loggedInUser == null
                    }

                    else -> false
                }
            }

            MessagePropertyType.Subscription.id -> {
                when (messageRequirement.comparator) {
                    MessageOperator.IsNotSet.id -> !user.isPremium
                    MessageOperator.IsSet.id -> user.isPremium
                    else -> false
                }
            }

            MessagePropertyType.SubStatus.id -> {
                val requiredValue = messageRequirement.value as? String
                // For anonymous users, cachedSub existing means active ("A")
                val userStatus = user.subStatus

                when (messageRequirement.comparator) {
                    MessageOperator.IsEqualTo.id -> requiredValue != null && requiredValue == userStatus
                    MessageOperator.IsNotEqualTo.id -> requiredValue != null && requiredValue != userStatus
                    else -> true
                }
            }

            MessagePropertyType.SubProduct.id -> {
                val requiredValue = messageRequirement.value as? String
                val resolvedProduct = PaywallUtil.resolveProductNamesFromSkus(user.cachedSub?.productSkuList)
                    .firstOrNull()
                    ?: user.loggedInUser?.accessLevel

                when (messageRequirement.comparator) {
                    MessageOperator.IsEqualTo.id -> requiredValue != null && requiredValue == resolvedProduct
                    MessageOperator.IsNotEqualTo.id -> requiredValue != resolvedProduct
                    else -> true
                }
            }

            MessagePropertyType.SubSource.id -> {
                val requiredValue = messageRequirement.value as? String
                when (messageRequirement.comparator) {
                    MessageOperator.IsEqualTo.id -> {
                        requiredValue != null && requiredValue == user.subSource
                    }

                    MessageOperator.IsNotEqualTo.id -> {
                        requiredValue != user.subSource
                    }

                    else -> true
                }
            }

            MessagePropertyType.DeviceSubscriptionId.id -> {
                val requiredValue = messageRequirement.value as? String
                val deviceSubId = user.deviceSubscriptionId

                when (messageRequirement.comparator) {
                    MessageOperator.IsEqualTo.id ->
                        !requiredValue.isNullOrEmpty() && requiredValue == deviceSubId

                    MessageOperator.IsNotEqualTo.id ->
                        requiredValue != deviceSubId

                    MessageOperator.IsSet.id ->
                        !deviceSubId.isNullOrEmpty()

                    MessageOperator.IsNotSet.id ->
                        deviceSubId.isNullOrEmpty()

                    else -> true
                }
            }

            MessagePropertyType.Subs.id -> {
                val subRequirements =
                    messageRequirement.value as? List<Map<String, Any?>> ?: return true

                val userStatus = user.subStatus

                // Check if all sub-requirements match
                val allMatch = subRequirements.all { req ->
                    val prop = req["property"] as? String
                    val comp = req["comparator"] as? String
                    val value = req["value"] as? String

                    when (prop) {
                        MessagePropertyType.SubStatus.id -> {
                            when (comp) {
                                MessageOperator.IsEqualTo.id -> userStatus == value
                                MessageOperator.IsNotEqualTo.id -> userStatus != value
                                else -> true
                            }
                        }

                        MessagePropertyType.SubProduct.id -> {
                            val hasProduct = user.userProducts.contains(value)
                            when (comp) {
                                MessageOperator.IsEqualTo.id -> value != null && hasProduct
                                MessageOperator.IsNotEqualTo.id -> value == null || !hasProduct
                                else -> true
                            }
                        }

                        else -> true
                    }
                }

                when (messageRequirement.comparator) {
                    MessageOperator.RequireNone.id -> !allMatch
                    MessageOperator.RequireAll.id -> allMatch
                    else -> true
                }
            }

            else -> {
                true
            }
        }

        return result
    }

    fun shouldShowBanner(requirements: List<MessageRequirement>?): Boolean {
        if (requirements.isNullOrEmpty()) {
            return true
        }

        val paywallUser = buildUserContext() ?: return false
        val finalDecision = requirements.all { evaluateSingleRequirement(it, paywallUser) }

        return finalDecision
    }

    /**
     * Evaluates a list of [MessageRequirement]s (from the "actionRequire" payload block)
     * and returns a map of property → boolean result.
     *
     * Each placement can define its own action requirements. The consumer decides
     * what the true/false result means for each property. For example, the ad-free
     * banner uses `device.sub.id` with `equal` to decide native (true) vs mWeb (false).
     *
     * JSON example:
     * ```
     * "actionRequire": [
     *   { "property": "device.sub.id", "comparator": "equal", "value": "12345" }
     * ]
     * ```
     */
    fun evaluateActionRequirements(actionRequirements: List<MessageRequirements>?): Boolean {
        if (actionRequirements.isNullOrEmpty()) return true

        val user = buildUserContext() ?: return false

        return actionRequirements.all { requirement ->
            evaluateSingleRequirement(
                requirement.toMessageRequirement(),
                user
            )
        }
    }

    fun hasIamMessages(): Boolean {
        return iterableSdk.hasIamMessages()
    }

    fun pauseIamMessages() {
        iterableSdk.pauseIamMessages()
    }

    fun resumeIamMessages() {
        iterableSdk.resumeIamMessages()
    }

    fun restartSession(screenName: String) {
        viewModelScope.launch {
            AirshipAnalytics.stopTracking(screenName)
            delay(10)
            AirshipAnalytics.startTracking(screenName)
        }
    }

    fun updateEmbeddedMessagesMap() {
        iterableSdk.updateEmbeddedMessagesMap()
    }

    fun trackEvent(
        eventType: PaywallSheet2ViewModel.EventType,
        attributionInfo: AttributionInfo,
        productId: String? = null,
        offerId: String? = null,
        url: String? = null
    ) {
        PaywallService.getOmniture().trackMessageEvent(
            eventType,
            attributionInfo,
            productId,
            offerId,
            null,
            url
        )
    }

    fun isInAppMessageDisplayed(): Boolean {
        return iterableSdk.isInAppMessageDisplayed()
    }

    fun sendIterableCustomEvent(eventName: String, dataFields: Map<String, Any>?) {
        iterableSdk.sendEvent(eventName, dataFields)
    }

    fun clearInAppMessage(messageId: String) {
        iterableSdk.removeInAppMessage(messageId)
    }

    fun syncInAppMessages() {
        iterableSdk.syncIamMessages()
    }

    fun trackBannerDisplayed(placementType: String, contentUrl: String? = null, kind: String? = "wall", campaignName: String?, campaignId: Int) {
        Measurement.trackBannerDisplayedEvent(placementType, contentUrl, kind, campaignName, campaignId)
    }

    fun trackBannerClicked(placementType: String, contentUrl: String? = null, kind: String? = "wall", campaignName: String?, campaignId: Int, navigationBehavior: String) {
        Measurement.trackBannerClickEvent(placementType, contentUrl, kind, campaignName, campaignId, navigationBehavior)
    }

    fun trackWallDisplayed(placementType: String, contentUrl: String? = null, kind: String? = "wall", campaignName: String?, campaignId: Int, navigationBehavior: String) {
        Measurement.trackWallDisplayedEvent(placementType, contentUrl, kind, campaignName, campaignId, navigationBehavior)
    }

    companion object {
        const val TAG = "IterableActivityViewModel"
    }
}
