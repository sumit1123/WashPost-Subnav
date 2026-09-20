package com.washingtonpost.android.paywall.util

import androidx.annotation.StyleRes

/**
 * Represents the encoding style that will be used as a reference and known by all team members.
 * e.g. if team agrees that <b></b> used for bold the [startTag] will be "<b>" and [endTag] will be "</b>"
 * [style] resource id for the style to be rendered.
 */
data class RichTextEncodingStyle(@StyleRes val style: Int, val startTag: String, val endTag: String)
