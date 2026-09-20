package com.wapo.flagship.features.settings.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.wapo.flagship.features.settings.SettingsViewModel
import com.washingtonpost.android.R

class MobileFreeTrialBannerPreference : Preference {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    private var settingsViewModel: SettingsViewModel? = null

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.run {
            settingsViewModel?.let {
                val freeTrialEndDate = it.getMobileFreeTrialFormatted()
                (findViewById(R.id.primary_text) as AppCompatTextView).text =
                    context.resources.getString(
                        com.washingtonpost.android.sections.R.string.mobile_free_trial_end_primary_text_settings,
                    )
                (findViewById(R.id.secondary_text) as AppCompatTextView).text =
                    if (freeTrialEndDate ==
                        null
                    ) {
                        "Subscribe to never miss a story."
                    } else {
                        "Subscribe by $freeTrialEndDate to never miss a story."
                    }
            }
        }
    }

    fun setViewModel(settingsViewModel: SettingsViewModel) {
        this.settingsViewModel = settingsViewModel
    }
}
