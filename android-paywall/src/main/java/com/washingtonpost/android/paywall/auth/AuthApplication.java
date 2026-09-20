package com.washingtonpost.android.paywall.auth;

public interface AuthApplication {

    /**
     *
     * @return true to use Chrome Custom Tabs to do the OAuth
     */
    boolean shouldUseCustomTab();

    /**
     *
     * @return The package name of the browser that is to be used to do the OAuth. This method
     * is used only if {@link #shouldUseCustomTab} returns false
     */
    String getAuthBrowserPackageName();

    String getAppRedirectScheme();

}
