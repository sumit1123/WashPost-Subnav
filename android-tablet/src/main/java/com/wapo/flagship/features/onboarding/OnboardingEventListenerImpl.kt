package com.wapo.flagship.features.onboarding

import android.view.View
import androidx.fragment.app.FragmentActivity
import com.wapo.flagship.IntentHelper
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.wapomain.MainConstants.ACTION_SHOW_PAYWALL
import com.wapo.flagship.wapomain.MainConstants.ACTION_SHOW_PAYWALL_REASON
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.auth.AuthIntentBuilder
import com.washingtonpost.android.paywall.util.PaywallConstants

open class OnboardingEventListenerImpl : BaseOnboardingFragment.OnboardingEventListener {
    override fun onClick(
        featureId: String?,
        v: View,
        activity: FragmentActivity?,
    ): Boolean {
        if (featureId == "onboarding_usw_1") { // feature id from onboarding_config.json

            if (v.tag == "LOGIN") {
                if (PaywallService.getInstance().wapoAccessServiceInstance.isPremiumUser) {
                    Measurement.trackSaveOnboardingContinue(false)
                } else if (PaywallService.getInstance().isPremiumUser) {
                    Measurement.setMiscellany(
                        Measurement.getDefaultMap(),
                        Measurement.SAVE_ONBOARDING,
                    )
                    if (activity?.isFinishing == false) {
                        PaywallService.getConnector().showSignInScreen(
                            activity?.supportFragmentManager,
                            AuthIntentBuilder().build(),
                            null,
                            PaywallConstants.WallType.ONBOARDING_PAYWALL,
                            false,
                            null
                        )
                    }
                } else {
                    if (activity?.isFinishing == false) {
                        val intent = IntentHelper.getMainActivityIntent(activity)
                        intent.action = ACTION_SHOW_PAYWALL
                        intent.putExtra(
                            ACTION_SHOW_PAYWALL_REASON,
                            PaywallConstants.ONBOARDING,
                        )
                        activity?.startActivity(intent)
                    }
                }
            }
        }

        return false
    }

    override fun getScreenConfig(
        onboardingConfig: OnboardingConfig,
        position: Int,
    ): Screen? {
        var screenConfig: Screen? = onboardingConfig?.screens?.get(position)

        if (onboardingConfig.id == "onboarding_usw_1") { // feature id from onboarding_config.json

            val contexts = onboardingConfig.screens?.get(position)?.contexts
            if (contexts != null) {
                if (PaywallService.getInstance().wapoAccessServiceInstance.isPremiumUser) {
                    screenConfig = contexts["onboardingSignedInSubscriber"]
                } else if (PaywallService.getInstance().isPremiumUser) {
                    screenConfig = contexts["onboardingIAPSubscriber"]
                } else {
                    screenConfig = contexts["onboardingNonSubscriber"]
                }
            }
        }

        return screenConfig
    }
}
