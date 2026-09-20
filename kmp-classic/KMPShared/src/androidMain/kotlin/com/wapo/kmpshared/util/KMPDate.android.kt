package com.wapo.kmpshared.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

actual typealias KMPDate = Date

private val threadLocalIsoFormats =
    object : ThreadLocal<List<SimpleDateFormat>>() {
        override fun initialValue(): List<SimpleDateFormat> =
            listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
            ).map { pattern ->
                SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                    isLenient = false
                }
            }
    }

actual fun createKMPDateFromISO8601(isoString: String): KMPDate? {
    // Safely gets the thread-isolated list copies
    val isoFormats = threadLocalIsoFormats.get() ?: return null

    for (format in isoFormats) {
        try {
            return format.parse(isoString)
        } catch (_: Exception) {
            // Keep trying the next format
        }
    }
    return null
}
