// Copyright (c) 2025 The Washington Post. All rights reserved.
package com.washingtonpost.android.paywall.models

import com.wapo.android.commons.iterable.AttributionInfo
import com.washingtonpost.android.config.domain.models.config.paywallconf.Blocker

open class PaywallMessage(
    open val attributionInfo: AttributionInfo,
    open val messageRequirements: List<MessageRequirements>?,
    open val messageTracking: MessageTracking? = null
)

open class BannerPaywallMessage(
    override val attributionInfo: AttributionInfo,
    override val messageRequirements: List<MessageRequirements>?,
    override val messageTracking: MessageTracking? = null,
    val title: String? = null,
    val body: String? = null,
    val action: String? = null,
    val blocker: String? = null,
    val productName: String? = null,
    val code: String? = null,
    val url: String? = null,
    val dismissible: Boolean? = null,
    val imageUrl: String? = null,
    val iterableImage: IterableImage? = null,
    val consume: String? = null,
    val displayTrigger: DisplayTrigger? = null,
    val promoAction: PromoAction? = null,
    val fallbackAction: PromoAction? = null
) : PaywallMessage(attributionInfo, messageRequirements, messageTracking)

data class BlockerPaywallMessage(
    override val attributionInfo: AttributionInfo,
    override val messageRequirements: List<MessageRequirements>?,
    override val messageTracking: MessageTracking? = null,
    val title: String? = null,
    val body: String? = null,
    val promo1: String? = null,
    val promo2: String? = null,
    val title1: String? = null,
    val title2: String? = null,
    val body1: String? = null,
    val body2: String? = null,
    val choice: String? = null,
    val items: List<PaywallMessageItem?>? = null,
    val blocker: Blocker? = null
) : PaywallMessage(attributionInfo, messageRequirements, messageTracking)

data class PaywallMessageProduct(
    val name: String? = null,
    val code: String? = null,
    val action: String? = null,
    val badge: String? = null
)

data class PaywallMessageItem(
    val id: String? = null,
    val name: String? = null,
    val code: String? = null,
    val action: String? = null,
    val title: String? = null,
    val label: String? = null,
    val caption: String? = null,
    val url: String? = null
)

data class DisplayTrigger(
    val type: DisplayTriggerType? = null,
    val depth: Float? = null
)

data class MessageRequirements(
    val property: String? = null,
    val comparator: String? = null,
    val value: Any? = null,
    val onlyOneOf: List<MessageRequirements>? = null
)

enum class MessagePropertyType(open val id: String) {
    Subscription("sub"),
    User("user"),
    SubscriptionId("sub.id"),
    SubStatus("sub.status"),
    SubProduct("sub.product"),
    Subs("subs"),
    DeviceSubscriptionId("device.sub.id"),
    SubSource("sub.source"),
}

enum class MessageOperator(open val id: String) {
    IsNotSet("isNotSet"),
    IsSet("isSet"),
    IsEqualTo("equal"),
    IsNotEqualTo("doesNotEqual"),
    RequireNone("requireNone"),
    RequireAll("requireAll"),
}

enum class DisplayTriggerType(open val id: String) {
    ScrollDepth("scroll"),
}

data class MessageTracking(
    val kind: String = "wall",
    val campaignName: String? = "unknown",
    val offerType: String? = null
)
