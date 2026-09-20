package com.washpost.airship

import android.content.Context
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.urbanairship.UrlAllowList
import com.urbanairship.actions.DeepLinkListener
import com.urbanairship.iam.InAppMessageExtender
import com.urbanairship.iam.InAppMessageListener
import com.wapo.android.commons.util.Logger
import com.wapo.android.push.PushManager
import com.wapo.android.push.PushService
import com.washingtonpost.android.config.domain.models.config.PushConfigStub

object AirshipProvider : PushService.PushProvider {

    private val TAG: String = AirshipProvider::class.java.simpleName
    private lateinit var pushManager: AirshipPushManager
    lateinit var privacyManager: AirshipPrivacyManager
        private set
    var deepLinkListener: DeepLinkListener? = null
    var urlAllowListCallback: UrlAllowList.OnUrlAllowListCallback? = null
    var inAppMessageListener: InAppMessageListener? = null
    var inAppMessageExtender: InAppMessageExtender? = null
    var urlAllowList: List<String>? = null

    override fun init(@NonNull context: Context, @Nullable pushConfig: PushConfigStub?) {
        Logger.d(TAG, "WPPush - init")
        pushManager = AirshipPushManager(pushConfig)
        privacyManager = AirshipPrivacyManager()
    }

    override fun getPushManager(): PushManager {
        return pushManager
    }

    fun getUserId(): String? {
        return pushManager.pushConfig?.userData
    }

    fun onAirshipReady() {
        pushManager.apply {
            setDeepLinkListener(deepLinkListener)
            setUrlAllowListCallback(urlAllowListCallback)
            setInAppMessageListener(inAppMessageListener)
            setUrlAllowListScopeOpenUrl(urlAllowList)
            setInAppMessageExtender(inAppMessageExtender)
        }
    }
}