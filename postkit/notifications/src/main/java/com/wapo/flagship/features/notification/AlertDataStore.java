package com.wapo.flagship.features.notification;

import com.wapo.flagship.content.notifications.NotificationData;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface AlertDataStore {

    boolean isAlertEnabled();

    void setAlertEnabled(boolean enabled);

    Date strToDate(String timestampStr);

    List<NotificationData> getAll();

    void addData(NotificationData data);

    void deleteData(NotificationData data);

    void updateData(Map<String, NotificationData> map);

    void clearAll();

}
