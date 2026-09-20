package com.wapo.flagship.features.onboarding

interface OnboardingProvider {
    fun getOnboardingInitializer(): OnboardingInitializer
}
