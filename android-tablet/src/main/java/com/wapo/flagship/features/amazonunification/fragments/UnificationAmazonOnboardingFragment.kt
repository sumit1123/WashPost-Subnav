package com.wapo.flagship.features.amazonunification.fragments

import android.content.Context
import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.wapo.android.commons.util.setClickSpan
import com.wapo.flagship.features.amazonunification.models.AccountSubState
import com.wapo.flagship.features.amazonunification.viewmodels.UnificationAmazonOnboardingViewModel
import com.wapo.flagship.features.amazonunification.viewstatehelper.UnificationAmazonOnboardingViewStateHelper
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.FragmentUnificationAmazonOnboardingBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * This is the first (probably the only) screen in the unification onboarding flow.
 */
@AndroidEntryPoint
class UnificationAmazonOnboardingFragment : Fragment() {
    private var _binding: FragmentUnificationAmazonOnboardingBinding? = null
    private val binding get() = _binding!!

    private val unificationAmazonOnboardingViewModel: UnificationAmazonOnboardingViewModel by activityViewModels()

    /**
     * ViewStateHelper manages the various screen states to be shown for different account and subs combinations.
     */
    private var _viewStateHelper: UnificationAmazonOnboardingViewStateHelper? = null
    private val viewStateHelper get() = _viewStateHelper!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentUnificationAmazonOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val signInFullText = getString(R.string.sign_in_as_someone_else)
        val signInSpannable = SpannableString(signInFullText)
        _viewStateHelper = UnificationAmazonOnboardingViewStateHelper()
        signInSpannable.setClickSpan(
            signInFullText,
            signInFullText,
            R.color.unification_onboarding_text,
            requireContext(),
        ) {
            /**
             * Send these event to owner (activity) via view model so it is handled there.
             */
            unificationAmazonOnboardingViewModel.signInClicked()
        }
        binding.btnGetStarted.setOnClickListener {
            /**
             * Send these event to owner (activity) via view model so it is handled there.
             */
            unificationAmazonOnboardingViewModel.getStartedClicked()
        }
        binding.tvWelcomeSubtitle.setOnClickListener {
            unificationAmazonOnboardingViewModel.learnMoreClicked()
        }
        binding.tvSignIn.text = signInSpannable
        binding.tvSignIn.movementMethod = LinkMovementMethod.getInstance()
        observeAccountAndSubsState()
        unificationAmazonOnboardingViewModel.initAccountProfile()
    }

    /**
     * Observe different account state/sub state events from the view model.
     */
    private fun observeAccountAndSubsState() {
        unificationAmazonOnboardingViewModel.accountSubState.observe(viewLifecycleOwner) { accountAndSubState ->
            when (accountAndSubState) {
                /**
                 * This is currently not used but if we ever get this state from ViewModel , we just use the generic screen to show
                 * when user has account irrespective of their subscription status
                 */
                is AccountSubState.AccountNoSub ->
                    viewStateHelper.showAccountWithNoSub(
                        accountAndSubState.accountInfo,
                        binding,
                        context,
                    )
                /**
                 * This is currently not used but if we ever get this state from ViewModel , we just use the generic screen to show
                 * when user has account irrespective of their subscription status
                 */
                is AccountSubState.AccountWithSub ->
                    viewStateHelper.showAccountWithSub(
                        accountAndSubState.accountInfo,
                        binding,
                        context,
                    )
                AccountSubState.GenericState ->
                    viewStateHelper.showGenericScreen(
                        binding,
                        context,
                    )
                AccountSubState.NoAccountWithSub ->
                    viewStateHelper.showNoAccountWithSub(
                        binding,
                        context,
                    )
                is AccountSubState.AccountPresent ->
                    viewStateHelper.showGenericAccountScreen(
                        accountAndSubState.accountInfo,
                        binding,
                        context,
                    )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        /**
         * Avoid any mem. leaks.
         */
        _binding = null
        _viewStateHelper = null
    }
}
