package com.washingtonpost.android.paywall.bottomsheet.ui.component

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.washingtonpost.android.paywall.R
import com.washingtonpost.android.paywall.databinding.PaywallTitleBinding
import com.washingtonpost.android.paywall.util.*

/**
 * Title component displays text that tells user why paywall is shown.
 */
class TitleView : FrameLayout {
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    private val binding = PaywallTitleBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    fun setTitle(title: String) {
        binding.mainTitle.text = getFormattedStringForText(
            context = context,
            encodedString = title,
            listOfEncodingStyles = mutableListOf(
                RichTextEncodingStyle(style = R.style.top_title_style, BOLD_START, BOLD_ENDED),
                RichTextEncodingStyle(style = R.style.top_title_style, ITALIC_START, ITALIC_END)
            )
        )
    }

    fun setFontSize(fontSize: Float?) {
        if (fontSize != null) {
            binding.mainTitle.textSize = fontSize
        }
    }

    /**
     * Configures the title TextView to prevent text truncation and apply proper margins.
     * This ensures the full title text is displayed without ellipsizing.
     */
    fun configureTextBehavior() {
        binding.mainTitle.apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                val horizontalMargin = android.util.TypedValue.applyDimension(
                    android.util.TypedValue.COMPLEX_UNIT_DIP,
                    16f,
                    resources.displayMetrics
                ).toInt()
                marginStart = horizontalMargin
                marginEnd = horizontalMargin
            }
            maxLines = Int.MAX_VALUE
            ellipsize = null
        }
    }
}