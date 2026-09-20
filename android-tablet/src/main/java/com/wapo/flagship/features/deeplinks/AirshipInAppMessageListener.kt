package com.wapo.flagship.features.deeplinks

import android.content.Context
import com.urbanairship.actions.DeepLinkAction
import com.urbanairship.iam.DisplayContent
import com.urbanairship.iam.InAppMessage
import com.urbanairship.iam.InAppMessageListener
import com.urbanairship.iam.ResolutionInfo
import com.urbanairship.iam.banner.BannerDisplayContent
import com.urbanairship.iam.fullscreen.FullScreenDisplayContent
import com.urbanairship.iam.modal.ModalDisplayContent
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.util.UtilsKt
import com.wapo.flagship.util.tracking.Events
import com.wapo.flagship.util.tracking.Measurement

class AirshipInAppMessageListener(
    val context: Context,
) : InAppMessageListener {
    val tag = AirshipInAppMessageListener::class.java.simpleName

    override fun onMessageDisplayed(
        scheduleId: String,
        message: InAppMessage,
    ) {
        Logger.d(tag, "InAppMessage, onMessageDisplayed, scheduleId=$scheduleId")
        EventLog
            .Builder()
            .apply {
                setMessage("InAppMessage, onMessageDisplayed")
                setModule(LogModules.ALERTS)
                set("schedule_id", scheduleId)
                set("user_status", AirshipAttributes.getUserStatusAttributeValue())
                set("features_status", AirshipAttributes.getFeaturesAttributeValue())
                set("identity_uuid", AirshipAttributes.getIdentityUUIDAttribute())
            }.run {
                RemoteLog.d(context, build())
            }
        messageDisplayed = true
        AirshipInAppMessageListener.message = message
        val displayContent = getDisplayContent(message)
        inAppMessageData =
            InAppMessageData(
                attributionInfo = null,
                scheduleId = scheduleId,
                title = message.name,
                headline = getHeadline(displayContent),
                eventLabel = message.extras.get("event_label")?.optString(),
            )
        Measurement.trackInAppMessage(
            Events.EVENT_IN_APP_MESSAGE_DISPLAYED,
            inAppMessageData,
        )
    }

    override fun onMessageFinished(
        scheduleId: String,
        message: InAppMessage,
        resolutionInfo: ResolutionInfo,
    ) {
        Logger.d(tag, "InAppMessage, onMessageFinished, scheduleId=$scheduleId")
        messageDisplayed = false
        // track only when message is finished with non deep link cases
        // (back press, dismiss button, close button etc.,)
        // deep link cases are tracked in [AirshipDeepLinkListener] class
        if (isButtonHasNoDeepLink(resolutionInfo)) {
            updateMiscellanyAsPerResolutionInfo(resolutionInfo)
            Measurement.trackInAppMessage(Events.EVENT_IN_APP_MESSAGE_FINISHED, inAppMessageData)
        }
        AirshipInAppMessageListener.message = null
        inAppMessageData = null
    }

    companion object {
        @JvmStatic
        @Volatile
        var message: InAppMessage? = null

        @JvmStatic
        @Volatile
        var messageDisplayed: Boolean = false

        @JvmStatic
        @Volatile
        var inAppMessageData: InAppMessageData? = null

        @JvmStatic
        fun getDisplayContent(message: InAppMessage): DisplayContent? =
            when (message.type) {
                InAppMessage.TYPE_MODAL -> message.getDisplayContent<ModalDisplayContent>()
                InAppMessage.TYPE_BANNER -> message.getDisplayContent<BannerDisplayContent>()
                InAppMessage.TYPE_FULLSCREEN -> message.getDisplayContent<FullScreenDisplayContent>()
                else -> null
            }

        @JvmStatic
        fun getHeadline(displayContent: DisplayContent?): String? =
            when (displayContent) {
                is ModalDisplayContent -> displayContent.heading?.text
                is BannerDisplayContent -> displayContent.heading?.text
                is FullScreenDisplayContent -> displayContent.heading?.text
                else -> null
            }

        /**
         * Returns the button's text in analytics snake case.
         */
        @JvmStatic
        fun getButtonName(deepLink: String): String? =
            message?.run {
                when (val displayContent = getDisplayContent(this)) {
                    is ModalDisplayContent ->
                        displayContent.buttons
                            .firstOrNull {
                                it.actions[DeepLinkAction.DEFAULT_REGISTRY_NAME]?.optString() == deepLink
                            }?.label
                            ?.text
                            ?.let { UtilsKt.toAnalyticsSnakeCase(it) }
                    is BannerDisplayContent ->
                        displayContent.buttons
                            .firstOrNull {
                                it.actions[DeepLinkAction.DEFAULT_REGISTRY_NAME]?.optString() == deepLink
                            }?.label
                            ?.text
                            ?.let { UtilsKt.toAnalyticsSnakeCase(it) }
                    is FullScreenDisplayContent ->
                        displayContent.buttons
                            .firstOrNull {
                                it.actions[DeepLinkAction.DEFAULT_REGISTRY_NAME]?.optString() == deepLink
                            }?.label
                            ?.text
                            ?.let { UtilsKt.toAnalyticsSnakeCase(it) }
                    else -> null
                }
            }

        @JvmStatic
        fun updateMiscellany(deepLink: String) {
            inAppMessageData?.miscellany = getButtonName(deepLink)
        }

        @JvmStatic
        fun updateMiscellanyAsPerResolutionInfo(resolutionInfo: ResolutionInfo) {
            inAppMessageData?.miscellany =
                when (resolutionInfo.type) {
                    ResolutionInfo.RESOLUTION_USER_DISMISSED -> "x_button"
                    ResolutionInfo.RESOLUTION_BUTTON_CLICK -> resolutionInfo.buttonInfo?.id
                    else -> resolutionInfo.type
                }
        }

        @JvmStatic
        fun isButtonHasNoDeepLink(resolutionInfo: ResolutionInfo): Boolean = resolutionInfo.buttonInfo?.actions?.isEmpty() ?: true
    }
}
