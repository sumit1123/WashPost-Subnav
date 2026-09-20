/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.agerestriction

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Surface
import androidx.core.content.IntentCompat
import com.washingtonpost.android.R
import com.wpds.theme.AndroidClassicTheme

class AgeRestrictionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {}
        })

        val screenMode = IntentCompat.getParcelableExtra(intent, AGE_RESTRICTIONS_SCREEN_MODE, AgeRestrictionsScreenMode::class.java) ?: AgeRestrictionsScreenMode.UnknownBlockAgeRestriction

        val uiModel = when (screenMode) {
            AgeRestrictionsScreenMode.BlockAgeRestriction -> {
                AgeRestrictionsUIModel(
                    title = resources.getString(R.string.age_restriction_restricted_title),
                    message = resources.getString(R.string.age_restriction_restricted_message),
                    buttonText = resources.getString(R.string.age_restriction_restricted_button_text)
                )
            }
            AgeRestrictionsScreenMode.ParentPermissionsBlockRestriction -> {
                AgeRestrictionsUIModel(
                    title = resources.getString(R.string.age_restriction_parent_permission_title),
                    message = resources.getString(R.string.age_restriction_parent_permission_message),
                    buttonText = resources.getString(R.string.age_restriction_parent_permission_button_text),
                )
            }
            AgeRestrictionsScreenMode.UnknownBlockAgeRestriction -> {
                AgeRestrictionsUIModel(
                    title = resources.getString(R.string.age_restriction_error_title),
                    message = resources.getString(R.string.age_restriction_error_message),
                    buttonText = resources.getString(R.string.age_restriction_restricted_button_text)
                )
            }
            AgeRestrictionsScreenMode.VisitPlayStoreBlockRestriction -> {
                AgeRestrictionsUIModel(
                    title = resources.getString(R.string.age_restriction_error_play_store_title),
                    message = resources.getString(R.string.age_restriction_error_play_store_message),
                    buttonText = resources.getString(R.string.age_restriction_restricted_visit_playstore_button_text)
                )
            }
        }

        setContent {
            AndroidClassicTheme {
                Surface {
                    AgeRestrictionUi(uiModel) {
                        if (screenMode is AgeRestrictionsScreenMode.VisitPlayStoreBlockRestriction) {
                            openPlayStoreProfileOrSettings(this)
                        } else {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(resources.getString(R.string.age_restriction_learn_more_link))
                            )
                            this@AgeRestrictionActivity.startActivity(intent)
                        }
                    }
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                setResult(Activity.RESULT_OK)
                finish()
            }
        })
    }

    fun openPlayStoreProfileOrSettings(context: Context) {
        val playStorePackage = "com.android.vending"

        if (isPackageInstalledAndEnabled(context, playStorePackage)) {
            try {
                // Direct intent to Play Store's Profile / Account Settings activity
                val profileIntent = Intent().apply {
                    component = ComponentName(
                        playStorePackage,
                        "com.google.android.finsky.setupwizard.AccountListActivity"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(profileIntent)
            } catch (e: Exception) {
                // Fallback: Open Play Store main app interface
                val mainIntent = context.packageManager.getLaunchIntentForPackage(playStorePackage)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (mainIntent != null) {
                    context.startActivity(mainIntent)
                } else {
                    openPlayStoreInBrowser(context)
                }
            }
        } else {
            // Play Store disabled -> Open System App Info Settings for Play Store
            try {
                val settingsIntent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$playStorePackage")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
            } catch (e: Exception) {
                openPlayStoreInBrowser(context)
            }
        }
    }

    private fun isPackageInstalledAndEnabled(context: Context, packageName: String): Boolean {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            appInfo.enabled
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun openPlayStoreInBrowser(context: Context) {
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/account")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(webIntent)
    }

    companion object {
        private const val AGE_RESTRICTIONS_SCREEN_MODE = "AGE_RESTRICTIONS_SCREEN_MODE"

        fun getAgeRestrictionActivityIntent(context: Context, ageRestrictionsScreenMode: AgeRestrictionsScreenMode): Intent {
            return Intent(context, AgeRestrictionActivity::class.java).apply {
                putExtra(AGE_RESTRICTIONS_SCREEN_MODE, ageRestrictionsScreenMode)
            }
        }
    }
}
