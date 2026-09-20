package com.wapo.flagship.features.articles2.viewholders

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.provider.Settings
import android.text.SpannableString
import android.text.style.StyleSpan
import androidx.core.app.NotificationManagerCompat
import com.wapo.android.commons.util.UiUtils.crossfadeViews
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.models.InlineAlertToggleItem
import com.wapo.flagship.features.notification.AlertsSettings
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.InlineAlertToggleBinding

class InlineAlertToggleViewHolder(
    private val binding: InlineAlertToggleBinding,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<InlineAlertToggleItem>(
        binding.root,
    ) {
    val context: Context = binding.root.context
    private lateinit var inlineAlertToggleItem: InlineAlertToggleItem
    private var notificationsBlockedDialog: AlertDialog? = null

    override fun bind(
        item: InlineAlertToggleItem,
        position: Int,
    ) {
        super.bind(item, position)
        inlineAlertToggleItem = item
        setText(item.topicDisplayName)
        binding.toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
            toggleStateChanged(item, isChecked)
        }
    }

    private fun toggleStateChanged(
        item: InlineAlertToggleItem,
        isEnabled: Boolean,
    ) {
        /* If trying to toggle the alert on while notifications are blocked at the system level,
         keep toggle off and show notifications blocked dialog. Otherwise, toggle the alert on or off. */
        if (isEnabled && !NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            binding.toggleSwitch.isChecked = false
            showNotificationsBlockedDialog()
        } else {
            item.topicKey?.let {
                FlagshipApplication.getInstance().alertsSettings.enableAlertsTopic(it, isEnabled)
                Measurement.trackAlertTopicEnroll(
                    item.topicDisplayName,
                    AlertsSettings.EntryPoint.INLINE_TOGGLE.trackingName,
                    isEnabled,
                )
                if (isEnabled) {
                    crossfadeViews(
                        arrayOf(binding.toggleEnabledText),
                        arrayOf(binding.toggleDisabledText),
                        400,
                    )
                } else {
                    crossfadeViews(
                        arrayOf(binding.toggleDisabledText),
                        arrayOf(binding.toggleEnabledText),
                        400,
                    )
                }
            }
        }
    }

    private fun showNotificationsBlockedDialog() {
        notificationsBlockedDialog?.dismiss()
        val alertDialog = AlertDialog.Builder(context).create()
        alertDialog.setTitle(
            context.resources.getString(
                com.washingtonpost.android.notifications.R.string.notifications_blocked_title,
            ),
        )
        alertDialog.setMessage(
            context.resources.getString(
                com.washingtonpost.android.notifications.R.string.notifications_blocked_message,
            ),
        )
        alertDialog.setButton(
            AlertDialog.BUTTON_NEUTRAL,
            context.resources.getString(
                com.washingtonpost.android.notifications.R.string.go_settings_message,
            ),
        ) { _, _ ->
            launchSystemAppSettings()
        }
        alertDialog.setButton(
            AlertDialog.BUTTON_NEGATIVE,
            context.resources.getString(
                com.washingtonpost.android.notifications.R.string.cancelLabel,
            ),
        ) { _, _ ->
            alertDialog.cancel()
        }

        notificationsBlockedDialog = alertDialog

        alertDialog.show()
    }

    private fun launchSystemAppSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
            )
        } else {
            context.startActivity(
                Intent()
                    .setClassName(
                        "com.android.settings",
                        "com.android.settings.Settings\$AppNotificationSettingsActivity",
                    ).putExtra("app_package", context.packageName)
                    .putExtra("app_uid", context.applicationInfo.uid)
                    .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS),
            )
        }
    }

    fun setText(displayName: String?) {
        val is7Briefing = displayName == THE_7_BRIEFING_DISPLAY_NAME
        val resources = context.resources

        var statusText = resources.getString(R.string.inline_alert_toggle_turn_on)
        var topicText =
            if (is7Briefing) {
                resources.getString(R.string.inline_alert_7_briefing_off)
            } else {
                String.format(resources.getString(R.string.inline_alert_topic), displayName)
            }
        var formattedText = SpannableString("$statusText $topicText")
        formattedText.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            formattedText.length,
            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        binding.toggleDisabledText.text = formattedText

        topicText =
            if (is7Briefing) {
                resources.getString(R.string.inline_alert_7_briefing_on)
            } else {
                String.format(resources.getString(R.string.inline_alert_topic), displayName)
            }
        statusText = resources.getString(R.string.inline_alert_toggle_are_now_on)
        formattedText = SpannableString("$topicText $statusText")
        formattedText.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            topicText.length,
            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        binding.toggleEnabledText.text = formattedText
    }

    companion object {
        private const val THE_7_BRIEFING_DISPLAY_NAME = "The 7 Briefing"
    }
}
