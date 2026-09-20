package com.wapo.flagship.features.settings.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.washingtonpost.android.paywall.models.BannerPaywallMessage
import com.wpds.components.ChevronView
import com.wpds.theme.AndroidClassicTheme
import com.washingtonpost.android.R

class SettingsTopBannerPreference : Preference {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    private var message: BannerPaywallMessage? = null
    private var onBindCallback: (() -> Unit)? = null
    private var onDetachedCallback: (() -> Unit)? = null

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val composeView = holder.findViewById(R.id.pref_upgrade_banner) as? ComposeView
        composeView?.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                message?.let { msg ->
                    AndroidClassicTheme {
                        ChevronView(
                            modifier = Modifier,
                            headlineText = msg.title.orEmpty(),
                            subtitle = msg.body.orEmpty(),
                            iconUrl = msg.imageUrl.orEmpty(),
                            darkIconUrl = msg.imageUrl.orEmpty(),
                            showButton = false,
                            buttonText = null,
                        ) {
                            onPreferenceClickListener?.onPreferenceClick(this@SettingsTopBannerPreference)
                        }
                    }
                }
            }
        }
        onBindCallback?.invoke()
    }

    override fun onDetached() {
        onDetachedCallback?.invoke()
        super.onDetached()
    }

    fun setMessage(message: BannerPaywallMessage?) {
        this.message = message
        notifyChanged()
    }

    fun setOnBindCallback(callback: () -> Unit) {
        this.onBindCallback = callback
    }

    fun setOnDetachedCallback(callback: () -> Unit) {
        this.onDetachedCallback = callback
    }
}

