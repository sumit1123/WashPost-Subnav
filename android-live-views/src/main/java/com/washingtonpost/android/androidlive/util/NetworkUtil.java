package com.washingtonpost.android.androidlive.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;

import com.wapo.android.commons.retrofit.DefaultHeadersInterceptor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import com.wapo.android.commons.util.Logger;

/**
 * Created by elamgodilj on 8/29/16.
 */

public class NetworkUtil {

    private static final String TAG = "AndroidLive$NetworkUtil";
    private static final int CONNECT_TIMEOUT_MILLISECONDS = 5000;
    private static final int READ_TIMEOUT_MILLISECONDS = 10000;
    private static final String REQUEST_TYPE = "GET";
    private static final int RESPONSE_OK = 200;

    public synchronized static String fetchData(String requestURL, Context ctx) {
        if (TextUtils.isEmpty(requestURL) || ctx == null) {
            return null;
        }
        if (isNetworkAvailable(ctx)) {
            try {
                return downloadUrlAndReturnString(requestURL);
            } catch (Exception ex) {
                Logger.e(TAG, "Error while fetching data for " + requestURL, ex);
            }
        }
        return null;
    }

    public static boolean isNetworkAvailable(Context ctx) {
        ConnectivityManager connectivityManager = (ConnectivityManager) ctx.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo info = connectivityManager.getActiveNetworkInfo();
            return (info != null && info.isConnected());
        }
        return false;
    }

    private static String downloadUrlAndReturnString(String myurl) throws IOException {
        InputStream is = null;
        HttpURLConnection conn = null;
        try {
            Logger.d(TAG, "Request URL is " + myurl);
            URL url = new URL(myurl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(READ_TIMEOUT_MILLISECONDS);
            conn.setConnectTimeout(CONNECT_TIMEOUT_MILLISECONDS);
            conn.setRequestMethod(REQUEST_TYPE);
            Map<String, String> defaultHeaders = DefaultHeadersInterceptor.Companion.getHeaders();
            Set<String> keys = defaultHeaders.keySet();
            for (String key : keys) {
                conn.setRequestProperty(key, defaultHeaders.get(key));
            }
            conn.connect();
            Logger.d(TAG, "downloadUrlAndReturnString Response code is " + conn.getResponseCode());
            Logger.d(TAG, "downloadUrlAndReturnString Response code is " + conn.getResponseMessage());
            if (conn.getResponseCode() != RESPONSE_OK) {
                return null;
            }
            is = conn.getInputStream();
            String contentAsString = convertInputStreamToString(is);
            return contentAsString;

        } finally {
            if (is != null) {
                is.close();
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String convertInputStreamToString(InputStream inputStream) throws IOException {

        if (inputStream == null) {
            return null;
        }
        StringBuilder total = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = r.readLine()) != null) {
            total.append(line);
        }
        return total.toString();
    }
}
