// Copyright (c) 2022 The Washington Post. All rights reserved.

package com.wapo.flagship.features.onboarding2.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.wapo.android.commons.util.setVisible
import com.wapo.flagship.features.audio.viewmodels.AudioMediaActivityViewModel
import com.wapo.flagship.features.onboarding2.viewmodel.onboarding.OnboardingViewModel
import com.wapo.flagship.features.settings.SettingsAudioFragment
import com.wapo.flagship.util.PrefUtils
import com.washingtonpost.android.databinding.FragmentCustomizeAudioBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CustomizeAudioFragment : Fragment() {
    private var binding: FragmentCustomizeAudioBinding? = null

    /**
     * Viewmodel to access to activity level logic
     */
    private val onboardingViewModel: OnboardingViewModel by activityViewModels()

    private val audioMediaActivityViewModel: AudioMediaActivityViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentCustomizeAudioBinding.inflate(inflater, container, false)
        return binding?.root
    }

    /**
     * Initialize button events, live data observing and initial values
     */
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        PrefUtils.setShouldShowAudioOnboarding(context, false)
        val settingsAudioFragment: SettingsAudioFragment? =
            childFragmentManager.findFragmentByTag(
                "settings_audio_fragment",
            ) as? SettingsAudioFragment

        binding?.apply {
            continueBtn.setOnClickListener {
                settingsAudioFragment?.trackAudioPrefsSelection("onboarding")
                audioMediaActivityViewModel.stopMedia()
                onboardingViewModel.continueClicked()
            }
            backButton.setOnClickListener {
                onboardingViewModel.backClicked()
            }
        }

        setStepText()
        setBackShow()
    }

    /**
     * Set step text if more than one screen is shown in onboarding. Otherwise hide the text.
     */
    private fun setStepText() {
        val step = onboardingViewModel.getCurrentStep()
        val stepCount = onboardingViewModel.getStepSize()

        binding?.stepCount?.apply {
            setVisible(onboardingViewModel.shouldShowSteps())
            val stepText = "Step $step of $stepCount"
            text = stepText
        }
    }

    /**
     * Show back button if there are more than 1 screens.
     */
    private fun setBackShow() {
        binding?.backButton?.setVisible(onboardingViewModel.shouldShowBack())
    }

    override fun onDestroyView() {
        super.onDestroyView()

        binding = null
    }
}
