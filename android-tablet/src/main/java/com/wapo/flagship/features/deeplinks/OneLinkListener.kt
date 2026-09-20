package com.wapo.flagship.features.deeplinks

import com.wapo.android.commons.util.Logger
import com.wapo.android.commons.appsFlyer.OneLinkInterface
import com.wapo.android.commons.util.URLParser
import com.wapo.flagship.IntentHelper
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.util.tracking.states.TrafficSource
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.paywall.PaywallService

/**
 * Listener for OneLink (AppsFlyer) deeplinks.
 */
class OneLinkListener : OneLinkInterface {
    private val TAG = "OneLink"

    /**
     * Called when a valid OneLink deeplink request is ready to be dispatched to the app's general [DeepLinksProcessor].
     */
    override fun onDeepLink(deepLink: String) {
        // Return if it is from the recent history.
        val activity = DeepLinksProcessor.getCurrentActivity()
        activity?.apply {
            if (IntentHelper.isLaunchedFromRecent(this.intent)) {
                return
            }
        }
        setOneLinkNavigationBehavior(deepLink)
        val urlParser = URLParser(deepLink)
        urlParser.uri?.let {
            Measurement.mark(TrafficSource.fromDeepLinkUri(it))
        }
        DeepLinksProcessor.processAsync(
            urlParser = urlParser,
            sourceType = DeepLinksProcessor.SourceType.ONE_LINK,
        )
    }

    /**
     * Called when a valid OneLink deeplink request contains an authentication token which we can log the user in with.
     */
    override fun onAuthentication(oneLinkToken: String) {
        val activity = DeepLinksProcessor.getCurrentActivity()
        PaywallService.getInstance().authenticateWithOneLinkToken(oneLinkToken, activity)
    }

    /**
     * Called in various OneLink error states when attempting to resolve a deeplink.
     */
    override fun onError(msg: String?) {
        if (msg != null) {
            Logger.e(TAG, msg)
//            RemoteLog.e(msg, context)
        }
    }

    /**
     * Sets [Measurement.oneLinkNavigationBehavior] to the sourceApp value if it exists and is valid.
     * Otherwise, sets [Measurement.oneLinkNavigationBehavior] to [Measurement.PATH_TO_VIEW_ONELINK].
     * If [Measurement.oneLinkNavigationBehavior] is set,
     * it is passed as the navigation_behavior in the next page_view event (and then cleared)
     */
    private fun setOneLinkNavigationBehavior(deepLink: String) {
        val sourceApp = URLParser(deepLink).getQueryParameter(Measurement.SOURCE_APP)
        if (ConfigManager.getInstance().config
                .deepLinkConfig.validSourceAppValues
                .contains(sourceApp)
        ) {
            Measurement.oneLinkNavigationBehavior = sourceApp
        } else {
            Measurement.oneLinkNavigationBehavior = Measurement.PATH_TO_VIEW_ONELINK
        }
    }
}
