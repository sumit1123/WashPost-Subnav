package com.washpost.airship

import androidx.annotation.Nullable
import com.urbanairship.UAirship
import com.urbanairship.UrlAllowList
import com.urbanairship.actions.DeepLinkListener
import com.urbanairship.automation.InAppAutomation
import com.urbanairship.channel.TagEditor
import com.urbanairship.iam.InAppMessageExtender
import com.urbanairship.iam.InAppMessageListener
import com.wapo.android.commons.util.Logger
import com.wapo.android.push.PushManager
import com.washingtonpost.android.config.domain.models.config.PushConfigStub

class AirshipPushManager(@Nullable val pushConfig: PushConfigStub?) : PushManager {

    override fun enablePushTopic(topicName: String, isEnabled: Boolean) {
        if (!isAirshipReady()) {
            return
        }
        val editor = UAirship.shared().channel.editTags()
        processTopic(topicName, isEnabled, editor)
        editor.apply()
    }

    override fun enablePushTopicBundle(topicNames: List<String>, isEnabled: Boolean) {
        if (!isAirshipReady()) {
            return
        }
        val editor = UAirship.shared().channel.editTags()
        for (topicName in topicNames) {
            processTopic(topicName, isEnabled, editor)
        }
        editor.apply()
    }

    override fun enablePushes(isEnabled: Boolean) {
        if (!isAirshipReady()) {
            return
        }
        UAirship.shared().pushManager.userNotificationsEnabled = isEnabled
    }

    override fun getPushId(): String? {
        if (!isAirshipReady()) {
            return null
        }
        return UAirship.shared().channel.id
    }

    override fun getUserId(): String? {
        if (!isAirshipReady()) {
            return null
        }
        return UAirship.shared().contact.namedUserId
    }

    private fun processTopic(topicName: String, isEnabled: Boolean, editor: TagEditor) {
        Logger.d(TAG, "WPPush - processTopic - $topicName: $isEnabled")
        if (isEnabled) {
            editor.addTag(topicName)
        } else {
            editor.removeTag(topicName)
        }
    }

    override fun enableRegistration() {
        if (!isAirshipReady()) {
            return
        }
        UAirship.shared().channel.enableChannelCreation()
    }

    fun setDeepLinkListener(deepLinkListener: DeepLinkListener?) {
        if (!isAirshipReady()) {
            return
        }
        UAirship.shared().deepLinkListener = deepLinkListener
    }

    fun setUrlAllowListCallback(urlAllowListCallback: UrlAllowList.OnUrlAllowListCallback?) {
        if (!isAirshipReady()) {
            return
        }
        UAirship.shared().urlAllowList.setUrlAllowListCallback(urlAllowListCallback)
    }

    override fun setUrlAllowListScopeOpenUrl(urlsList: List<String>?) {
        if (!isAirshipReady()) {
            return
        }
        urlsList?.forEach { entry -> UAirship.shared().urlAllowList.addEntry(entry) }
    }

    override fun getEnabledPushTopics(): Set<String> {
        if (!isAirshipReady()) {
            return emptySet()
        }
        return UAirship.shared().channel.tags
    }

    fun setInAppMessageListener(inAppMessageListener: InAppMessageListener?) {
        inAppMessageListener ?: return
        if (!isAirshipReady()) {
            return
        }
        InAppAutomation.shared().inAppMessageManager.addListener(inAppMessageListener)
    }

    fun setInAppMessageExtender(inAppMessageExtender: InAppMessageExtender?) {
        inAppMessageExtender ?: return
        if (!isAirshipReady()) {
            return
        }
        InAppAutomation.shared().inAppMessageManager.setMessageExtender(inAppMessageExtender)
    }

    override fun trackScreen(screenName: String) {
        if (!isAirshipReady()) {
            return
        }
        UAirship.shared().analytics.trackScreen(screenName)
    }

    override fun pauseInAppAutomation(paused: Boolean) {
        if (!isAirshipReady()) {
            return
        }
        InAppAutomation.shared().isPaused = paused
    }

    companion object {
        private val TAG = AirshipPushManager::class.java.simpleName

        @JvmStatic
        fun isAirshipReady(): Boolean {
            return UAirship.isTakingOff() || UAirship.isFlying()
        }
    }
}