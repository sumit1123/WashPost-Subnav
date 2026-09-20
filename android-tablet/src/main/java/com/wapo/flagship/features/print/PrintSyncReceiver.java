package com.wapo.flagship.features.print;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import com.wapo.android.commons.util.Logger;
import android.util.Log;

import com.wapo.flagship.FlagshipApplication;
import com.washingtonpost.android.config.domain.manager.ConfigManager;
import com.washingtonpost.android.config.domain.models.config.PrintConfigStub;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static com.wapo.flagship.Utils.oneDayinMilliseconds;

public class PrintSyncReceiver extends BroadcastReceiver {
    private static final String TAG = PrintSyncReceiver.class.getName();

    public void onReceive(Context context, Intent intent) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm == null ? null : pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, PrintSyncReceiver.class.getName());
        if (wl != null) {
            wl.acquire(TimeUnit.MINUTES.toMillis(15));
        }

        try {
            FlagshipApplication application = FlagshipApplication.getInstance();
            if (application != null && application.getArchiveManager() != null) {
                Logger.d(TAG, "Performing daily update for print edition.");
                application.getArchiveManager().performDailyUpdate();
            }
        } finally {
            if (wl != null) {
                try {
                    wl.release();
                } catch (NullPointerException e) {
                    //
                    // there were reports about NPE from within of the WakeLoc.release method.
                    // the cause of this is unknown so just prevent the app from crashing
                    Logger.e(TAG, Log.getStackTraceString(e));
                }
            }
        }
    }

    public static void setAlarm(Context context) {
        Logger.d(TAG, "Creating Print Edition sync alarm.");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(context, PrintSyncReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
        //4:30 AM ET is 9:30 AM GMT
        PrintConfigStub printConfig = ConfigManager.Companion.getInstance().getConfig().getPrintConfig();
        int hour = printConfig.getDailyDownloadHourUTC();
        int minute = printConfig.getDailyDownloadMinuteUTC();
        int variance = printConfig.getDailyDownloadVarianceMinutes();
        //Set the alarm time to be somewhere between the exact time and the max variance time.
        //Helps fight server overload.
        int randomMinute = minute + ((int)(Math.random() * (variance + 1)));

        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, randomMinute);
        //If this was called after 4:30 AM, set the next alarm occurrence to tomorrow, otherwise it'll trigger as soon as the user opens the app.
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.setTimeInMillis((calendar.getTimeInMillis() + oneDayinMilliseconds));
        }


        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
    }
}
