package com.washingtonpost.android.paywall.bottomsheet.ui.component.old

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.TextAppearanceSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.wapo.android.commons.util.setClickSpan
import com.wapo.android.commons.util.setStyleSpan
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.R
import com.washingtonpost.android.paywall.databinding.PaywallSubTextBinding
import com.washingtonpost.android.paywall.util.PaywallConstants

class SubTextView : FrameLayout {
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    private val binding = PaywallSubTextBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        setFinePrint()
    }

    /**
     * Set [fine_print_text] at bottom to make TOC and PP clickable.
     */
    private fun setFinePrint() {
        val finePrintText = context.getString(R.string.tos_pp_fine_print)
        val cancelBoldText = context.getString(R.string.cancel_anytime_bold)
        val tosText = context.getString(R.string.terms_of_service_text)
        val ppText = context.getString(R.string.privacy_policy_text)

        val spanString = SpannableString(finePrintText)
        spanString.apply {
            setClickSpan(finePrintText, ppText, R.color.sub_text, context) {
                if (PaywallService.getConnector() != null) {
                    PaywallService.getConnector().showPolicy(PaywallConstants.PRIVACY_POLICY, context)
                }
            }
            setClickSpan(finePrintText, tosText, R.color.sub_text, context) {
                if (PaywallService.getConnector() != null) {
                    PaywallService.getConnector().showPolicy(PaywallConstants.TERMS_OF_SERVICE, context)
                }
            }
            setStyleSpan(finePrintText, cancelBoldText, R.style.paywall_sheet_fine_print_bold, context)
        }
        binding.finePrintText.text = spanString
        binding.finePrintText.movementMethod = LinkMovementMethod.getInstance()
    }

    /**
     * Update [full_sign_in_text] based on if user [isLoggedIn]. Show [loginId] if user is logged in. Also
     * set call [clickAction] when user wants to sign in.
     */
    fun setSignInText(isLoggedIn: Boolean, loginId: String = "Unknown_Id", clickAction: () -> Unit) {
        val signedInText = context.getString(R.string.signed_in_text)
        val commonText = context.getString(R.string.common_text)
        val signedInLinkText = context.getString(R.string.signed_in_link_text)
        val normalLinkText = context.getString(R.string.normal_link_text)

        val fullText = if (isLoggedIn) {
            "${signedInText.replace("[loginId]", loginId)}\n$commonText\n$signedInLinkText"
        } else {
            "$commonText $normalLinkText"
        }

        val spannable = SpannableString(fullText)

        if (isLoggedIn) {
            spannable.setSpan(TextAppearanceSpan(context, R.style.paywall_sheet_sub_text_bold), fullText.indexOf(loginId), fullText.indexOf(loginId) + loginId.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
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