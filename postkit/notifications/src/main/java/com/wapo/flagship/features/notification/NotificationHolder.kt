package com.wapo.flagship.features.notification

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.wapo.flagship.content.notifications.NotificationModel
import com.wapo.view.ImageStreamModule
import com.wapo.view.StreamModuleView
import com.washingtonpost.android.notifications.R
import com.washingtonpost.android.volley.toolbox.AnimatedImageLoader
import java.text.SimpleDateFormat
import java.util.*

class NotificationHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var panel = itemView.findViewById<StreamModuleView>(R.id.sf_module_text_panel)

    fun bind(notification: NotificationModel, itemClickListener: NotificationClickListener?, format: SimpleDateFormat, nightMode: Boolean) {
        panel.isNightMode = nightMode
        panel.setBlurb(notification.title)
        var timeString = getTimeToBeDisplayed(notification.time.time, format, itemView.context)
        timeString?.let {
            panel.setTime(timeString)
        }
        panel.setSectionType(notification.notificationData.type)
        val bgColor = getNotificationItemBgColorRes(nightMode, notification.notificationData.isRead)
        itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, bgColor))
        itemView.setOnClickListener {
            itemClickListener?.onNotificationClick(notification)
        }
    }
}

class NotificationItemImageHolder(itemView: View, private val imageLoader: AnimatedImageLoader) : RecyclerView.ViewHolder(itemView) {

    var panel = itemView.findViewById<ImageStreamModule>(R.id.sf_module_phone_panel)
    val DEFAULT_ASPECT_RATIO = 1.60001063f


    fun bind(notification: NotificationModel, itemClickListener: NotificationClickListener?, format: SimpleDateFormat, nightMode: Boolean) {
        panel.isNightMode = nightMode
        panel.setBlurb(notification.title)
//        panel.setHeadline(notification.title)
        var timeString = getTimeToBeDisplayed(notification.time.time, format, itemView.context)
        timeString?.let {
            panel.setTime(timeString)
        }
        panel.setSectionType(notification.notificationData.type)
        panel.setAspectRatio(DEFAULT_ASPECT_RATIO)
        panel.disableAnimatedSpinner()
        panel.setImageUrl(notification.imageUrl, imageLoader, false)
        val bgColor = getNotificationItemBgColorRes(nightMode, notification.notificationData.isRead)
        itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, bgColor))
        itemView.setOnClickListener {
            itemClickListener?.onNotificationClick(notification)
        }
    }

}

class NotificationHeaderHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

class NotificationNothingHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val textView = itemView.findViewById<TextView>(R.id.text)

    fun bind(nightMode: Boolean) {
        textView.setTextColor(itemView.context.resources.getColor(getTextColorRes(nightMode)))
    }
}

class NotificationFooterHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(notificationFooterClickListener: NotificationFooterClickListener?, nightMode: Boolean) {
        val footer_text = itemView.findViewById<TextView>(R.id.notification_footer_text)
        footer_text.text = itemView.context.resources.getText(R.string.edit_your_settings)
        footer_text.setTextColor(itemView.context.resources.getColor(getTextColorRes(nightMode)))
        itemView.setOnClickListener {
            notificationFooterClickListener?.onNotificationFooterClicked()
        }
    }
}

/**
 * As for the timestamps attached to alerts, rules should be as follows:
If an alert was sent more recently then midnight, it should read "Sent at [time]" (ie, "Sent at 5:45AM")
If an alert was sent yesterday, it should read "Sent Yesterday at [time]" (ie, "Sent Yesterday at 11:50PM")
If an alert was sent more recently than 48 hours, but from the day before yesterday, it should read "Sent [day of week] at [time]" (ie, "Sent Monday at 9:00PM")

 */
fun getTimeToBeDisplayed(time : Long, format: SimpleDateFormat, context: Context) : String? {
    var timeInMillis = time
    var daysArray = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    var calToday = Calendar.getInstance()
    var calNotificationDay = Calendar.getInstance()
    calNotificationDay.timeInMillis = timeInMillis

    var calTodayDayOfWeek = calToday.get(Calendar.DAY_OF_WEEK)
    var calNotificationDayOfWeek = calNotificationDay.get(Calendar.DAY_OF_WEEK)

    if (calTodayDayOfWeek == calNotificationDayOfWeek) {
        return "${context.getString(R.string.notification_sent_at)} ${format.format(calNotificationDay.time)}"
    } else if (calTodayDayOfWeek - calNotificationDayOfWeek == 1 || (calTodayDayOfWeek - calNotificationDayOfWeek + 7) == 1) {
        return "${context.getString(R.string.notification_sent_yesterday)} ${format.format(calNotificationDay.time)}"
    } else if (calTodayDayOfWeek - calNotificationDayOfWeek == 2 || (calTodayDayOfWeek - calNotificationDayOfWeek + 7) == 2) {
        return "${context.getString(R.string.notification_sent)} ${daysArray[calNotificationDayOfWeek -1]} at ${format.format(calNotificationDay.time)}"
    }
    return null
}