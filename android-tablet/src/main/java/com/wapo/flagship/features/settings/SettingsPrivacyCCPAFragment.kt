package com.wapo.flagship.features.settings

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import com.wapo.flagship.Utils
import com.wapo.flagship.features.onetrust.OneTrustHelper
import com.wapo.flagship.util.ComScoreHelper
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.util.tracking.states.NavigationBehavior
import com.washingtonpost.android.consent.ccpa.PrivacySettingsFragment
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.auth.AuthIntentBuilder
import com.washingtonpost.android.paywall.features.ccpa.FLAG_NO
import com.washingtonpost.android.paywall.features.ccpa.FLAG_YES
import com.washingtonpost.android.paywall.features.ccpa.isCCPAOptedOut
import com.washingtonpost.android.paywall.features.ccpa.setHasUserOptedOutCCPAAdsTracking
import com.washingtonpost.android.paywall.helper.WpPaywallHelper
import com.washingtonpost.android.paywall.util.PaywallConstants

class SettingsPrivacyCCPAFragment : PrivacySettingsFragment() {
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        Measurement.setNavigationBehavior(NavigationBehavior.CCPA)
    }

    override fun getPrivacySettingsListener(): PrivacySettingsListener? = AppPrivacySettingsListener()

    val loginActivityResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                updateViewsOnUserSignIn(PaywallService.getInstance().isWpUserLoggedIn)
            }
        }

    inner class AppPrivacySettingsListener : PrivacySettingsListener {
        override fun privacySettingsUrlClicked(url: String?) {
            Utils.startWebActivity(url, requireActivity(), false)
        }

        override fun privacySettingsSwitchChanged(isChecked: Boolean) {
            setHasUserOptedOutCCPAAdsTracking(requireContext(), !isChecked)
            OneTrustHelper.updatePersonalizedAdvertisingConsent(isChecked)
            ComScoreHelper.updateConsent(!isChecked)
            val record =
                WpPaywallHelper.getIdentityPreferences().apply {
                    adsOptOut = if (!isChecked) FLAG_YES else FLAG_NO
                    explicitNotice = FLAG_YES
                    dataSynchronized = FLAG_NO
                    switchTimestamp = System.currentTimeMillis()
                }
            WpPaywallHelper.setIdentityPreferences(record)
            PaywallService.getInstance().makeSaveIdentityPreferencesCallIfConditionsAreMet()
            Measurement.ccpaAdsConsentChanged(!isChecked)
        }

        override fun privacySettingsSignInClicked() {
            PaywallService.getConnector().showSignInScreen(
                activity?.supportFragmentManager,
                AuthIntentBuilder().build(),
                null,
                PaywallConstants.WallType.CCPA_PAYWALL,
                false,
                null
            )
        }

        override fun getOptOutStatus(): Boolean = isCCPAOptedOut()
    }
}
