package com.wapo.android.commons.util

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.TextAppearanceSpan
import android.view.View
import androidx.core.content.ContextCompat


fun SpannableString.setStyleSpan(
    fullText: String,
    styleText: String,
    style: Int,
    context: Context
) {
    val styleStartIndex = fullText.indexOf(styleText)
    val styleEndIndex = fullText.indexOf(styleText) + styleText.length
    if (styleStartIndex < 0) {
        return
    }
    this.setSpan(
        TextAppearanceSpan(context, style),
        styleStartIndex,
        styleEndIndex,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
}

fun SpannableString.setColorSpan(
    fullText: String,
    styleText: String,
    color: Int,
    context: Context
) {
    val styleStartIndex = fullText.indexOf(styleText)
    val styleEndIndex = fullText.indexOf(styleText) + styleText.length
    if (styleStartIndex < 0) {
        return
    }
    this.setSpan(
        ForegroundColorSpan(ContextCompat.getColor(context, color)),
        styleStartIndex,
        styleEndIndex,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
}

fun SpannableString.setClickSpan(
    fullText: String,
    styleText: String,
    color: Int,
    context: Context,
    action: () -> Unit
) {
    val styleStartIndex = fullText.indexOf(styleText)
    val styleEndIndex = fullText.indexOf(styleText) + styleText.length

    if (styleStartIndex < 0) {
        return
    }
    val clickableSpan = object : ClickableSpan() {
        override fun onClick(p0: View) {
            action()
        }

        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.color = ContextCompat.getColor(context, color)
        }
    }
    this.setSpan(clickableSpan, styleStartIndex, styleEndIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
}