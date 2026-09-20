package com.washingtonpost.android.paywall.bottomsheet.ui.component

import android.content.Context
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.wapo.android.commons.util.setClickSpan
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.R
import com.washingtonpost.android.paywall.databinding.PaywallSignInBinding

/**
 * Sign In Component that updates based on user sign-in state.
 */
class SignInView : FrameLayout {
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    private val binding = PaywallSignInBinding.inflate(LayoutInflater.from(context), this, true)

    /**
     * Update [full_sign_in_text] based on if user [isLoggedIn]. Show [loginId] if user is logged in. Also
     * set call [clickAction] when user wants to sign in.
     */
    fun setSignInText(isLoggedIn: Boolean, clickAction: () -> Unit) {

        val commonText = if(PaywallService.getInstance().isSubActive) {
            context.getString(R.string.common_text_subscribed)
        } else {
            context.getString(R.string.common_text)
        }

        val signedInLinkText = context.getString(R.string.signed_in_link_text)
        val normalLinkText = context.getString(R.string.normal_link_text)

        val fullText = if (isLoggedIn) {
            "$commonText $signedInLinkText"
        } else {
            "$commonText $normalLinkText"
        }

        val spannable = SpannableString(fullText)

        when {
            isLoggedIn -> {
                spannable.setClickSpan(fullText, signedInLinkText, R.color.sign_in_text, context) {
                    clickAction()
                }
            }
            else -> {
                spannable.setClickSpan(fullText, normalLinkText, R.color.sign_in_text, context) {
                    clickAction()
                }
            }
        }

        binding.fullSignInText.text = spannable
        binding.fullSignInText.movementMethod = LinkMovementMethod.getInstance()
    }

}