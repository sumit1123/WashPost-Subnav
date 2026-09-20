package com.wapo.flagship.features.settings.preferences

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.wapo.flagship.features.subscribebanner.utils.PauseUtil
import com.washingtonpost.android.R
import com.washingtonpost.android.paywall.PaywallService

class SubPauseBannerPreference : Preference {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        var primaryText = ""
        var secondaryText = ""
        var buttonText = ""
        holder.run {
            when {
                PaywallService.getInstance().isSubscriptionPaused -> {
                    primaryText = context.resources.getString(com.washingtonpost.android.sections.R.string.paused_primary_text)
                    secondaryText =
                        PauseUtil.buildPauseText(
                            context.resources.getString(com.washingtonpost.android.sections.R.string.paused_secondary_text),
                            context.resources.getString(com.washingtonpost.android.sections.R.string.paused_secondary_fallback_text),
                            PaywallService.getConnector().autoResumeTime,
                        )
                    buttonText = context.resources.getString(com.washingtonpost.android.sections.R.string.paused_button_text)
                }
                PaywallService.getInstance().isSubscriptionPauseScheduled -> {
                    primaryText =
                        PauseUtil.buildPauseText(
                            context.resources.getString(com.washingtonpost.android.sections.R.string.scheduled_pause_primary_text),
                            context.resources.getString(com.washingtonpost.android.sections.R.string.scheduled_pause_primary_fallback_text),
                            PaywallService.getConnector().pauseTime,
                        )
                    secondaryText =
                        context.resources.getString(
                            com.washingtonpost.android.sections.R.string.scheduled_pause_secondary_text,
                        )
                    buttonText = context.resources.getString(com.washingtonpost.android.sections.R.string.scheduled_pause_button_text)
                }
                else -> {}
            }
            (findViewById(R.id.primary_text) as AppCompatTextView).text = primaryText
            (findViewById(R.id.secondary_text) as AppCompatTextView).text = secondaryText
            val buttonView = findViewById(R.id.main_button) as AppCompatButton
            buttonView.text = buttonText
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                buttonView.stateListAnimator = null // hide drop shadow
            }
        }
    }
}
