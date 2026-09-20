package com.wapo.flagship.features.settings.preferences

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.wapo.flagship.features.settings.SettingsViewModel
import com.wapo.flagship.features.settings.SignInState
import com.wapo.flagship.features.settings.SubscriptionState
import com.washingtonpost.android.R

class AccountSubPrimaryPreference : Preference {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    private var primaryActionTextView: AppCompatTextView? = null
    private var secondaryActionTextView: AppCompatTextView? = null
    private var actionIconImageView: AppCompatImageView? = null
    private var errorIconImageView: AppCompatImageView? = null
    private var settingsViewModel: SettingsViewModel? = null
    private var isPaymentErrorMode: Boolean = false

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.run {
            primaryActionTextView = findViewById(R.id.primary_action_text) as AppCompatTextView
            secondaryActionTextView = findViewById(R.id.secondary_action_text) as AppCompatTextView
            actionIconImageView = findViewById(R.id.action_icon) as AppCompatImageView
            errorIconImageView = findViewById(R.id.error_icon) as AppCompatImageView
            if (!isPaymentErrorMode) {
                update()
            }
        }
    }

    private fun update() {
        settingsViewModel?.signInState?.value?.apply {
            when (this) {
                is SignInState.SignedIn ->
                    updateAction(
                        true,
                        settingsViewModel?.subscriptionState?.value,
                    )
                is SignInState.SignedOut ->
                    updateAction(
                        false,
                        settingsViewModel?.subscriptionState?.value,
                    )
                else -> {}
            }
        }
    }

    fun showPaymentError(subState: SubscriptionState?) {
        this.isVisible = subState == SubscriptionState.OnHold || subState == SubscriptionState.GracePeriod
        if (this.isVisible) {
            updateAction(false, subState)
        }
    }

    fun updateAction(
        isSignedIn: Boolean,
        subState: SubscriptionState?,
    ) {
        this.isVisible = !isSignedIn ||
            (
                subState != SubscriptionState.Subscribed &&
                    subState !is SubscriptionState.Paused &&
                    subState !is SubscriptionState.ScheduledPause
            )
        if (!this.isVisible) {
            return
        }

        primaryActionTextView?.apply {
            text =
                when (subState) {
                    SubscriptionState.NotSubscribed, SubscriptionState.FreeDays, is SubscriptionState.FreeArticles, is SubscriptionState.MobileFreeTrial ->
                        resources.getString(
                            R.string.primary_no_sub,
                        )
                    SubscriptionState.GracePeriod -> resources.getString(R.string.primary_grace_period)
                    SubscriptionState.OnHold -> resources.getString(R.string.primary_on_hold)
                    SubscriptionState.Terminated -> resources.getString(R.string.primary_sub_expired)
                    SubscriptionState.Subscribed, is SubscriptionState.Paused, is SubscriptionState.ScheduledPause -> {
                        if (!isSignedIn) {
                            resources.getString(R.string.primary_sub_no_account)
                        } else {
                            ""
                        }
                    }
                    else -> ""
                }
        }

        secondaryActionTextView?.apply {
            text =
                when (subState) {
                    SubscriptionState.NotSubscribed, SubscriptionState.FreeDays, is SubscriptionState.FreeArticles, is SubscriptionState.MobileFreeTrial ->
                        resources.getString(
                            R.string.secondary_no_sub,
                        )
                    SubscriptionState.GracePeriod ->
                        resources.getString(
                            R.string.secondary_grace_period,
                        )
                    SubscriptionState.OnHold -> resources.getString(R.string.secondary_on_hold)
                    SubscriptionState.Terminated -> resources.getString(R.string.secondary_sub_expired)
                    SubscriptionState.Subscribed, is SubscriptionState.Paused, is SubscriptionState.ScheduledPause -> {
                        if (!isSignedIn) {
                            resources.getString(R.string.secondary_sub_no_account)
                        } else {
                            ""
                        }
                    }
                    else -> ""
                }
        }


        when (subState) {
            SubscriptionState.NotSubscribed,
            SubscriptionState.Terminated,
            SubscriptionState.FreeDays,
            is SubscriptionState.FreeArticles, is SubscriptionState.MobileFreeTrial,
            -> {
                actionIconImageView?.visibility = View.VISIBLE
                errorIconImageView?.visibility = View.GONE
                actionIconImageView?.setImageResource(R.drawable.promo_buy_sub)
            }
            SubscriptionState.OnHold,
            SubscriptionState.GracePeriod,
            -> {
                actionIconImageView?.visibility = View.GONE
                errorIconImageView?.visibility = View.VISIBLE
            }
            SubscriptionState.Subscribed,
            is SubscriptionState.Paused,
            is SubscriptionState.ScheduledPause,
            -> {
                if (!isSignedIn) {
                    actionIconImageView?.visibility = View.VISIBLE
                    errorIconImageView?.visibility = View.GONE
                    actionIconImageView?.setImageResource(R.drawable.sign_in_icon)
                }
            }
            else -> {
                // no op
            }
        }
    }

    fun setViewModel(settingsViewModel: SettingsViewModel) {
        this.settingsViewModel = settingsViewModel
    }

    fun setPaymentErrorMode() {
        this.isPaymentErrorMode = true
    }

    companion object {
        private const val SUB_PRIMARY_TEXT = "Subscribe to The Post"
        private const val SUB_SECONDARY_TEXT = "View Subscription options"
        private const val SIGN_IN_PRIMARY_TEXT =
            "Sign in to get the most out of your subscription"
        private const val SIGN_IN_SECONDARY_TEXT = "or create an account"
    }
}
