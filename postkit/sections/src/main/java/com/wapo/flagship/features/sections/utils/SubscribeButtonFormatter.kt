package com.wapo.flagship.features.sections.utils

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import com.wapo.text.WpTextAppearanceSpan
import com.washingtonpost.android.sections.R

class SubscribeButtonFormatter {

    fun format(text: CharSequence, context: Context): CharSequence {
        val builder = SpannableStringBuilder()
        if (TextUtils.isEmpty(text)) {
            return builder
        }
        builder.append(text)
        builder.setSpan(
                WpTextAppearanceSpan(context, R.style.SubscribeButtonTheme),
                0,
                text.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return builder
    }
}