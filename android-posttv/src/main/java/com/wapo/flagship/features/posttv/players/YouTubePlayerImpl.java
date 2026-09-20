package com.wapo.flagship.features.posttv.players;

import com.wapo.android.commons.util.Logger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;
import com.wapo.flagship.features.posttv.listeners.VideoListener;
import com.wapo.flagship.features.posttv.listeners.VideoPlayer;
import com.wapo.flagship.features.posttv.model.TrackingType;
import com.wapo.flagship.features.posttv.model.Video;
import com.wapo.flagship.features.posttv.players.legacy.LegacyVideoTracker;

public class YouTubePlayerImpl implements VideoPlayer {
    private static final String TAG = YouTubePlayerImpl.class.getSimpleName();
    private static final String DEVELOPER_KEY = "AI39si41y7cCk9VuIKIltADGm_d0XgY0J1raL_07KmMUPNSqMOS96DbTaj9QxDwJh9aXsWtL2-9u5fAp5Nmy3r5srQWX9iZ7fg";

    @NonNull
    private final VideoListener mListener;
    @NonNull
    private final YouTubePlayerSupportFragment mYouTubePlayerFragment;
    @Nullable
    private Video mVideo;
    @Nullable
    private YouTubePlayer mYouTubePlayer;
    @Nullable
    private String mYouTubeId;
    @Nullable
    private LegacyVideoTracker mVideoTracker;
    @Nullable
    private Runnable mVideoTrackingRunnable;

    public YouTubePlayerImpl(@NonNull VideoListener listener) {
        mListener = listener;
        mYouTubePlayerFragment = YouTubePlayerSupportFragment.newInstance();
    }

    @Override
    public void playVideo(@NonNull final Video video) {
        mVideo = video;
        mYouTubeId = video.getId();
        mListener.addVideoFragment(mYouTubePlayerFragment, false);
        mYouTubePlayerFragment.initialize(DEVELOPER_KEY, new YouTubePlayer.OnInitializedListener() {
            @Override
            public void onInitializationSuccess(YouTubePlayer.Provider provider, final YouTubePlayer youTubePlayer, boolean wasRestored) {
                if (!wasRestored) {
                    mYouTubePlayer = youTubePlayer;
                    mYouTubePlayer.setShowFullscreenButton(false);
                    youTubePlayer.loadVideo(mYouTubeId);
                    youTubePlayer.seekToMillis((int) video.getStartPos());
                    mVideoTracker = new LegacyVideoTracker();
                    mVideoTrackingRunnable = new Runnable() {
                        @Override
                        public void run() {
                            if (mVideoTracker != null && mYouTubePlayer != null) {
                                mListener.onTrackingEvent(TrackingType.VIDEO_PERCENTAGE_WATCHED, mVideoTracker.trackPercentageComplete(mYouTubePlayer.getCurrentTimeMillis()));
                            }
                        }
                    };
                    youTubePlayer.setPlaybackEventListener(new YouTubePlayer.PlaybackEventListener() {
                        @Override
                        public void onPlaying() {
                            mListener.onTrackingEvent(TrackingType.ON_PLAY_STARTED, null);
                            mVideoTracker.startVideoTracking(mVideoTrackingRunnable, mYouTubePlayer.getDurationMillis());
                        }

                        @Override
                        public void onPaused() {
                            mListener.onTrackingEvent(TrackingType.ON_PLAY_COMPLETED, youTubePlayer.getCurrentTimeMillis());
                            mVideoTracker.stopVideoTracking();
                        }

                        @Override
                        public void onStopped() {
                            mListener.onTrackingEvent(TrackingType.ON_PLAY_COMPLETED, youTubePlayer.getCurrentTimeMillis());
                            mVideoTracker.stopVideoTracking();
                        }

                        @Override
                        public void onBuffering(boolean b) {
                        }

                        @Override
                        public void onSeekTo(int i) {
                        }
                    });
                }
                pausePlay(true);
            }

            @Override
            public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
                mListener.openYoutubeWeb(video.getId());
            }
        });
    }

    @Override
    public void pausePlay(boolean shouldPlay) {
        if (mYouTubePlayer != null) {
            if (shouldPlay) {
                mYouTubePlayer.play();
            } else {
                mYouTubePlayer.pause();
            }
        }
    }

    @Override
    public void toggleCaptions() {

    }

    @Override
    public boolean isPlaying() {
        if (mYouTubePlayer != null) {
            mYouTubePlayer.isPlaying();
        }
        return false;
    }

    @Override
    public boolean isFullScreen() {
        return false;
    }

    @Override
    public boolean isInPiP() {
        return false;
    }

    @Override
    public void onAdEvent(VideoListener.AdEvent adEvent) {

    }

    @Override
    public void mute() {

    }

    @Override
    public void respectAudioFocus(Boolean flag) {

    }

    @Override
    public void release() {
        if (mYouTubePlayer != null) {
            try {
                mYouTubePlayer.pause();
                mYouTubePlayer.release();
                mYouTubePlayer = null;
                mVideoTracker = null;
            } catch (Exception e) {
                Logger.d(TAG, "YouTube Error", e);
            }
        }
        mListener.removeVideoFragment(mYouTubePlayerFragment, false);
        mListener.removePlayerFrame();
    }

    @Nullable
    @Override
    public String getId() {
        return mYouTubeId;
    }

    @Nullable
    @Override
    public Video getVideo() {
        return mVideo;
    }

    @Override
    public void onActivityResume() {
    }
}