package com.wapo.flagship.features.unification.models

/**
 * These are the different actions user can make on the unification onboarding screen.
 */
sealed class UserClickEvent {
    /**
     * The main CTA action buttin click
     */
    object GetStartedClicked : UserClickEvent()

    /**
     * Sign-in as a different user link click.
     */
    object SignInClicked : UserClickEvent()
}
