package com.washingtonpost.android.paywall.bottomsheet.ui.component

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.wapo.android.commons.util.setGone
import com.wapo.android.commons.util.setVisible
import com.washingtonpost.android.paywall.R
import com.washingtonpost.android.paywall.databinding.PaywallSeporatorBinding
import com.washingtonpost.android.paywall.util.BOLD_ENDED
import com.washingtonpost.android.paywall.util.BOLD_START
import com.washingtonpost.android.paywall.util.ITALIC_END
import com.washingtonpost.android.paywall.util.ITALIC_START
import com.washingtonpost.android.paywall.util.RichTextEncodingStyle
import com.washingtonpost.android.paywall.util.getFormattedStringForText

/**
 * Separator component used to separate products.
 */
class SeparatorView : FrameLayout {
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    private val binding = PaywallSeporatorBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    fun init (isWide:Boolean) {
        if (isWide) {
            binding.separator.layoutParams = LayoutParams(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics).toInt(), LayoutParams.WRAP_CONTENT)
        } else {
            binding.separator.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics).toInt(), )
        }
    }

    fun setText(text: String?) {
        binding.text.setGone(text == null)
        binding.separatorLeft.setGone(text == null)
        binding.separatorRight.setGone(text == null)
        binding.separator.setVisible(text == null)

        text?.apply {
            binding.text.text = getFormattedStringForText(
                context = context,
                encodedString = " $text ",
                listOfEncodingStyles = mutableListOf(
                    RichTextEncodingStyle(
                        style = R.style.check_text_included_bold,
                        BOLD_START,
                        BOLD_ENDED
                    ),
                    RichTextEncodingStyle(
                        style = R.style.check_text_included_bold_italic,
                        ITALIC_START,
                        ITALIC_END
                    )
                )
            )
        }
    }

    /**
     * Styling for a separator which is used in the middle of a list of Offer items.
     * Matches bottom margin to the CheckTextViews it is embedded in. Bolds and sets font size.
     */
    fun styleForOffer() {
        binding.text.setTypeface(null, Typeface.BOLD)
        binding.text.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.resources.getDimension(R.dimen.check_separator_text_size))
        val params = binding.separatorLayout.layoutParams as MarginLayoutParams
        params.setMargins(0,0,0, context.resources.getDimension(R.dimen.check_text_bottom_margin).toInt())
        binding.separatorLayout.layoutParams = params
    }

    fun styleForList() {
        val encodedString = binding.text.text
        binding.text.text = getFormattedStringForText(
            context = context,
            encodedString = encodedString.toString(),
            listOfEncodingStyles = mutableListOf(
                RichTextEncodingStyle(style = R.style.check_text_included_bold, BOLD_START, BOLD_ENDED),
                RichTextEncodingStyle(style = R.style.check_text_included_bold_italic, ITALIC_START, ITALIC_END)
            )
        )
        binding.text.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.resources.getDimension(R.dimen.check_separator_text_size))
    }
}