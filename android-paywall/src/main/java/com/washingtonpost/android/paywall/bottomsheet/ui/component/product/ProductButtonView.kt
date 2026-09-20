package com.washingtonpost.android.paywall.bottomsheet.ui.component.product

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.washingtonpost.android.paywall.R
import com.washingtonpost.android.paywall.databinding.ProductButtonBinding
import com.washingtonpost.android.paywall.util.BOLD_ENDED
import com.washingtonpost.android.paywall.util.BOLD_START
import com.washingtonpost.android.paywall.util.ITALIC_END
import com.washingtonpost.android.paywall.util.ITALIC_START
import com.washingtonpost.android.paywall.util.RichTextEncodingStyle
import com.washingtonpost.android.paywall.util.getFormattedStringForText

class ProductButtonView : FrameLayout {
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    private val binding = ProductButtonBinding.inflate(LayoutInflater.from(context), this, true)

    /**
     * Initialize Subscribe Buttons to remove default material design shadows.
     */
    init {
        //Remove shadow on button
        binding.productButton.stateListAnimator = null
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    /**
     * Set and format text on Button
     */
    fun setText(text: String) {
        binding.productButton.text = getFormattedStringForText(
            context = context,
            encodedString = text,
            listOfEncodingStyles = mutableListOf(
                RichTextEncodingStyle(style = R.style.check_text_included_bold, BOLD_START, BOLD_ENDED),
                RichTextEncodingStyle(style = R.style.check_text_included_bold_italic, ITALIC_START, ITALIC_END)
            )
        )
    }

    /**
     * Used to force Regwall button text to bold.
     * Implemented because bold tags from config do not work on some Fire devices.
     * TODO (AWA-6748): Remove when bold tags from config work on all Fire devices.
     */
    fun setTypeface(tf: Typeface?, style: Int) {
        binding.productButton.setTypeface(tf, style)
    }

    /**
     * Toggles button width style.
     * Currently paywalls are full width and regwalls are not.
     */
    fun fullWidth(fullWidth: Boolean) {
        val params = binding.productButton.layoutParams
        if (fullWidth) {
            params.width = LayoutParams.MATCH_PARENT
        } else {
            params.width = LayoutParams.WRAP_CONTENT
        }
        binding.productButton.layoutParams = params
    }

    /**
     * Sets the badge visible on top of a button
     * [text] text for the button.
     */
    fun setBadgeVisible(text: String){
        binding.badge.visibility = View.VISIBLE
        binding.badgeText.text = text
    }

    /**
     * Set and format text on Button
     */
    fun setButtonClickListener(onClickListener: OnClickListener) {
        binding.productButton.setOnClickListener(onClickListener)
    }
}