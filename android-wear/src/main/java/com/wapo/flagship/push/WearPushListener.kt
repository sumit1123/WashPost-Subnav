/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */
package com.wapo.flagship.push

import com.wapo.flagship.WearFlagshipApplication
import com.wapo.flagship.WearAppContext
import android.content.Intent
import com.wapo.android.push.PushNotification
import android.text.TextUtils
import android.app.PendingIntent
import android.app.NotificationManager
import android.app.NotificationChannel
import android.content.Context
import com.wapo.android.commons.util.LogUtil
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.wapo.android.push.PushListener
import com.wapo.flagship.features.articles2.utils.UrlUtils
import com.wapo.flagship.features.section.activities.SectionActivity
import com.wapo.flagship.utils.CrashWrapper
import com.washingtonpost.android.R
import java.lang.Exception
import java.lang.NumberFormatException
import java.net.URL

class WearPushListener : PushListener {
    private val context = WearFlagshipApplication.getInstance().applicationContext

    override fun onRegistered(registrationId: String) {
        if (registrationId.isNotEmpty()) {
            LogUtil.d(TAG, "Device registration successful: $registrationId")
            WearAppContext.pushRegistrationId = registrationId
            WearAppContext.checkPushStatus()
        }
    }

    override fun onRegistrationError(errorMessage: String) {
        LogUtil.d(TAG, "Device registration error, errorMessage=$errorMessage")
        WearAppContext.isPushEnabled = false
    }

    override fun onUnRegistered() {
        WearAppContext.isPushEnabled = false
    }

    override fun onMessage(intent: Intent) {}

    override fun onMessage(
        notificationId: Int,
        pushNotification: PushNotification
    ): NotificationCompat.Builder? {
        if (!WearAppContext.isPushEnabled) return null

        val now = System.currentTimeMillis()
        val timestampInMillis = pushNotification.timestamp
        return if (now - timestampInMillis > DEFAULT_NOTIFICATION_TTL) {
            // Notification is more than 48 hours old, do not create.
            null
        } else buildNotification(
            context,
            pushNotification.title,
            pushNotification.headline,
            pushNotification.category,
            pushNotification,
            notificationId
        )
    }

    override fun getAppContext(): Context = context

    override fun clearTopicData() {}

    override fun logError(log: String) {}

    companion object {
        private const val TAG = "WearPushListener"

        private const val DEFAULT_NOTIFICATION_TTL = 1000L * 60L * 60L * 48L
        const val DEFAULT_CHANNEL_ID = "news_alerts"
        const val DEFAULT_CHANNEL_TITLE = "News alerts"

        fun buildNotification(
            context: Context,
            title: String?,
            headline: String?,
            category: String?,
            pushNotification: PushNotification,
            notificationId: Int
        ): NotificationCompat.Builder? {
            val contentAvailable = pushNotification.contentAvailableParam

            if (contentAvailable != null) {
                try {
                    if (contentAvailable.toInt() != 0) return null
                } catch (e: NumberFormatException) {
                    CrashWrapper.sendException(e)
                }
            }

            val applicationContext = WearFlagshipApplication.getInstance().applicationContext
            val articleIntent = (
                    Intent(applicationContext, SectionActivity::class.java).apply {
                        putExtra(SectionActivity.SECTION, "Top Stories")
                        putExtra(
                            SectionActivity.ARTICLE_URL,
                            UrlUtils.getUrlWithoutParameters(pushNotification.url)
                        )
                    })
                .apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }

            val pendingArticleIntent = PendingIntent.getActivity(
                applicationContext,
                notificationId,
                articleIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notificationBuilder = NotificationCompat.Builder(context, DEFAULT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_wapo_logo_white)
                .setContentTitle(if (title.isNullOrEmpty()) "Wash Post" else title)
                .setAutoCancel(false)
                .setContentIntent(pendingArticleIntent)
                .setContentText(if (headline.isNullOrEmpty()) "Breaking news" else headline)
                .setStyle(NotificationCompat.BigTextStyle().bigText(headline))
                .setColor(ContextCompat.getColor(applicationContext, R.color.black))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            val kicker = pushNotification.kicker
            if (kicker != null && kicker != "null") {
                notificationBuilder.setTicker(kicker)
            }

            val notification = notificationBuilder.build()

            // set notification channel
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (notificationManager.getNotificationChannel(DEFAULT_CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    DEFAULT_CHANNEL_ID,
                    DEFAULT_CHANNEL_TITLE,
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                notificationManager.createNotificationChannel(channel)
            }

            notificationManager.notify(notificationId, notification)

            return notificationBuilder
        }

        private fun isValidStoryUrl(url: String): Boolean {
            if (TextUtils.isEmpty(url) || url.equals("null", ignoreCase = true)) {
                return false
            }
            val u: URL = try {
                URL(url)
            } catch (e: Exception) {
                CrashWrapper.sendException(e)
                return false
            }
            try {
                u.toURI()
            } catch (e: Exception) {
                CrashWrapper.sendException(e)
                return false
            }
            return true
        }
    }

}