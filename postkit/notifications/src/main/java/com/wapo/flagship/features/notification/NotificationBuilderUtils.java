package com.wapo.flagship.features.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import android.text.TextUtils;

import com.wapo.flagship.content.notifications.NotificationData;
import com.washingtonpost.android.notifications.R;

import java.util.Random;

import static android.content.Context.NOTIFICATION_SERVICE;

public class NotificationBuilderUtils {

    private static final String CHANNEL_ID = "channel_id";
    private static final String CHANNEL_NAME = "channel_name";
    public static final String ADDITIONAL_DATA = "additional_data";
    private static final int REQUEST_CODE_READ = 0;
    private static final int REQUEST_CODE_SAVE = 1;
    public static final String EXTRA_NOTIFICATION_ID = "notification_id";

    public static void displayNotification(Context context, Notification notification) {
        int notificationId = generateNotifId();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        createChannels(notificationManager);
        notificationManager.notify(notificationId, notification);
    }

    public static void createChannels(NotificationManager manager) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel androidChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            androidChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            manager.createNotificationChannel(androidChannel);
        }
    }

    public static Notification buildNotification(
            Context context,
            NotificationBuilderProvider notificationBuilderProvider,
            @NonNull NotificationData parsedNotification,
            NotificationData mappedArticle,
            @Nullable Bitmap bitmap) {
        return buildNotification(context, notificationBuilderProvider, parsedNotification, mappedArticle, bitmap, null, null, null, null);
    }

    public static Notification buildNotification(
            Context context,
            NotificationBuilderProvider notificationBuilderProvider,
            @NonNull NotificationData parsedNotification,
            NotificationData mappedArticle,
            @Nullable Bitmap bitmap,
            @Nullable Bitmap pushIconImage,
            @Nullable Bundle additionalReadData,
            @Nullable Bundle additionalSaveData,
            @Nullable Integer notificationId) {

        if (notificationId == null) notificationId = generateNotifId();

        String title;
        if (!TextUtils.isEmpty(parsedNotification.getHeadline())) {
            title = parsedNotification.getHeadline();
        } else {
            title = getAppName(context);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(parsedNotification.getKicker())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(parsedNotification.getKicker()))
                .setAutoCancel(true);

        if (notificationBuilderProvider != null && notificationBuilderProvider.getPushIcon() != 0) {
            builder.setSmallIcon(notificationBuilderProvider.getPushIcon());
        }

        if (pushIconImage != null) {
            builder.setLargeIcon(pushIconImage);
        } else if (notificationBuilderProvider != null && notificationBuilderProvider.getLargePushIcon() != 0) {
            Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(), notificationBuilderProvider.getLargePushIcon());
            if (largeIcon != null) {
                builder.setLargeIcon(largeIcon);
            }
        }

        if (bitmap != null) {
            NotificationCompat.BigPictureStyle style = new NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap);
            if (mappedArticle != null) {
                if (!TextUtils.isEmpty(mappedArticle.getHeadline())) {
                    style.setBigContentTitle(mappedArticle.getHeadline());
                }
                if (!TextUtils.isEmpty(mappedArticle.getKicker())) {
                    style.setSummaryText(mappedArticle.getKicker());
                }
            } else {
                style.setSummaryText(parsedNotification.getKicker());
            }
            builder.setStyle(style);
        }

        String storyUrl = parsedNotification.getStoryUrl();
        String type = parsedNotification.getType();
        if (notificationBuilderProvider != null) {

            Intent readIntent = new Intent(context, notificationBuilderProvider.getReadArticleReceiver());
            if (additionalReadData != null) {
                readIntent.putExtra(ADDITIONAL_DATA, additionalReadData);
            }
            readIntent.putExtra("url", storyUrl);
            readIntent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);
            if(!TextUtils.isEmpty(type)) {
                readIntent.putExtra("type", type);
            }
            PendingIntent readPendingIntent =
                    PendingIntent.getBroadcast(
                            context,
                            generateNotifId(),
                            readIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            builder.setContentIntent(readPendingIntent);

            if (!TextUtils.isEmpty(storyUrl)) {
                if (notificationBuilderProvider.getReadArticleReceiver() != null) {
                    builder.addAction(notificationBuilderProvider.getReadActionButtonResId(), context.getString(R.string.notification_read), readPendingIntent);
                }

                if (notificationBuilderProvider.getSaveArticleReceiver() != null) {
                    Intent saveIntent = new Intent(context, notificationBuilderProvider.getSaveArticleReceiver());
                    if (additionalSaveData != null) {
                        saveIntent.putExtra(ADDITIONAL_DATA, additionalSaveData);
                    }
                    saveIntent.putExtra("url", storyUrl);
                    saveIntent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);
                    if(!TextUtils.isEmpty(type)) {
                        saveIntent.putExtra("type", type);
                    }
                    PendingIntent savePendingIntent =
                            PendingIntent.getBroadcast(
                                    context,
                                    generateNotifId(),
                                    saveIntent,
                                    PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    builder.addAction(notificationBuilderProvider.getSaveActionButtonResId(), context.getString(R.string.notification_save), savePendingIntent);
                }
            }
        }

        return builder.build();
    }

    private static String getAppName(Context context) {
        try {
            ApplicationInfo applicationInfo = context.getApplicationInfo();
            int stringId = applicationInfo.labelRes;
            if (stringId != 0) {
                return context.getString(stringId);
            }
            return context.getPackageManager().getApplicationLabel(applicationInfo).toString();
        } catch (Throwable e) {
            return "";
        }
    }

    public static int generateNotifId() {
        Random random = new Random(System.currentTimeMillis());
        return random.nextInt(Integer.MAX_VALUE);
    }
}
