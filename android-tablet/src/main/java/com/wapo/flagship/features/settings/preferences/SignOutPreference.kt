package com.wapo.flagship.features.settings.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.washingtonpost.android.R

class SignOutPreference : Preference {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        holder.findViewById(R.id.pref_sign_out)?.setOnClickListener {
            this.onPreferenceClickListener?.onPreferenceClick(this)
        }
    }
}