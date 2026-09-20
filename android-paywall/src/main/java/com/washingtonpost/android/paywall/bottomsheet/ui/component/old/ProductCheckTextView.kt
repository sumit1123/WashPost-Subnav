package com.washingtonpost.android.paywall.bottomsheet.ui.component.old

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.washingtonpost.android.paywall.R
import com.washingtonpost.android.paywall.databinding.ProductCheckTextBinding
import com.washingtonpost.android.paywall.util.*

class ProductCheckTextView : FrameLayout {
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    /**
     * call [updateState] when [isInclusive] value is updated.
     */
    var isInclusive: Boolean = true
        set(value) {
            updateState(value)
        }

    private val binding = ProductCheckTextBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    /**
     * Set Check text with bold style.
     */
    fun setText(text: String) {
        binding.featureText.text = getFormattedStringForText(
            context = context,
            encodedString = text,
            listOfEncodingStyles = mutableListOf(
                RichTextEncodingStyle(style = R.style.check_text_included_bold, BOLD_START, BOLD_ENDED),
                RichTextEncodingStyle(style = R.style.check_text_included_bold_italic, ITALIC_START, ITALIC_END)
            )
        )
    }

    /**
     * Set appropriate [check_mark] and [feature_text] style based on [isInclusive] value
     */
    private fun updateState(isInclusive: Boolean) {
        if (isInclusive) {
            binding.checkMark.setImageDrawable(VectorDrawableCompat.create(resources, R.drawable.check_inclusive, context.theme))
            binding.featureText.setTextColor(ContextCompat.getColor(context, R.color.check_text_included_color))
        } else {
            binding.checkMark.setImageDrawable(VectorDrawableCompat.create(resources, R.drawable.check_exclusive, context.theme))
            binding.featureText.setTextColor(ContextCompat.getColor(context, R.color.check_text_excluded_color))
        }
    }
}