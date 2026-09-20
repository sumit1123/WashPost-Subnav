package com.wapo.flagship.features.sections.utils;

import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

public class AnimationHelper {

    public static void fadeIn(final View view, final Animation.AnimationListener listener){
        if (view.getVisibility() != View.GONE){
            return;
        }
        Animation fadeIn = new AlphaAnimation(0,1);
        fadeIn.setDuration(1000);
        fadeIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                view.setVisibility(View.VISIBLE);
                if (listener != null) {listener.onAnimationStart(animation);}
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (listener != null) {listener.onAnimationEnd(animation);}
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                if (listener != null) {listener.onAnimationRepeat(animation);}
            }
        });
        view.startAnimation(fadeIn);
    }

    public static void fadeOut(final View view, final Animation.AnimationListener listener){
        if (view.getVisibility() == View.GONE){
            return;
        }
        Animation fadeOut = new AlphaAnimation(1,0);
        fadeOut.setDuration(1000);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                if (listener != null) {listener.onAnimationStart(animation);}
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
                if (listener != null) {listener.onAnimationEnd(animation);}
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                if (listener != null) {listener.onAnimationRepeat(animation);}
            }
        });
        view.startAnimation(fadeOut);
    }

}
