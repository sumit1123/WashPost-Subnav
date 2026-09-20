package com.washingtonpost.android.paywall.bottomsheet.ui.component

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.washingtonpost.android.paywall.R
import com.washingtonpost.android.paywall.databinding.PaywallSubtitleBinding
import com.washingtonpost.android.paywall.util.*

/**
 * Subtitle component displays text that gives user additional context about the paywall.
 */
open class SubtitleView : FrameLayout {
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    protected val binding = PaywallSubtitleBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    fun setSubtitle(subtitle: String) {
        binding.mainTitle.text = getFormattedStringForText(
            context = context,
            encodedString = subtitle,
            listOfEncodingStyles = mutableListOf(
                RichTextEncodingStyle(style = R.style.check_text_included_bold, BOLD_START, BOLD_ENDED),
                RichTextEncodingStyle(style = R.style.check_text_included_bold_italic, ITALIC_START, ITALIC_END)
            )
        )
    }

    fun setFontSize(fontSize: Float?) {
        if (fontSize != null) {
            binding.mainTitle.textSize = fontSize
        }
    }

    fun setTextColor(color: Int) {
        binding.mainTitle.setTextColor(color)
    }
}