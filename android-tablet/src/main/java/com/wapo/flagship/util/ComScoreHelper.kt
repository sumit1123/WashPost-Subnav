package com.wapo.flagship.util

import android.content.BroadcastReceiver
import android.content.ContextWrapper
import androidx.lifecycle.ProcessLifecycleOwner
import com.comscore.analytics.comScore
import com.wapo.android.commons.config.sec.helper.WapoSecDataProvider
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.lifecycle.ComscoreLifecycleObserver
import com.washingtonpost.android.BuildConfig
import com.washingtonpost.android.paywall.features.ccpa.hasUserOptedOutCCPAAdsTracking

object ComScoreHelper {
    const val ComScoreCCPALabelName = "cs_ucfr"
    const val ComScoreCCPAConsentNotGiven = "0"
    const val ComScoreCCPAConsentUnknown = ""

    private var initialized = false

    fun init(app: FlagshipApplication) {
        comScore.setAppContext(
            object : ContextWrapper(app) {
                //
                // workaround for a library crash we've registered
                override fun unregisterReceiver(receiver: BroadcastReceiver) {
                    try {
                        super.unregisterReceiver(receiver)
                    } catch (e: IllegalArgumentException) {
                        // just do not crash
                    }
                }
            },
        )

        comScore.setCustomerC2(WapoSecDataProvider.comscoreC2)
        comScore.setPublisherSecret(WapoSecDataProvider.comscoreSecret)

        val hasUserOptedOutCCPAAdsTracking = hasUserOptedOutCCPAAdsTracking(app)
        val labels = HashMap<String, String>()
        if (hasUserOptedOutCCPAAdsTracking) {
            labels[ComScoreCCPALabelName] = ComScoreCCPAConsentNotGiven
        } else {
            labels[ComScoreCCPALabelName] = ComScoreCCPAConsentUnknown
        }

        comScore.setLabels(labels)
        comScore.setDebug(BuildConfig.DEBUG)

        ProcessLifecycleOwner.get().lifecycle.addObserver(
            ComscoreLifecycleObserver(
                ProcessLifecycleOwner.get().lifecycle,
                hasUserOptedOutCCPAAdsTracking,
                this,
            ),
        )
        initialized = true
    }

    fun updateConsent(isOptedOut: Boolean) {
        if (!initialized) {
            return
        }
        if (isOptedOut) {
            comScore.setLabel(ComScoreCCPALabelName, ComScoreCCPAConsentNotGiven)
        } else {
            comScore.setLabel(ComScoreCCPALabelName, ComScoreCCPAConsentUnknown)
        }
        comScore.hidden()
    }

    fun isInitialized(): Boolean = initialized
}
