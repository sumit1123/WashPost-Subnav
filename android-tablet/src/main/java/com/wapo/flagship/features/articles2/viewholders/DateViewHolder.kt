package com.wapo.flagship.features.articles2.viewholders

import android.content.Context
import android.icu.util.TimeZone.SHORT_GENERIC
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.format.DateUtils
import android.text.style.ForegroundColorSpan
import com.wapo.android.commons.util.Logger
import android.view.View
import androidx.core.content.ContextCompat
import com.wapo.android.commons.util.isRecentMinutes
import com.wapo.android.commons.util.isRecentSeconds
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.models.deserialized.Date
import com.wapo.flagship.features.articles2.utils.DateLineStyleHelper
import com.wapo.flagship.features.articles2.utils.KeyHelper
import com.wapo.flagship.features.articles2.utils.StylesHelper
import com.wapo.text.WpTextAppearanceSpan
import com.wapo.text.WpTextFormatter
import com.washingtonpost.android.databinding.ItemDateBinding
import com.washingtonpost.android.sections.R
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar.SHORT
import java.util.concurrent.TimeUnit

class DateViewHolder(
    private val binding: ItemDateBinding,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<Date>(binding.root) {
    var isLiveUpdate = false
    var isLiveReporterInsight = false
    var isExpandedByline = false

    override fun bind(
        item: Date,
        position: Int,
    ) {
        val context = itemView.context
        val formattedPublishedDate = getDate(context, item.content, item.subtype)
        isLiveUpdate = item.subtype == Date.SubType.LIVE_UPDATE.value ||
            item.subtype == Date.SubType.LIVE_REPORTER_INSIGHT.value
        isLiveReporterInsight = item.subtype == Date.SubType.LIVE_REPORTER_INSIGHT.value
        val textDateLineStyle =
            if (isLiveUpdate) {
                DateLineStyleHelper.getDateLineLiveUpdateStyle(binding.root.context)
            } else {
                DateLineStyleHelper.getDateLineStyle(binding.root.context)
            }
        val dateLineText = SpannableStringBuilder()
        if (formattedPublishedDate != null) {
            dateLineText.append(formattedPublishedDate)
            dateLineText.setSpan(
                WpTextAppearanceSpan(context, textDateLineStyle),
                0,
                dateLineText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            if (isLiveUpdate && isRecentMinutes(item.content, item.recencyThreshold)) {
                dateLineText.setSpan(
                    ForegroundColorSpan(
                        ContextCompat.getColor(context, R.color.live_update_recency_threshold),
                    ),
                    0,
                    dateLineText.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
            WpTextFormatter.applyLineSpacing(binding.articleHeadingDateline, textDateLineStyle)
            binding.articleHeadingDateline.text = dateLineText
            binding.articleHeadingDateline.visibility = View.VISIBLE
            binding.articleHeadingDateline.key =
                KeyHelper.createKey(position, dateLineText.toString())
        } else {
            binding.articleHeadingDateline.visibility = View.GONE
        }
        isExpandedByline = item.subtype == Date.SubType.EXPANDED_BYLINE.value
    }

    companion object {
        private val LIVE_UPDATE_TIME_FORMAT_STRING =
            SimpleDateFormat("h:mm a", Locale.US).apply {
                formatAmPmStrings(this, arrayOf("a.m.", "p.m."))
            }

        private const val RELATIVE_TIMESTAMP_THRESHOLD = 120L
        private const val LIVE_UPDATE_NOW = "Now"

        /**
         * The default is "AM" and "PM", this function replaces them with custom values
         */
        private fun formatAmPmStrings(
            format: SimpleDateFormat,
            newStrings: Array<String>,
        ) {
            val symbols = DateFormatSymbols(Locale.US)
            symbols.amPmStrings = newStrings
            format.dateFormatSymbols = symbols
        }

        private val TAG = DateViewHolder::class.java.simpleName

        @JvmStatic
        fun getDate(
            context: Context,
            content: Long?,
            subType: String?,
        ): String? {
            val isLiveUpdate =
                subType == Date.SubType.LIVE_UPDATE.value ||
                    subType == Date.SubType.LIVE_REPORTER_INSIGHT.value
            var date: String? = null
            try {
                if (content != null) {
                    date =
                        when {
                            isLiveUpdate -> {
                                if (isRecentMinutes(content, RELATIVE_TIMESTAMP_THRESHOLD)) {
                                    if (isRecentSeconds(content, RELATIVE_TIMESTAMP_THRESHOLD)) {
                                        LIVE_UPDATE_NOW
                                    } else {
                                        DateUtils
                                            .getRelativeTimeSpanString(content)
                                            .toString()
                                    }
                                } else {
                                    LIVE_UPDATE_TIME_FORMAT_STRING.format(content)
                                }
                            }
                            else -> {
                                formatTimestamp(content)
                            }
                        }
                }
            } catch (e: IllegalArgumentException) {
                Logger.d(TAG, "Date format error", e)
            }
            return if (date != null &&
                StylesHelper.isAllCaps(DateLineStyleHelper.getDateLineStyle(context), context)
            ) {
                date.uppercase(Locale.getDefault())
            } else {
                date
            }
        }

        private fun getFormattedTimeZone(): String =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                android.icu.util.TimeZone
                    .getDefault()
                    .getDisplayName(false, SHORT_GENERIC, Locale.US)
            } else {
                TimeZone.getDefault().getDisplayName(false, SHORT, Locale.US)
            }

        fun formatTimestamp(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            val calendar = Calendar.getInstance()
            calendar.timeInMillis = timestamp

            val today = Calendar.getInstance()
            today.timeInMillis = now
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)

            val yesterday = Calendar.getInstance()
            yesterday.timeInMillis = today.timeInMillis
            yesterday.add(Calendar.DAY_OF_MONTH, -1)

            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "just now"
                diff < TimeUnit.MINUTES.toMillis(60) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)} minutes ago"
                calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Today at ${formatTime(Date(timestamp))} ${getFormattedTimeZone()}"
                calendar.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> "Yesterday at ${formatTime(Date(timestamp))} ${getFormattedTimeZone()}"
                else -> formatDate(Date(timestamp))
            }
        }


        private fun formatTime(date: java.util.Date): String {
            val formatter =
                SimpleDateFormat("h:mm a", Locale.US).apply {
                    formatAmPmStrings(this, arrayOf("a.m.", "p.m."))
                }
            return formatter.format(date)
        }

        private fun formatDate(date: java.util.Date): String {
            val formatter = SimpleDateFormat("MMMM d, yyyy", Locale.US)
            return formatter.format(date)
        }
    }
}
