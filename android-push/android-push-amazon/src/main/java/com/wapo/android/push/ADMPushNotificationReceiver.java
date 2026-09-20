/*
 * Copyright (c) 2019. The Washington Post
 */
package com.wapo.android.push;

import com.amazon.device.messaging.ADMMessageReceiver;

/**
 * Moved this class out of ADMMessageHandler to add additional code to support FIREOS7(Android 9) using
 * ADMMessageHandlerJob
 */
public class ADMPushNotificationReceiver extends ADMMessageReceiver {

    private final int JOB_ID = Integer.MAX_VALUE;

    public ADMPushNotificationReceiver() {
        super(ADMMessageHandler.class);

        boolean isLatestADMAvailable = false;
        try {
            Class.forName("com.amazon.device.messaging.ADMMessageHandlerJobBase");
            isLatestADMAvailable = true;
        } catch (ClassNotFoundException e) {
            // Handle the exception.
        }
        if (isLatestADMAvailable) {
            registerJobServiceClass(ADMMessageHandlerJob.class, JOB_ID);
        }
    }
}