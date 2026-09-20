package com.wapo.flagship.features.amazonunification.models

/**
 * These are the different actions user can make on the unification onboarding screen.
 */
sealed class UserClickEvent {
    /**
     * The main CTA action button click
     */
    object GetStartedClicked : UserClickEvent()

    /**
     * Sign-in as a different user link click.
     */
    object SignInClicked : UserClickEvent()

    /**
     * Learn more link click.
     */
    object LearnMoreClicked : UserClickEvent()
}
