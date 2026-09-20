package com.wapo.flagship.util

import android.content.Context
import com.wapo.android.commons.util.Logger
import com.chartbeat.androidsdk.Tracker
import com.wapo.flagship.features.onetrust.OneTrustHelper
import com.wapo.flagship.wrappers.CrashWrapper
import com.washingtonpost.android.BuildConfig
import java.lang.Exception

object ChartbeatManager {
    private var isInitialized = false
    private val TAG: String = ChartbeatManager::class.java.simpleName

    @JvmStatic
    fun init(
        accountId: String,
        domain: String,
        context: Context,
    ) = process({
        Tracker.DEBUG_MODE = BuildConfig.DEBUG
        Tracker.setupTracker(accountId, domain, context)
        isInitialized = true
    }, true)

    @JvmStatic
    fun setUserPaid() =
        process({
            Tracker.setUserPaid()
        })

    @JvmStatic
    fun setUserLoggedIn() =
        process({
            Tracker.setUserLoggedIn()
        })

    @JvmStatic
    fun setUserAnonymous() =
        process({
            Tracker.setUserAnonymous()
        })

    @JvmStatic
    fun userInteracted() =
        process({
            Tracker.userInteracted()
        })

    @JvmStatic
    fun setAppReferrer(referrer: String?) =
        process({
            Tracker.setAppReferrer(referrer)
        })

    @JvmStatic
    fun setPushReferrer(referrer: String?) =
        process({
            Tracker.setPushReferrer(referrer)
        })

    @JvmStatic
    fun pauseTracker() =
        process({
            Tracker.pauseTracker()
        })

    /**
     * Do not track view if OneTrust Performance consent (analytics) is not given.
     */
    @JvmStatic
    fun trackView(
        context: Context,
        id: String?,
        title: String?,
    ) = process({
        if (OneTrustHelper.isPerformanceEnabled()) {
            Tracker.trackView(context, id, title)
        }
    })

    @JvmStatic
    fun setSections(sections: String?) =
        process({
            Tracker.setSections(sections)
        })

    @JvmStatic
    fun setAuthors(authors: String?) =
        process({
            Tracker.setAuthors(authors)
        })

    @JvmStatic
    private fun process(
        wrappingFunction: () -> Unit,
        shouldSkipCheck: Boolean = false,
    ) {
        try {
            if (isInitialized || shouldSkipCheck) {
                wrappingFunction()
            } else {
                Logger.d(TAG, "Chartbeat is not initialized")
            }
        } catch (e: Exception) {
            Logger.d(TAG, "Chartbeat error", e)
            CrashWrapper.sendException(e)
        }
    }
}
