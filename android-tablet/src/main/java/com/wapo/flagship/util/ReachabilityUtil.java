/* Copyright (c) 2019 The Washington Post. All rights reserved. */
package com.wapo.flagship.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

import com.wapo.flagship.features.settings.AppPreferences;

public class ReachabilityUtil {

    /**
     * Checks if data network or wifi network is totally connected.
     * NetworkCapabilties ensures that internet is available on the wireless network you are connected to
     * connectivityManager.getAllNetworks is the recommended approach for Lollipop versions, but it does not guarantee that internet is actually available.
     * connectivityManager.getActiveNetworkInfo is the deprecated approach, and also does not guarantee that internet is actually available.
     * @param ctx
     * @return
     */
    public static boolean isConnected(Context ctx) {

        try {
            final ConnectivityManager connectivityManager = (ConnectivityManager)ctx.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                final Network network;
                network = connectivityManager.getActiveNetwork();
                final NetworkCapabilities capabilities = connectivityManager
                        .getNetworkCapabilities(network);

                return capabilities != null
                        && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                boolean isWifiConn = false;
                boolean isMobileConn = false;
                for (Network network : connectivityManager.getAllNetworks()) {
                    NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
                    if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                        isWifiConn |= networkInfo.isConnected();
                    }
                    if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                        isMobileConn |= networkInfo.isConnected();
                    }
                }
                return isMobileConn || isWifiConn;
            } else {
                NetworkInfo ni = connectivityManager.getActiveNetworkInfo();
                return ni != null && ni.isConnected() && ni.isAvailable();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean is3gSupported(Context ctx) {
        ConnectivityManager mgr = (ConnectivityManager) ctx.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (mgr == null) {
            return false;
        }
        NetworkInfo mobileInfo = mgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        return mobileInfo != null;
    }

    public static boolean shouldUseNetwork(Context context) {
        return shouldUseNetwork((ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE));
    }

    public static boolean shouldUseNetwork(ConnectivityManager cm) {
        return cm != null && (AppPreferences.INSTANCE.canSyncOverCellular() || isOnWiFi(cm));
    }

    public static boolean isOnWiFi(Context context) {
        return isOnWiFi((ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE));
    }

    public static boolean isOnWiFi(ConnectivityManager cm) {
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isAvailable()) {
            return false;
        }
        switch (networkInfo.getType()) {
            case ConnectivityManager.TYPE_ETHERNET:
            case ConnectivityManager.TYPE_WIFI:
            case ConnectivityManager.TYPE_DUMMY:
                return true;
        }

        return false;
    }
}
