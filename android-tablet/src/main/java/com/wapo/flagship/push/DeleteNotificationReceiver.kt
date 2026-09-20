// Copyright (c) 2018 The Washington Post. All rights reserved.

package com.wapo.flagship.push

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Process
import com.wapo.flagship.AppContext
// import com.wapo.flagship.util.tracking.Measurement

class DeleteNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context?,
        intent: Intent?,
    ) {
        synchronized(this) {
            Thread(
                Runnable {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                    val extras = intent?.extras
                /*
                Comment this for now as per ticket CLASSIC-843
                val data = intent?.data

                if (data != null && extras != null) {
                    val pushUrl = data.toString()
                    val trackingId = extras.get(PushListener.TRACKING_NOTIFICATION_ID)?.toString() ?: ""
                    val headline = extras.get(PushListener.HEADLINE)?.toString() ?: ""
                    val kicker = extras.get(PushListener.KICKER)?.toString() ?: ""
                    val analyticsId = extras.get(PushListener.ANALYTICS_ID)?.toString() ?: ""
                    val pushTimestamp = extras.get(PushListener.NOTIFICATION_TIMESTAMP)?.toString() ?: ""
                    Measurement.trackDismissNotification(trackingId, pushUrl, headline, kicker, analyticsId, pushTimestamp)
                }
                 */

                    val notificationId = extras?.getInt(PushListener.NOTIFICATION_ID, -1) ?: -1

                    if (notificationId != -1) {
                        AppContext.updateActiveNotification(notificationId, false)
                    }
                },
            ).start()
        }
    }
}
