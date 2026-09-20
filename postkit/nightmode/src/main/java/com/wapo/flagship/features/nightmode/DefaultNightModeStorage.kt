package com.wapo.flagship.features.nightmode

import android.content.Context
import android.preference.PreferenceManager

@Deprecated("Use PREF_IS_NIGHT_MODE_ENABLED")
const val PREF_ENABLE_NIGHT_MODE = "prefEnableNightMode"

const val PREF_IS_NIGHT_MODE_ENABLED = "prefIsNightModeEnabled"
const val PREF_NIGHT_MODE_DROP_DOWN = "prefNightModeDropDown"
private const val PREF_HAS_USER_CHANGED_NIGHT_MODE = "prefHasUserChangedNightMode"
private const val PREF_HAS_LEGACY_VERSION_SETTING_RESPECTED = "prefHasLegacyVersionSettingRespected"

internal class DefaultNightModeStorage(context: Context) : NightModeStorage {
    private val preferenceManager = PreferenceManager.getDefaultSharedPreferences(context)

    override fun setNightMode(status: Boolean) {
        preferenceManager
            .edit()
            .putBoolean(PREF_IS_NIGHT_MODE_ENABLED, status)
            .apply()
    }

    @Deprecated("Use isNightModeEnabled()")
    override fun readNightModeStatus(): Boolean {
        val status = preferenceManager.getBoolean(PREF_ENABLE_NIGHT_MODE, false)
        if (status) {
            preferenceManager
                    .edit()
                    .putBoolean(PREF_ENABLE_NIGHT_MODE, false)
                    .apply()
        }
        return status
    }

    override fun isNightModeEnabled(): Boolean {
        return preferenceManager.getBoolean(PREF_IS_NIGHT_MODE_ENABLED, false)
    }

    /**
     * Sets if user made an explicit action of enabling/disabling night mode.
     */
    override fun setUserExplicitlySelectedAMode() {
        preferenceManager
                .edit()
                .putBoolean(PREF_HAS_USER_CHANGED_NIGHT_MODE, true)
                .apply()
    }

    /**
     * Resets if user made a selection of "system setting" in night mode dropdown.
     */
    override fun resetUserExplicitlySelectedAMode() {
        preferenceManager
                .edit()
                .putBoolean(PREF_HAS_USER_CHANGED_NIGHT_MODE, false)
                .apply()
    }

    /**
     * Returns true if user explicitly enabled/disabled night mode, false otherwise.
     */
    override fun hasUserExplicitlySelectedAMode(): Boolean {
        return preferenceManager.getBoolean(PREF_HAS_USER_CHANGED_NIGHT_MODE, false)
    }

    /**
     * Sets if user's legacy version (prior to 4.45) has already been respected and need not be considered
     * again in the future.
     */
    override fun setLegacyVersionSettingRespected() {
        preferenceManager
                .edit()
                .putBoolean(PREF_HAS_LEGACY_VERSION_SETTING_RESPECTED, true)
                .apply()
    }

    /**
     * Returns true if the legacy version (prior to 4.45) setting has been respected in newer implementation.
     * False otherwise.
     */
    override fun hasLegacyVersionSettingRespected(): Boolean =
         preferenceManager.getBoolean(PREF_HAS_LEGACY_VERSION_SETTING_RESPECTED, false)

}