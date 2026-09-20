/*
 * Copyright (c) 2018. The Washington Post
 */

package com.washingtonpost.android.gdpr.util;

import com.wapo.android.commons.util.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Created by adkinsj on 5/14/18.
 */
public class GDPRUtil {

    private final static String[] euLocaleArray = new String[] {
            "AT", // Austria
            "BE", // Belgium
            "BG", // Bulgaria
            "HR", // Croatia
            "CY", // Cyprus
            "CZ", // Czech Republic
            "DK", // Denmark
            "EE", // Estonia
            "FI", // Finland
            "FR", // France
            "DE", // Germany
            "GR", // Greece
            "HU", // Hungary
            "IS", // Iceland
            "IE", // Ireland
            "IT", // Italy
            "LV", // Latvia
            "LI", // Liechtenstein
            "LT", // Lithuania
            "LU", // Luxembourg
            "MT", // Malta
            "NL", // Netherlands
            "NO", // Norway
            "PL", // Poland
            "PT", // Portugal
            "RO", // Romania
            "SK", // Slovakia
            "SI", // Slovenia
            "ES", // Spain
            "SE", // Sweden
            "CH", // Switzerland
            "GB"  // United Kingdom
    };
    private final static List<String> euLocaleList = Arrays.asList(euLocaleArray);

    public static boolean isInEU() {
        String locale = Locale.getDefault().getCountry().toUpperCase();
        if(locale.isEmpty()) {
            locale = Locale.getDefault().getLanguage().toUpperCase();
        }
        return euLocaleList.contains(locale);
    }
}