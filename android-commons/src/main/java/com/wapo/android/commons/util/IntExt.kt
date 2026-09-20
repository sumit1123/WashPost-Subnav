package com.wapo.android.commons.util

/**
 * returns: a human-readable, truncated string representation of the integer.
 * - For values 1,000,000 and above, returns the number in millions with an "M" suffix.
 * - For values 1,000 and above, returns the number in thousands with a "k" suffix.
 * - For values 0 and above, returns the number as a string.
 * - For negative values, returns null.
 */
fun Int.truncatedString(): String? {
    return when {
        this >= 1000000 -> "${(this / 1000000)}M"
        this >= 1000 -> "${(this / 1000)}k"
        this >= 0 -> "$this"
        else -> null
    }
}