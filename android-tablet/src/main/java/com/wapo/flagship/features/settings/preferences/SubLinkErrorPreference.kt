package com.wapo.flagship.features.settings.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.washingtonpost.android.R
import com.washingtonpost.android.paywall.PaywallService

class SubLinkErrorPreference : Preference {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.run {
            (findViewById(R.id.message_body) as AppCompatTextView).text =
                PaywallService.getConnector()?.subscriptionLinkMessage
                    ?: context.resources.getString(com.washingtonpost.android.paywall.R.string.sub_link_error_body_default)
        }
    }
}
