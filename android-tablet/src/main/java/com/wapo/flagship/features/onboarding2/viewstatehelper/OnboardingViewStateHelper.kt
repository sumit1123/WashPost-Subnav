package com.wapo.flagship.features.onboarding2.viewstatehelper

import android.text.SpannableString
import android.text.method.LinkMovementMethod
import com.wapo.android.commons.util.setClickSpan
import com.wapo.android.commons.util.setVisible
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.FragmentOnboardingBinding
import com.washingtonpost.android.config.domain.models.config.paywall.OnboardingReminderModel

class OnboardingViewStateHelper {
    /**
     * Set primary cta to subscribe and secondary to sign in
     */
    fun showNoAccountNoSub(
        isTerminated: Boolean,
        binding: FragmentOnboardingBinding?,
        onboardingReminderModel: OnboardingReminderModel,
        onPrimaryClick: () -> Unit,
        onSecondaryClick: () -> Unit,
    ) {
        binding?.apply {
            setHeader(binding, onboardingReminderModel, isTerminated)
            primaryCta.text = if (isTerminated) "Resubscribe" else "Subscribe"
            primaryCta.setOnClickListener { onPrimaryClick() }

            val context = root.context
            val commonText = context.getString(R.string.already_subscriber)
            val linkText = context.getString(R.string.sign_in)
            val fullText = "$commonText $linkText"
            val signInSpannable = SpannableString(fullText)

            signInSpannable.setClickSpan(
                fullText,
                linkText,
                R.color.unification_onboarding_text,
                context,
                onSecondaryClick,
            )

            secondaryCta.text = signInSpannable
            secondaryCta.movementMethod = LinkMovementMethod.getInstance()

            secondaryCta.setVisible(true)
        }
    }

    /**
     * Set primary cta to subscribe and secondary to sign in as someone else
     */
    fun showAccountNoSub(
        isTerminated: Boolean,
        binding: FragmentOnboardingBinding?,
        onPrimaryClick: () -> Unit,
        onSecondaryClick: () -> Unit,
    ) {
        binding?.apply {
            primaryCta.text = if (isTerminated) "Resubscribe" else "Subscribe"
            primaryCta.setOnClickListener { onPrimaryClick() }

            val context = root.context
            val commonText = context.getString(R.string.already_subscriber)
            val linkText = context.getString(R.string.sign_in_as_someone_else)
            val fullText = "$commonText\n$linkText"
            val signInSpannable = SpannableString(fullText)

            signInSpannable.setClickSpan(
                fullText,
                linkText,
                R.color.unification_onboarding_text,
                context,
                onSecondaryClick,
            )

            secondaryCta.text = signInSpannable
            secondaryCta.movementMethod = LinkMovementMethod.getInstance()

            secondaryCta.setVisible(true)
        }
    }

    /**
     * Set primary cta to Sign In. Hide secondary cta
     */
    fun showNoAccountWithSub(
        binding: FragmentOnboardingBinding?,
        onboardingReminderModel: OnboardingReminderModel,
        onPrimaryClick: () -> Unit,
    ) {
        binding?.apply {
            header.text =
                onboardingReminderModel.newSubscriber.heading
            message.text =
                onboardingReminderModel.newSubscriber.message

            val signInText = root.context.getString(R.string.sign_in)
            primaryCta.text = signInText

            primaryCta.setOnClickListener { onPrimaryClick() }
            secondaryCta.setVisible(false)
        }
    }

    /**
     * Set primary cta to subscribe and hide secondary CTA for LWA signed in users.
     */
    fun showLwaAccountNoSub(
        isTerminated: Boolean,
        binding: FragmentOnboardingBinding?,
        onboardingReminderModel: OnboardingReminderModel,
        onPrimaryClick: () -> Unit,
    ) {
        setHeader(binding, onboardingReminderModel, isTerminated)

        binding?.apply {
            primaryCta.text = if (isTerminated) "Resubscribe" else "Subscribe"
            primaryCta.setOnClickListener { onPrimaryClick() }
            secondaryCta.setVisible(false)
        }
    }

    private fun setHeader(
        binding: FragmentOnboardingBinding?,
        onboardingReminderModel: OnboardingReminderModel,
        isTerminated: Boolean
    ) {
        binding?.apply {
            header.text =
                if (isTerminated) onboardingReminderModel.terminatedSubscriber.heading else onboardingReminderModel.newSubscriber.heading
            message.text =
                if (isTerminated) onboardingReminderModel.terminatedSubscriber.message else onboardingReminderModel.newSubscriber.message
        }
    }
}
