// Copyright (c) 2018 The Washington Post. All rights reserved.

package com.wapo.flagship.push

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.push.PushNotification
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.AppContext
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.network.request.LiveMapImageRequest
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.util.UIUtil
import com.washingtonpost.android.R
import com.washingtonpost.android.volley.DefaultRetryPolicy
import com.washingtonpost.android.volley.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PushNotificationUpdater : PushUpdaterCallback {
    private var notificationManager: NotificationManager? = null
    private var notificationBuilder: NotificationCompat.Builder? = null
    private var title: String? = null
    private var notifId: Int? = null
    private var context: Context? = null
    private val formatter = SimpleDateFormat("h:mm a", Locale.US)

    override fun init(
        context: Context,
        notificationBuilder: NotificationCompat.Builder?,
        title: String,
        notifId: Int,
    ) {
        this.context = context
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        this.notificationBuilder = notificationBuilder
        this.title = title
        this.notifId = notifId
    }

    override fun onArticleLoadError(
        storyUrl: String?,
        throwable: Throwable?,
    ) {
        context?.run {
            // Log non-network errors only
            val isNetworkError = !AppContextUtils.isConnectingOrConnected()

            if (!isNetworkError) {
                EventLog
                    .Builder()
                    .apply {
                        setMessage("Error fetching push article.")
                        setModule(LogModules.ALERTS)
                        setErrorMessage("${throwable?.message}")
                        setContentUrl(storyUrl)
                    }.run {
                        RemoteLog.e(context, build())
                    }
            }
        }
    }

    override fun onUpdateSegmentedImages(pushNotification: PushNotification) {
        pushNotification.segmentedImages?.let {
            if (pushNotification.segmentedImages.size > 0) {
                val firstImageUrl = it[0].imageURL
                val listener =
                    Response.Listener<Bitmap> { response ->
                        if (response is Bitmap) {
                            Logger.d(
                                PushListener.TAG,
                                "Successfully downloaded image for notification",
                            )
                            val scaledBitmap: Bitmap = UIUtil.scaleBitmap(response, 500, 500)
                            buildSegmentedNotification(
                                context,
                                pushNotification,
                                notifId,
                                scaledBitmap,
                                notificationBuilder,
                                notificationManager,
                                R.id.notification_tab1,
                            )
                        }
                    }
                val errorListener =
                    Response.ErrorListener { error ->
                        Logger.e(PushListener.TAG, "Failed to download image for notification")
                        context?.run {
                            EventLog
                                .Builder()
                                .apply {
                                    setMessage("Error fetching tab push image")
                                    setModule(LogModules.ALERTS)
                                    setErrorMessage(error?.message)
                                    setContentUrl(pushNotification.url)
                                    set("image_url", firstImageUrl)
                                }.run {
                                    RemoteLog.e(context, build())
                                }
                        }
                    }
                val liveMapImageRequest =
                    LiveMapImageRequest(
                        firstImageUrl,
                        listener,
                        0,
                        0,
                        errorListener,
                    )
                liveMapImageRequest.setShouldBypassCache(true)
                liveMapImageRequest.retryPolicy = DefaultRetryPolicy(5000, 2, 2f)
                FlagshipApplication.getInstance()?.requestQueue?.add(liveMapImageRequest)
            }
        }
    }

    /**
     * Update existing notification with tabs and images if available.
     */
    fun buildSegmentedNotification(
        context: Context?,
        pushNotification: PushNotification,
        notifId: Int?,
        bitmap: Bitmap,
        notificationBuilder: NotificationCompat.Builder?,
        notificationManager: NotificationManager?,
        selectedTabId: Int,
    ) {
        if (context != null && notifId != null && notificationBuilder != null && notificationManager != null) {
            val numTabs = Math.min(3, pushNotification.segmentedImages.size)
            val tabViewIds =
                arrayOf(
                    R.id.notification_tab1,
                    R.id.notification_tab2,
                    R.id.notification_tab3,
                )
            val expandedRemoteView = RemoteViews(context.packageName, R.layout.election_alert)
            for (i in 0 until numTabs) {
                val intent = Intent(context, TabSelectReceiver::class.java)
                intent.putExtra(TabSelectReceiver.TAB_VIEW_ID, tabViewIds[i])
                intent.putExtra(TabSelectReceiver.NOTIFICATION_ID, notifId)
                intent.putExtra(
                    TabSelectReceiver.IMAGE_URL,
                    pushNotification.segmentedImages[i].imageURL,
                )
                intent.putExtra(
                    TabSelectReceiver.PUSH_NOTIFICATION,
                    Gson().toJson(pushNotification),
                )
                expandedRemoteView.setViewVisibility(tabViewIds[i], View.VISIBLE)
                expandedRemoteView.setTextColor(
                    tabViewIds[i],
                    ContextCompat.getColor(
                        context,
                        if (selectedTabId == tabViewIds[i]) com.wapo.view.R.color.post_blue else R.color.dark_gray,
                    ),
                )
                expandedRemoteView.setTextViewText(
                    tabViewIds[i],
                    pushNotification.segmentedImages[i].name,
                )
                if (numTabs > 1) {
                    val pendingIntent =
                        PendingIntent.getBroadcast(
                            context,
                            tabViewIds[i],
                            intent,
                            FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        )
                    expandedRemoteView.setOnClickPendingIntent(tabViewIds[i], pendingIntent)
                    expandedRemoteView.setTextViewCompoundDrawables(
                        tabViewIds[i],
                        0,
                        0,
                        0,
                        if (selectedTabId == tabViewIds[i]) R.drawable.expanded_notification_tab_indicator_selected else 0,
                    )
                }
            }

            // set selected tab image
            expandedRemoteView.setImageViewBitmap(R.id.notification_tab_image, bitmap)
            expandedRemoteView.setViewVisibility(R.id.notification_tab_image, View.VISIBLE)
            expandedRemoteView.setTextViewText(
                R.id.last_updated_timestamp,
                getTimeToBeDisplayed(System.currentTimeMillis(), formatter),
            )
            expandedRemoteView.setViewVisibility(R.id.last_updated_timestamp, View.VISIBLE)
            expandedRemoteView.setTextViewText(
                R.id.expanded_notification_title,
                if (pushNotification.title.isEmpty()) "Wash Post" else pushNotification.title,
            )
            expandedRemoteView.setTextViewText(
                R.id.expanded_notification_text,
                pushNotification.headline,
            )
            notificationBuilder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
            if (!title.isNullOrEmpty()) notificationBuilder.setSubText(title)
            notificationBuilder.setCustomBigContentView(expandedRemoteView)
            if (AppContext.isNotificationActive(notifId)) {
                notificationManager.notify(notifId, notificationBuilder.build())
            }
        }
    }

    private fun getTimeToBeDisplayed(
        time: Long,
        format: SimpleDateFormat,
    ): String? {
        var timeInMillis = time
        var daysArray =
            arrayOf(
                "Sunday",
                "Monday",
                "Tuesday",
                "Wednesday",
                "Thursday",
                "Friday",
                "Saturday",
            )

        var calToday = Calendar.getInstance()
        var calNotificationDay = Calendar.getInstance()
        calNotificationDay.timeInMillis = timeInMillis

        var calTodayDayOfWeek = calToday.get(Calendar.DAY_OF_WEEK)
        var calNotificationDayOfWeek = calNotificationDay.get(Calendar.DAY_OF_WEEK)

        if (calTodayDayOfWeek == calNotificationDayOfWeek) {
            return "Last updated at ${format.format(calNotificationDay.time)}"
        } else if (calTodayDayOfWeek - calNotificationDayOfWeek == 1 || (calTodayDayOfWeek - calNotificationDayOfWeek + 7) == 1) {
            return "Last updated yesterday"
        } else if (calTodayDayOfWeek - calNotificationDayOfWeek == 2 || (calTodayDayOfWeek - calNotificationDayOfWeek + 7) == 2) {
            return "Last updated on ${daysArray[calNotificationDayOfWeek - 1]}"
        }
        return null
    }
}

interface PushUpdaterCallback {
    fun init(
        context: Context,
        notificationBuilder: NotificationCompat.Builder?,
        title: String,
        notifId: Int,
    )

    fun onArticleLoadError(
        storyUrl: String?,
        throwable: Throwable?,
    )

    fun onUpdateSegmentedImages(pushNotification: PushNotification)
}
