package com.wapo.flagship.features.articles;

import android.content.Context;
import android.util.DisplayMetrics;
import com.wapo.android.commons.util.Logger;

import com.wapo.view.FlowableLayout;

import java.text.SimpleDateFormat;
import java.util.Locale;

import rx.Subscription;

public class Utils {

    private final static String TAG = Utils.class.getSimpleName();
    /**
     * Return FLOAT_{OPTION} based on floatPosition recommended
     *
     * @param floatPosition
     * @return
     */
    public static int floatPositionToIntValue(String floatPosition) {
        if (floatPosition == null || floatPosition.equalsIgnoreCase("center")) {
            return FlowableLayout.FLOAT_NONE;
        } else if (floatPosition.equalsIgnoreCase("left")) {
            return FlowableLayout.FLOAT_LEFT;
        } else if (floatPosition.equalsIgnoreCase("right")) {
            return FlowableLayout.FLOAT_RIGHT;
        } else {
            //If it's unknown, we handle as FLOAT_NONE
            return FlowableLayout.FLOAT_NONE;
        }
    }

    public static void unsubscribe(Subscription... subs) {
        for (Subscription sub : subs) {
            if (sub != null && !sub.isUnsubscribed()) {
                sub.unsubscribe();
            }
        }
    }

    public static String getDate(String dateFormat, Long millis) {
        String date = null;
        try {
            if (millis != null) {
                date = new SimpleDateFormat(dateFormat, Locale.getDefault()).format(millis);
            }
        } catch (IllegalArgumentException e) {
            Logger.d(TAG, "Date format error", e);
        } catch (NullPointerException e) {
            Logger.d(TAG, "Date error", e);
        }

        return date;
    }

    public static float convertDpToPixel(float dp, Context context){
        return dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }
}
