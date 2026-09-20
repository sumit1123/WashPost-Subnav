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

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

/**
 * Maxim Ignatyev
 * Date: 6/4/13
 */
public class ReachabilityUtil {

    public static boolean is3gSupported(Context ctx) {
        ConnectivityManager mgr = (ConnectivityManager) ctx.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (mgr == null) {
            return false;
        }
        NetworkInfo mobileInfo = mgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        return mobileInfo != null;
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

    /**
     * Returns true when the device is connected to VPN. Otherwise returns false.
     * API support is there only from Android M. So returning false on below versions.
     */
    public static boolean isOnVPN(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false;
        Object service = context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (service instanceof ConnectivityManager) {
            ConnectivityManager connectivityManager = (ConnectivityManager) service;
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) return false;
            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            if (networkCapabilities == null) return false;
            return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
        }
        return false;
    }
}
