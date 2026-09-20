// Copyright (c) 2025 The Washington Post. All rights reserved.
package com.wapo.flagship.sdk.iterable.models

import com.iterable.iterableapi.EmbeddedMessageElements
import com.iterable.iterableapi.IterableEmbeddedMessage
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Types
import com.wapo.android.commons.iterable.AttributionInfo
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.Logger
import com.wapo.android.remotelog.logger.RemoteLog
import com.washingtonpost.android.config.data.datasources.dto.config.paywallconf.RawBlockerJsonAdapter
import com.washingtonpost.android.config.data.datasources.utils.ConfigMoshiAdapters.moshi
import com.washingtonpost.android.config.domain.models.config.paywallconf.Blocker
import org.json.JSONArray
import org.json.JSONObject

// Regex to match tokens in the format {string:num} or {string:num:string}
private val tokenRegex = Regex(".*\\{[a-zA-Z]+:[0-9]+(:[a-zA-Z]+)?\\}.*")
const val DEFAULT_CAMPAIGN_ID = -1

fun IterableEmbeddedMessage.mapToBannerMessage(): PlacementMessage {
    var imageUrl: String? = null
    if (!elements?.mediaURL.isNullOrEmpty()) {
        imageUrl = elements?.mediaURL
    }

    var iterableImage: IterableImage? = null
    if (payload != null && payload?.isNull(PayloadKeys.Image.id) == false && payload?.get(
            PayloadKeys.Image.id
        ) is JSONObject
    ) {
        try {
            val adapter = moshi.adapter(IterableImage::class.java)
            iterableImage = adapter.fromJson(payload?.get(PayloadKeys.Image.id).toString())
            iterableImage?.light
                ?.takeIf { it.isNotBlank() }
                ?.let { imageUrl = it }
        } catch (e: Exception) {
            EventLog.Builder().apply {
                setMessage("Iterable image parsing error")
                setModule(LogModules.ITERABLE)
                setErrorMessage(e.message)
                setForceUpload()
            }.run {
                if (AppContextUtils.isConnectingOrConnected()) {
                    RemoteLog.e(AppContextUtils.appContext, build())
                }
            }
        }

    }

    return BannerMessage(
        attributionInfo = AttributionInfo(
            metadata.placementId,
            metadata.campaignId ?: DEFAULT_CAMPAIGN_ID,
            metadata.messageId
        ),
        title = elements?.title,
        body = elements?.body,
        action = elements?.getCustomTextFieldValue(CustomTextField.Action.id),
        blocker = elements?.getCustomTextFieldValue(CustomTextField.Blocker.id),
        productName = elements?.getCustomTextFieldValue(CustomTextField.Product.id),
        code = elements?.getCustomTextFieldValue(CustomTextField.Code.id),
        url = elements?.getCustomTextFieldValue(CustomTextField.Url.id),
        messageRequirements = getMessageRequirements(payload),
        actionRequirements = getActionRequirements(),
        messageTracking = getMessageTracking(payload),

        imageUrl = imageUrl,
        iterableImage = iterableImage,
        consume = getConsume(payload),
        displayTrigger = getDisplayTrigger(payload),
    )
}

fun IterableEmbeddedMessage.mapToBlockerMessage(): PlacementMessage {
    val tokens = elements?.text?.associate {
        "{${it.id.lowercase().replace(".", ":")}}" to it.text
    }?.toMap() ?: emptyMap()
    return BlockerMessage(
        attributionInfo = AttributionInfo(
            metadata.placementId,
            metadata.campaignId ?: DEFAULT_CAMPAIGN_ID,
            metadata.messageId
        ),
        contentTitle = elements?.title,
        contentBody = elements?.body,
        promo1 = elements?.getCustomTextFieldValue(CustomTextField.Promo1.id),
        promo2 = elements?.getCustomTextFieldValue(CustomTextField.Promo2.id),
        title1 = elements?.getCustomTextFieldValue(CustomTextField.Title1.id),
        title2 = elements?.getCustomTextFieldValue(CustomTextField.Title2.id),
        body1 = elements?.getCustomTextFieldValue(CustomTextField.Body1.id),
        body2 = elements?.getCustomTextFieldValue(CustomTextField.Body2.id),
        choice = elements?.getCustomTextFieldValue(CustomTextField.Choice.id),
        items = elements?.mapItemsList(),
        blocker = payload?.mapToBlocker(tokens),
        messageRequirements = getMessageRequirements(payload),
        messageTracking = getMessageTracking(payload)
    )
}

private fun EmbeddedMessageElements.getCustomTextFieldValue(id: String): String? {
    return text?.firstOrNull { it.id == id }?.text?.let { value ->
        if (id == CustomTextField.Code.id
            || (id.startsWith(CustomTextField.Product.id) && id.endsWith(CustomTextField.Code.id))
            || (id.startsWith(CustomTextField.Item.id) && id.endsWith(CustomTextField.Code.id))
        ) {
            value.lowercase()
        } else value
    }
}

private fun EmbeddedMessageElements.mapItemsList(): List<Item> {
    val items = mutableListOf<Item>()
    // Iterable supports up to 4 items in the custom text fields, so we loop through indices 1 to 4
    (1..4).forEach { index ->
        items.add(
            Item(
                id = getCustomTextFieldValue("${CustomTextField.Item.id}.$index.${CustomTextField.ProductID.id}"),
                name = getCustomTextFieldValue("${CustomTextField.Item.id}.$index.${CustomTextField.ProductID.id}"),
                code = getCustomTextFieldValue("${CustomTextField.Item.id}.$index.${CustomTextField.Code.id}"),
                action = getCustomTextFieldValue("${CustomTextField.Item.id}.$index.${CustomTextField.Action.id}"),
                title = getCustomTextFieldValue("${CustomTextField.Item.id}.$index.${CustomTextField.Title.id}"),
                label = getCustomTextFieldValue("${CustomTextField.Item.id}.$index.${CustomTextField.Label.id}"),
                caption = getCustomTextFieldValue("${CustomTextField.Item.id}.$index.${CustomTextField.Caption.id}"),
                url = getCustomTextFieldValue("${CustomTextField.Item.id}.$index.${CustomTextField.Url.id}")
            )
        )
    }
    return items
}

private fun JSONObject.mapToBlocker(tokens: Map<String, String?>): Blocker? {
    try {
        replaceValuesRecursive(this, tokenRegex, tokens)
        return RawBlockerJsonAdapter(
            moshi
        ).fromJson(this.toString())?.mapToDomain()
    } catch (e: Exception) {
        EventLog.Builder().apply {
            setMessage("Iterable tokens mapping error")
            setModule(LogModules.ITERABLE)
            setErrorMessage(e.message)
            setForceUpload()
        }.run {
            if (AppContextUtils.isConnectingOrConnected()) {
                RemoteLog.e(AppContextUtils.appContext, build())
            }
        }
        return null
    }
}

private fun replaceValuesRecursive(json: Any, regex: Regex, tokens: Map<String, String?>) {
    when (json) {
        is JSONObject -> {
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = json.get(key)
                when (value) {
                    is String -> {
                        if (value.matches(regex)) {
                            json.put(key, replaceToken(value, tokens))
                        }
                    }

                    is JSONObject, is JSONArray -> {
                        replaceValuesRecursive(value, regex, tokens)
                    }
                }
            }
        }

        is JSONArray -> {
            for (i in 0 until json.length()) {
                val element = json.get(i)
                when (element) {
                    is String -> {
                        if (element.matches(regex)) {
                            json.put(i, replaceToken(element, tokens))
                        }
                    }

                    is JSONObject, is JSONArray -> {
                        replaceValuesRecursive(element, regex, tokens)
                    }
                }
            }
        }
    }
}

private fun replaceToken(value: String, tokens: Map<String, String?>): String {
    var replacedJsonValue = value
    tokens.forEach { (token, tokenValue) ->
        if (replacedJsonValue.contains(token)) {
            replacedJsonValue = replaceTokenIfJsonContains(token, tokenValue, replacedJsonValue)
        }
    }
    return replacedJsonValue
}

fun IterableEmbeddedMessage.getRequirements(): List<MessageRequirement>? {
    return getMessageRequirements(payload, PayloadKeys.Require.id)
}

fun IterableEmbeddedMessage.getActionRequirements(): List<MessageRequirement>? {
    return getMessageRequirements(payload, PayloadKeys.ActionRequire.id)
}

private fun replaceTokenIfJsonContains(
    token: String,
    tokenValue: String?,
    jsonValue: String
): String = tokenValue
    ?.takeIf { it.isNotEmpty() }
    ?.run { jsonValue.replace(token, this) }
    ?: throw IllegalArgumentException()

fun getMessageRequirements(
    payload: JSONObject?,
    key: String = PayloadKeys.Require.id
): List<MessageRequirement>? {
    return if (payload != null && !payload.isNull(key) && payload.get(key) is JSONArray) {
        try {
            val adapter = moshi.adapter<List<MessageRequirement>>(
                Types.newParameterizedType(
                    List::class.java,
                    MessageRequirement::class.java
                ), emptySet(), key
            )
            adapter.fromJson(payload.get(key).toString())
        } catch (e: Exception) {
            EventLog.Builder().apply {
                setMessage("Iterable message requirements parsing error")
                setModule(LogModules.ITERABLE)
                setErrorMessage(e.message)
                setForceUpload()
            }.run {
                if (AppContextUtils.isConnectingOrConnected()) {
                    RemoteLog.e(AppContextUtils.appContext, build())
                }
            }
            null
        }

    } else null
}

fun getConsume(payload: JSONObject?): String? {
    return if (payload != null) {
        try {
            val consumeValue = payload.opt(PayloadKeys.Consume.id)
            if (consumeValue !is JSONArray) return null
            val listType = Types.newParameterizedType(List::class.java, String::class.java)
            val adapter: JsonAdapter<List<String>> = moshi.adapter(listType)
            val stringList: List<String>? = adapter.fromJson(consumeValue.toString())
            stringList?.firstOrNull()
        } catch (e: Exception) {
            EventLog.Builder().apply {
                setMessage("Iterable consume parsing error")
                setModule(LogModules.ITERABLE)
                setErrorMessage(e.message)
                setForceUpload()
            }.run {
                if (AppContextUtils.isConnectingOrConnected()) {
                    RemoteLog.e(AppContextUtils.appContext, build())
                }
            }
            null
        }
    } else null
}

fun getDisplayTrigger(payload: JSONObject?): DisplayTrigger? {
    return (payload?.opt("displayTrigger") as? JSONObject)?.let {
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
}

fun getMessageTracking(payload: JSONObject?): MessageTracking? {
    return (payload?.opt(PayloadKeys.Tracking.id) as? JSONObject)?.let {
        try {
            val adapter = moshi.adapter(MessageTracking::class.java)
            val parsedTracking = adapter.fromJson(it.toString())

            // defaults to wall
            if (parsedTracking?.kind == null) {
                parsedTracking?.copy(kind = "wall")
            } else {
                parsedTracking
            }
        } catch (e: Exception) {
            EventLog.Builder().apply {
                setMessage("Iterable tracking parsing error")
                setModule(LogModules.ITERABLE)
                setErrorMessage(e.message)
                setForceUpload()
            }.run {
                if (AppContextUtils.isConnectingOrConnected()) {
                    RemoteLog.e(AppContextUtils.appContext, build())
                }
            }
            null
        }
    }
}
