@file:JvmName("ComicsUtils")

package com.wapo.flagship.features.comics

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import com.washingtonpost.android.comics.model.ComicStrip
import java.util.*

fun getByline(
    name: String?,
    author: String?,
): Spannable {
    val spannableBuilder = SpannableStringBuilder()
    return if (!name.isNullOrEmpty() && !author.isNullOrEmpty()) {
        spannableBuilder.append("$name by $author").apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, name.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        }
    } else if (!name.isNullOrEmpty()) {
        spannableBuilder.append(name)
    } else if (!author.isNullOrEmpty()) {
        spannableBuilder.append(author)
    } else {
        spannableBuilder
    }
}

class ComicsStripComparator : Comparator<ComicStrip> {
    override fun compare(
        lhs: ComicStrip,
        rhs: ComicStrip,
    ): Int {
        var left = lhs.name
        var right = rhs.name
        if (left.startsWith("The ")) {
            left = left.substring(4)
        }
        if (right.startsWith("The ")) {
            right = right.substring(4)
        }
        return left.compareTo(right)
    }
}
