package com.wapo.flagship.util;

import android.content.Context;
import android.content.Intent;

import com.wapo.android.commons.logs.EventLog;
import com.wapo.android.commons.logs.LogModules;
import com.wapo.android.remotelog.logger.RemoteLog;
import com.wapo.flagship.AppContext;
import com.wapo.flagship.FlagshipApplication;


import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by curacamalitod on 6/11/15.
 */
public class RemoteLogUtil {


    public static void logLiveVideoNotificationClicked(Context context, Intent intent, String url) {
        //Adding logging for when a push notification is clicked.
        String currentDate = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS").format(new Date());
        RemoteLog.d(FlagshipApplication.getInstance(), new EventLog.Builder()
                .setMessage("Live Video Bar Clicked")
                .setModule(LogModules.ALERTS)
                .set("intent_action", intent.getAction())
                .set("time_opened", currentDate)
                .set("device_token", AppContext.getRegistrationId())
                .set("url", url).build());
    }
}
