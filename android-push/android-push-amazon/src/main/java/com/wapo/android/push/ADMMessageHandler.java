package com.wapo.android.push;

import android.content.Intent;

import com.amazon.device.messaging.ADMMessageHandlerBase;
import com.wapo.android.commons.util.Logger;

public class ADMMessageHandler extends ADMMessageHandlerBase {


    public ADMMessageHandler() {
        super("WPPush - ADMMessageHandler");
    }

    @Override
    protected void onMessage(Intent intent) {
        Logger.i(PushServiceConstants.LOG_TAG,"WPPush - ADM Message Received ");
        PushService.getInstance().listener.onMessage(intent);
    }

    @Override
    protected void onRegistrationError(String s) {
        Logger.i(PushServiceConstants.LOG_TAG,"WPPush - ADM Registration Error: "+ s);
        PushService.getInstance().listener.onRegistrationError(s);
    }

    @Override
    protected void onRegistered(String s) {
        Logger.i(PushServiceConstants.LOG_TAG,"WPPush - ADM Registration Successful, RegistrationId= "+ s);
        PushService.getInstance().listener.onRegistered(s);
    }

    @Override
    protected void onUnregistered(String s) {
        Logger.i(PushServiceConstants.LOG_TAG,"WPPush - ADM UnRegistration Successful, RegistrationId= "+ s);
        PushService.getInstance().listener.onUnRegistered();
    }
}
