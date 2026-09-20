package com.wapo.flagship.features.deeplinks

import com.wapo.android.commons.util.Logger
import com.urbanairship.UrlAllowList
import com.wapo.android.commons.util.URLParser
import com.wapo.flagship.util.tracking.Events
import com.wapo.flagship.util.tracking.Measurement

/**
 * Airship actions of type "Web Page" come here. For type "Deep Link," see [AirshipDeepLinkListener].
 * This callback only gets called if the url is listed in the static list (openURLPatterns)
 */
class AirshipUrlAllowListCallback : UrlAllowList.OnUrlAllowListCallback {
    private val TAG = this.javaClass.simpleName

    override fun allowUrl(
        url: String,
        scope: Int,
    ): Boolean {
        Logger.d(TAG, "Airship Web Page received: $url")
        AirshipInAppMessageListener.updateMiscellany(url)
        Measurement.trackInAppMessage(
            Events.EVENT_IN_APP_MESSAGE_CTA,
            AirshipInAppMessageListener.inAppMessageData,
        )
        DeepLinksProcessor.processAsync(
            urlParser = URLParser(url),
            sourceType = DeepLinksProcessor.SourceType.IAA,
        )
        return false // delegate to [DeepLinksProcessor] instead of letting Airship handle it
    }
}
