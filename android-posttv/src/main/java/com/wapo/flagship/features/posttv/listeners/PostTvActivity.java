package com.wapo.flagship.features.posttv.listeners;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.wapo.android.commons.logs.EventLog;
import com.wapo.flagship.features.posttv.VideoTracker2;
import com.wapo.flagship.features.posttv.model.TrackingType;
import com.wapo.flagship.features.posttv.model.Video;

public interface PostTvActivity {
    void addFragment(@IdRes int viewID, @NonNull Fragment fragment, boolean shouldSaveState);

    void removeFragment(@Nullable Fragment fragment, boolean shouldSaveState);

    void shareVideo(String headline, String shareUrl);

    void startPIP(Video mVideo);

    boolean isPIPEnabled();

    @Deprecated
    void onTrackingEvent(@NonNull TrackingType type, @NonNull Video video, @Nullable Object value);

    void onTrackingEvent(@NonNull VideoTracker2.VideoType videoType, @NonNull TrackingType type, @NonNull Video video, @Nullable Object value);

    void onVideoStarted();

    void openWeb(String url);

    void logVideoError(EventLog.Builder eventLogBuilder);

    void startPIP(Video video, String playerName);

    void startFullScreen(Video video, String playerName);
}