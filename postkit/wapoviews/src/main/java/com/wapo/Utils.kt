package com.wapo

import android.content.Context
import android.util.DisplayMetrics
import com.wapo.android.commons.util.Logger
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.wapo.view.FlowableLayout
import rx.Subscription
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object Utils {
    private val TAG = Utils::class.java.simpleName

    /**
     * Return FLOAT_{OPTION} based on floatPosition recommended
     *
     * @param floatPosition
     * @return
     */
    @JvmStatic
    fun floatPositionToIntValue(floatPosition: String?): Int {
        return if (floatPosition == null || floatPosition.equals("center", ignoreCase = true)) {
            FlowableLayout.FLOAT_NONE
        } else if (floatPosition.equals("left", ignoreCase = true)) {
            FlowableLayout.FLOAT_LEFT
        } else if (floatPosition.equals("right", ignoreCase = true)) {
            FlowableLayout.FLOAT_RIGHT
        } else {
            //If it's unknown, we handle as FLOAT_NONE
            FlowableLayout.FLOAT_NONE
        }
    }

    fun unsubscribe(vararg subs: Subscription?) {
        for (sub in subs) {
            if (sub != null && !sub.isUnsubscribed) {
                sub.unsubscribe()
            }
        }
    }

    fun setNightMode(
        isOn: Boolean,
        @ColorRes onColor: Int,
        @ColorRes offColor: Int,
        vararg items: TextView
    ) {
        for (item in items) {
            item.setTextColor(ContextCompat.getColor(item.context, if (isOn) onColor else offColor))
        }
    }

    fun getDate(dateFormat: String?, millis: Long?): String? {
        var date: String? = null
        try {
            if (millis != null) {
                date = SimpleDateFormat(dateFormat, Locale.getDefault()).format(millis)
            }
        } catch (e: IllegalArgumentException) {
            Logger.d(TAG, "Date format error", e)
        } catch (e: NullPointerException) {
            Logger.d(TAG, "Date error", e)
        }
        return date
    }

    fun convertDpToPixel(dp: Float, context: Context): Float {
        return dp * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }

    fun getAbbreviatedRelativeTime(pastTime: Long): String {
        val nowTime = System.currentTimeMillis()
        val delta = nowTime - pastTime

        if (delta < 0L) return ""
        return when {
            // Less than a minute
            delta < TimeUnit.MINUTES.toMillis(1) -> "Just now"

            // Less than an hour
            delta < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(delta)
                val unit = if (minutes == 1L) "minute" else "minutes"
                "$minutes $unit ago"
            }

            // Less than 24 hours
            delta < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(delta)
                val unit = if (hours == 1L) "hour" else "hours"
                "$hours $unit ago"
            }

            // 24 hours or older: Fallback to absolute date format
            else -> {
                val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.US)
                dateFormat.format(Date(pastTime))
            }
        }
    }

    fun toDateLong(dateString: String?, dateFormat: SimpleDateFormat): Long {
        if (dateString == null) {
            return 0
        }
        try {
            val date = dateFormat.parse(dateString)
            return date?.time ?: 0
        } catch (t: Throwable) {
            return 0
        }
    }

    /**
     * Returns SimpleDateFormat in standard Wapo time format ("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
     */
    fun getDefaultDateFormat(): SimpleDateFormat {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("GMT")
        return dateFormat
    }

    fun isRecentlyUpdated(displayDate: String?): Boolean {
        val date: Long = toDateLong(
            displayDate,
            getDefaultDateFormat()
        )
        val currentTime = System.currentTimeMillis()
        val difference = currentTime - date
        return difference <= 2 * 60 * 60 * 1000 // 2 hours in milliseconds
    }

    fun dateToString(date: Date?): String? {
        if (date == null) {
            return null
        }
        val formatter = getDefaultDateFormat()
        return formatter.format(date)
    }
}