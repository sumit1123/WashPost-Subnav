package com.wapo.flagship.features.onetrust

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.analytics.FirebaseAnalytics
import com.onetrust.otpublishers.headless.Public.Keys.OTBroadcastServiceKeys
import com.wapo.android.commons.appsFlyer.AppsFlyer
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.deeplinks.AirshipAttributes
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.util.PrefUtils
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.features.ccpa.FLAG_NO
import com.washingtonpost.android.paywall.features.ccpa.FLAG_YES
import com.washingtonpost.android.paywall.features.ccpa.setHasUserOptedOutCCPAAdsTracking
import com.washingtonpost.android.paywall.helper.WpPaywallHelper
import com.washpost.airship.AirshipPrivacyManager
import com.washpost.airship.AirshipProvider

class OneTrustBroadcastReceivers(
    val appContext: Context,
    private val performanceState: MutableLiveData<Boolean>,
    private val functionalityState: MutableLiveData<Boolean>,
    private val targetingState: MutableLiveData<Boolean>,
    private val socialMediaState: MutableLiveData<Boolean>,
) {
    private val tag = OneTrustBroadcastReceivers::class.java.simpleName

    private var receiver: BroadcastReceiver? = null

    private val oneTrustConsentList =
        listOf(
            OneTrustConsents.STRICTLY_NECESSARY,
            OneTrustConsents.PERFORMANCE,
            OneTrustConsents.FUNCTIONALITY,
            OneTrustConsents.TARGETING,
            OneTrustConsents.SOCIAL_MEDIA,
        )

    private var registeredReceivers = false

    init {
        observePerformanceStateChanges()
        observeTargetingStateChanges()
    }

    fun registerOTBroadcastReceivers() {
        if (registeredReceivers) return

        val intentFilter = IntentFilter()

        oneTrustConsentList.forEach {
            when (it) {
                OneTrustConsents.PERFORMANCE -> {
                    intentFilter.addAction(OneTrustConsents.PERFORMANCE.groupId)
                }
                OneTrustConsents.FUNCTIONALITY -> {
                    intentFilter.addAction(OneTrustConsents.FUNCTIONALITY.groupId)
                }
                OneTrustConsents.SOCIAL_MEDIA -> {
                    intentFilter.addAction(OneTrustConsents.SOCIAL_MEDIA.groupId)
                }
                OneTrustConsents.TARGETING -> {
                    intentFilter.addAction(OneTrustConsents.TARGETING.groupId)
                }
                else -> {
                    /*
                        Stubbed
                     */
                }
            }
        }
        receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent,
                ) {
                    when (intent.action) {
                        OneTrustConsents.PERFORMANCE.groupId ->
                            onOneTrustPerformanceConsentChanged(
                                context,
                                intent,
                            )
                        OneTrustConsents.FUNCTIONALITY.groupId ->
                            onOneTrustFunctionalityConsentChanged(
                                context,
                                intent,
                            )
                        OneTrustConsents.SOCIAL_MEDIA.groupId ->
                            onOneTrustSocialMediaConsentChanged(
                                context,
                                intent,
                            )
                        OneTrustConsents.TARGETING.groupId ->
                            onOneTrustTargetingConsentChanged(
                                context,
                                intent,
                            )
                    }
                }
            }

        ContextCompat.registerReceiver(
            appContext,
            receiver,
            intentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        registeredReceivers = true
    }

    fun unregisterOTBroadcastReceivers() {
        receiver?.let { appContext.unregisterReceiver(it) }
        receiver = null
        registeredReceivers = false
    }

    /**
     * Performance category enables/disables Firebase Analytics here.
     * Crashlytics should be unaffected.
     * Chartbeat tracking is also controlled by Performance in [com.wapo.flagship.util.ChartbeatManager]
     */
    private fun onOneTrustPerformanceConsentChanged(
        context: Context,
        intent: Intent?,
    ) {
        val status = intent?.getIntExtra(OTBroadcastServiceKeys.EVENT_STATUS, -1)
        Logger.d(tag, "OTDebug, OTBroadcastReceiver, Performance, intent=$status")
        when (status) {
            1 -> {
                performanceState.value = true
            }
            else -> {
                performanceState.value = false
            }
        }
        updateConsentTokenInfo()
    }

    /**
     * This category currently has no effect in apps but must be present to sync with site.
     */
    private fun onOneTrustFunctionalityConsentChanged(
        context: Context?,
        intent: Intent?,
    ) {
        val status = intent?.getIntExtra(OTBroadcastServiceKeys.EVENT_STATUS, -1)
        Logger.d(tag, "OTDebug, OTBroadcastReceiver, Functionality, intent=$status")
        when (status) {
            1 -> {
                functionalityState.value = true
            }
            else -> {
                functionalityState.value = false
            }
        }
        updateConsentTokenInfo()
        onOneTrustConsentChanged()
    }

    /**
     * This category currently has no effect in apps but must be present to sync with site.
     */
    private fun onOneTrustSocialMediaConsentChanged(
        context: Context?,
        intent: Intent?,
    ) {
        val status = intent?.getIntExtra(OTBroadcastServiceKeys.EVENT_STATUS, -1)
        Logger.d(tag, "OTDebug, OTBroadcastReceiver, SocialMedia, intent=$status")
        when (status) {
            1 -> {
                socialMediaState.value = true
            }
            else -> {
                socialMediaState.value = false
            }
        }
        updateConsentTokenInfo()
    }

    /**
     * Targeting category enables/disables AppsFlyer tracking, RTE/For You, and IAA messages/data.
     * Cached data from RTE, For You, and IAA profile is cleared when Targeting consent is revoked.
     * Ad personalization should also be disabled in EU in response to this toggle.
     * Ad personalization is not controlled here but by the OneTrust SDK itself due to the IAB purpose nested within the Targeting toggle.
     */
    private fun onOneTrustTargetingConsentChanged(
        context: Context?,
        intent: Intent?,
    ) {
        val status = intent?.getIntExtra(OTBroadcastServiceKeys.EVENT_STATUS, -1)
        Logger.d(tag, "OTDebug, OTBroadcastReceiver, Targeting, intent=$status")
        when (status) {
            1 -> {
                targetingState.value = true
            }
            else -> {
                targetingState.value = false
            }
        }
        updateConsentTokenInfo()
        onOneTrustConsentChanged()
    }

    /**
     * Method to observe performance state livedata.
     * ProcessLifecycleOwner is the lifecycleOwner to keep observing as long as process is running.
     */
    private fun observePerformanceStateChanges() {
        performanceState.observe(ProcessLifecycleOwner.get()) { authorized ->
            initializePerformanceTracking(authorized)
        }
    }

    /**
     * Method to observe targeting state livedata.
     * ProcessLifecycleOwner is the lifecycleOwner to keep observing as long as process is running.
     */
    private fun observeTargetingStateChanges() {
        targetingState.observe(ProcessLifecycleOwner.get()) { authorized ->
            initializeTargetingTracking((authorized))
        }
    }

    /**
     * Method to initialize tracking when performance cookie changes its state from OT Cookies.
     * Chartbeat belongs to Performance category but is controlled independently.
     */
    fun initializePerformanceTracking(isAuthorized: Boolean) {
        Logger.d(
            tag,
            "OTDebug, OTBroadcastReceiver, initializePerformanceTracking, isAuthorized=$isAuthorized",
        )
        if (isAuthorized) {
            FirebaseAnalytics.getInstance(appContext).setAnalyticsCollectionEnabled(true)
            AirshipProvider.privacyManager.enable(AirshipPrivacyManager.Feature.ANALYTICS)
        } else {
            FirebaseAnalytics.getInstance(appContext).setAnalyticsCollectionEnabled(false)
            AirshipProvider.privacyManager.disable(AirshipPrivacyManager.Feature.ANALYTICS)
        }
        AirshipAttributes.updateFeaturesAttribute()
    }

    /**
     * Method to initialize tracking when targeting cookie changes its state from OT Cookies.
     * RTE/For You belong to Targeting category but are controlled independently.
     */
    fun initializeTargetingTracking(isAuthorized: Boolean) {
        Logger.d(
            tag,
            "OTDebug, OTBroadcastReceiver, initializeTargetingTracking, isAuthorized=$isAuthorized",
        )
        if (isAuthorized) {
            AppsFlyer.start(FlagshipApplication.getInstance().currentActivity)
            AirshipProvider.privacyManager.enable(AirshipPrivacyManager.Feature.IAA)
        } else {
            AppsFlyer.stopTracking(FlagshipApplication.getInstance().currentActivity)
            AirshipProvider.privacyManager.disable(AirshipPrivacyManager.Feature.IAA)
        }
        AirshipAttributes.updateFeaturesAttribute()
        Measurement.targetingConsentChanged(isAuthorized)
    }

    /**
     * OneTrust SDK notifies change of consent
     * Store the values in the Local pref with consent flag un-synchronized
     * Make a Identity Preference call to update the record
     * updateConsentTokenInfo calls when the app detects change from other platforms in cold_start
     * Updating the check to see if the consent is empty (if empty we will avoid making call to identity )
     */

    private fun updateConsentTokenInfo() {
        if (OneTrustHelper.isBannerShown()) {
            val consentToken =
                oneTrustConsentList.joinToString(separator = ",") {
                    it.groupId + ":" +
                        OneTrustHelper.ot.getConsentStatusForGroupId(
                            it.groupId,
                        )
                }
            if (consentToken.contains("-")) {
                if (consentToken.filter { it == '-' }.count() < oneTrustConsentList.size) {
                    EventLog
                        .Builder()
                        .apply {
                            setMessage("OneTrust Error")
                            setModule(LogModules.PRIVACY)
                            setErrorMessage("Mixed Consent Signal")
                            set("consent_token", consentToken)
                        }.run {
                            RemoteLog.e(appContext, build())
                        }
                }
                return
            }
            if (consentToken.isNotEmpty() && PrefUtils.getPrefConsentToken(appContext) != consentToken) {
                val adsOptedOut = !OneTrustHelper.isPersonalizedAdvertisingEnabled()
                val record =
                    WpPaywallHelper.getIdentityPreferences().apply {
                        switchTimestamp = System.currentTimeMillis()
                        otContentSynchronized = FLAG_NO
                        adsOptOut = if (adsOptedOut) FLAG_YES else FLAG_NO
                        dataSynchronized = FLAG_NO
                    }
                setHasUserOptedOutCCPAAdsTracking(appContext, adsOptedOut)
                WpPaywallHelper.setIdentityPreferences(record)
                PrefUtils.setPrefConsentToken(appContext, consentToken)
                PaywallService.getInstance().makeSaveIdentityPreferencesCallIfOneTrustConditionsAreMet()
                PaywallService.getInstance().makeSaveIdentityPreferencesCallIfConditionsAreMet()
            }
        }
    }

    private fun onOneTrustConsentChanged() {
        FlagshipApplication.getInstance().onPrivacyConsentStateChanged(
            functionalityState.value,
            targetingState.value
        )
    }
}
