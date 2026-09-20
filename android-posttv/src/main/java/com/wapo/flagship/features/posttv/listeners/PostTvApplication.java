package com.wapo.flagship.features.posttv.listeners;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wapo.android.commons.logs.EventLog;
import com.wapo.flagship.features.posttv.VideoManager;
import com.wapo.flagship.features.posttv.VideoManager2;

public interface PostTvApplication {
    @NonNull
    VideoManager getVideoManager();

    VideoManager2 getVideoManager2();

    void releaseVideoManager();

    void releaseVideoManager2();

    void resetVideoManager2();

    @Nullable
    Activity getCurrentActivity();

    boolean shouldUseLegacyPlayer();

    boolean shouldSuppressAds();

    void logPostTvError(EventLog.Builder eventLogBuilder);

    void logVideoAdError(String errorLog);

    void pausePIP();
}
