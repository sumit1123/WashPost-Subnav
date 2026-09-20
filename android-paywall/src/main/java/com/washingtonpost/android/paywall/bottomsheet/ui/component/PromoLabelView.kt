package com.washingtonpost.android.paywall.bottomsheet.ui.component

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.washingtonpost.android.paywall.R
import com.washingtonpost.android.paywall.databinding.PaywallPromoLabelBinding
import com.washingtonpost.android.paywall.databinding.PaywallTopLabelBinding
import com.washingtonpost.android.paywall.util.BOLD_ENDED
import com.washingtonpost.android.paywall.util.BOLD_START
import com.washingtonpost.android.paywall.util.ITALIC_END
import com.washingtonpost.android.paywall.util.ITALIC_START
import com.washingtonpost.android.paywall.util.RichTextEncodingStyle
import com.washingtonpost.android.paywall.util.getFormattedStringForText

/**
 * Promo label on paywall (ie. Special Offer)
 */
class PromoLabelView : FrameLayout {
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    private val binding = PaywallPromoLabelBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    fun setLabel(mainLabel: String?, secondaryLabel: String = "") {
        binding.topLabelGroup.visibility = if (mainLabel == null) View.GONE else View.VISIBLE
        mainLabel?.apply {
            binding.mainLabel.text = getFormattedStringForText(
                context = context,
                encodedString = this,
                listOfEncodingStyles = mutableListOf(
                    RichTextEncodingStyle(style = R.style.check_text_included_bold, BOLD_START, BOLD_ENDED),
                    RichTextEncodingStyle(style = R.style.check_text_included_bold_italic, ITALIC_START, ITALIC_END)
                )
            )
        }

        binding.secondaryLabel.visibility = if (secondaryLabel.isEmpty()) View.GONE else View.VISIBLE
        binding.separator.visibility = binding.secondaryLabel.visibility
        secondaryLabel.apply {
            binding.secondaryLabel.text = getFormattedStringForText(
                context = context,
                encodedString = this,
                listOfEncodingStyles = mutableListOf(
                    RichTextEncodingStyle(style = R.style.check_text_included_bold, BOLD_START, BOLD_ENDED),
                    RichTextEncodingStyle(style = R.style.check_text_included_bold_italic, ITALIC_START, ITALIC_END)
                )
            )
        }
    }
}