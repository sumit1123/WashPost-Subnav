package com.wapo.flagship.features.unification.models

/**
 * This is Account and subscription state when user migrated from rainbow app.
 */
sealed class AccountSubState {
    /**
     * When user has migrated both account and the subscription.
     * [accountInfo] - this is used to show on the onboarding screen in unification.
     * NOTE:
     * This is currently not used but if we ever get this state from ViewModel , we just use the generic screen to show
     * when user has account irrespective of their subscription status
     */
    data class AccountWithSub(
        val accountInfo: String,
    ) : AccountSubState()

    /**
     * When user has migrated their account but no subscription
     * [accountInfo] - this is used to show on the onboarding screen in unification.
     *  NOTE:
     * This is currently not used but if we ever get this state from ViewModel , we just use the generic screen to show
     * when user has account irrespective of their subscription status
     */
    data class AccountNoSub(
        val accountInfo: String,
    ) : AccountSubState()

    /**
     * When user has migrated their account regardless of their subscription status.
     * Keep in mind that this is the only state that's currenly being used when user has account
     * and [AccountNoSub] and [AccountWithSub] are added just in case we have those specific requirements in the future.
     * [accountInfo] - this is used to show on the onboarding screen in unification.
     */
    data class AccountPresent(
        val accountInfo: String,
    ) : AccountSubState()

    /**
     * When user has migrated but has not brought subscription or their account to classic app.
     * Probably skipping all those actions in rainbow.
     * This state shows a generic screen to user without any info.
     * No Account/ No sub case also shows this screen.
     */
    object GenericState : AccountSubState()

    /**
     * When user has just moved their subscription (This is done behind the scenes using content providers)
     * but has not moved their account (not linked)
     */
    object NoAccountWithSub : AccountSubState()
}
