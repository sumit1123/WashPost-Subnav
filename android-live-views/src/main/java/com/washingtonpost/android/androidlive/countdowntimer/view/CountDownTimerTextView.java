package com.washingtonpost.android.androidlive.countdowntimer.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import com.washingtonpost.android.androidlive.countdowntimer.timer.CountDownTimerExt;

/**
 * Created by elamgodilj on 10/18/16.
 */

public class CountDownTimerTextView extends TextView implements CountDownTimerExt.CountDownListener{

    private CountDownTimerExt countDownTimer;
    private String message;
    private String onCompleteMessage;
    private int secondsRemaining;
    private final String SPACE_PLUS_SECOND = " second";
    private final String SPACE_PLUS_SECONDS = " seconds";
    private boolean isRepeat;

    public CountDownTimerTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CountDownTimerTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void startTimer(final String message, String onCompleteMessage, int seconds, boolean isRepeat) {
        cancelTimer();
        this.message = message;
        this.onCompleteMessage = onCompleteMessage;
        this.isRepeat = isRepeat;
        countDownTimer = new CountDownTimerExt(seconds, this);
        countDownTimer.start();
    }

    public void cancelTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        isRepeat = false;
    }

    @Override
    public void onTick(long secondsUntilFinished) {
        setText(message + secondsUntilFinished + (secondsUntilFinished == 1 ? SPACE_PLUS_SECOND : SPACE_PLUS_SECONDS));
        secondsRemaining = (int) secondsUntilFinished;
    }

    @Override
    public void onFinish() {
        setText(onCompleteMessage);
        secondsRemaining = 0;
        handleRepeat();
    }

    public int getSecondsRemaining() {
        return secondsRemaining;
    }

    private void handleRepeat() {
        if (isRepeat) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {

                    }

                    if (countDownTimer != null) {
                        countDownTimer.start();
                    }
                }
            }).start();
        }
    }
}
