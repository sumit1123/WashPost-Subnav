// Copyright (c) 2022 The Washington Post. All rights reserved.

package com.wapo.flagship.features.onboarding2.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.wapo.flagship.features.onboarding2.models.AccountSubState
import com.wapo.flagship.features.onboarding2.viewmodel.onboarding.OnboardingViewModel
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.FragmentWelcomeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WelcomeFragment : Fragment() {
    private var binding: FragmentWelcomeBinding? = null

    private val onboardingViewModel: OnboardingViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        binding?.apply {
            createAccount.setOnClickListener {
                onboardingViewModel.createAccountClicked()
            }
            noThanks.setOnClickListener {
                onboardingViewModel.dismissClicked()
            }
            signIn.setOnClickListener {
                onboardingViewModel.signInClicked()
            }
        }

        observerAccountSubState()
    }

    private fun observerAccountSubState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                onboardingViewModel.uiState.collect { event ->
                    when (event.accountSubState) {
                        is AccountSubState.AccountNoSub,
                        is AccountSubState.AccountWithSub -> {
                            findNavController().navigate(R.id.action_welcomeFragment_to_personalizeFragment)
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        binding = null
    }
}
