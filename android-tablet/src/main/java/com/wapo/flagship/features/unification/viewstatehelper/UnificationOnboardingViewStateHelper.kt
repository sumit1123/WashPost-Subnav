package com.wapo.flagship.features.unification.viewstatehelper

import android.content.Context
import androidx.core.text.HtmlCompat
import com.wapo.android.commons.util.setVisible
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.FragmentUnificationOnboardingBinding

/**
 * View state helper facilitates different states in the Unification Onboarding Fragment (for play store)
 */
class UnificationOnboardingViewStateHelper {
    /**
     * Called when migrated user has account transferred/logged in irrespective of their subscription status.
     */
    fun showGenericAccountScreen(
        accountInfo: String,
        binding: FragmentUnificationOnboardingBinding,
        context: Context?,
    ) {
        binding.tvSignIn.setVisible(true)
        binding.tvMessage.text =
            context
                ?.getString(R.string.unification_onboarding_logged_in_message, accountInfo)
                ?.let { HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_COMPACT) }
    }

    /**
     * Called when migrated user has both account and the subcription transferred.
     */
    fun showAccountWithSub(
        accountInfo: String,
        binding: FragmentUnificationOnboardingBinding,
        context: Context?,
    ) {
        /*
            For this state we're currently only showing the generic Account Present Screen.
         */
        showGenericAccountScreen(accountInfo, binding, context)
    }

    /**
     * Called when migrated user has account but no subcription transferred.
     */
    fun showAccountWithNoSub(
        accountInfo: String,
        binding: FragmentUnificationOnboardingBinding,
        context: Context?,
    ) {
        /*
            For this state we're currently only showing the generic Account Present Screen.
         */
        showGenericAccountScreen(accountInfo, binding, context)
    }

    /**
     * Called when migrated user has neither account nor subcription transferred.
     * Or we deliberately want to show the generic screen to the user cause we did not have the right info.
     */
    fun showGenericScreen(
        binding: FragmentUnificationOnboardingBinding,
        context: Context?,
    ) {
        binding.tvSignIn.setVisible(false)
        binding.tvMessage.text =
            context?.getString(
                R.string.unification_onboarding_no_sub_no_account_message,
            )
    }

    /**
     * Called when migrated user has transferred their subscription (happens in the background) but does not have an
     * account associated with it.
     */
    fun showNoAccountWithSub(
        binding: FragmentUnificationOnboardingBinding,
        context: Context?,
    ) {
        binding.tvSignIn.setVisible(false)
        binding.tvMessage.text =
            context
                ?.getString(R.string.unification_onboarding_subs_without_account_message)
                ?.let { HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_COMPACT) }
    }
}
