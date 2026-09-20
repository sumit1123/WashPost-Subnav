/* Copyright (c) 2023 The Washington Post. All rights reserved. */
package com.wapo.android.commons.util

import android.text.Spannable
import android.text.SpannableStringBuilder

/**
 * Extension function to capitalize the first letter of a string, and also lower case the
 * remaining letters (irrespective of the number of sentences in a string).
 * Note: Add more cases if it contains more than a sentence or any additional cases.
 */
fun Spannable.sentenceCase(): Spannable {
    if (isNullOrEmpty()) return this

    val spannableString = SpannableStringBuilder(substring(0, 1).uppercase())
    if (length > 1) spannableString.append(substring(1).lowercase())
    // reapply the spans
    val spans: Array<Any> = getSpans(0, length, Any::class.java)
    for (span in spans) {
        spannableString.setSpan(span, getSpanStart(span), getSpanEnd(span), 0)
    }
    return spannableString
}