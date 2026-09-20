/*
 * Copyright (c) 2019. The Washington Post
 */

package com.wapo.android.push;

/**
 * Created by adkinsj on 2019-10-21.
 */
public interface FCMPushNotificationReceiverInterface {
    void onNewToken(String token);
}