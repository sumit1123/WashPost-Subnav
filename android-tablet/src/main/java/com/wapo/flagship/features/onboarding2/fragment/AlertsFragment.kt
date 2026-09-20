// Copyright (c) 2022 The Washington Post. All rights reserved.

package com.wapo.flagship.features.onboarding2.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.wapo.android.commons.util.setVisible
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.onboarding2.viewmodel.onboarding.OnboardingViewModel
import com.wapo.flagship.features.settings.SettingsAlertsFragment
import com.wapo.flagship.push.PushPreferencesHelper
import com.wapo.flagship.util.PrefUtils
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.FragmentAlertsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AlertsFragment : Fragment() {
    private var binding: FragmentAlertsBinding? = null

    private val onboardingViewModel: OnboardingViewModel by activityViewModels()
    private val alertsSettings by lazy { FlagshipApplication.getInstance().alertsSettings }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentAlertsBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        PrefUtils.setShouldShowAlertsOnboarding(context, false)
        val settingsAlertsFragment: SettingsAlertsFragment? =
            childFragmentManager.findFragmentByTag(
                "settings_alerts_fragment",
            ) as? SettingsAlertsFragment
        settingsAlertsFragment?.onSubscribed = {
            onboardingViewModel.setAlertsContinueActive(true)
            binding?.continueBtn?.isEnabled = true
        }

        binding?.apply {
            val hasPendingUpdates = alertsSettings.getAlertsTopicsList().any { it.isEnabled } && !PushPreferencesHelper.areNotificationsEnabled(context)
            continueBtn.isEnabled = onboardingViewModel.getAlertsContinueActive() || hasPendingUpdates

            val hasMultipleSteps = onboardingViewModel.getStepSize() > 1
            if (hasMultipleSteps) {
                continueBtn.text = resources.getString(R.string.onboarding_continue_btn_text)
                chooseLater.text = resources.getString(R.string.choose_later)
            } else {
                continueBtn.text = resources.getString(R.string.save)
                chooseLater.text = resources.getString(R.string.not_now)
            }

            continueBtn.setOnClickListener {
                val context = this@AlertsFragment.context
                if (!PushPreferencesHelper.areNotificationsEnabled() && context != null) {
                    PushPreferencesHelper.showNotificationsBlockedDialog(parentFragmentManager)
                    return@setOnClickListener
                }
                onboardingViewModel.continueClicked()
            }
            chooseLater.setOnClickListener {
                Measurement.trackOnboardingClick(Measurement.PROFILE_PREFERENCE_ALERTS_DISMISS)
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
