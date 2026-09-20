// Copyright (c) 2018 The Washington Post. All rights reserved.

package com.wapo.flagship.push

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.push.PushNotification
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.network.request.LiveMapImageRequest
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.util.UIUtil
import com.washingtonpost.android.volley.DefaultRetryPolicy
import com.washingtonpost.android.volley.Response

class TabSelectReceiver : BroadcastReceiver() {
    companion object {
        const val TAB_VIEW_ID = "TAB_VIEW_ID"
        const val NOTIFICATION_ID = "NOTIFICATION_ID"
        const val IMAGE_URL = "IMAGE_URL"
        const val PUSH_NOTIFICATION = "PUSH_NOTIFICATION"
    }

    override fun onReceive(
        context: Context?,
        intent: Intent?,
    ) {
        intent?.let {
            it.extras?.let {
                val notifId = it.getInt(NOTIFICATION_ID)
                val imageUrl = it.getString(IMAGE_URL)
                val tabViewId = it.getInt(TAB_VIEW_ID)
                val pushNotificationMsg = it.getString(PUSH_NOTIFICATION)
                val type = object : TypeToken<PushNotification>() {}.type
                val pushNotification = Gson().fromJson<PushNotification>(pushNotificationMsg, type)

                context?.let {
                    val notificationManager =
                        it.applicationContext?.getSystemService(
                            Context.NOTIFICATION_SERVICE,
                        ) as NotificationManager?
                    val listener =
                        Response.Listener<Bitmap> { response ->
                            if (response is Bitmap) {
                                Logger.d(
                                    PushListener.TAG,
                                    "Successfully downloaded image for notification",
                                )
                                val scaledBitmap: Bitmap = UIUtil.scaleBitmap(response, 500, 500)
                                val baseNotificationBuilder =
                                    PushListener.buildNotification(
                                        it,
                                        pushNotification,
                                        notifId,
                                    )
                                PushNotificationUpdater().buildSegmentedNotification(
                                    context,
                                    pushNotification,
                                    notifId,
                                    scaledBitmap,
                                    baseNotificationBuilder,
                                    notificationManager,
                                    tabViewId,
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
                                        set("image_url", imageUrl)
                                    }.run {
                                        RemoteLog.e(context, build())
                                    }
                            }
                        }
                    val liveMapImageRequest =
                        LiveMapImageRequest(
                            imageUrl,
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
    }
}
