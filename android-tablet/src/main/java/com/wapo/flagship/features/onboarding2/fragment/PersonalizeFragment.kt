// Copyright (c) 2022 The Washington Post. All rights reserved.

package com.wapo.flagship.features.onboarding2.fragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.wapo.flagship.features.onboarding2.viewmodel.onboarding.OnboardingViewModel
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.databinding.FragmentPersonalizeBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PersonalizeFragment : Fragment() {
    private var binding: FragmentPersonalizeBinding? = null

    private val onboardingViewModel: OnboardingViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentPersonalizeBinding.inflate(inflater, container, false)
        return binding?.root
    }

    private fun removeButtonShadow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Remove shadow on button
            binding?.apply {
                getStarted.stateListAnimator = null
                maybeLater.stateListAnimator = null
            }
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        binding?.apply {
            getStarted.setOnClickListener {
                Measurement.trackOnboardingClick(Measurement.PROFILE_PERSONALIZE_START)
                onboardingViewModel.continueClicked()
            }
            maybeLater.setOnClickListener {
                Measurement.trackOnboardingClick(Measurement.PROFILE_PERSONALIZE_DISMISS)
                onboardingViewModel.dismissClicked()
            }
        }

        removeButtonShadow()
        Measurement.trackOnboardingSeen(Measurement.PROFILE_PERSONALIZE)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        binding = null
    }
}
