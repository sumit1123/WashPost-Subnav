package com.wapo.flagship.features.onboarding2.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.wapo.flagship.features.onboarding2.models.AccountSubState
import com.wapo.flagship.features.onboarding2.viewmodel.onboarding.OnboardingViewModel
import com.wapo.flagship.features.onboarding2.viewstatehelper.OnboardingViewStateHelper
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.databinding.FragmentOnboardingBinding
import com.washingtonpost.android.paywall.PaywallService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OnboardingFragment : DialogFragment() {
    private var binding: FragmentOnboardingBinding? = null

    private val onboardingViewModel: OnboardingViewModel by activityViewModels()

    /**
     * ViewStateHelper manages the various screen states to be shown for different account and subs combinations.
     */
    private var _viewStateHelper: OnboardingViewStateHelper? = null
    private val viewStateHelper get() = _viewStateHelper!!

    private var backgroundView: View? = null
    private var originalAccessibilityMode: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        Measurement.trackSaveOnboardingShown()
        _viewStateHelper = OnboardingViewStateHelper()
        binding?.maybeLater?.setOnClickListener {
            dismiss()
        }
        observeAccountAndSubsUiState()
        onboardingViewModel.initializeSettingSubAndAccountState()
    }

    override fun onResume() {
        super.onResume()
        val mainContent = (requireActivity().findViewById<ViewGroup>(android.R.id.content))?.getChildAt(0)
        if (mainContent != null) {
            backgroundView = mainContent
            originalAccessibilityMode = backgroundView?.importantForAccessibility
            backgroundView?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        }
    }

    override fun onPause() {
        super.onPause()
        originalAccessibilityMode?.let {
            backgroundView?.importantForAccessibility = it
        }
        backgroundView = null
        originalAccessibilityMode = null
    }

    /**
     * Observe different account state/sub state events from the view model.
     */
    private fun observeAccountAndSubsUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                onboardingViewModel.uiState.collect { state ->
                    val accountSubState = state.accountSubState
                    when (accountSubState) {
                        /**
                         * Update UI and click logic for Signed In User with No Sub
                         */
                        is AccountSubState.AccountNoSub ->
                            viewStateHelper.showAccountNoSub(
                                accountSubState.isTerminated,
                                binding,
                                // Primary CTA -> show paywall on subscribe button
                                {
                                    onboardingViewModel.subscribeClicked()
                                    dismiss()
                                },
                                // Secondary CTA -> show sign in screen
                                {
                                    onboardingViewModel.signInClicked()
                                    dismiss()
                                },
                            )
                        /**
                         * Update UI and click logic for Not Signed In with No Sub
                         */
                        is AccountSubState.NoAccountNoSub ->
                            viewStateHelper.showNoAccountNoSub(
                                accountSubState.isTerminated,
                                binding,
                                PaywallService.getInstance().onboardingReminderModel,
                                // Primary CTA -> show paywall on subscribe button
                                {
                                    onboardingViewModel.subscribeClicked()
                                    dismiss()
                                },
                                // Secondary CTA -> show sign in screen
                                {
                                    onboardingViewModel.signInClicked()
                                    dismiss()
                                },
                            )
                        /**
                         * Update UI and click logic for Not Signed In with Sub
                         */
                        AccountSubState.NoAccountWithSub ->
                            viewStateHelper.showNoAccountWithSub(
                                binding,
                                PaywallService.getInstance().onboardingReminderModel
                            ) {
                                // Primary CTA -> show sign in screen
                                onboardingViewModel.signInClicked()
                                dismiss()
                            }

                        is AccountSubState.AccountWithSub -> dismiss()
                        /**
                         * If user is LWA logged in, we hide the sign in button so that the LWA
                         * migration can happen in the background.
                         */
                        is AccountSubState.LwaLoggedIn ->
                            viewStateHelper.showLwaAccountNoSub(
                                accountSubState.isTerminated,
                                binding,
                                PaywallService.getInstance().onboardingReminderModel
                            ) {
                                onboardingViewModel.subscribeClicked()
                            }
                        else -> {}
                    }
                }
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        onboardingViewModel.dismissClicked()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        binding = null
        _viewStateHelper = null
        onboardingViewModel.setShown()
    }
}
