package com.wapo.flagship.features.settings.preferences

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.forEach
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.nightmode.NightModeStatus
import com.washingtonpost.android.R

class ThemePreference : Preference {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.let { h ->
            val summaryView = h.findViewById(android.R.id.summary) as TextView
            (h.findViewById(R.id.theme_button_toggle_group) as MaterialButtonToggleGroup).let { group ->
                group.forEach { button ->
                    button.setOnClickListener {
                        (button as MaterialButton).run {
                            onPreferenceChange(button.id)
                            updateSummary(button.id, summaryView)
                        }
                    }
                }
                bindNightModePreference(group)
                updateSummary(group.checkedButtonId, summaryView)
            }
        }
    }

    private fun updateSummary(
        viewId: Int,
        summaryView: TextView,
    ) {
        summaryView.apply {
            text =
                when (viewId) {
                    R.id.theme_system -> {
                        "Match display to Android System Settings"
                    }
                    else -> {
                        null
                    }
                }
            visibility = if (text.isNotEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun onPreferenceChange(activeButtonId: Int) {
        val nightModeManager = FlagshipApplication.getInstance().nightModeManager
        when (activeButtonId) {
            R.id.theme_light -> {
                nightModeManager.setUserExplicitlySelectedAMode()
                nightModeManager.setNightModeStatus(false)
            }
            R.id.theme_dark -> {
                nightModeManager.setUserExplicitlySelectedAMode()
                nightModeManager.setNightModeStatus(true)
            }
            R.id.theme_system -> {
                /*
                    When user selects "System setting", we need to make sure that we're not treating it as a users choice
                    for selecting night mode. We rely on system to enable/disable dark mode in the app.
                 */
                nightModeManager.resetUserExplicitlySelectedAMode()
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }

    /**
     * This function binds night-mode preference in app settings.
     * In this function, we're still using the older preference as a mediator to inform the status of night mode in the previous versions
     * of app (prior to 4.45).
     * block with @` {
     * if (nightMode instanceof TwoStatePreference && legacyVersionSettingShouldBeApplied)
     * }` can be safely removed
     * once we have enough adoption of version after 4.44. At that time we can just keep the code in else block and get rid of all of the
     * helper variables used in removed
     * @` if ` block
     */
    private fun bindNightModePreference(buttonToggleGroup: MaterialButtonToggleGroup) {
        val nightModeManager = FlagshipApplication.getInstance().nightModeManager

        /*
            We check first if legacy version setting is needed to be applied.
         */
        val legacyVersionSettingShouldBeApplied = !nightModeManager.hasLegacyVersionSettingRespected()
        /*
            SwitchPreferenceCompat and CheckBoxPreference are both of type TwoStatePreference.
         */
        if (legacyVersionSettingShouldBeApplied) {
            /*
                Check user setting in previous version of the app.
                We don't know yet, if this was enabled explicitly by user or it was system setting that turned on the night mode.
             */
            handleNightModeSettingAsPerLegacyVersion(buttonToggleGroup)
            nightModeManager.setLegacyVersionSettingRespected()
        } else if (buttonToggleGroup != null) {
            /*
                This indicates that this is a fresh install OR legacy setting has already been respected and applied.
             */
            val nightModeStatus =
                if (!nightModeManager.hasUserExplicitlySelectedAMode()) {
                    NightModeStatus.SYSTEM_SETTING
                } else if (nightModeManager.isNightModeEnabled()) {
                    NightModeStatus.DARK_MODE
                } else {
                    NightModeStatus.LIGHT_MODE
                }
            bindNightModeToggleGroup(buttonToggleGroup, nightModeStatus)
        }
    }

    /**
     * @param buttonToggleGroup - The preference for which this determination is required.
     * This function handles all 3 cases -
     * 1. User enabled explicitly.
     * 2. User disabled explicitly.
     * 3. User never interacted with night mode setting
     * in the previous version of the app.
     */
    private fun handleNightModeSettingAsPerLegacyVersion(buttonToggleGroup: MaterialButtonToggleGroup) {
        val nightModeManager = FlagshipApplication.getInstance().nightModeManager
        /*
            This could be by the system or user's explicit action.
         */
        val isEnabled = nightModeManager.isNightModeEnabled()
        /*
            This means user explicitly selected by switching ON/OFF night mode setting.
         */
        val hasUserSelectedAMode = nightModeManager.hasUserExplicitlySelectedAMode()
        /*
            This case essentially implies that either user has not explicitly selected night mode (which falls in else case) OR if they did, we respect their setting and apply night mode for them.
         */
        if (isEnabled && hasUserSelectedAMode) {
            bindNightModeToggleGroup(buttonToggleGroup, NightModeStatus.DARK_MODE)
        } else {
            /*
                If we ever fall in this case (hasUserSelectedAMode is true), it means the user had explicitly disabled the night mode from app settings.
                That's why we're respecting their choice here and applying LIGHT_MODE for them.
                Otherwise, we're just using system setting (in either case).
             */
            bindNightModeToggleGroup(
                buttonToggleGroup,
                if (hasUserSelectedAMode) NightModeStatus.LIGHT_MODE else NightModeStatus.SYSTEM_SETTING,
            )
        }
    }

    /**
     * @param buttonToggleGroup - preference that is used for binding night mode ToggleGroup buttons.
     * @param nightModeStatus - one of the following night mode statuses -
     * Enabled, Disabled, System Setting.
     * This function binds preference just like any other preference above + sets the value to the MaterialButtonToggleGroup depending on the status.
     * To understand how these values are passed, read documentation for setupSimplePreferencesScreen.
     */
    private fun bindNightModeToggleGroup(
        buttonToggleGroup: MaterialButtonToggleGroup,
        nightModeStatus: NightModeStatus,
    ) {
        when (nightModeStatus) {
            NightModeStatus.LIGHT_MODE -> {
                buttonToggleGroup.check(R.id.theme_light)
            }
            NightModeStatus.DARK_MODE -> {
                buttonToggleGroup.check(R.id.theme_dark)
            }
            NightModeStatus.SYSTEM_SETTING -> {
                buttonToggleGroup.check(R.id.theme_system)
            }
        }
    }
}
