/*
 * Copyright (C) 2015 . The Washington Post. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.washingtonpost.android.paywall.util;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.washingtonpost.android.paywall.PaywallService;
import com.washingtonpost.android.paywall.R;

/**
 * Created by muppallav on 4/9/15.
 */
public class Util {

    private static Uri getUrlWithNoNav(Context context, String url) {
        Uri uri = Uri.EMPTY;
        if (!TextUtils.isEmpty(url)) {
            uri = Uri.parse(url);
            String noNavKey = context.getString(R.string.no_nav_param_key);
            if (uri.isHierarchical() && uri.getQueryParameter(noNavKey) == null) {
                return uri.buildUpon()
                        .appendQueryParameter(noNavKey, context.getString(R.string.no_nav_param_value))
                        .build();
            }
        }
        return uri;
    }

    public static String getUrlWithJwtBypass(Context context, String url, boolean isAnonymousUser) {
        Uri uri = getUrlWithNoNav(context, url);
        CookiesService cookiesService = PaywallService.getInstance().getCookiesService();
        // If user is Anonymous Subscriber (IAP Sub / Not signed in) set pwapi_token cookie to let webview know
        // that the paywall should be bypassed.
        if (uri != Uri.EMPTY && isAnonymousUser && cookiesService != null) {
            cookiesService.setAnonymousUserCookie(context);
        }
        // If user is does not have IAP Sub (canceled/terminated) remove cookie so their access to webiew articles
        // is removed.
        else if (!isAnonymousUser && cookiesService != null) {
            cookiesService.clearSessionCookies();
        }
        return uri == Uri.EMPTY ? "" : uri.toString();
    }
}
