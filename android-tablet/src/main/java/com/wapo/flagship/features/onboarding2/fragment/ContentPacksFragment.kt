// Copyright (c) 2022 The Washington Post. All rights reserved.

package com.wapo.flagship.features.onboarding2.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.wapo.android.commons.util.setVisible
import com.wapo.flagship.features.onboarding2.viewmodel.onboarding.OnboardingViewModel
import com.wapo.flagship.features.settings.ContentPacksUiState
import com.wapo.flagship.features.settings.SettingsContentPacksFragment
import com.wapo.flagship.util.PrefUtils
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.databinding.FragmentContentPacksBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ContentPacksFragment : Fragment() {
    private var binding: FragmentContentPacksBinding? = null

    /**
     * Viewmodel to access to activity level logic
     */
    private val onboardingViewModel: OnboardingViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentContentPacksBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        PrefUtils.setShouldShowContentPacksOnboarding(context, false)
        val settingsContentPacksFragment: SettingsContentPacksFragment? =
            childFragmentManager.findFragmentByTag(
                "settings_content_packs_fragment",
            ) as? SettingsContentPacksFragment

        settingsContentPacksFragment?.done = {
            onboardingViewModel.setContentPacksContinueActive(true)
            binding?.continueBtn?.isEnabled = true
        }

        settingsContentPacksFragment?.uiStateChanged = {
            if (it is ContentPacksUiState.Success) {
                binding?.fragmentContainerView?.layoutParams?.height = ViewGroup.LayoutParams.WRAP_CONTENT
            } else {
                binding?.fragmentContainerView?.layoutParams?.height = 0
            }
        }

        binding?.apply {
            continueBtn.isEnabled = onboardingViewModel.getContentPacksContinueActive()
            continueBtn.setOnClickListener {
                settingsContentPacksFragment?.submitContentPacks()
                onboardingViewModel.continueClicked()
            }
            chooseLater.setOnClickListener {
                Measurement.trackOnboardingClick(Measurement.PROFILE_PREFERENCE_CONTENT_PACK_DISMISS)
                onboardingViewModel.continueClicked()
            }
            backButton.setOnClickListener {
                onboardingViewModel.backClicked()
            }
        }

        setStepText()
        setBackShow()
    }

    private fun setStepText() {
        val step = onboardingViewModel.getCurrentStep()
        val stepCount = onboardingViewModel.getStepSize()

        binding?.stepCount?.apply {
            setVisible(onboardingViewModel.shouldShowSteps())
            val stepText = "Step $step of $stepCount"
            text = stepText
        }
    }

    private fun setBackShow() {
        binding?.backButton?.setVisible(onboardingViewModel.shouldShowBack())
    }

    override fun onDestroyView() {
        super.onDestroyView()

        binding = null
    }
}
