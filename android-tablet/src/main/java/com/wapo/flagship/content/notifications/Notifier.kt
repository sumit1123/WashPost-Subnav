package com.wapo.flagship.content.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import com.wapo.android.commons.util.Logger
import androidx.core.app.NotificationCompat
import com.wapo.flagship.IntentHelper
import com.washingtonpost.android.R

class Notifier {
    private val NOTIFICATION_ID_BASIC = 1
    private val GROUP_KEY: String = "GROUP_KEY"
    val TAG = "Notifier"

    fun show(
        context: Context,
        notifications: List<NotificationData>,
    ) {
        val notificationsToShow = notifications.filter { !it.isRead }.reversed()

        if (notificationsToShow.size > 1) {
            showGrouped(notificationsToShow, context)
        } else {
            Logger.d(TAG, "nothing to show")
        }
    }

    fun showGrouped(
        notificationsToShow: List<NotificationData>,
        context: Context,
    ) {
        val summaryStyle =
            NotificationCompat
                .InboxStyle()
                .setSummaryText("${notificationsToShow.size} new alerts")
                .setBigContentTitle("Wash Post")

        notificationsToShow
            .forEach { notification ->
                val topicName = getDisplayName(notification.type!!)
                val sb = SpannableString(topicName + ": " + notification.headline)
                sb.setSpan(
                    StyleSpan(Typeface.BOLD),
                    0,
                    topicName.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
                summaryStyle.addLine(sb)
            }

        val launch =
            IntentHelper.getMainActivityIntent(context).apply {
                action = "ACTION_MY_POST"
            }

        val intent =
            PendingIntent.getActivity(
                context,
                NOTIFICATION_ID_BASIC,
                launch,
                PendingIntent.FLAG_IMMUTABLE,
            )

        val builder =
            NotificationCompat
                .Builder(context)
                .setContentTitle("Wash Post")
                .setContentText("${notificationsToShow.size} new alerts")
                .setSmallIcon(R.drawable.wp_logo_white)
                .setDefaults(android.app.Notification.DEFAULT_ALL)
                .setStyle(summaryStyle)
                .setAutoCancel(true)
                .setGroup(GROUP_KEY)
                .setContentIntent(intent)
                .setGroupSummary(true)
                .setColor(Color.BLACK)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll() // cancel all current notifications
        notificationManager.notify(NOTIFICATION_ID_BASIC, builder.build())
    }

    fun getDisplayName(type: String): String {
        when (type) {
            "breaking-news" -> return "Breaking"
            "the7_briefs" -> return "The 7 Briefing"
            "politics" -> return "Politics"
            "health_and_science" -> return "Health and Science"
            "entertainment" -> return "Entertainment"
            "world" -> return "World"
            "local" -> return "Local"
            "sports" -> return "Sports"
            "business_and_tech" -> return "Business and Tech"
            else -> return "News Alert"
        }
    }
}
