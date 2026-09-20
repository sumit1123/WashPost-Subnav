package com.wapo.flagship.features.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import androidx.preference.Preference
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.Utils
import com.wapo.flagship.features.onetrust.OneTrustHelper
import com.washingtonpost.android.R
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.paywall.PaywallService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsPrivacyFragment :
    BasePreferenceFragmentCompat(),
    Preference.OnPreferenceClickListener {

    private val config get() = ConfigManager.getInstance().config

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        setPreferencesFromResource(R.xml.pref_settings_privacy, rootKey)
        findPreference<Preference>(AppPreferences.PREF_AD_INFO)?.onPreferenceClickListener = this
        findPreference<Preference>(AppPreferences.PREF_NOTICE_OF_COLLECTION)?.onPreferenceClickListener = this
        findPreference<Preference>(AppPreferences.PREF_DO_NOT_SELL_INFO)?.onPreferenceClickListener = this
        findPreference<Preference>(AppPreferences.PREF_PRIVACY_POLICY)?.onPreferenceClickListener = this
        findPreference<Preference>(AppPreferences.PREF_ONETRUST)?.onPreferenceClickListener = this

        val context = FlagshipApplication.getInstance().applicationContext
        val gdprApplies = OneTrustHelper.gdprApplies(context)
        findPreference<Preference>(AppPreferences.PREF_NOTICE_OF_COLLECTION)?.isVisible = !gdprApplies
        findPreference<Preference>(AppPreferences.PREF_DO_NOT_SELL_INFO)?.isVisible = !gdprApplies
        findPreference<Preference>(AppPreferences.PREF_ONETRUST)?.isVisible = gdprApplies
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        when (preference.key) {
            AppPreferences.PREF_AD_INFO -> {
                Utils.startWebActivity(getString(R.string.ad_choices_url), activity, false)
                return true
            }
            AppPreferences.PREF_NOTICE_OF_COLLECTION -> {
                Utils.startWebActivity(config.noticeOfCollectionUrl, activity, false)
                return true
            }
            AppPreferences.PREF_DO_NOT_SELL_INFO -> {
                val action =
                    SettingsPrivacyFragmentDirections.settingsPrivacyCcpa().apply {
                        config = ConfigManager.getInstance().config.privacyConsentConfig
                        isSignedIn = PaywallService.getInstance().isWpUserLoggedIn
                    }
                Navigation.findNavController(requireView()).navigate(action)
                return true
            }
            AppPreferences.PREF_PRIVACY_POLICY -> {
                Utils.startWebActivity(config.privacyPolicyUrl, activity, false)
                return true
            }
            AppPreferences.PREF_ONETRUST -> {
                val context = FlagshipApplication.getInstance()
                if (OneTrustHelper.ot.isBannerShown(context) == -1) {
                    val activity = activity
                    if (activity is AppCompatActivity) {
                        OneTrustHelper.initSdk(activity)
                    }
                } else {
                    activity?.let { OneTrustHelper.ot.showPreferenceCenterUI(it) }
                }
                return true
            }
        }
        return false
    }
}
