package com.wapo.flagship.features.onboarding

import android.content.Context

class OnboardingInitializerImpl(
    context: Context,
) : OnboardingInitializer {
    private var onboardingService = OnboardingService(context)

    override fun getOnboardingService(): OnboardingService = onboardingService

    override fun getOnboardingEventListener(): BaseOnboardingFragment.OnboardingEventListener? = null
}
