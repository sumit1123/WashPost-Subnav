/*
 * Copyright (c) 2019. The Washington Post
 */

package com.wapo.android.push;

import android.content.Context;
import android.content.Intent;

import com.amazon.device.messaging.ADMMessageHandlerJobBase;
import com.wapo.android.commons.util.Logger;

/**
 * Created by Jayesh Elamgodil on 11/06/19.
 *
 * This class is to be used for devices running FireOS7(Android 9) and above
 */
public class ADMMessageHandlerJob extends ADMMessageHandlerJobBase {

    @Override
    protected void onMessage(Context context, Intent intent) {
        Logger.i(PushServiceConstants.LOG_TAG,"WPPush - ADMJ Message Received ");
        PushService.getInstance().listener.onMessage(intent);
    }

    @Override
    protected void onRegistrationError(Context context, String s) {
        Logger.i(PushServiceConstants.LOG_TAG,"WPPush - ADMJ Registration Error: "+ s);
        PushService.getInstance().listener.onRegistrationError(s);
    }

    @Override
    protected void onRegistered(Context context, String s) {
        Logger.i(PushServiceConstants.LOG_TAG,"WPPush - ADMJ Registration Successful, RegistrationId= "+ s);
        PushService.getInstance().listener.onRegistered(s);
    }

    @Override
    protected void onUnregistered(Context context, String s) {
        Logger.i(PushServiceConstants.LOG_TAG,"WPPush - ADMJ UnRegistration Successful, RegistrationId= "+ s);
        PushService.getInstance().listener.onUnRegistered();
    }
}
