package com.wapo.flagship.features.amazonunification.activity

import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.loader.content.Loader
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.IntentHelper
import com.wapo.flagship.Utils
import com.wapo.flagship.features.amazonunification.models.UserClickEvent
import com.wapo.flagship.features.amazonunification.viewmodels.UnificationAmazonOnboardingViewModel
import com.wapo.flagship.features.shared.activities.BaseActivity
import com.wapo.flagship.util.PrefUtils
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.ActivityUnificationAmazonOnboardingBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * This is a stand-alone activity that starts the unification onboarding flow.
 * Currently there's only one screen here.
 */
@AndroidEntryPoint
class UnificationAmazonOnboardingActivity :
    BaseActivity() {

    private lateinit var binding: ActivityUnificationAmazonOnboardingBinding

    private val unificationAmazonOnboardingViewModel: UnificationAmazonOnboardingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUnificationAmazonOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        unificationAmazonOnboardingViewModel.initializeSettingSubAndAccountState()
        observeUserEvents()
        observeErrorStates()
    }

    override fun onLoadFinished(
        loader: Loader<Cursor>,
        cursor: Cursor?,
    ) {
        super.onLoadFinished(loader, cursor)
        unificationAmazonOnboardingViewModel.initializeSettingSubAndAccountState()
    }

    /**
     * Observe any errors thrown by view model in the business logic so they can be caught in splunk.
     */
    private fun observeErrorStates() {
        unificationAmazonOnboardingViewModel.remoteLogger.observe(this) { errorMessage ->
            RemoteLog.e(
                applicationContext,
                EventLog
                    .Builder()
                    .setMessage(errorMessage)
                    .setModule(LogModules.ONBOARDING)
                    .build(),
            )
        }
    }

    /**
     * In this function we observe different user events on the onboarding screen(s)
     */
    private fun observeUserEvents() {
        unificationAmazonOnboardingViewModel.userClickEvent.observe(this) { userClickEvent ->
            when (userClickEvent) {
                /**
                 * Navigate to main activity on click of a CTA
                 */
                UserClickEvent.GetStartedClicked -> {
                    startMainActivity()
                }
                /**
                 * Start sign in flow if user chooses to use a different account.
                 */
                UserClickEvent.SignInClicked -> {
                    logUserOutAndNavigateToSignInScreen()
                }
                /**
                 * Open Learn more link article in web
                 */
                UserClickEvent.LearnMoreClicked -> {
                    Utils.startWeb(getString(R.string.unification_lean_more_link), this)
                }
            }
        }
    }

    /**
     * This sets the flag in shared prefs so user does not see this screen again.
     */
    private fun setScreenViewedAndCloseOut() {
        PrefUtils.setUserActedOnUnificationOnboarding(this, true)
        finish()
    }

    /**
     * Start sign in flow if user chooses to use a different account.
     */
    private fun logUserOutAndNavigateToSignInScreen() {
        unificationAmazonOnboardingViewModel.logOutUser {
            val link = getString(com.washingtonpost.android.R.string.uri_subs_signin)
            val intent =
                IntentHelper.getMainActivityIntent(this).apply {
                    this.data = Uri.parse(link)
                }
            startActivity(intent)
            setScreenViewedAndCloseOut()
        }
    }

    /**
     * Navigate to main activity on click of a CTA
     */
    private fun startMainActivity() {
        startActivity(IntentHelper.getMainActivityIntent(this))
        setScreenViewedAndCloseOut()
    }
}
