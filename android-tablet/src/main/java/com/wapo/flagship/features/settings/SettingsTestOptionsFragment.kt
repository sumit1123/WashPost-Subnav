package com.wapo.flagship.features.settings

import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.remotelog.logger.RemoteLog
import com.washingtonpost.android.R
import dagger.hilt.android.AndroidEntryPoint

/**
 * Class to handle "Test Options" preference
 */
@AndroidEntryPoint
class SettingsTestOptionsFragment :
    BasePreferenceFragmentCompat(),
    Preference.OnPreferenceChangeListener {
    private var prefTestAdsValue: EditTextPreference? = null

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        setPreferencesFromResource(R.xml.pref_settings_test_options, rootKey)

        findPreference<Preference>(AppPreferences.PREF_TEST_ADS)?.apply {
            this.onPreferenceChangeListener = this@SettingsTestOptionsFragment
        }
        prefTestAdsValue =
            findPreference<EditTextPreference>(AppPreferences.PREF_TEST_ADS_VALUE)?.apply {
                this.onPreferenceChangeListener = this@SettingsTestOptionsFragment
                isVisible = AppPreferences.isTestAdsEnabled()
            }
    }

    override fun onPreferenceChange(
        preference: Preference,
        newValue: Any?,
    ): Boolean {
        when (preference.key) {
            AppPreferences.PREF_TEST_ADS -> {
                prefTestAdsValue?.isVisible = newValue as? Boolean ?: false
            }
            AppPreferences.PREF_TEST_ADS_VALUE -> {
                if (prefTestAdsValue?.text != newValue) {
                    EventLog
                        .Builder()
                        .apply {
                            setMessage(
                                "Settings Ads Test Options value changed. old=${prefTestAdsValue?.text}, new=$newValue",
                            )
                            setModule(LogModules.SETTINGS)
                        }.run {
                            RemoteLog.d(requireContext(), build())
                        }
                }
            }
        }
        return true
    }

    companion object {
        const val TAG = "SettingsTestOptionsFragment"
    }
}
