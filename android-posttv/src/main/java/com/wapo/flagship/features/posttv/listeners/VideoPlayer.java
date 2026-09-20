package com.wapo.flagship.features.posttv.listeners;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wapo.flagship.features.posttv.model.Video;

public interface VideoPlayer {
    /**
     * This method is responsible for freeing any resources before the object is destroyed,
     * it should stop any video that is playing in addition to removing any created views.
     */
    void release();

    @Nullable
    String getId();

    @Nullable
    Video getVideo();

    void onActivityResume();

    void playVideo(@NonNull final Video video);

    void pausePlay(boolean shouldPlay);

    void toggleCaptions();

    boolean isPlaying();

    boolean isFullScreen();

    boolean isInPiP();

    void onAdEvent(VideoListener.AdEvent adEvent);

    void mute();

    void respectAudioFocus(Boolean flag);
}