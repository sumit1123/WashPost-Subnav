package com.washingtonpost.android.paywall.bottomsheet.ui.component.product

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.washingtonpost.android.paywall.R
import com.washingtonpost.android.paywall.databinding.ProductNameBinding
import com.washingtonpost.android.paywall.util.*

/**
 * This is the view for Product Name Component (ie. Core)
 */
class ProductNameView : FrameLayout {
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    private val binding = ProductNameBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    fun setName(name: String) {
        binding.productName.text = getFormattedStringForText(
            context = context,
            encodedString = name,
            listOfEncodingStyles = mutableListOf(
                RichTextEncodingStyle(style = R.style.check_text_included_bold, BOLD_START, BOLD_ENDED),
                RichTextEncodingStyle(style = R.style.check_text_included_bold_italic, ITALIC_START, ITALIC_END)
            )
        )
    }

    fun setFontSize(fontSize: Float?) {
        if (fontSize != null) {
            binding.productName.textSize = fontSize
        }
    }
}