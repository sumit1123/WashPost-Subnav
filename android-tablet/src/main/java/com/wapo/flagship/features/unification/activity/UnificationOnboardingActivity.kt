package com.wapo.flagship.features.unification.activity

import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.loader.content.Loader
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.IntentHelper
import com.wapo.flagship.features.shared.activities.BaseActivity
import com.wapo.flagship.features.unification.models.UserClickEvent
import com.wapo.flagship.features.unification.viewmodels.UnificationOnboardingViewModel
import com.wapo.flagship.util.PrefUtils
import com.washingtonpost.android.databinding.ActivityUnificationOnboardingBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * This is a stand-alone activity that starts the unification onboarding flow.
 * Currently there's only one screen here.
 */
@AndroidEntryPoint
class UnificationOnboardingActivity :
    BaseActivity() {

    private lateinit var binding: ActivityUnificationOnboardingBinding

    private val unificationOnboardingViewModel: UnificationOnboardingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUnificationOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        unificationOnboardingViewModel.initializeSettingSubAndAccountState()
        observeUserEvents()
        observeErrorStates()
    }

    override fun onLoadFinished(
        loader: Loader<Cursor>,
        cursor: Cursor?,
    ) {
        super.onLoadFinished(loader, cursor)
        unificationOnboardingViewModel.initializeSettingSubAndAccountState()
    }

    /**
     * Observe any errors thrown by view model in the business logic so they can be caught in splunk.
     */
    private fun observeErrorStates() {
        unificationOnboardingViewModel.remoteLogger.observe(this) { errorMessage ->
            EventLog
                .Builder()
                .apply {
                    setMessage("Unification Onboarding Error")
                    setModule(LogModules.ONBOARDING)
                    setErrorMessage(errorMessage)
                }.run {
                    RemoteLog.e(this@UnificationOnboardingActivity.applicationContext, build())
                }
        }
    }

    /**
     * In this function we observe different user events on the onboarding screen(s)
     */
    private fun observeUserEvents() {
        unificationOnboardingViewModel.userClickEvent.observe(this) { userClickEvent ->
            when (userClickEvent) {
                /**
                 * Navigate to main activity on click of a CTA
                 */
                UserClickEvent.GetStartedClicked -> {
                    EventLog
                        .Builder()
                        .apply {
                            setMessage("Unification onboarding CTA tapped")
                            setModule(LogModules.ONBOARDING)
                        }.run {
                            RemoteLog.d(this@UnificationOnboardingActivity.applicationContext, build())
                        }
                    startMainActivity()
                }
                /**
                 * Start sign in flow if user chooses to use a different account.
                 */
                UserClickEvent.SignInClicked -> {
                    logUserOutAndNavigateToSignInScreen()
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
        unificationOnboardingViewModel.logOutUser {
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
