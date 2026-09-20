package com.wapo.flagship.features.notification;

import com.wapo.flagship.content.notifications.NotificationData;
import com.wapo.flagship.content.notifications.NotificationModel;

import java.util.List;

import rx.Observable;

public interface AlertManager {
    Observable<List<NotificationData>> getRecentNotifications();
    Observable<NotificationModel> getNotificationModel(final NotificationData notificationData);
    Observable<Void> readNotifications(final List<NotificationData> notifications);
    Observable<Void> deleteNotification(final NotificationData notification);
    Observable<Void> clearAllNotifications();
}
