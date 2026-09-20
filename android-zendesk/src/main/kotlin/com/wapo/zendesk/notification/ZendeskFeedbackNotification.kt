package com.wapo.zendesk.notification

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.wapo.zendesk.ZendeskApplication
import com.wapo.zendesk.model.ZendeskImage
import java.io.File

object ZendeskFeedbackNotification {

    const val EXTRA_BETA_FEEDBACK = "EXTRA_BETA_FEEDBACK"
    const val SCREENSHOT_FILE_NAME = "feedback-screenshot.jpg"
    const val NOTIFICATION_ID = 1001
    private const val CHANNEL_ID = "Beta"

    fun displayFeedbackNotification(context: Context, iconId: Int, intent: Intent) {
        val app = context.applicationContext
        if (app is ZendeskApplication) {
            if (!app.zendeskProvider.isBeta) {
                return
            }
        }
        createNotificationChannel(context)
        postFeedbackNotification(context, iconId, intent)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Beta feedback"
            val descriptionText = "Beta feedback"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system.
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun postFeedbackNotification(context: Context, iconId: Int, intent: Intent) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val pendingIntent =
            PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(iconId)
            .setContentTitle("Beta feedback")
            .setContentText("Tap to send feedback...")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setSilent(true)
            .setOngoing(true)
        val notification = builder.build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun removeNotification(context: Context) {
        val app = context.applicationContext
        if (app is ZendeskApplication) {
            if (!app.zendeskProvider.isBeta) {
                return
            }
        }
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    fun startFeedbackFlow(activity: Activity, feedbackActivityIntent: Intent) {
        val bitmap = takeScreenshot(activity)
        saveScreenshot(bitmap, activity)
        feedbackActivityIntent.putExtra(EXTRA_BETA_FEEDBACK, true)
        activity.startActivity(feedbackActivityIntent)
    }

    private fun takeScreenshot(activity: Activity): Bitmap {
        val root: View = activity.window.decorView.getRootView()
        val bitmap = Bitmap.createBitmap(
            root.width,
            root.height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        root.draw(canvas)
        return bitmap
    }

    private fun saveScreenshot(bitmap: Bitmap, activity: Activity) {
        File(activity.filesDir, SCREENSHOT_FILE_NAME)
            .outputStream()
            .use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                out.flush()
            }
    }

    /**
     * Creates a ZendeskImage model with pre-filled values pointing to the automatically takes screenshot.
     * Returns null if the screenshot file does not exist
     */
    fun composeBetaZendeskImage(activity: Activity): ZendeskImage? {
        val file = File(activity.filesDir, SCREENSHOT_FILE_NAME)
        if (file.exists()) {
            return ZendeskImage(Uri.fromFile(file).toString(), SCREENSHOT_FILE_NAME, "image/jpeg")
        }
        return null
    }
}