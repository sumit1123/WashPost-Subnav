package com.washingtonpost.android.paywall.bottomsheet.ui.component

import android.content.Context
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.wapo.android.commons.util.setClickSpan
import com.wapo.android.commons.util.setStyleSpan
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.R
import com.washingtonpost.android.paywall.databinding.PaywallTermsBinding
import com.washingtonpost.android.paywall.util.BOLD_ENDED
import com.washingtonpost.android.paywall.util.BOLD_START
import com.washingtonpost.android.paywall.util.PaywallConstants
import com.washingtonpost.android.paywall.util.RichTextEncodingStyle
import com.washingtonpost.android.paywall.util.getFormattedStringForText

/**
 * Terms of Service and Privacy Policy Text component
 */
class TermsView : FrameLayout {
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    private val binding = PaywallTermsBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        setFinePrint()
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    /**
     * Replace the default fine-print with dynamic text from iterable
     * Parses HTML bold tags and makes "Terms of Service" and "Privacy Policy" clickable if present in the text.
     */
    fun setTermsText(text: String) {
        val formatted = getFormattedStringForText(
            context = context,
            encodedString = text,
            listOfEncodingStyles = mutableListOf(
                RichTextEncodingStyle(
                    style = R.style.paywall_sheet_fine_print_bold,
                    BOLD_START,
                    BOLD_ENDED
                ),
            )
        )
        val spanString = SpannableString(formatted)
        applyLinkSpans(spanString, spanString.toString())
        binding.finePrintText.text = spanString
        binding.finePrintText.movementMethod = LinkMovementMethod.getInstance()
    }

    /**
     * Set [fine_print_text] at bottom to make TOC and PP clickable.
     */
    private fun setFinePrint() {
        val finePrintText = context.getString(R.string.tos_pp_fine_print)
        val cancelBoldText = context.getString(R.string.cancel_anytime_bold)

        val spanString = SpannableString(finePrintText)
        spanString.apply {
            applyLinkSpans(this, finePrintText)
            setStyleSpan(
                finePrintText,
                cancelBoldText,
                R.style.paywall_sheet_fine_print_bold,
                context
            )
        }
        binding.finePrintText.text = spanString
        binding.finePrintText.movementMethod = LinkMovementMethod.getInstance()
    }

    /**
     * Apply clickable spans for "Terms of Service" and "Privacy Policy" if present in [terms].
     */
    private fun applyLinkSpans(spannable: SpannableString, terms: String) {
        val tosText = context.getString(R.string.terms_of_service_text)
        val ppText = context.getString(R.string.privacy_policy_text)

        spannable.apply {
            setClickSpan(terms, ppText, R.color.sub_text, context) {
                if (PaywallService.getConnector() != null) {
                    PaywallService.getConnector().showPolicy(PaywallConstants.PRIVACY_POLICY, context)
                }
            }
            setClickSpan(terms, tosText, R.color.sub_text, context) {
                if (PaywallService.getConnector() != null) {
                    PaywallService.getConnector().showPolicy(PaywallConstants.TERMS_OF_SERVICE, context)
                }
            }
        }
    }
}