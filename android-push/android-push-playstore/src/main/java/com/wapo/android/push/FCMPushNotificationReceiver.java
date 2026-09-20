package com.wapo.android.push;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.iterable.iterableapi.IterableFirebaseMessagingService;
import com.urbanairship.UAirship;
import com.urbanairship.push.fcm.AirshipFirebaseIntegration;
import com.wapo.android.commons.util.Logger;

public class FCMPushNotificationReceiver extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage message) {
        Logger.i(PushServiceConstants.LOG_TAG, "WPPush - FCM Message Received ");
        if (message != null) {
            PushService.getInstance().listener.onMessage(PushUtils.createBundleIntentFromMap(message.getData()));
            if (UAirship.isTakingOff() || UAirship.isFlying()) {
                AirshipFirebaseIntegration.processMessageSync(getApplicationContext(), message);
            }
            IterableFirebaseMessagingService.handleMessageReceived(this, message);
        }
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Logger.i(PushServiceConstants.LOG_TAG, "WPPush - New FCM token generated: " + token);
        PushService.getInstance().listener.onRegistered(token);
        if (getApplication() instanceof FCMPushNotificationReceiverInterface) {
            ((FCMPushNotificationReceiverInterface) getApplication()).onNewToken(token);
        }
        if (UAirship.isTakingOff() || UAirship.isFlying()) {
            AirshipFirebaseIntegration.processNewToken(getApplicationContext(), token);
        }
        IterableFirebaseMessagingService.handleTokenRefresh();
    }
}