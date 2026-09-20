package com.wapo.flagship.features.onboarding2.models

/**
 * This is Account and subscription state when user migrated from rainbow app.
 */
sealed class AccountSubState {
    /**
     * User is Signed In and has an Active Sub
     */
    object AccountWithSub : AccountSubState()

    /**
     * User is Signed in but has no Active Sub
     * [isTerminated] - send whether user is a new subscriber or terminated
     */
    data class AccountNoSub(
        val isTerminated: Boolean,
    ) : AccountSubState()

    /**
     * User is Signed in
     * [isTerminated] - send whether user is a new subscriber or terminated
     */
    data class NoAccountNoSub(
        val isTerminated: Boolean,
    ) : AccountSubState()

    /**
     * User is Not Signed In but does have an IAP sub
     */
    object NoAccountWithSub : AccountSubState()

    /**
     * Users who are LWA logged in. We are hiding the sign in button in this case.
     */
    class LwaLoggedIn(
        val isTerminated: Boolean,
    ) : AccountSubState()
}
