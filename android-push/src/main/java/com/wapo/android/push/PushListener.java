/*
 *
 *  *  Copyright (c) 2018. The Washington Post. All rights reserved.
 *
 */

package com.wapo.android.push;

import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

public interface PushListener {

    void onRegistered(String registrationId);

    void onRegistrationError(String errorMessage);

    void onUnRegistered();

    void onMessage(Intent intent);

    NotificationCompat.Builder onMessage(int notificationId, PushNotification pushNotification);

    Context getAppContext();

    void logError(String log);
}
