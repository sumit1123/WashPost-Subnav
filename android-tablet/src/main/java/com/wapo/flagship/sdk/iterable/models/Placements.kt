// Copyright (c) 2025 The Washington Post. All rights reserved.
package com.wapo.flagship.sdk.iterable.models

import com.squareup.moshi.JsonClass
import com.wapo.android.commons.iterable.AttributionInfo
import com.washingtonpost.android.config.domain.models.config.paywallconf.Blocker

open class PlacementMessage(
    open val attributionInfo: AttributionInfo,
    open val messageRequirements: List<MessageRequirement>?,
    open val actionRequirements: List<MessageRequirement>? = null,
    open val messageTracking: MessageTracking? = null
)

open class BannerMessage(
    override val attributionInfo: AttributionInfo,
    override val messageRequirements: List<MessageRequirement>?,
    override val actionRequirements: List<MessageRequirement>? = null,
    override val messageTracking: MessageTracking? = null,
    open val title: String? = null,
    open val body: String? = null,
    open val action: String? = null,
    open val blocker: String? = null,
    open val productName: String? = null,
    open val code: String? = null,
    open val url: String? = null,
    open val dismissible: Boolean? = null,
    open val imageUrl: String? = null,
    open val iterableImage: IterableImage? = null,
    open val consume: String? = null,
    open val displayTrigger: DisplayTrigger? = null,
) : PlacementMessage(attributionInfo, messageRequirements, actionRequirements, messageTracking)

data class BlockerMessage(
    override val attributionInfo: AttributionInfo,
    override val messageRequirements: List<MessageRequirement>?,
    override val messageTracking: MessageTracking? = null,
    val contentTitle: String? = null,
    val contentBody: String? = null,
    val promo1: String? = null,
    val promo2: String? = null,
    val title1: String? = null,
    val title2: String? = null,
    val body1: String? = null,
    val body2: String? = null,
    val choice: String? = null,
    val items: List<Item?>? = null,
    val blocker: Blocker? = null
) : PlacementMessage(attributionInfo, messageRequirements, messageTracking = messageTracking)

data class Product(
    val name: String? = null,
    val code: String? = null,
    val action: String? = null,
    val badge: String? = null
)

data class Item(
    val id: String? = null,
    val name: String? = null,
    val code: String? = null,
    val action: String? = null,
    val title: String? = null,
    val label: String? = null,
    val caption: String? = null,
    val url: String? = null
)

@JsonClass(generateAdapter = true)
data class IterableImage(
    val light: String? = null,
    val dark: String? = null,
)

@JsonClass(generateAdapter = true)
data class MessageRequirement(
    val property: String? = null,
    val comparator: String? = null,
    val value: Any? = null,
    val onlyOneOf: List<MessageRequirement>? = null
)

@JsonClass(generateAdapter = true)
data class DisplayTrigger(
    val type: String? = null,
    val depth: Float? = null
)

@JsonClass(generateAdapter = true)
data class MessageTracking(
    val kind: String = "wall",
    val campaignName: String? = null,
    val offerType: String? = null
)

enum class CustomTextField(open val id: String) {
    Action("Action"),
    Blocker("Blocker"),
    Product("Product"),
    Code("Code"),
    Url("URL"),
    Promo1("Promo.1"),
    Promo2("Promo.2"),
    Title1("Title.1"),
    Title2("Title.2"),
    Body1("Body.1"),
    Body2("Body.2"),
    Choice("Choice"),
    ProductID("ID"),
    Badge("Badge"),
    Item("Item"),
    Title("Title"),
    Label("Label"),
    Caption("Caption"),
}

enum class PayloadKeys(open val id: String) {
    Image("image"),
    Require("require"),
    ActionRequire("actionRequire"),
    Consume("consume"),
    Tracking("tracking")
}
