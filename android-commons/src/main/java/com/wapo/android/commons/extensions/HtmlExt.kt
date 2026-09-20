/* Copyright (c) 2025 The Washington Post. All rights reserved. */

package com.wapo.android.commons.extensions

import android.os.Build
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.Spanned

/**
 * This file contains extension methods for the Html related classes.
 */

/**
 * Helper method to format given html text to a Spanned object
 */
fun htmlTextToSpanned(htmlText: String?): Spanned? {
    htmlText ?: return null
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY)
    else
        Html.fromHtml(htmlText)
}

fun String.toSpannableBuilder(): SpannableStringBuilder? {
    return htmlTextToSpanned(this)?.let { SpannableStringBuilder(it) }
}
