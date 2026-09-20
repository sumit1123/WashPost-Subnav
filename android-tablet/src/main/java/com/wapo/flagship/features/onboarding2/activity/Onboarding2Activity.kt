// Copyright (c) 2022 The Washington Post. All rights reserved.

package com.wapo.flagship.features.onboarding2.activity

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.wapo.flagship.features.onboarding2.models.UserClickEvent
import com.wapo.flagship.features.onboarding2.viewmodel.onboarding.OnboardingViewModel
import com.wapo.flagship.features.shared.activities.BaseActivity
import com.wapo.flagship.features.signin.LoginUtil.restorePreviousActivity
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.ActivityOnboarding2Binding
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.auth.AuthEntryPoint
import com.washingtonpost.android.paywall.auth.AuthIntentBuilder
import com.washingtonpost.android.paywall.util.PaywallConstants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class Onboarding2Activity :
    BaseActivity() {
    private lateinit var binding: ActivityOnboarding2Binding

    private val onboardingViewModel: OnboardingViewModel by viewModels()

    private val args by lazy { intent.extras }
    private val entryPoint by lazy {
        args?.getString(AuthIntentBuilder.ENTRY_POINT)
            ?.let {  raw ->
                runCatching { AuthEntryPoint.valueOf(raw) }.getOrNull()
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboarding2Binding.inflate(layoutInflater)
        observeOnboardingEvents()
        onboardingViewModel.initializeSettingSubAndAccountState()
        onboardingViewModel.loadPersonalizePages(entryPoint)
        setContentView(binding.root)
        setStartDestination()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    onboardingViewModel.isFirstPage() -> {
                        // Do nothing
                    }
                    !onboardingViewModel.shouldShowBack() -> {
                        finish()
                    }
                    else -> onboardingViewModel.backClicked()
                }
            }
        })
    }

    private fun setStartDestination() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val graph = navController.navInflater.inflate(R.navigation.nav_onboarding)
        graph.setStartDestination(
            when (entryPoint) {
                AuthEntryPoint.MY_POST -> R.id.alertsFragment
                else -> R.id.personalizeFragment
            }
        )
        navController.setGraph(graph, intent.extras)
    }

    /**
     * Observer high level click events from user
     */
    private fun observeOnboardingEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                onboardingViewModel.onboardingEvent.collect { event ->
                    when (event) {
                        UserClickEvent.Subscribe -> {
                            showWallDialog(
                                onboardingViewModel.isSignedIn,
                                PaywallConstants.ONBOARDING,
                                PaywallConstants.WallType.ONBOARDING_PAYWALL,
                            )
                        }
                        UserClickEvent.SignIn -> {
                            onboardingViewModel.logOutUser {
                                PaywallService.getConnector().showSignInScreen(
                                    supportFragmentManager,
                                    AuthIntentBuilder().build(),
                                    null,
                                    PaywallConstants.WallType.ONBOARDING_PAYWALL,
                                    false,
                                    null
                                )
                            }
                        }
                        UserClickEvent.CreateAccount -> {
                            PaywallService.getConnector().showSignUpScreen(
                                supportFragmentManager,
                                AuthIntentBuilder().build(),
                                null,
                                PaywallConstants.WallType.ONBOARDING_PAYWALL,
                            )
                        }
                        UserClickEvent.Dismiss -> {
                            finish()
                        }
                        UserClickEvent.Continue -> {
                            onboardingViewModel.nextPage()
                            onboardingViewModel.getCurrentPageId()?.let { pageId ->
                                findNavController(R.id.nav_host_fragment).navigate(pageId)
                                onboardingViewModel.trackCurrentScreen()
                            }
                        }
                        UserClickEvent.Back -> {
                            onboardingViewModel.previousPage()
                            findNavController(R.id.nav_host_fragment).popBackStack()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        restorePreviousActivity {
            startActivity(it)
        }
    }
}
