package paywall.test.utils;/* Copyright (c) 2019 The Washington Post. All rights reserved. */

import android.app.Application;

import com.washingtonpost.android.paywall.auth.AuthApplication;

public class CustomAuthTestApplication extends Application implements AuthApplication {
    public static final String APP_REDIRECT_SCREEN = "com.washingtonpost.classic://oauth-callback";

    @Override
    public boolean shouldUseCustomTab() {
        return true;
    }

    @Override
    public String getAuthBrowserPackageName() {
        return null;
    }

    @Override
    public String getAppRedirectScheme() {
        return APP_REDIRECT_SCREEN;
    }
}
