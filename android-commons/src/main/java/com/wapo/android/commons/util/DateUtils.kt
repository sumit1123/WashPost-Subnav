/* Copyright (c) 2021 The Washington Post. All rights reserved. */

package com.wapo.android.commons.util

import org.threeten.bp.OffsetDateTime
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime

val ISO8601_FORMAT_WITH_TIMEZONE = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
val ISO8601_FORMAT_WITHOUT_TIMEZONE = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
const val SHARE_LINK_EXPIRY_DAYS = 30L
const val EXPIRY_WARNING_THRESHOLD_DAYS = 5L


/**
 * Method to figure out how long ago [pastTime] is relative to [nowTime]
 * Returns
 *  X day(s) ago when days > 0
 *  X hour(s) ago when hours > 0 and days = 0
 *  X minute(s) ago when minutes > 0 and hours = 0 / days = 0
 */
fun timeAgo(pastTime: Long): String {
    val nowTime = Date().time
    val delta = nowTime - pastTime
    val time = Calendar.getInstance()
    time.timeInMillis = delta
    val days = TimeUnit.MILLISECONDS.toDays(delta)
    val hours = TimeUnit.MILLISECONDS.toHours(delta)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(delta)

    return when {
        days >= 365 && days % 365 >= 30 -> "${passedTime(days = days)} ${passedTime(days = days % 365)} ago"
        else -> "${passedTime(days, hours, minutes)} ago"
    }
}

fun commentsTimeAgo(pastTime: Long): String {
    val nowTime = Date().time
    val delta = nowTime - pastTime
    val days = TimeUnit.MILLISECONDS.toDays(delta)
    val hours = TimeUnit.MILLISECONDS.toHours(delta)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(delta)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(delta)

    return when {
        seconds < 60 -> "Just now"
        days >= 365 && days % 365 >= 30 -> "${passedTime(days = days)} ${passedTime(days = days % 365)} ago"
        else -> "${passedTime(days, hours, minutes)} ago"
    }
}

fun timePeriodString(period: Long): String {
    val time = Calendar.getInstance()
    time.timeInMillis = period
    val days = TimeUnit.MILLISECONDS.toDays(period)
    val hours = TimeUnit.MILLISECONDS.toHours(period)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(period)
    return passedTime(days, hours, minutes)
}

fun passedTime(days: Long = 0, hours: Long = 0, minutes: Long = 0): String {
    return when {
        days >= 730 -> "${days / 365} years"
        days >= 365 -> "1 year"
        days in 360..364 -> "11 months"
        days >= 60 -> "${days / 30} months"
        days >= 30 -> "1 month"
        days > 1 -> "$days days"
        days == 1L -> "$days day"
        hours > 1 -> "$hours hours"
        hours == 1L -> "$hours hour"
        minutes > 1 -> "$minutes minutes"
        minutes == 1L -> "$minutes minute"
        else -> ""
    }
}

private fun checkIfRecent(
    timeInMillis: Long?,
    recencyThreshold: Long?,
    timeUnit: TimeUnit
): Boolean {
    if (timeInMillis == null || recencyThreshold == null) {
        return false
    }

    if (timeInMillis <= 0 || recencyThreshold <= 0) {
        return false
    }

    val delta = Calendar.getInstance().timeInMillis - timeInMillis

    return if (delta <= 0) {
        false
    } else {
        delta <= timeUnit.toMillis(recencyThreshold)
    }
}

/**
 * Method to verify given [timeInMillis] is recent or not from currentTimeMillis
 * with the given [recencyThresholdMinutes]
 */
fun isRecentMinutes(
    timeInMillis: Long?,
    recencyThresholdMinutes: Long?
) = checkIfRecent(
    timeInMillis,
    recencyThresholdMinutes,
    TimeUnit.MINUTES
)

/**
 * Method to verify given [timeInMillis] is recent or not from currentTimeMillis
 * with the given [recencyThresholdSeconds]
 */
fun isRecentSeconds(
    timeInMillis: Long?,
    recencyThresholdSeconds: Long?
) = checkIfRecent(
    timeInMillis,
    recencyThresholdSeconds,
    TimeUnit.SECONDS
)

/**
 * provides a String representation of the given time
 * @return `millis` in hh:mm:ss format
 */
fun formatTimeMillis(millis: Long?): String? {
    millis ?: return null
    if (millis < 0) return null
    val secs = millis / 1000
    return formatTimeSeconds(secs)
}

/**
 * provides a String representation of the given time
 * @return `secs` in hh:mm:ss format
 */
fun formatTimeSeconds(secs: Long): String? {
    if (secs < 0) return null
    return if (secs >= 3600) {
        String.format("%02d:%02d:%02d", secs / 3600, secs % 3600 / 60, secs % 60)
    } else {
        String.format("%02d:%02d", secs % 3600 / 60, secs % 60)
    }
}

/**
 * provides a String representation of the given time
 * @return `secs` in hh:mm:ss format
 */
fun formatTimeSecondsVideoDuration(secs: Long): String? {
    if (secs < 0) return null
    return if (secs >= 3600) {
        String.format("%02d:%02d:%02d", secs / 3600, secs % 3600 / 60, secs % 60)
    } else {
        String.format("%d:%02d", secs % 3600 / 60, secs % 60)
    }
}

/**
 * Subtracts the given times that are in hh:mm:ss format and returns the result in hh:mm:ss format
 * @return result in hh:mm:ss format
 */
fun subtractTimes(time1: String?, time2: String?): String? {
    time1 ?: return null
    time2 ?: return null
    var result: String? = null
    try {
        if (time1.length == time2.length) {
            val t1 = time1.split(":").map { it.toInt() }
            val t2 = time2.split(":").map { it.toInt() }
            var c = 0
            for (i in t1.size - 1 downTo 0) {
                var s = t1[i] - t2[i] - c
                if (s < 0) {
                    s += 60
                    c = 1
                } else {
                    c = 0
                }
                if (result?.isNotBlank() == true) {
                    result = "${String.format("%02d", s)}:$result"
                } else {
                    result = String.format("%02d", s)
                }
            }
        }
    } catch (e: Exception) {
    }
    return result
}

/**
 * Returns SimpleDateFormat in standard Wapo time format ("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
 */
fun getDefaultDateFormat(): SimpleDateFormat {
    return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("GMT")
    }
}

/**
 * Converts the given millis to the given SimpleDateFormat format. It uses default SimpleDateFormat
 * when @param dateFormat is not provided.
 * @return String - formatted millis
 */
fun toDateFormat(
    timeMillis: Long?,
    dateFormat: SimpleDateFormat? = getDefaultDateFormat()
): String? {
    timeMillis ?: return null
    dateFormat ?: return null
    return try {
        return dateFormat.format(timeMillis)
    } catch (t: Throwable) {
        null
    }
}

/**
 * Converts the given dateString to millis by using the given format.
 * It uses default getDefaultDateFormat when @param format is not provided.
 * @returns Long - time millis
 */
fun toDateLong(
    dateString: String?,
    format: SimpleDateFormat = getDefaultDateFormat()
): Long {
    if (dateString.isNullOrBlank()) return 0L
    return try {
        format.parse(dateString)?.time ?: 0L
    } catch (e: ParseException) {
        0L
    }
}

/**
 * Converts a datetime string to milliseconds.
 * @return the time in milliseconds.
 */
fun formattedDateToMillis(dateString: String, format: String): Long {
    return SimpleDateFormat(format, Locale.US).parse(dateString)?.time ?: 0L
}

/**
 * Returns true if Date is in the past.
 */
fun isDatePast(date: Date): Boolean {
    val now = Date()
    return date.before(now)
}

/**
 * Converts a Date to a String in "Month Day" format, e.g. "September 21."
 */
fun monthDayFormat(date: Date): String {
    return SimpleDateFormat("MMMM d", Locale.US).format(date)
}

/**
 * Converts a Date to a String in "Month Day, Year" format, e.g. "September 21, 2023"
 */
fun monthDayYearFormat(date: Date): String {
    return SimpleDateFormat("MMMM d, yyyy", Locale.US).format(date)
}

fun getDateInISO8601WithTimezone(date: Date): String {
    return try {
        ISO8601_FORMAT_WITH_TIMEZONE.format(date);
    } catch (_: Exception) {
        "";
    }
}

fun getHourOfDay(): Int {
    val calendar = Calendar.getInstance()
    calendar.time = Date()
    return calendar.get(Calendar.HOUR_OF_DAY)
}

@OptIn(ExperimentalTime::class)
fun secondsToDuration(seconds: Double?): String? {
    seconds ?: return null
    val minutes = Duration.convert(
        seconds,
        DurationUnit.SECONDS,
        DurationUnit.MINUTES
    )
    val seconds = Duration.convert(
        seconds,
        DurationUnit.SECONDS,
        DurationUnit.SECONDS
    )
    val leftoverSeconds = seconds - (minutes.toInt() * 60)
    val formattedMinutes = minutes.toInt().toString()
    val formattedSeconds = if (leftoverSeconds < 10) {
        "0" + leftoverSeconds.toInt().toString()
    } else {
        leftoverSeconds.toInt().toString()
    }
    return "$formattedMinutes:$formattedSeconds"
}

fun convertIsoToMillis(isoString: String): Long? {
    if (isoString.isBlank() || isoString == "null") {
        return null
    }
    val offsetDateTime = OffsetDateTime.parse(isoString)
    val instant = offsetDateTime.toInstant()

    return instant.toEpochMilli()
}

fun getDateISOFormat(date: String): Date? {
    try {
        ISO8601_FORMAT_WITH_TIMEZONE.timeZone = TimeZone.getTimeZone("UTC")
        return ISO8601_FORMAT_WITHOUT_TIMEZONE.parse(
            date
        )
    } catch (e: java.lang.Exception) {
        return null
    }
}


fun getDaysUntilExpiryIfNearExpiration(pastTime: String, thresholdDays: Long, daysUntilExpires: Long): Long? {
    val nowTime = Date().time
    val delta = nowTime - (getDateISOFormat(pastTime)?.time ?: 0L)
    val days = TimeUnit.MILLISECONDS.toDays(delta)
    return if ((daysUntilExpires - days) <= thresholdDays) {
        daysUntilExpires - days
    } else null
}