package com.washingtonpost.android.config.domain.models.config

import android.content.Context
import android.os.Parcelable
import androidx.annotation.Keep
import com.washingtonpost.android.config.R
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class PrivacyConsentConfig(
    val titleText: String,
    val bodyText: String,
    val accountText: String,
    val switchTextOn: String,
    val switchTextOff: String,
    val bottomText: String
) : Parcelable {
    companion object {
        @JvmStatic
        fun getConfig(context: Context, obj: Any?): PrivacyConsentConfig {
            val config = obj as? PrivacyConsentConfig
            return PrivacyConsentConfig(
                getText(config?.titleText) ?: context.getString(R.string.privacy_settings_header),
                getText(config?.bodyText) ?: context.getString(R.string.privacy_settings_body),
                getText(config?.accountText) ?: context.getString(R.string.privacy_settings_account),
                getText(config?.switchTextOn) ?: context.getString(R.string.privacy_settings_switch_text_enabled),
                getText(config?.switchTextOff) ?: context.getString(R.string.privacy_settings_switch_text_disabled),
                getText(config?.bottomText) ?: context.getString(R.string.privacy_settings_bottom_text)
            )
        }

        private fun getText(text: String?): String? {
            if (!text.isNullOrBlank()) return text
            return null
        }
    }
}