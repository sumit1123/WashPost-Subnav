package com.wapo.flagship.features.settings

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceFragmentCompat
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.auth.AuthEntryPoint
import com.washingtonpost.android.paywall.auth.AuthIntentBuilder
import com.washingtonpost.android.paywall.newdata.model.WpUser
import com.washingtonpost.android.paywall.util.PaywallConstants

abstract class BasePreferenceFragmentCompat : PreferenceFragmentCompat() {
    protected val settingsViewModel: SettingsViewModel by viewModels()

    abstract override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    )

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        activity?.title = preferenceScreen?.title
    }

    override fun onResume() {
        super.onResume()
        settingsViewModel.updateStates()
    }

    open fun refreshSettings() {
        // Sub classes can implement to refresh Account related settings.
    }

    protected fun openPlayStore() {
        context?.apply {
            PaywallService.getConnector().openPlaystore(activity)
        }
    }

    protected fun openAmazonStore() {
        context?.apply {
            val intent =
                Intent(
                    "android.intent.action.VIEW",
                    Uri.parse("amzn://apps/library/subscriptions"),
                )
            this.startActivity(intent)
        }
    }

    protected fun startSignIn() {
        settingsViewModel.startSignInProcess()
        PaywallService.getConnector().showSignInScreen(
            activity?.supportFragmentManager,
            AuthIntentBuilder().addEntryPoint(AuthEntryPoint.MY_POST).build(),
            null,
            PaywallConstants.WallType.SETTINGS_PAYWALL,
            false,
            null
        )
    }

    protected fun startSignOut(user: WpUser) {
        var confirmationText = "Are you sure you want to log out"
        if (!TextUtils.isEmpty(user.userId)) {
            confirmationText += " as " + user.userId
        }
        confirmationText += "?"
        val confirmation = AlertDialog.Builder(activity)
        confirmation.setMessage(confirmationText)
        confirmation.setCancelable(false)
        confirmation.setPositiveButton("yes") { dialog, id ->
            settingsViewModel.startSignOutProcess()
        }
        confirmation.setNegativeButton("no") { dialog, id ->
            settingsViewModel.updateStates()
        }
        val alert = confirmation.create()
        alert.show()
    }

    protected fun showPaywallDialog() {
        val activity = activity as SettingsActivity?
        if (activity != null && !activity.isFinishing) {
            activity.showWallDialog(
                settingsViewModel.isUserSignedIn(),
                PaywallConstants.METERED,
                PaywallConstants.WallType.SETTINGS_PAYWALL,
            )
        }
    }
}
