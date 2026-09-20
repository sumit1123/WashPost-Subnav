/*
 *
 *   Copyright (C) 2014 . The Washington Post. All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.wapo.android.commons.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.DisplayMetrics;

import java.io.File;
import java.util.UUID;

/**
 * Created by saneeshc on 9/8/14.
 */
@Deprecated(since = "Use DeviceUtilRepo instead. This class will be removed in a future update.")
public class DeviceUtils {

    public static final String TAG = DeviceUtils.class.getSimpleName();
    public static final String PREF_NEW_LOGGING_ID = "pref.new.logging_id";
    @Deprecated
    public static final String PREF_LOGGING_ID = "pref.logging_id";
    public static final String PREF_DEVICE_SERIAL_ID="pref.serial_id";

    @Deprecated
    public static boolean isTablet(Context ctx) {
        Resources res = ctx.getResources();
        Configuration config = res.getConfiguration();
        DisplayMetrics metrics = res.getDisplayMetrics();

        // Checks Screen Width in dp
        if (config.smallestScreenWidthDp >= 600) {
            return true;
        }

        // Calculates Physical Screen Size in inches
        float widthInches = metrics.widthPixels / metrics.xdpi;
        float heightInches = metrics.heightPixels / metrics.ydpi;
        double diagonalInches = Math.sqrt(widthInches * widthInches + heightInches * heightInches);

        if (diagonalInches >= 7.0) {
            return true;
        }
        return false;
    }

    @Deprecated
    public static File getDataDirectory(Context ctx) {
        return ctx.getFilesDir();
    }

    @Deprecated
    public static File getAppDirectory(Context ctx) {
        File f = null;
        try {
            if (Build.VERSION.SDK_INT >= 17) {
                f = new File(ctx.getApplicationInfo().dataDir);
            } else {
                f = new File("/data/data/" + ctx.getPackageName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return f;
    }

    @Deprecated
    public static String getUniqueDeviceId(Context context) {
        String android_id = null;

        try {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            String new_android_id = sharedPreferences.getString(PREF_NEW_LOGGING_ID, null);

            if (new_android_id != null && !new_android_id.isEmpty()) {
                return new_android_id;
            } else {
                //migrate from deprecated pref
                android_id = sharedPreferences.getString(PREF_LOGGING_ID, null);

                if (android_id == null || android_id.equalsIgnoreCase("unknown") || android_id.isEmpty()) {
                    //deprecated pref is empty or unknown, assign new device id
                    android_id = UUID.randomUUID().toString();
                    Logger.d(TAG, "getUniqueDeviceId(), new id is generated! id=" + android_id);
                }
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(PREF_NEW_LOGGING_ID, android_id);
                editor.apply();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return android_id;
    }

    @Deprecated
    @SuppressLint("HardwareIds")
    public static String getDeviceSerialId(Context context) {
        String android_id = null;

        try {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            android_id = sharedPreferences.getString(PREF_DEVICE_SERIAL_ID, null);
            if (android_id == null || Build.UNKNOWN.equals(android_id)) {
                android_id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
                sharedPreferences.edit()
                        .putString(PREF_DEVICE_SERIAL_ID, android_id)
                        .apply();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return android_id;
    }

    @Deprecated
    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    @Deprecated
    public static String generateJUcid(){
        return UUID.randomUUID().toString();
    }

    @Deprecated
    public static String getLoggingId(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PREF_LOGGING_ID, null);
    }

    @Deprecated
    public static String getNewLoggingId(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PREF_NEW_LOGGING_ID, null);
    }

    @Deprecated
    public static int getNumberOfCores() {
        return Runtime.getRuntime().availableProcessors();
    }
}
