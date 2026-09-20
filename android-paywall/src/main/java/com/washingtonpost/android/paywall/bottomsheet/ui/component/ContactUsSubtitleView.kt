package com.washingtonpost.android.paywall.bottomsheet.ui.component

import android.content.Context
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.wapo.android.commons.util.setClickSpan
import com.washingtonpost.android.paywall.R
import com.washingtonpost.android.paywall.databinding.PaywallSubtitleBinding
import com.washingtonpost.android.paywall.util.*

/**
 * Subtitle component displays text that gives user additional context about the paywall.
 * Has a clickable "contact us" link that directs a user from a wall to the contact us page
 */
class ContactUsSubtitleView : SubtitleView {
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    fun setSubtitle(clickAction: () -> Unit) {
        val subtitle = context.resources.getString(R.string.contact_us_subtitle_text)
        val clickableText = context.resources.getString(R.string.contact_us_link_text)

        val formattedText = getFormattedStringForText(
            context,
            subtitle,
            listOfEncodingStyles = mutableListOf(
                RichTextEncodingStyle(style = R.style.check_text_included_bold, BOLD_START, BOLD_ENDED),
                RichTextEncodingStyle(style = R.style.check_text_included_bold_italic, ITALIC_START, ITALIC_END)
            )
        )

        val spannable = SpannableString(formattedText)

        spannable.setClickSpan(subtitle, clickableText, R.color.contact_us_subtitle_text, context) {
            clickAction()
        }

        binding.mainTitle.text = spannable
        binding.mainTitle.movementMethod = LinkMovementMethod.getInstance()
    }
}