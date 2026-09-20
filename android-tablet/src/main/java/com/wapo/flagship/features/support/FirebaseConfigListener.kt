package com.wapo.flagship.features.support

import android.content.Context
import com.wapo.android.commons.util.Logger
import com.google.android.gms.tasks.Task
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.installations.InstallationTokenResult
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.wapo.android.commons.util.AppContextUtils.appContext
import com.wapo.flagship.util.PrefUtils
import com.washingtonpost.android.BuildConfig
import com.washingtonpost.android.R
import com.washingtonpost.android.paywall.helper.PaywallPrefHelper

object FirebaseConfigListener {
    private const val TAG = "FirebaseRemoteConfig"

    fun initAndFetchFirebaseConfig(
        appContext: Context,
        onPreFetch: ((FirebaseRemoteConfig, storedValues: Map<String, String>) -> Unit)? = null,
        onPostFetch: ((FirebaseRemoteConfig) -> Unit)? = null
    ) {
        val mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val remoteConfigSettingsBuilder = FirebaseRemoteConfigSettings.Builder()
        if (BuildConfig.DEBUG) {
            remoteConfigSettingsBuilder.minimumFetchIntervalInSeconds = 30
        } else {
            remoteConfigSettingsBuilder.minimumFetchIntervalInSeconds = 3600
        }
        val configSettings = remoteConfigSettingsBuilder.build()
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings)
        mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        if (BuildConfig.DEBUG) {
            FirebaseInstallations
                .getInstance()
                .getToken(false)
                .addOnCompleteListener { task: Task<InstallationTokenResult> ->
                    if (task.isSuccessful) {
                        Logger.d("IID_TOKEN ", task.result.token)
                    } else {
                        Logger.e("IID_TOKEN", "Unable to get Installation auth token")
                    }
                }
        }

        onPreFetch?.invoke(mFirebaseRemoteConfig, PrefUtils.getABParametersMap(appContext))
        mFirebaseRemoteConfig
            .fetchAndActivate()
            .addOnCompleteListener {
                val remoteAbParameters: MutableMap<String, String> = HashMap()
                mFirebaseRemoteConfig.all.forEach { (abParameter, _) ->
                    val abValue = mFirebaseRemoteConfig.getString(abParameter)
                    Logger.d(TAG, "$abParameter:$abValue")
                    remoteAbParameters[abParameter] = abValue
                }

                val knownAbParameters =
                    remoteAbParameters
                        // This is not a great solution. Having to remember to update a constant whenever
                        // we want start an A/B test is bound to fail, and gives us less flexibility.
                        .filter {
                            ABTests.ACTIVE_AB_PARAMS.contains(it.key) ||
                                it.key.startsWith(ABTests.AIRSHIP_PATTERN) ||
                                    it.key.startsWith(ABTests.IAP_PATTERN)
                        }.toMutableMap()

                handleRctABTest(knownAbParameters)

                handleDailyReadABTest(knownAbParameters)
                
                PrefUtils.saveABParametersMap(appContext, knownAbParameters)

                onPostFetch?.invoke(mFirebaseRemoteConfig)
            }
    }

    /**
     * Handles the A/B test parameter for daily read.
     * Retrieves the daily read A/B test value from shared preferences and adds it to the known abParameters map.
     */
    private fun handleDailyReadABTest(knownAbParameters: MutableMap<String, String>) {
        PrefUtils.getABParametersMap(appContext)[ABTests.DAILY_READ]?.let { dailyReadValue ->
            knownAbParameters[ABTests.DAILY_READ] = dailyReadValue
        }
    }

    private fun handleRctABTest(abParameters: MutableMap<String, String>) {
        // wp_ak_v_mab=0|0|0|20240905; max-age=31536000; path=/; domain=.washingtonpost.com; SameSite=None; secure
        val rctCookieSplit = PaywallPrefHelper.getPrefTetroRctCookie()?.split("=") // separate out wp_ak_v_mab=
        val rctCookieValue = rctCookieSplit?.get(1)?.split(";") // get RCT value 0|0|0|20240905
        rctCookieValue?.get(0)?.let {
            abParameters.put("rct", it)
        }
    }
}

/**
 * List of A/B tests used by this version of the app.
 * Remove your A/B test from here when it is no longer used.
 */
object ABTests {
    const val AIRSHIP_PATTERN = "airship_" // This defines the pattern of Airship A/B tests, not an exact match
    const val IAP_PATTERN = "iap_"
    const val DAILY_READ = "dr_2503"
    // TODO PRICING can be removed after 8/21/25
    const val PRICING = "pricing_2507"
    const val PAYWALL_VERSION = "tt_2510"
    const val PROGRESS_BAR = "pb_2608"
    val ACTIVE_AB_PARAMS = listOf(PRICING, DAILY_READ, PAYWALL_VERSION, PROGRESS_BAR)
}
