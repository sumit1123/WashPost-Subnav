package com.wapo.flagship.sdk.iterable.models

import com.wapo.flagship.sdk.iterable.models.PaywallUser.Companion.SOURCE_DEVICE
import com.wapo.flagship.sdk.iterable.models.PaywallUser.Companion.SOURCE_DIGITAL
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.newdata.model.Subscription
import com.washingtonpost.android.paywall.newdata.model.WpUser
import com.washingtonpost.android.paywall.util.PaywallConstants
import com.washingtonpost.android.paywall.util.PaywallUtil

data class PaywallUser(
    val loggedInUser: WpUser? = null,
    val isPremium: Boolean,
    val subStatus: String?,
    val userProducts: List<String>,
    val subSource: String?,
    val accessLevel: String?,
    val cachedSub: Subscription?,
    val deviceSubscriptionId: String?,
) {
    companion object {
        const val SOURCE_DEVICE = "DEVICE"
        const val SOURCE_DIGITAL = "DIGITAL"
    }
}

fun buildUserContext(): PaywallUser? {
    val paywallService = PaywallService.getInstance() ?: return null

    val isLoggedInUser = paywallService.loggedInUser
    val isPremiumUser = paywallService.isPremiumUser

    // Build userProducts from the cached subscription's productSkuList by reverse-mapping
    // SKUs to server-side product names (e.g., "wp.classic.basic" -> "BASIC",
    // "wp.classic.no.ads.monthly" -> "AD_FREE"). Works for both anonymous and logged-in users.
    val cachedSub = PaywallService.getBillingHelper().cachedSubscription()
    var userProducts = PaywallUtil.resolveProductNamesFromSkus(cachedSub?.productSkuList)

    // Fallback to profile subscriptions for logged-in site/cross-platform users
    if (userProducts.isEmpty()) {
        val user = PaywallService.getInstance().loggedInUser
        val profileProducts = user?.subscriptions?.mapNotNull { it?.product }
        userProducts = if (!profileProducts.isNullOrEmpty()) {
            profileProducts
        } else if (user?.subStatus == PaywallConstants.ACTIVE) {
            listOfNotNull(user.accessLevel)
        } else {
            emptyList()
        }
    }

    // Resolve sub source
    val source = when {
        cachedSub != null || PaywallService.getBillingHelper().isClassicOrRainbowSubscriptionActive() -> SOURCE_DEVICE
        isLoggedInUser?.subStatus == PaywallConstants.ACTIVE || isLoggedInUser?.subStatus == PaywallConstants.SUSPENDED -> SOURCE_DIGITAL
        else -> null
    }

    return PaywallUser(
        loggedInUser = isLoggedInUser,
        isPremium = isPremiumUser,
        subStatus = isLoggedInUser?.subStatus ?: if (cachedSub != null) PaywallConstants.ACTIVE else null,
        userProducts = userProducts,
        subSource = source,
        accessLevel = isLoggedInUser?.accessLevel,
        cachedSub = cachedSub,
        deviceSubscriptionId = paywallService.subscriptionID
    )
}
