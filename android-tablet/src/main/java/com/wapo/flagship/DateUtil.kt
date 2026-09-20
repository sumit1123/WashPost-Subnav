package com.wapo.flagship

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class DateUtil {
    @RequiresApi(Build.VERSION_CODES.O)
    fun formatRelativeDate(isoDateString: String): String {
        return try {
            val zoneId = ZoneId.systemDefault()
            val eventInstant = Instant.parse(isoDateString)
            val eventDate = eventInstant.atZone(zoneId).toLocalDate()
            val today = LocalDate.now(zoneId)

            val daysBetween = ChronoUnit.DAYS.between(eventDate, today)

            when {
                daysBetween == 0L -> "Today"
                daysBetween == 1L -> "Yesterday"
                daysBetween < 7L -> "$daysBetween days ago"
                daysBetween < 30L -> {
                    val weeks = daysBetween / 7
                    if (weeks == 1L) "1 week ago" else "$weeks weeks ago"
                }
                daysBetween < 365L -> {
                    val months = daysBetween / 30
                    if (months == 1L) "1 month ago" else "$months months ago"
                }
                else -> {
                    val years = daysBetween / 365
                    if (years == 1L) "1 year ago" else "$years years ago"
                }
            }
        } catch (e: Exception) {
            "Invalid date"
        }
    }
}