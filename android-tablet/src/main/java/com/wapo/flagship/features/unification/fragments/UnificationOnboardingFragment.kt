package com.wapo.flagship.features.unification.fragments

import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.wapo.android.commons.util.setClickSpan
import com.wapo.flagship.features.unification.models.AccountSubState
import com.wapo.flagship.features.unification.viewmodels.UnificationOnboardingViewModel
import com.wapo.flagship.features.unification.viewstatehelper.UnificationOnboardingViewStateHelper
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.FragmentUnificationOnboardingBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * This is the first (probably the only) screen in the unification onboarding flow.
 */
@AndroidEntryPoint
class UnificationOnboardingFragment : Fragment() {
    private var _binding: FragmentUnificationOnboardingBinding? = null
    private val binding get() = _binding!!

    private val unificationOnboardingViewModel: UnificationOnboardingViewModel by activityViewModels()

    /**
     * ViewStateHelper manages the various screen states to be shown for different account and subs combinations.
     */
    private var _viewStateHelper: UnificationOnboardingViewStateHelper? = null
    private val viewStateHelper get() = _viewStateHelper!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentUnificationOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val fullText = getString(R.string.sign_in_as_someone_else)
        val spannable = SpannableString(fullText)
        _viewStateHelper = UnificationOnboardingViewStateHelper()
        spannable.setClickSpan(
            fullText,
            fullText,
            R.color.unification_onboarding_text,
            requireContext(),
        ) {
            /**
             * Send these event to owner (activity) via view model so it is handled there.
             */
            unificationOnboardingViewModel.signInClicked()
        }
        binding.btnGetStarted.setOnClickListener {
            /**
             * Send these event to owner (activity) via view model so it is handled there.
             */
            unificationOnboardingViewModel.getStartedClicked()
        }
        binding.tvSignIn.text = spannable
        binding.tvSignIn.movementMethod = LinkMovementMethod.getInstance()
        observeAccountAndSubsState()
    }

    /**
     * Observe different account state/sub state events from the view model.
     */
    private fun observeAccountAndSubsState() {
        unificationOnboardingViewModel.accountSubState.observe(viewLifecycleOwner) { accountAndSubState ->
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
