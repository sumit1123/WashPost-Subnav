package com.washingtonpost.android.follow.helper

import com.wapo.android.commons.util.Logger
import java.text.SimpleDateFormat
import java.util.*

fun parseDisplayDate(displayDate: String?): Long? {
    if (displayDate == null) {
        return null
    }

    val dateFormatSeconds =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    val dateFormatMilliseconds =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    try {
        return dateFormatSeconds.parse(displayDate)?.time
    } catch (e: Exception) {
    }

    try {
        return dateFormatMilliseconds.parse(displayDate)?.time
    } catch (e: Exception) {
    }

    val dateParsingException = Exception("Unable to parse RFC dates from ${displayDate}")
    Logger.e("ArticleItem", "parseDateString failed", dateParsingException)
    return null
}