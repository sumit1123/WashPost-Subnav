/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.tts.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.wapo.flagship.features.tts.domain.TtsNotificationAction
import com.wapo.flagship.features.tts.domain.TtsRepository
import com.wapo.view.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TtsForegroundService : Service() {

    private var notificationBuilder: NotificationCompat.Builder? = null

    @Inject
    lateinit var ttsRepository: TtsRepository

    @Inject
    lateinit var ttsNotificationAction: TtsNotificationAction

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val textShowing = intent?.getStringExtra(TITLE_EXTRA) ?: PLACEHOLDER_TITLE

        if (notificationBuilder == null) {
            notificationBuilder = initializeNotificationBuilder()
        }

        notificationBuilder?.let { notificationBuilderValue ->
            val notification = notificationBuilderValue
                .setContentTitle(textShowing)
                .build()

            if (intent?.action == ACTION_STOP_TTS) {
                ttsRepository.pause()
                stopSelf()
                return START_NOT_STICKY
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }

        return START_NOT_STICKY
    }

    private fun initializeNotificationBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.play_icon_btn)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setDeleteIntent(createCloseIntent())
            .setContentIntent(createContentIntent())
    }

    private fun createCloseIntent(): PendingIntent {
        return PendingIntent.getService(
            this,
            0,
            Intent(this, TtsForegroundService::class.java).apply {
                action = ACTION_STOP_TTS
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createContentIntent(): PendingIntent {
        return PendingIntent.getActivity(
            this,
            0,
            ttsNotificationAction.openIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )

            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(serviceChannel)
        }
    }

    companion object {
        const val TITLE_EXTRA = "TITLE_EXTRA"
        private const val PLACEHOLDER_TITLE = "Reading text..."
        private const val CHANNEL_ID = "tts_playback_channel"
        private const val CHANNEL_NAME = "Text-to-Speech Background Service"
        private const val NOTIFICATION_ID = 888
        const val ACTION_STOP_TTS = "com.wapo.flagship.ACTION_STOP_TTS"
    }
}