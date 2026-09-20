package com.wapo.flagship.features.articles3.views

import android.icu.util.TimeZone.SHORT_GENERIC
import android.os.Build
import android.text.format.DateUtils
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.wapo.android.commons.util.isRecentMinutes
import com.wapo.android.commons.util.isRecentSeconds
import com.wapo.flagship.features.articles3.models.ui.DateUiModel
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.ArticleTextStyles
import com.wpds.theme.wpdsColors
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar.SHORT
import java.util.concurrent.TimeUnit

@Composable
fun DateView(uiModel: DateUiModel) {
    val formattedDate = getFormattedDate(uiModel.content, uiModel.uiStyle) ?: return

    val style = getDateLineStyle(uiModel.uiStyle)
    val textColor = if (uiModel.uiStyle == DateUiStyle.LIVE_UPDATE && isRecentMinutes(uiModel.content, uiModel.recencyThreshold)) {
        wpdsColors.liveUpdateTextColor
    } else {
        style.color
    }

    Text(
        text = formattedDate,
        style = style.copy(color = textColor)
    )
}

@Composable
private fun getDateLineStyle(uiStyle: DateUiStyle): TextStyle {
    return when (uiStyle) {
        DateUiStyle.LIVE_UPDATE -> ArticleTextStyles.DATELINE_LIVE_UPDATE.style
        DateUiStyle.DEFAULT -> ArticleTextStyles.DATELINE.style
    }
}

private val LIVE_UPDATE_TIME_FORMAT =
    SimpleDateFormat("h:mm a", Locale.US).apply {
        val symbols = DateFormatSymbols(Locale.US)
        symbols.amPmStrings = arrayOf("a.m.", "p.m.")
        dateFormatSymbols = symbols
    }

private const val RELATIVE_TIMESTAMP_THRESHOLD = 120L
private const val LIVE_UPDATE_NOW = "Now"

private fun getFormattedDate(
    content: Long?,
    uiStyle: DateUiStyle
): String? {
    if (content == null) return null
    return try {
        when {
            uiStyle == DateUiStyle.LIVE_UPDATE -> {
                if (isRecentMinutes(content, RELATIVE_TIMESTAMP_THRESHOLD)) {
                    if (isRecentSeconds(content, RELATIVE_TIMESTAMP_THRESHOLD)) {
                        LIVE_UPDATE_NOW
                    } else {
                        DateUtils.getRelativeTimeSpanString(content).toString()
                    }
                } else {
                    LIVE_UPDATE_TIME_FORMAT.format(Date(content))
                }
            }
            else -> formatTimestamp(content)
        }
    } catch (e: IllegalArgumentException) {
        null
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }

    val today = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val yesterday = Calendar.getInstance().apply {
        timeInMillis = today.timeInMillis
        add(Calendar.DAY_OF_MONTH, -1)
    }

    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
        diff < TimeUnit.MINUTES.toMillis(60) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)} minutes ago"
        calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) ->
            "Today at ${formatTime(Date(timestamp))} ${getFormattedTimeZone()}"
        calendar.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) ->
            "Yesterday at ${formatTime(Date(timestamp))} ${getFormattedTimeZone()}"
        else -> formatDate(Date(timestamp))
    }
}

private fun formatTime(date: Date): String {
    val formatter = SimpleDateFormat("h:mm a", Locale.US).apply {
        val symbols = DateFormatSymbols(Locale.US)
        symbols.amPmStrings = arrayOf("a.m.", "p.m.")
        dateFormatSymbols = symbols
    }
    return formatter.format(date)
}

private fun formatDate(date: Date): String {
    return SimpleDateFormat("MMMM d, yyyy", Locale.US).format(date)
}

private fun getFormattedTimeZone(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        android.icu.util.TimeZone
            .getDefault()
            .getDisplayName(false, SHORT_GENERIC, Locale.US)
    } else {
        TimeZone.getDefault().getDisplayName(false, SHORT, Locale.US)
    }

enum class DateUiStyle {
    LIVE_UPDATE,
    DEFAULT
}

@Preview(showBackground = true)
@Composable
private fun DateViewDefaultPreview() {
    AndroidClassicTheme {
        DateView(
            uiModel = DateUiModel(
                content = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(3),
                recencyThreshold = null,
                uiStyle = DateUiStyle.DEFAULT
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DateViewJustNowPreview() {
    AndroidClassicTheme {
        DateView(
            uiModel = DateUiModel(
                content = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(30),
                recencyThreshold = null,
                uiStyle = DateUiStyle.DEFAULT
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DateViewLiveUpdateRecentPreview() {
    AndroidClassicTheme {
        DateView(
            uiModel = DateUiModel(
                content = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5),
                recencyThreshold = 120,
                uiStyle = DateUiStyle.LIVE_UPDATE
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DateViewLiveUpdateOlderPreview() {
    AndroidClassicTheme {
        DateView(
            uiModel = DateUiModel(
                content = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(5),
                recencyThreshold = null,
                uiStyle = DateUiStyle.LIVE_UPDATE
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DateViewYesterdayPreview() {
    AndroidClassicTheme {
        DateView(
            uiModel = DateUiModel(
                content = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1),
                recencyThreshold = null,
                uiStyle = DateUiStyle.DEFAULT
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DateViewOldDatePreview() {
    AndroidClassicTheme {
        DateView(
            uiModel = DateUiModel(
                content = 1700000000000L, // Nov 14, 2023
                recencyThreshold = null,
                uiStyle = DateUiStyle.DEFAULT
            )
        )
    }
}
