/*
 * Copyright (c) 2018. The Washington Post
 */

package com.washingtonpost.android.gdpr.util;

import android.net.Uri;

/**
 * Created by adkinsj on 5/15/18.
 */
public class GDPRConstants {
    public static final String CONTENT_PATH =  "consent";
    public static String RAINBOW_AUTHORITY = "com.washingtonpost.rainbow.providers.provider";
    public static final Uri RAINBOW_CONTENT_URI = Uri.parse("content://" + RAINBOW_AUTHORITY + "/" + CONTENT_PATH);
    public static String CLASSIC_AUTHORITY = "com.wapo.flagship.providers.provider";
    public static final Uri CLASSIC_CONTENT_URI = Uri.parse("content://" + CLASSIC_AUTHORITY + "/" + CONTENT_PATH);

    public static final String DATABASE_NAME = "GDPRConsentDb";
    public static final String GDPR_TABLE_NAME = "gdpr";
    public static final String GDPR_TABLE_COLUMN = "isallowed";
    public static final int DATABASE_VERSION = 1;

    public static final String INTENT_ACTION = "gdprIntentAction";
}
