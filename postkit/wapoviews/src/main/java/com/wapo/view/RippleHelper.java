/*
 * Copyright (c) 2019. The Washington Post
 */
package com.wapo.view;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public class RippleHelper {

    public static void addRippleEffectToView(@NonNull View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int rippleDrawableId = R.drawable.ripple_effect;
            Drawable rippleDrawable = ContextCompat.getDrawable(v.getContext(), rippleDrawableId);
            // set rippleDrawable as foreground drawable.
            v.setForeground(rippleDrawable);
        }
        //Ripple Effect no longer works on Android 5, disabling for now but maybe find a solution later https://console.firebase.google.com/u/0/project/divine-voice-825/crashlytics/app/android:com.washingtonpost.rainbow/issues/0ff18c88cea0c1fdcb1a96a8e3edd615?time=last-thirty-days&types=crash&sessionEventKey=64E8659A033C000131B1B783527C8747_1849618999649832038
    }
}
