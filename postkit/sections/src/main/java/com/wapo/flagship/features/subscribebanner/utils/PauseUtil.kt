package com.wapo.flagship.features.subscribebanner.utils

import com.wapo.android.commons.util.isDatePast
import com.wapo.android.commons.util.monthDayFormat
import com.wapo.android.commons.util.monthDayYearFormat
import java.text.ParseException
import java.util.*

object PauseUtil {

    private const val PAUSE_DATE_PLACEHOLDER = "{date}"

    /**
     * Replaces {date} placeholder in Pause text with the date when subscription will automatically Pause or Resume
     * Uses "Month Day" format, e.g. "September 21"
     * Falls back to static text if date cannot be read or is in the past
     */
    fun buildPauseText(rawText: String, fallbackText: String, dateInMillis: Long): String {
        val date = try {
            Date(dateInMillis)
        } catch (e: ParseException) {
            null
        }

        if (date == null || isDatePast(date)) {
            return fallbackText
        }

        val text = rawText.replace(PAUSE_DATE_PLACEHOLDER, monthDayYearFormat(date))
        return if (text.contains(PAUSE_DATE_PLACEHOLDER)) {
            fallbackText
        } else {
            text
        }
    }
}