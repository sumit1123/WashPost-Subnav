package com.washingtonpost.android.androidlive.countdowntimer.timer;

import android.os.CountDownTimer;
import com.wapo.android.commons.util.Logger;

import java.util.Locale;

public class CountDownTimerExt extends CountDownTimer {
    private static final String TAG = CountDownTimerExt.class.getSimpleName();
    private int seconds;
    private CountDownListener countDownListener;

    public CountDownTimerExt(int seconds, CountDownListener countDownListener) {
        super(seconds * 1000, 500);
        this.seconds = seconds;
        this.countDownListener = countDownListener;
        Logger.d(TAG, String.format(Locale.US, "Initialized countdown timer for %d seconds.", seconds));
    }

    @Override
    public void onTick(long millisUntilFinished) {
        if (countDownListener != null) {
            countDownListener.onTick(millisUntilFinished / 1000);
        }
    }

    @Override
    public void onFinish() {
        if (countDownListener != null) {
            countDownListener.onFinish();
        }
    }

    public int getSeconds() {
        return seconds;
    }

    public interface CountDownListener {

        void onTick(long secondsUntilFinished);

        void onFinish();
    }
}
