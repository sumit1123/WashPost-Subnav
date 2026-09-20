package com.washingtonpost.android.androidlive.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by elamgodilj on 8/27/16.
 */

public class DateUtil {

    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("h:mm a", Locale.US);
    private static final DateFormat ISO8601_FORMAT_WITHOUT_TIMEZONE  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",Locale.US);
    private static final SimpleDateFormat ISO8601_FORMAT_WITH_TIMEZONE  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);



    public static String getDateStringInHoursMinutesFormat(Date date) {
        try {
            return TIMESTAMP_FORMAT.format(date)
                    .toLowerCase()
                    .replace("am", "a.m.")
                    .replace("pm", "p.m.");
        } catch (Exception ex) {
            return date.toString();
        }
    }

    public static Date getDateISOFormat(String date) {
        try {
            ISO8601_FORMAT_WITHOUT_TIMEZONE.setTimeZone(TimeZone.getTimeZone("UTC"));
            return ISO8601_FORMAT_WITHOUT_TIMEZONE.parse(date);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getDateInISO8601WithTimezone(Date date) {
        try {
            return ISO8601_FORMAT_WITH_TIMEZONE.format(date);
        } catch (Exception e) {
            return "";
        }
    }
}
