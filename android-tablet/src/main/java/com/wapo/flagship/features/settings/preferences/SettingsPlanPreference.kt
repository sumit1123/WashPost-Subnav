package com.wapo.flagship.features.settings.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.washingtonpost.android.R
import com.washingtonpost.android.paywall.models.BannerPaywallMessage

class SettingsPlanPreference : Preference {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    var settingsMessage: BannerPaywallMessage? = null
        set(value) {
            field = value
            updateView()
        }
    private var titleView: AppCompatTextView? = null
    private var subtitleView: AppCompatTextView? = null
    private var buttonView: AppCompatButton? = null
    private var onBindCallback: (() -> Unit)? = null
    private var onDetachedCallback: (() -> Unit)? = null

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        holder.run {
            titleView = findViewById(R.id.wp_weekly_title) as AppCompatTextView
            subtitleView = findViewById(R.id.wp_weekly_subtitle) as AppCompatTextView
            buttonView = findViewById(R.id.wp_weekly_button) as AppCompatButton

            updateView()
        }

        holder.findViewById(R.id.wp_weekly_button)?.setOnClickListener {
            onPreferenceClickListener?.onPreferenceClick(this)
        }
        onBindCallback?.invoke()
    }

    override fun onDetached() {
        onDetachedCallback?.invoke()
        super.onDetached()
    }

    private fun updateView() {
        titleView?.text = settingsMessage?.title
        subtitleView?.text = settingsMessage?.body
        buttonView?.text = settingsMessage?.action
    }

    fun setOnBindCallback(callback: () -> Unit) {
        this.onBindCallback = callback
    }

    fun setOnDetachedCallback(callback: () -> Unit) {
        this.onDetachedCallback = callback
    }
}