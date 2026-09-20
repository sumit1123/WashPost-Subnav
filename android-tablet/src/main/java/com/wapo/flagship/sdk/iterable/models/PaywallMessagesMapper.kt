package com.wapo.flagship.sdk.iterable.models

import com.washingtonpost.android.paywall.models.BannerPaywallMessage
import com.washingtonpost.android.paywall.models.BlockerPaywallMessage
import com.washingtonpost.android.paywall.models.DisplayTriggerType
import com.washingtonpost.android.paywall.models.IterableImage
import com.washingtonpost.android.paywall.models.MessageRequirements
import com.washingtonpost.android.paywall.models.PromoAction
import com.washingtonpost.android.paywall.models.PaywallMessageItem

fun BannerMessage.mapToBannerPaywallMessage(): BannerPaywallMessage {
    val promoAction = determinePromoAction(this)
    val fallbackAction = determineFallbackAction(promoAction, this)
    return BannerPaywallMessage(
        attributionInfo = attributionInfo,
        messageRequirements = messageRequirements?.mapToMessageRequirements(),
        messageTracking = messageTracking?.mapToPaywallMessageTracking(),
        title = title,
        body = body,
        action = action,
        blocker = blocker,
        productName = productName,
        code = code,
        url = url,
        imageUrl = imageUrl,
        iterableImage = iterableImage?.let { IterableImage(light = it.light, dark = it.dark) },
        consume = consume,
        displayTrigger = displayTrigger?.mapToPaywallDisplayTrigger(),
        promoAction = promoAction,
        fallbackAction = fallbackAction
    )
}

fun BlockerMessage.mapToBlockerPaywallMessage(): BlockerPaywallMessage {
    return BlockerPaywallMessage(
        attributionInfo = attributionInfo,
        messageRequirements = messageRequirements?.mapToMessageRequirements(),
        messageTracking = messageTracking?.mapToPaywallMessageTracking(),
        title = contentTitle,
        body = contentBody,
        promo1 = promo1,
        promo2 = promo2,
        title1 = title1,
        title2 = title2,
        body1 = body1,
        body2 = body2,
        choice = choice,
        items = items?.map {
            PaywallMessageItem(
                id = it?.id,
                name = it?.name,
                code = it?.code,
                action = it?.action,
                title = it?.title,
                label = it?.label,
                caption = it?.caption,
                url = it?.url
            )
        },
        blocker = blocker
    )
}

fun determinePromoAction(banner: BannerMessage): PromoAction? {
    val product = banner.productName
    val code = banner.code
    val url = banner.url
    val button = banner.action
    val blocker = banner.blocker

    return if (!blocker.isNullOrEmpty()) {
        PromoAction.Blocker(
            name = blocker,
            action = button,
            actionRequirements = banner.actionRequirements?.mapToMessageRequirements()
        )
    } else if (!product.isNullOrEmpty()) {
        PromoAction.Code(
            product = product,
            code = code,
            action = button,
            actionRequirements = banner.actionRequirements?.mapToMessageRequirements()
        )
    } else if (!url.isNullOrEmpty()) {
        PromoAction.Open(
            url = url,
            action = button ?: "Go"
        )
    } else {
        null
    }
}

fun determineFallbackAction(promoAction: PromoAction?, bannerMessage: BannerMessage): PromoAction? {
    // currently only supports opening a url as the fallback
    return when (promoAction) {
        is PromoAction.Open -> null
        else -> bannerMessage.url?.let { url ->
            PromoAction.Open(url, bannerMessage.action ?: "Go")
        }
    }
}

fun List<MessageRequirement>.mapToMessageRequirements(): List<com.washingtonpost.android.paywall.models.MessageRequirements> {
    return this.map {
        com.washingtonpost.android.paywall.models.MessageRequirements(
            property = it.property,
            comparator = it.comparator,
            value = it.value,
            onlyOneOf = it.onlyOneOf?.mapToMessageRequirements()
        )
    }
}

fun DisplayTrigger.mapToPaywallDisplayTrigger(): com.washingtonpost.android.paywall.models.DisplayTrigger? {
    return if (type == null && depth == null) {
        null
    } else {
        val displayTriggerType = when (this.type) {
            "scroll" -> DisplayTriggerType.ScrollDepth
            else -> null
        }
        com.washingtonpost.android.paywall.models.DisplayTrigger(
            type = displayTriggerType,
            depth = depth
        )
    }
}

fun MessageTracking.mapToPaywallMessageTracking(): com.washingtonpost.android.paywall.models.MessageTracking {
    return com.washingtonpost.android.paywall.models.MessageTracking(
        kind = this.kind,
        campaignName = this.campaignName,
        offerType = this.offerType
    )
}

fun MessageRequirements.toMessageRequirement(): MessageRequirement {
    return MessageRequirement(
        property = property,
        comparator = comparator,
        value = value,
        onlyOneOf = onlyOneOf?.map { it.toMessageRequirement() }
    )
}