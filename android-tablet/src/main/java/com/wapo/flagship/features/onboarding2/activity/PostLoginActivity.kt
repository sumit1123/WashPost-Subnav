// Copyright (c) 2022 The Washington Post. All rights reserved.

package com.wapo.flagship.features.onboarding2.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.wapo.flagship.features.onboarding2.viewmodel.PostLoginViewModel
import com.wapo.flagship.features.preferencesapi.GetUserContentPacksApiStatus
import com.wapo.flagship.features.signin.LoginUtil.restorePreviousActivity
import com.wapo.flagship.util.PrefUtils
import com.washingtonpost.android.R
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity to determine which screens to shown. This activity will show a progress dialog
 * until next onboarding is determined.
 */
@AndroidEntryPoint
class PostLoginActivity :
    AppCompatActivity() {

    private val postLoginViewModel: PostLoginViewModel by viewModels()

    private var progressDialog: AlertDialog? = null

    /**
     * Create and start progress dialog and call
     * fun to check which screens need to be shown
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progressDialog =
            AlertDialog
                .Builder(this)
                .setView(R.layout.loading_dialog)
                .setOnCancelListener {
                    finish()
                }.create()

        progressDialog?.setCancelable(true)
        observeScreensChecked()
        observeGetUserContentPacks()
        postLoginViewModel.updateShowAlertsPref(this)
        postLoginViewModel.updateShowAudioPref(this)
        postLoginViewModel.updateShowContentPacks()
        postLoginViewModel.updateNewsprint()
        postLoginViewModel.syncNewsletters()
        progressDialog?.show()
    }

    /**
     * Dismiss dialog when activity has finished
     */
    override fun onDestroy() {
        super.onDestroy()
        progressDialog?.dismiss()
    }

    /**
     * Observe response of call to get user's content packs.
     * - If call failure, do not change shouldShowContentPacks pref from its current value.
     * - If response has content packs, store them and set shouldShowContentPacks pref to false.
     *      We do not show content packs screen to users who have content packs selected.
     * - If response's content packs are null, user should be shown the content packs screen.
     *      Null means user has never made a content pack selection before.
     * - If response's content packs are empty, user should NOT be shown the content packs screen.
     *      Empty means user has seen the screen before and selected none.
     */
    private fun observeGetUserContentPacks() {
        postLoginViewModel.userContentPacksPrefsStatus.observe(this) {
            when (it) {
                is GetUserContentPacksApiStatus.Failure -> postLoginViewModel.updateScreensChecked()
                is GetUserContentPacksApiStatus.Success -> {
                    PrefUtils.setSelectedContentPacks(this, it.contentPacks)
                    PrefUtils.setShouldShowContentPacksOnboarding(this, false)
                    postLoginViewModel.updateScreensChecked()
                }
                GetUserContentPacksApiStatus.NewUser -> {
                    PrefUtils.setSelectedContentPacks(this, listOf())
                    PrefUtils.setShouldShowContentPacksOnboarding(this, true)
                    postLoginViewModel.updateScreensChecked()
                }
                GetUserContentPacksApiStatus.NoUserContentPacks -> {
                    PrefUtils.setSelectedContentPacks(this, listOf())
                    PrefUtils.setShouldShowContentPacksOnboarding(this, false)
                    postLoginViewModel.updateScreensChecked()
                }
            }
        }
    }

    /**
     * Observe if screens to show has been determined.
     * - If no onboarding screens are to be shown, show successful login toast
     * - If there is 1 or more onboarding screens, start Onboarding2Activity
     */
    private fun observeScreensChecked() {
        postLoginViewModel.allChecksCompleted.observe(this) {
            if (postLoginViewModel.shouldShowOnboarding(this)) {
                startOnboardingSubscriber(this)
            } else {
                restorePreviousActivity {
                    startActivity(it)
                }
                Toast.makeText(this, "You have successfully logged in", Toast.LENGTH_SHORT).show()
            }

            finish()
        }
    }

    /**
     * Start Onboarding2Activity
     */
    private fun startOnboardingSubscriber(context: Context) {
        val sourceExtras = this@PostLoginActivity.intent.extras
        val onboardingIntent = Intent(context, Onboarding2Activity::class.java).apply {
            sourceExtras?.let { putExtras(it) }
        }
        context.startActivity(onboardingIntent)
    }
}
