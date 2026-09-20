package com.wapo.flagship.push

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

/**
 * A dialog that informs the user that notifications are blocked for this app and provides a shortcut
 * to the system's notification settings.
 */
class NotificationsBlockedDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Build the alert dialog
        return AlertDialog.Builder(requireContext())
            .setTitle(
                resources.getString(
                    com.washingtonpost.android.notifications.R.string.notifications_blocked_title
                )
            )
            .setMessage(
                resources.getString(
                    com.washingtonpost.android.notifications.R.string.notifications_blocked_message
                )
            )
            .setNeutralButton(
                resources.getString(
                    com.washingtonpost.android.notifications.R.string.go_settings_message
                )
            ) { _, _ ->
                // When the "Settings" button is clicked, launch the system's app notification settings
                context?.let { launchSystemAppSettings(it) }
            }
            .setNegativeButton(resources.getString(com.washingtonpost.android.notifications.R.string.cancelLabel)) { dialog, _ ->
                // When the "Cancel" button is clicked, dismiss the dialog
                dialog.cancel()
            }
            .create()
    }

    /**
     * Launches the system's app notification settings for this app.
     */
    private fun launchSystemAppSettings(context: Context) {
        // For Android O (26) and above, use the modern ACTION_APP_NOTIFICATION_SETTINGS intent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } else {
            // For older versions, use the legacy intent
            context.startActivity(
                Intent()
                    .setClassName(
                        "com.android.settings",
                        "com.android.settings.Settings\$AppNotificationSettingsActivity",
                    ).putExtra("app_package", context.packageName)
                    .putExtra("app_uid", context.applicationInfo.uid)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS),
            )
        }
    }

    companion object {
        /**
         * The tag used to identify the [NotificationsBlockedDialog] in the [FragmentManager].
         */
        const val TAG = "NotificationsBlockedDialog"

        /**
         * Shows the [NotificationsBlockedDialog].
         *
         * @param fragmentManager The [FragmentManager] to use to show the dialog.
         */
        fun show(fragmentManager: FragmentManager) {
            // Only show the dialog if it's not already showing
            if (fragmentManager.findFragmentByTag(TAG) == null) {
                NotificationsBlockedDialog().show(fragmentManager, TAG)
            }
        }

        /**
         * Dismisses the [NotificationsBlockedDialog], if it is showing.
         *
         * @param fragmentManager The [FragmentManager] to use to dismiss the dialog.
         */
        fun dismiss(fragmentManager: FragmentManager) {
            // Find the dialog by its tag and dismiss it
            (fragmentManager.findFragmentByTag(TAG) as? NotificationsBlockedDialog)
                ?.dismissAllowingStateLoss()
        }
    }
}