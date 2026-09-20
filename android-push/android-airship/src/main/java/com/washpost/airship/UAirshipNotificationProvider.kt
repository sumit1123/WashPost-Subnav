package com.washpost.airship

import android.app.Notification
import android.content.Context
import com.google.gson.*
import com.urbanairship.actions.DeepLinkAction
import com.urbanairship.actions.OpenExternalUrlAction
import com.urbanairship.json.JsonSerializable
import com.urbanairship.json.JsonValue
import com.urbanairship.push.PushMessage
import com.urbanairship.push.notifications.NotificationArguments
import com.urbanairship.push.notifications.NotificationProvider
import com.urbanairship.push.notifications.NotificationResult
import com.urbanairship.util.NotificationIdGenerator
import com.urbanairship.util.UAStringUtil
import com.wapo.android.commons.util.Logger
import com.wapo.android.push.PushNotification
import com.wapo.android.push.PushService
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.reflect.Type
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


class UAirshipNotificationProvider : NotificationProvider {

    override fun onCreateNotificationArguments(context: Context, pushMessage: PushMessage): NotificationArguments {
        Logger.d(TAG, "WPPush - onCreateNotificationArguments")

        val notificationChannelId = context.getString(R.string.notification_channel_id)
        return NotificationArguments.newBuilder(pushMessage)
                .setNotificationChannelId(notificationChannelId)
                .setNotificationId(pushMessage.notificationTag, NotificationIdGenerator.nextID())
                .build()
    }

    override fun onNotificationCreated(context: Context, notification: Notification, notificationArguments: NotificationArguments) {
        Logger.d(TAG, "WPPush - onNotificationCreated")
    }

    override fun onCreateNotification(context: Context, notificationArguments: NotificationArguments): NotificationResult {
        Logger.d(TAG, "WPPush - onCreateNotification")

        val pushMessage = notificationArguments.message

        if (!UAStringUtil.isEmpty(pushMessage.alert)) {
            // message from Airship.
            Logger.d(TAG, "WPPush - Airship message")
            val pushMessageResponse = getPushMessageResponse(pushMessage)
            pushMessageResponse?.let { response ->
                getPushNotificationFromMessageResponse(response)?.let {
                    PushService.getInstance().listener.onMessage(notificationArguments.notificationId, it)?.let { builder ->
                        Logger.d(TAG, "WPPush - Airship message - notification builder is ready")
                        return NotificationResult.notification(builder.build())
                    }
                }
            }
        }

        // cancel non-airship/non-revere messages.
        Logger.d(TAG, "WPPush - No custom handling, passing by")
        return NotificationResult.cancel()
    }

    companion object {
        private val TAG: String = UAirshipNotificationProvider::class.java.simpleName
        private const val PAYLOAD_KEY = "custom"
        private const val DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

        private fun getPushMessageResponse(message: PushMessage): PushMessageResponse? {
            try {
                val custom = message.getExtra(PAYLOAD_KEY)
                if (!custom.isNullOrEmpty()) {
                    val customPayload = GsonBuilder()
                            .registerTypeAdapter(Date::class.java, DateDeserializer())
                            .create()
                            .fromJson<CustomPayload>(custom, CustomPayload::class.java)
                    return PushMessageResponse(message.alert, message.summary, message.title, customPayload)
                }
            } catch (e: Throwable) {
                Logger.e(TAG, "WPPush - Error in parsing PushMessage, $e")
                PushService.getInstance().listener.logError("${e.javaClass.simpleName}, errorMessage=${e.message}")
            }
            return null
        }

        private fun getPushNotificationFromMessageResponse(response: PushMessageResponse): PushNotification? {
            val segments = response.custom?.segments?.let {
                JSONArray().apply {
                    for (segmentedImage in it) {
                        if (segmentedImage != null) {
                            put(JSONObject().apply {
                                put(PushNotification.SegmentedImage.PARAM_NAME, segmentedImage.name)
                                put(PushNotification.SegmentedImage.PARAM_IMAGE_URL, segmentedImage.imageURL)
                            })
                        }
                    }
                }
            }
            val jsonObject = JSONObject().apply {
                put(PushNotification.PARAM_HEADLINE, response.headline)
                put(PushNotification.PARAM_TITLE, response.custom?.title)
                put(PushNotification.PARAM_BLURB, response.custom?.blurb)
                put(PushNotification.PARAM_URL, response.custom?.contentUrl)
                put(PushNotification.PARAM_IMAGE_URL, response.custom?.imageUrl)
                put(PushNotification.PARAM_TARGET_TOPIC, response.custom?.targetTopic)
                put(PushNotification.PARAM_SENT_TIMESTAMP, response.custom?.dateTime?.time)
                put(PushNotification.PARAM_PUSH_ID, response.custom?.pushId)
                put(PushNotification.PARAM_ANALYTICS_ID, response.custom?.analyticsTopic)
                put(PushNotification.PARAM_INTERACTION_TYPE, response.custom?.interactionType)
                put(PushNotification.PARAM_SEGMENTED_IMAGES, segments)
                put(PushNotification.PARAM_TYPE, response.custom?.type)
                put(PushNotification.PARAM_TEST_GROUPS, response.custom?.testGroups?.let { JSONObject(it) })
                put(PushNotification.PARAM_CAROUSEL_ACTION, response.custom?.shouldUpdateCarousel)
            }
            try {
                return PushNotification(jsonObject)
            } catch (e: JSONException) {
                Logger.e(TAG, "WPPush - JSON Error in creating PushNotification from PushMessage, $e")
                PushService.getInstance().listener.logError("JSONException, errorMessage=${e.message} content=$jsonObject")
            } catch (e: Throwable) {
                Logger.e(TAG, "WPPush - Error in creating PushNotification from PushMessage, $e")
                PushService.getInstance().listener.logError("${e.javaClass.simpleName}, errorMessage=${e.message} content=$jsonObject")
            }
            return null
        }

        private fun getUrl(actions: Map<String?, JsonSerializable?>): String? {
            try {
                if (actions.containsKey(OpenExternalUrlAction.DEFAULT_REGISTRY_SHORT_NAME)) {
                    return actions[OpenExternalUrlAction.DEFAULT_REGISTRY_SHORT_NAME]?.toJsonValue()?.string
                }
                if (actions.containsKey(OpenExternalUrlAction.DEFAULT_REGISTRY_NAME)) {
                    return actions[OpenExternalUrlAction.DEFAULT_REGISTRY_NAME]?.toJsonValue()?.string
                }
                if (actions.containsKey(DeepLinkAction.DEFAULT_REGISTRY_SHORT_NAME)) {
                    return actions[DeepLinkAction.DEFAULT_REGISTRY_SHORT_NAME]?.toJsonValue()?.string
                }
                if (actions.containsKey(DeepLinkAction.DEFAULT_REGISTRY_NAME)) {
                    return actions[DeepLinkAction.DEFAULT_REGISTRY_NAME]?.toJsonValue()?.string
                }
            } catch (e: Throwable) {
                Logger.e(TAG, "WPPush - Couldn't get article url from the payload")
            }
            return null
        }

        private fun getImageUrl(message: PushMessage): String? {
            return try {
                val stylePayload = message.stylePayload ?: return null
                val styleJson = JsonValue.parseString(stylePayload).optMap()
                styleJson.opt("big_picture").string
            } catch (e: Throwable) {
                Logger.e(TAG, "WPPush - Can not get image url from the payload")
                null
            }
        }
    }

    class DateDeserializer : JsonDeserializer<Date?> {
        @Throws(JsonParseException::class)
        override fun deserialize(element: JsonElement, arg1: Type, arg2: JsonDeserializationContext): Date? {
            val formatter = SimpleDateFormat(DATE_FORMAT).apply { timeZone = TimeZone.getTimeZone("UTC") }
            return try {
                formatter.parse(element.asString)
            } catch (e: ParseException) {
                Logger.e(TAG, "Failed to parse Date due to:", e)
                null
            }
        }
    }
}