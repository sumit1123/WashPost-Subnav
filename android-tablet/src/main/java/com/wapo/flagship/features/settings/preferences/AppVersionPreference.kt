package com.wapo.flagship.features.settings.preferences

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.wapo.android.commons.util.DeviceUtils
import com.wapo.android.commons.util.Utils.getAppVersionCode
import com.wapo.android.commons.util.Utils.getAppVersionName
import com.wapo.flagship.features.settings.SettingsViewModel
import com.washingtonpost.android.BuildConfig
import com.washingtonpost.android.R
import java.util.*

class AppVersionPreference : Preference {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    private var settingsViewModel: SettingsViewModel? = null

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.run {
            (findViewById(R.id.pref_app_version) as TextView).apply {
                val appVersionCode = if (BuildConfig.DEBUG) " (${getAppVersionCode(context)})" else ""
                text =
                    String.format(
                        Locale.US,
                        "App Version %s%s",
                        getAppVersionName(context),
                        appVersionCode,
                    )
                setOnClickListener {
                    settingsViewModel?.handleAppVersionClick()
                }
            }

            (findViewById(R.id.pref_support_id) as TextView).apply {
                val prefix = "Support ID"
                text =
                    String.format(
                        Locale.US,
                        "%s %s",
                        prefix,
                        DeviceUtils.getUniqueDeviceId(context),
                    )
                setOnClickListener {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip: ClipData = ClipData.newPlainText(prefix, text.toString())
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun setViewModel(settingsViewModel: SettingsViewModel) {
        this.settingsViewModel = settingsViewModel
    }
}
