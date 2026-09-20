package com.wapo.flagship.features.deeplinks

import com.wapo.android.commons.util.Logger
import com.urbanairship.actions.DeepLinkListener
import com.urbanairship.analytics.CustomEvent
import com.wapo.android.commons.util.URLParser
import com.wapo.flagship.features.articles2.utils.appendTrackingParams
import com.wapo.flagship.util.tracking.Events
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.BuildConfig

/**
 * Airship actions of type "Deep Link" come here. For type "Web Page," see [AirshipUrlAllowListCallback].
 */
class AirshipDeepLinkListener(
    private val processor: DeepLinksProcessor,
) : DeepLinkListener {
    private val TAG = this.javaClass.simpleName

    override fun onDeepLink(deepLink: String): Boolean {
        Logger.d(TAG, "Airship Deep Link received: $deepLink")
        val deepLinkWithTrackingParams = appendTrackingParams(deepLink, "in_app_prompt", null)
        AirshipInAppMessageListener.updateMiscellany(deepLinkWithTrackingParams)
        Measurement.trackInAppMessage(
            Events.EVENT_IN_APP_MESSAGE_CTA,
            AirshipInAppMessageListener.inAppMessageData,
        )

        Measurement.isInAppMessageOriginated = true

        processor.processAsync(
            urlParser = URLParser(deepLinkWithTrackingParams),
            sourceType = DeepLinksProcessor.SourceType.IAA,
        )
        return true
    }

    companion object {
        @JvmStatic
        fun fireCustomEvent() {
            if (BuildConfig.DEBUG) {
                // Helper method to trigger Airship's IAA Full Screen dialog for testing.
                // CustomEvent.Builder("page_name").setEventValue(1).build().track()
                // to trigger Media dialog
                CustomEvent
                    .Builder("media_test")
                    .setEventValue(1)
                    .build()
                    .track()
                // to trigger Banner
                // CustomEvent.Builder("banner_test").setEventValue(1).build().track()
                // to trigger Fullscreen
                // CustomEvent.Builder("fullscreen_test").setEventValue(1).build().track()
            }
        }
    }
}
