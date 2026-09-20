package com.wapo.flagship.features.posttv.listeners;

import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.ads.interactivemedia.v3.api.AdError;
import com.wapo.flagship.features.posttv.model.TrackingType;
import com.wapo.flagship.features.posttv.model.Video;

public interface VideoListener {

    enum AdEvent {
        STARTED,
        COMPLETED
    }
    @NonNull
    FrameLayout getPlayerFrame();

    void removePlayerFrame();

    void addVideoView(View view);

    void addVideoFragment(Fragment fragment, boolean shouldSaveState);

    void removeVideoFragment(Fragment fragment, boolean shouldSaveState);

    void setIsLoading(boolean isLoading);

    void release();

    void onError(String message);

    void logError(String log);

    void onTrackingEvent(@NonNull TrackingType type, @Nullable Object value);

    long getSavedPosition(String id);

    void setSavedPosition(String id, long value);

    long getSavedAdStatus(String id);

    void setSavedAdStatus(String id, long value);

    void onActivityResume();

    boolean isInPIP();

    boolean shouldSuppressAds();

    void onAdEvent(AdEvent adEvent);

    void onAdError(AdError adError, Video video);

    void logAdError(AdError adError, Video video);

    void openYoutubeWeb(String videoId);
}