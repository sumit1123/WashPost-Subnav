package com.wapo.flagship.features.onboarding2.models

/**
 * These are the different actions user can make on the unification onboarding screen.
 */
sealed class UserClickEvent: OnboardingEvent() {
    /**
     * The main CTA action button click
     */
    object Subscribe : UserClickEvent()

    /**
     * Sign-in as a different user link click.
     */
    object SignIn : UserClickEvent()

    /**
     * Create an account
     */
    object CreateAccount : UserClickEvent()

    /**
     * Dismiss flow
     */
    object Dismiss : UserClickEvent()

    /**
     * Continue to next page
     */
    object Continue : UserClickEvent()

    /**
     * Continue to next page
     */
    object Back : UserClickEvent()
}
