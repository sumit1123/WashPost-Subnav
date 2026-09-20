package com.wapo.flagship.features.onboarding

interface OnboardingInitializer {
    fun getOnboardingService(): OnboardingService

    fun getOnboardingEventListener(): BaseOnboardingFragment.OnboardingEventListener?
}
