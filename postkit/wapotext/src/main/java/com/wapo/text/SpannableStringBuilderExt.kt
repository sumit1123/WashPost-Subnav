package com.wapo.text

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned

// Extension functions for SpannableStringBuilder operations
fun SpannableStringBuilder.applyUnderline(
    context: Context,
    start: Int,
    end: Int,
    color: Int,
    paddingLeft: Float = 0f,
    paddingRight: Float = 0f,
    underlineHeight: Float = 0f
) {
    val underlineSpan = WpTextUnderlineSpan(context, color, paddingLeft, paddingRight)
    underlineSpan.height = underlineHeight
    setSpan(underlineSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
}
