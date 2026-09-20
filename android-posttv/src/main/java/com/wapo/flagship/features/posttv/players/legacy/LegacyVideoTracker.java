package com.wapo.flagship.features.posttv.players.legacy;

import android.os.Handler;

/**
 * Created by kattim on 9/15/17.
 */

public class LegacyVideoTracker {

    private Handler mVideoTrackingHandler;
    private Runnable mVideoTrackingRunnable;
    private long mDuration;

    public LegacyVideoTracker() {
        mVideoTrackingHandler = new Handler();
    }

    public int trackPercentageComplete(long currentPosition) {
        int percentComplete = getVideoPercentage(currentPosition);
        if (percentComplete < 75) {
            mVideoTrackingHandler.postDelayed(mVideoTrackingRunnable, mDuration/4);
        }
        return percentComplete;
    }

    public void startVideoTracking(Runnable videoTrackingRunnable,long duration) {
        if (mVideoTrackingHandler == null) {
            mVideoTrackingHandler = new Handler();
        }
        mVideoTrackingRunnable = videoTrackingRunnable;
        mDuration = duration;
        mVideoTrackingHandler.postDelayed(mVideoTrackingRunnable, mDuration/4);
    }

    public void stopVideoTracking() {
        if (mVideoTrackingHandler != null && mVideoTrackingRunnable != null)  {
            mVideoTrackingHandler.removeCallbacks(mVideoTrackingRunnable);
        }
        mVideoTrackingHandler = null;
    }

    private int getVideoPercentage(long currentPosition) {
        int percentageComplete = Math.round((float)currentPosition/mDuration*100);

        if (percentageComplete >= 75) {
            return 75;
        } else if (percentageComplete >= 50) {
            return 50;
        } else if (percentageComplete >= 25) {
            return 25;
        }
        return 0;
    }

}
