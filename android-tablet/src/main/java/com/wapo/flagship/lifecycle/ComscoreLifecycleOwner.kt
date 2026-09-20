package com.wapo.flagship.lifecycle

import androidx.lifecycle.Lifecycle
import com.comscore.analytics.comScore
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.util.ComScoreHelper
import com.washingtonpost.android.paywall.features.ccpa.hasUserOptedOutCCPAAdsTracking

class ComscoreLifecycleObserver(
    lifecycle: Lifecycle,
    private var initialCcpaOptedOutStatus: Boolean,
    private val comScoreHelper: ComScoreHelper,
) : FlagshipLifecycleObserver(
        lifecycle,
    ) {
    override fun onApplicationStart() {
        super.onApplicationStart()
        val hasUserOptedOutCCPAAdsTracking =
            hasUserOptedOutCCPAAdsTracking(
                FlagshipApplication.getInstance(),
            )
        if (initialCcpaOptedOutStatus != hasUserOptedOutCCPAAdsTracking) {
            initialCcpaOptedOutStatus = hasUserOptedOutCCPAAdsTracking
            comScoreHelper.updateConsent(hasUserOptedOutCCPAAdsTracking)
        }
        comScore.onEnterForeground()
    }

    override fun onApplicationPause() {
        super.onApplicationPause()
        comScore.onExitForeground()
    }
}
