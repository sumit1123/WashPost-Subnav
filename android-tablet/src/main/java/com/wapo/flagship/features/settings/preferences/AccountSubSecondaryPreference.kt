package com.wapo.flagship.features.settings.preferences

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.compose.Visibility
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso
import com.wapo.android.commons.util.setVisible
import com.wapo.flagship.features.settings.SettingsViewModel
import com.wapo.flagship.features.settings.SignInState
import com.wapo.flagship.features.settings.SubscriptionState
import com.washingtonpost.android.R

class AccountSubSecondaryPreference : Preference {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    private var settingsViewModel: SettingsViewModel? = null
    private var nameView: AppCompatTextView? = null
    private var emailView: AppCompatTextView? = null
    private var profileImageView: ShapeableImageView? = null
    private var profileGroup: ViewGroup? = null
    private var signedInStatusTextView: AppCompatTextView? = null
    private var subsIcon: AppCompatImageView? = null
    private var separator: View? = null

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.run {
            nameView = findViewById(R.id.profile_name) as AppCompatTextView
            emailView = findViewById(R.id.profile_email) as AppCompatTextView
            profileImageView = findViewById(R.id.profile_photo) as ShapeableImageView
            profileGroup = findViewById(R.id.profile_name_email_group) as ViewGroup
            signedInStatusTextView = findViewById(R.id.sign_in_status) as AppCompatTextView
            separator = findViewById(R.id.separator)
            update()
        }
    }

    private fun update() {
        settingsViewModel?.signInState?.value?.apply {
            when (this) {
                is SignInState.SignedIn ->
                    updateProfile(
                        this.name,
                        this.email,
                        this.subscription,
                        this.photoUrl,
                        true,
                    )
                is SignInState.SignedOut ->
                    updateProfile(
                        null,
                        null,
                        this.subscription,
                        null,
                        settingsViewModel?.isUserSubscribed() == true,
                    )
                else -> {}
            }
        }
    }

    fun updateProfile(
        name: String?,
        email: String?,
        subscription: String?,
        photoUrl: String?,
        isVisible: Boolean,
    ) {
        this.isVisible = isVisible
        when {
            (name == null && email == null) -> {
                profileGroup?.apply {
                    visibility = View.GONE
                }
                signedInStatusTextView?.apply {
                    visibility = View.VISIBLE
                }
                profileImageView?.apply {
                    visibility = View.GONE
                }
            }
            else -> {
                profileGroup?.apply {
                    visibility = View.VISIBLE
                }
                nameView?.apply {
                    this.setVisible(name != null)
                    text = name
                }
                emailView?.apply {
                    this.setVisible(email != null)
                    email?.let {
                        this.text = it
                    }
                }
                separator?.setVisible(email != null && name != null)
                signedInStatusTextView?.apply {
                    visibility = View.GONE
                }
                profileImageView?.visibility = View.VISIBLE
                if (profileImageView != null && !photoUrl.isNullOrEmpty()) {
                    val modUrl = photoUrl.replace("http:", "https:")
                    val placeholder =
                        VectorDrawableCompat.create(
                            context.resources,
                            R.drawable.profile_photo_placeholder,
                            context.theme,
                        )
                    placeholder?.apply {
                        Picasso
                            .get()
                            .load(modUrl)
                            .fit()
                            .centerCrop()
                            .placeholder(placeholder)
                            .error(placeholder)
                            .into(profileImageView)
                    }
                }
            }
        }

        subsIcon?.apply {
            when (settingsViewModel?.subscriptionState?.value) {
                SubscriptionState.GracePeriod -> {
                    visibility = View.VISIBLE
                    this.setImageResource(R.drawable.ic_subs_burst)
                }
                SubscriptionState.Subscribed, is SubscriptionState.ScheduledPause -> {
                    visibility = View.VISIBLE
                    this.setImageResource(R.drawable.ic_subs_burst)
                }
                is SubscriptionState.Paused -> {
                    visibility = View.VISIBLE
                    this.setImageResource(R.drawable.ic_subs_pause)
                }
                else -> {
                    visibility = View.GONE
                }
            }
        }

        signedInStatusTextView?.apply {
            text =
                when (settingsViewModel?.subscriptionState?.value) {
                    SubscriptionState.GracePeriod,
                    SubscriptionState.Subscribed, is SubscriptionState.ScheduledPause,
                    -> if (subscription.isNullOrEmpty()) SUBSCRIPTION else "$subscription"
                    SubscriptionState.FreeDays, is SubscriptionState.FreeArticles, SubscriptionState.MobileFreeTrial ->
                        if (subscription
                                .isNullOrEmpty()
                        ) {
                            FREE_TRIAL
                        } else {
                            "$subscription Free Trial"
                        }
                    SubscriptionState.OnHold -> settingsViewModel?.getPaymentErrorDate() ?: PAYMENT_ERROR
                    is SubscriptionState.Paused -> if (subscription.isNullOrEmpty()) PAUSED else "$subscription (paused)"
                    SubscriptionState.Terminated -> if (!settingsViewModel?.getExpiredSubscription().isNullOrEmpty()) {
                        this.visibility = View.VISIBLE
                        settingsViewModel?.getExpiredSubscription()
                    } else {
                        this.visibility = View.GONE
                        ""
                    }
                    else -> NO_SUBSCRIPTION
                }
        }
    }

    fun setViewModel(settingsViewModel: SettingsViewModel) {
        this.settingsViewModel = settingsViewModel
    }

    companion object {
        private const val FREE_TRIAL = "Free Trial"
        private const val SUBSCRIPTION = "Subscriber"
        private const val NO_SUBSCRIPTION = "No Subscription"
        private const val PAYMENT_ERROR = "Payment error"
        private const val PAUSED = "Paused"
    }
}
