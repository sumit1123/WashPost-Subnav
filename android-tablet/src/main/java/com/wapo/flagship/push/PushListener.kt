/*
 * Copyright (C) 2015 . The Washington Post. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wapo.flagship.push

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import com.wapo.android.commons.util.Logger
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.push.PushNotification
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.AppContext
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.IntentHelper
import com.wapo.flagship.content.notifications.NotificationData
import com.wapo.flagship.features.articles2.activities.ARTICLES_URL_PARAM
import com.wapo.flagship.features.articles2.activities.BREAKING_NEWS_ORIGINATED
import com.wapo.flagship.features.articles2.activities.OPINION_PUSH_ORIGINATED
import com.wapo.flagship.features.articles2.activities.PUSH_HEADLINE
import com.wapo.flagship.features.articles2.activities.PUSH_ORIGINATED
import com.wapo.flagship.features.shared.activities.BaseActivity
import com.wapo.flagship.features.support.ABTests.DAILY_READ
import com.wapo.flagship.util.PrefUtils
import com.wapo.flagship.wapomain.MainConstants.ACTION_PRINT_EDITION
import com.wapo.flagship.wapomain.MainConstants.ACTION_TOP_STORIES
import com.washingtonpost.android.R
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Created by muppallav on 2/18/15.
 */

class PushListener(val onMessageReceived:((PushNotification?))-> Unit = {}) : com.wapo.android.push.PushListener {
    private val context = FlagshipApplication.getInstance().applicationContext

    private val currentDate: String
        private get() {
            val s = SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS")
            return s.format(Date())
        }

    override fun onRegistered(registrationId: String) {
        EventLog
            .Builder()
            .apply {
                setMessage("Push Registration Successful")
                setModule(LogModules.ALERTS)
                set("device_token", registrationId)
            }.run {
                RemoteLog.d(context, build())
            }
        if (registrationId.isNotEmpty()) {
            AppContext.saveRegistrationId(registrationId)
        }
    }

    override fun onRegistrationError(errorMessage: String) {
        EventLog
            .Builder()
            .apply {
                setMessage("Push Device Registration Error")
                setModule(LogModules.ALERTS)
                setErrorMessage(errorMessage)
                set("device_token", AppContext.getRegistrationId())
            }.run {
                RemoteLog.e(context, build())
            }
    }

    override fun onUnRegistered() {
        EventLog
            .Builder()
            .apply {
                setMessage("Push Device Registered")
                setModule(LogModules.ALERTS)
                set("device_token", AppContext.getRegistrationId())
            }.run {
                RemoteLog.d(context, build())
            }
    }

    override fun onMessage(intent: Intent) {
        // direct messages from FCM and ADM receivers.

    }

    override fun onMessage(
        notificationId: Int,
        pushNotification: PushNotification?,
    ): NotificationCompat.Builder? {
        var builder: NotificationCompat.Builder? = null
        try {
            pushNotification?.let { n ->
                // if topic is not enabled in the preferences, do not create.
                if (!isTopicEnabled(n.type)) {
                    val logMessage =
                        String.format(
                            "Topic(%s) is not enabled in device but received push from Airship. Just ignore push and return.",
                            n.type
                                ?: "null",
                        )
                    Logger.w(TAG, logMessage)
                    EventLog
                        .Builder()
                        .apply {
                            setMessage("Topic is not enabled but received a push message.")
                            setModule(LogModules.ALERTS)
                            set("time_received", currentDate)
                            set("device_token", AppContext.getRegistrationId())
                            set("story_url", n.url)
                            set("topic", n.type)
                        }.run {
                            RemoteLog.e(context, build())
                        }
                    return null
                }

                // Extract and save the testGroup value if available
                   setDailyReadTestGroup(n)


            // if notification is more than 48 hours old, do not create.
                val now = System.currentTimeMillis()
                if (now - n.timestamp > DEFAULT_NOTIFICATION_TTL) {
                    return null
                }

                // set notification channel
                val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (mNotificationManager.getNotificationChannel(DEFAULT_CHANNEL_ID) == null) {
                        val channel =
                            NotificationChannel(
                                DEFAULT_CHANNEL_ID,
                                DEFAULT_CHANNEL_TITLE,
                                NotificationManager.IMPORTANCE_DEFAULT,
                            )
                        mNotificationManager.createNotificationChannel(channel)
                    }
                }

                // build notification
                builder =
                    buildNotification(context, n, notificationId)?.apply {
                        mNotificationManager.notify(notificationId, build())
                        AppContext.updateActiveNotification(notificationId, true)
                    }

                if (n.type == null || !isTodaysPaperTopic(n.type)) {
                    val model =
                        NotificationData(null).apply {
                            notifId = notificationId.toString()
                            headline = n.headline
                            storyUrl = n.url
                            type = n.type
                            kicker = n.title
                            // n.timestamp is in millis. Convert millis to seconds for backward compatibility.
                            timestamp = (n.timestamp / 1000).toString()
                        }
                    val pushUpdaterCallback =
                        PushNotificationUpdater().apply {
                            init(context, builder, n.title, notificationId)
                            if (n.interactionType == PushNotification.InteractionType.SEGMENTED.name) {
                                onUpdateSegmentedImages(n)
                            }
                        }

                    val contentManager = FlagshipApplication.getInstance().contentManager
                    contentManager
                        .deleteOldNotification()
                        .cast(MutableList::class.java)
                        .concatWith(contentManager.addNotification(model, pushUpdaterCallback))
                        .subscribe(
                            { /* onNext NoOp */ },
                            { /* onError NoOp */ },
                            { /* onCompleted NoOp */ },
                        )
                }
            }
        } catch (e: Exception) {
            EventLog
                .Builder()
                .apply {
                    setMessage("Push Processing Error")
                    setModule(LogModules.ALERTS)
                    setErrorMessage(e.message)
                    setForceUpload()
                }.run {
                    RemoteLog.e(context, build())
                }
        } finally {
            //Message Received
            onMessageReceived.invoke(pushNotification)

            EventLog
                .Builder()
                .apply {
                    setMessage("Received a Push Message")
                    setModule(LogModules.ALERTS)
                    set("timestamp", pushNotification?.timestamp)
                    set("time_received", currentDate)
                    set("device_token", AppContext.getRegistrationId())
                    set("story_url", pushNotification?.url)
                    set("topic", pushNotification?.type)
                    set("identity_uuid", AppContext.getAirshipNamedUserId(context))
                    set("testGroups", pushNotification?.testGroups)
                    setForceUpload()
                }.run {
                    RemoteLog.d(context, build())
                }
        }
        return builder
    }


    private fun setDailyReadTestGroup(n: PushNotification) {
        val newValue = n.testGroups?.get(DAILY_READ) ?: return Logger.d(TAG,
            "No DAILY_READ testGroup found, skipping update."
        )

        val map = PrefUtils.getABParametersMap(context)
        if (map[DAILY_READ] == newValue) return Logger.d(TAG, "DAILY_READ testGroup unchanged, skipping update.")

        map[DAILY_READ] = newValue
        PrefUtils.saveABParametersMap(context, map)
        Logger.i(TAG, "Updated DAILY_READ testGroup to: $newValue")
    }

    override fun getAppContext(): Context = context

    override fun logError(log: String) {
        Logger.e(TAG, log)
        EventLog
            .Builder()
            .apply {
                setMessage("Push Notification Error")
                setModule(LogModules.ALERTS)
                setErrorMessage(log)
            }.run {
                RemoteLog.e(context, build())
            }
    }

    companion object {
        const val TAG = "PushListener"
        val DEFAULT_CHANNEL_ID =
            FlagshipApplication.getInstance().getString(
                R.string.notification_channel_id,
            )
        const val DEFAULT_CHANNEL_TITLE = "News alerts"
        const val DEFAULT_TITLE = "News alert"
        const val ANALYTICS_ID = "AnalyticsId"
        const val TRACKING_NOTIFICATION_ID = "TrackingNotificationId"
        const val NOTIFICATION_ID = "NotificationId"
        const val HEADLINE = "Headline"
        const val KICKER = "Kicker"
        const val NOTIFICATION_TIMESTAMP = "NotificationTimestamp"
        private const val DEFAULT_NOTIFICATION_TTL = 1000L * 60L * 60L * 48L
        const val OPINION_TOPIC_KEY = "opinions"
        const val TODAY_PAPER_TOPIC_NAME = "todays_paper"
        const val PREFS_NAME = "notification_prefs_name"

        fun buildNotification(
            context: Context,
            pushNotification: PushNotification,
            notificationId: Int,
        ): NotificationCompat.Builder? {
            var title = pushNotification.title
            var headline = pushNotification.headline
            val category = pushNotification.category
            val type = pushNotification.type
            val storyUrl = pushNotification.url
            val trackingId = pushNotification.pushID
            val analyticsId = pushNotification.analyticsID
            val pushTimestamp = pushNotification.timestamp.toString()
            val kicker = pushNotification.kicker

            val launch: Intent
            val shareArticle: Intent

            val deletePush =
                Intent(context, DeleteNotificationReceiver::class.java).apply {
                    action = FlagshipApplication.ACTION_DELETE
                    putExtra(NOTIFICATION_ID, notificationId)
                }
            val mBuilder =
                NotificationCompat
                    .Builder(context, DEFAULT_CHANNEL_ID)
                    .setSmallIcon(R.drawable.wp_logo_white)
                    .setAutoCancel(true)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(headline))
                    .setColor(
                        ContextCompat.getColor(
                            FlagshipApplication.getInstance().applicationContext,
                            R.color.notification_icon_background,
                        ),
                    )
            if (!title.isNullOrEmpty()) {
                mBuilder.setContentTitle(title)
            } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
                mBuilder.setContentTitle(DEFAULT_TITLE)
            }
            if (!headline.isNullOrEmpty()) {
                mBuilder.setContentText(headline)
            }
            if (isTodaysPaperTopic(type)) {
                if (!FlagshipApplication.getInstance().isApplicationInBackground &&
                    FlagshipApplication.getInstance().isActivityPrintRelated
                ) {
                    return null
                }
                launch =
                    IntentHelper.getDeepLinkDelegatorActivityIntent(context).apply {
                        action = ACTION_PRINT_EDITION
                        putExtra(PUSH_ORIGINATED, true)
                    }
            } else if (isValidStoryUrl(storyUrl)) {
                deletePush.data = Uri.parse(storyUrl)
                // second condition to be removed later

                launch =
                    IntentHelper.getDeepLinkDelegatorActivityIntent(context).apply {
                        data = Uri.parse(storyUrl)
                        putExtra(ARTICLES_URL_PARAM, storyUrl)
                        putExtra(BREAKING_NEWS_ORIGINATED, true)
                        putExtra(PUSH_ORIGINATED, true)
                        putExtra(
                            OPINION_PUSH_ORIGINATED,
                            TextUtils.equals(pushNotification.type, OPINION_TOPIC_KEY),
                        )
                        putExtra(PUSH_HEADLINE, headline)
                    }
                if (TextUtils.equals(
                        pushNotification.interactionType,
                        PushNotification.InteractionType.DEFAULT.name,
                    ) ||
                    TextUtils.equals(
                        pushNotification.interactionType,
                        PushNotification.InteractionType.BREAKING_NEWS.name,
                    )
                ) {
                    shareArticle =
                        Intent(launch).apply {
                            putExtras(launch)
                            action = FlagshipApplication.ACTION_SHARE
                        }
                    val shareIntent =
                        PendingIntent.getActivity(
                            context,
                            notificationId,
                            shareArticle,
                            PendingIntent.FLAG_IMMUTABLE,
                        )
                    mBuilder.addAction(R.drawable.empty, "Share", shareIntent)
                }
            } else if (type == "personalized_podcast"){
                launch =
                    IntentHelper.getDeepLinkDelegatorActivityIntent(context).apply {
                        action = ACTION_TOP_STORIES
                        putExtra(PUSH_ORIGINATED, true)
                 }

            } else {
                launch = Intent(context, BaseActivity::class.java)
                launch.action = "ACTION_MAIN_SCREEN"
                launch.putExtra(PUSH_ORIGINATED, true)
            }
            launch.apply {
                putExtra(NOTIFICATION_ID, notificationId)
                putExtra(TRACKING_NOTIFICATION_ID, trackingId)
                putExtra(HEADLINE, headline)
                putExtra(KICKER, title)
                putExtra(ANALYTICS_ID, analyticsId)
                putExtra(NOTIFICATION_TIMESTAMP, pushTimestamp)
            }
            deletePush.apply {
                putExtra(NOTIFICATION_ID, notificationId)
                putExtra(TRACKING_NOTIFICATION_ID, trackingId)
                putExtra(HEADLINE, headline)
                putExtra(KICKER, title)
                putExtra(ANALYTICS_ID, analyticsId)
                putExtra(NOTIFICATION_TIMESTAMP, pushTimestamp)
            }
            val readIntent =
                PendingIntent.getActivity(
                    context,
                    notificationId,
                    launch,
                    PendingIntent.FLAG_IMMUTABLE,
                )
            val deleteIntent =
                PendingIntent.getBroadcast(
                    context,
                    notificationId,
                    deletePush,
                    PendingIntent.FLAG_IMMUTABLE,
                )

            mBuilder.apply {
                setContentIntent(readIntent)
                setDeleteIntent(deleteIntent)
                if (isTodaysPaperTopic(type)) {
                    setDefaults(0)
                }
                if (!kicker.isNullOrEmpty() && kicker != "null") {
                    setTicker(kicker)
                }
                if (Build.VERSION.SDK_INT >= 21) {
                    setVisibility(Notification.VISIBILITY_PUBLIC)
                }
                setOnlyAlertOnce(true) // no sound for updates
            }

            return mBuilder
        }

        private fun isValidStoryUrl(url: String): Boolean {
            if (TextUtils.isEmpty(url) || url.equals("null", ignoreCase = true)) {
                return false
            }
            val u: URL =
                try {
                    URL(url)
                } catch (e: Exception) {
                    return false
                }
            try {
                u.toURI()
            } catch (e: Exception) {
                return false
            }
            return true
        }

        private fun isTodaysPaperTopic(type: String): Boolean = type == TODAY_PAPER_TOPIC_NAME

        private fun isTopicEnabled(topicKey: String?): Boolean =
            if (TextUtils.isEmpty(topicKey)) {
                false
            } else {
                AppContext.isTopicEnabled(topicKey)
            }

    }
}
