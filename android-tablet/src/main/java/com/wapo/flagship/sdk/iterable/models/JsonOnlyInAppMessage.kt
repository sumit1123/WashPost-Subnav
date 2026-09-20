package com.wapo.flagship.sdk.iterable.models

import com.iterable.iterableapi.IterableInAppMessage
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Types
import com.wapo.android.commons.iterable.AttributionInfo
import com.wapo.android.commons.util.Logger
import com.washingtonpost.android.config.data.datasources.utils.ConfigMoshiAdapters.moshi
import com.washingtonpost.android.paywall.models.BannerPaywallMessage
import org.json.JSONArray
import org.json.JSONObject

const val CONSUME_DISMISS = "dismiss"
const val CONSUME_ACTION = "action"

data class JsonOnlyInAppMessage(
    val attributionInfo: AttributionInfo? = null,
    val banner: BannerMessage? = null,
    val placement: String? = null,
    val consume: String? = null,
    val displayTrigger: DisplayTrigger? = null,
)

fun IterableInAppMessage.toJsonOnlyInAppMessage(): JsonOnlyInAppMessage {
    val attributionInfo = AttributionInfo(
        campaignId = campaignId?.toInt() ?: DEFAULT_CAMPAIGN_ID,
        messageId = messageId
    )
    var isDismissable = true
    val consume = (customPayload?.opt("consume") as? JSONArray)?.let {
        try {
            val listType = Types.newParameterizedType(List::class.java, String::class.java)
            val adapter: JsonAdapter<List<String>> = moshi.adapter(listType)
            val stringList: List<String>? =
                adapter.fromJson(customPayload?.get("consume").toString())
            isDismissable =
                stringList?.contains(CONSUME_DISMISS) == true || stringList.isNullOrEmpty()
            stringList?.firstOrNull() ?: CONSUME_DISMISS
        } catch (e: Exception) {
            Logger.v(
                "JsonOnlyInAppMessage",
                "Failed to parse 'consume' field from customPayload - ${e.message}, defaulting to 'dismiss'",
            )
            CONSUME_DISMISS
        }
    } ?: CONSUME_DISMISS


    val displayTrigger = (customPayload?.opt("displayTrigger") as? JSONObject)?.let {
        try {
            val adapter = moshi.adapter(DisplayTrigger::class.java)
            adapter.fromJson(it.toString())
        } catch (e: Exception) {
            Logger.v(
                "JsonOnlyInAppMessage",
                "Failed to parse 'displayTrigger' field from customPayload - ${e.message}, defaulting to 'null'",
            )
            null
        }
    }

    val messageTracking = (customPayload?.opt("tracking") as? JSONObject)?.let {
        try {
            val adapter = moshi.adapter(MessageTracking::class.java)
            adapter.fromJson(it.toString())
        } catch (e: Exception) {
            Logger.v(
                "JsonOnlyInAppMessage",
                "Failed to parse 'tracking' field from customPayload - ${e.message}",
            )
            null
        }
    }


    return JsonOnlyInAppMessage(
        attributionInfo = attributionInfo,
        banner = (customPayload?.opt("banner") as? JSONObject)?.let { banner ->
            BannerMessage(
                attributionInfo = attributionInfo,
                messageRequirements = getMessageRequirements(customPayload),
                actionRequirements = getMessageRequirements(customPayload, PayloadKeys.ActionRequire.id),
                messageTracking = messageTracking,
                title = banner.opt("title") as? String,
                body = banner.opt("text") as? String,
                productName = banner.opt("product") as? String,
                code = (banner.opt("code") as? String)?.takeIf { it.isNotEmpty() }?.lowercase(),
                action = banner.opt("action") as? String,
                url = banner.opt("url") as? String,
                blocker = banner.opt("blocker") as? String,
                imageUrl = banner.opt("image") as? String,
                dismissible = isDismissable,
                consume = consume,
                displayTrigger = displayTrigger
            )
        },
        consume = consume,
        placement = (customPayload?.opt("placement") as? String),
        displayTrigger = displayTrigger
    )
}

fun JsonOnlyInAppMessage.mapToBannerPaywallMessage(): BannerPaywallMessage? {
    val attributionInfo = attributionInfo ?: return null
    val message = this
    return banner?.run {
        val promoAction = determinePromoAction(this)
        BannerPaywallMessage(
            attributionInfo = AttributionInfo(
                attributionInfo.placementId,
                attributionInfo.campaignId,
                attributionInfo.messageId
            ),
            messageRequirements = messageRequirements?.mapToMessageRequirements(),
            messageTracking = messageTracking?.mapToPaywallMessageTracking(),
            title = title,
            body = body,
            action = action,
            blocker = blocker,
            productName = productName,
            code = code,
            url = url,
            dismissible = dismissible,
            consume = consume,
            displayTrigger = message.displayTrigger?.mapToPaywallDisplayTrigger(),
            promoAction = promoAction,
            fallbackAction = determineFallbackAction(promoAction, this)
        )
    }
}

