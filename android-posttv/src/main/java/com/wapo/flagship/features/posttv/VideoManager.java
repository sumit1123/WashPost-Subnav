package com.wapo.flagship.features.posttv;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import com.wapo.android.commons.util.Logger;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;

import com.google.ads.interactivemedia.v3.api.AdError;
import com.wapo.android.commons.logs.EventLog;
import com.wapo.flagship.features.posttv.listeners.PostTvActivity;
import com.wapo.flagship.features.posttv.listeners.PostTvApplication;
import com.wapo.flagship.features.posttv.listeners.VideoListener;
import com.wapo.flagship.features.posttv.listeners.VideoPlayer;
import com.wapo.flagship.features.posttv.model.TrackingType;
import com.wapo.flagship.features.posttv.model.Video;
import com.wapo.flagship.features.posttv.players.PostTvPlayerImpl;
import com.wapo.flagship.features.posttv.players.VimeoPlayerImpl;
import com.wapo.flagship.features.posttv.players.YouTubePlayerImpl;
import com.wapo.flagship.features.posttv.views.VideoFrameLayout;

import java.util.HashMap;

public class VideoManager implements VideoListener {
    public static final String TAG = VideoManager.class.getSimpleName();
    public static final long NO_POSITION = -1;

    @NonNull
    private Context mAppContext;
    @NonNull
    private HashMap<String, Long> mIdToPosition;
    @NonNull
    private HashMap<String, Long> mIdToAdStatus;
    @NonNull
    private VideoFrameLayout mVideoFrameLayout;
    @Nullable
    private VideoPlayer mVideoPlayer;
    @NonNull
    private RelativeLayout mProgressLayout;
    @NonNull
    private TextView mMessageText;
    @NonNull
    private RelativeLayout mMessageOverlay;

    private boolean mIsPlaying = false;
    private boolean mIsInPIP = false;
    private boolean hasPIPInitiated = false;
    private boolean mIsBeingShared = false;

    public VideoManager(@NonNull Context appContext) {
        mAppContext = appContext;
        mIdToPosition = new HashMap<>();
        mIdToAdStatus = new HashMap<>();
        mVideoFrameLayout = new VideoFrameLayout(mAppContext);
        mVideoFrameLayout.setId(ViewCompat.generateViewId());
        mVideoFrameLayout.setBackgroundColor(Color.BLACK);

        mProgressLayout = new RelativeLayout(mAppContext);
        ProgressBar progressBar = new ProgressBar(mAppContext, null, android.R.attr.progressBarStyleLarge);
        RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(100, 100);
        params1.addRule(RelativeLayout.CENTER_IN_PARENT);
        mProgressLayout.addView(progressBar, params1);

        mMessageText = new TextView(mAppContext);
        mMessageText.setTextColor(Color.WHITE);
        mMessageText.setGravity(Gravity.CENTER);
        mMessageOverlay = new RelativeLayout(mAppContext);
        mMessageOverlay.setBackgroundColor(Color.BLACK);
        RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params2.addRule(RelativeLayout.CENTER_IN_PARENT);
        mMessageOverlay.addView(mMessageText, params2);
    }

    public synchronized void initMedia(@NonNull final Video video) throws IllegalStateException {
        try {
            mIsPlaying = false;
            //never initialize more than one video player
            if (mVideoPlayer != null && !mIsInPIP) {
                release();
            } else if (mVideoPlayer != null && mIsInPIP && !mIsPlaying) {
                mVideoPlayer.release();
            }
            final Activity activity = ((PostTvApplication) mAppContext).getCurrentActivity();
            if (activity instanceof PostTvActivity) {
                ((PostTvActivity) activity).onVideoStarted();
            }
            long savedPos = getSavedPosition(video.getId());
            long targetPos = video.getStartPos();
            setSavedPosition(video.getId(), (savedPos == NO_POSITION) ? targetPos : savedPos);
            if (video.isYouTube()) {
                if (isAmazonDevice()) {
                    String youtubeURL = mAppContext.getString(R.string.youtube_base_url) + video.getId();
                    ((PostTvActivity) activity).openWeb(youtubeURL);
                } else {
                    mVideoPlayer = new YouTubePlayerImpl(this);
                    mVideoPlayer.playVideo(video);
                }
            } else {
                if (mAppContext instanceof PostTvApplication) {
                    if (video.isVimeo()) {
                        mVideoPlayer = new VimeoPlayerImpl(mAppContext, this);
                    } else {
                        mVideoPlayer = new PostTvPlayerImpl(mAppContext, this);
                    }
                    mVideoPlayer.playVideo(video);
                    mIsPlaying = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isPlaying(){
        if (mVideoPlayer != null) {
            return mVideoPlayer.isPlaying();
        }
        return false;
    }

    public boolean hasPIPInitiated(){
        return hasPIPInitiated;
    }

    public void setHasPIPInitiated(boolean hasInit) {
        hasPIPInitiated = hasInit;
    }

    @Override
    public void removePlayerFrame() {
        if (mAppContext instanceof PostTvApplication) {
            if (mMessageOverlay.getParent() instanceof ViewGroup) {
                //remove the error message if showing
                ((ViewGroup) mMessageOverlay.getParent()).removeView(mMessageOverlay);
            }
            if (mVideoFrameLayout.getParent() instanceof ViewGroup) {
                //remove inline player from view holder
                ((ViewGroup) mVideoFrameLayout.getParent()).removeView(mVideoFrameLayout);
                mVideoFrameLayout.setTag(null);
            }
        }
    }

    public void pausePlay(boolean shouldPlay){
        if (mVideoPlayer != null)
            mVideoPlayer.pausePlay(shouldPlay);
    }

    public void toggleCaptions(){
        if (mVideoPlayer != null)
            mVideoPlayer.toggleCaptions();
    }

    public void onScrolled(@Nullable View.OnClickListener listener) {
        if (mIsPlaying) {
            if (mVideoPlayer instanceof YouTubePlayerImpl) {
                release();
            }
        }
    }

    public Video getPlayingVideo() {
        if (mVideoPlayer != null) {
            if (mVideoPlayer.isPlaying() && !hasPIPInitiated) {
                return getVideo();
            }
        }
        return null;
    }

    public boolean getIsFullScreen() {
        if (mVideoPlayer != null) {
            return mVideoPlayer.isFullScreen();
        }
        return false;
    }

    public void setIsBeingShared(boolean isBeingShared) {
        mIsBeingShared = isBeingShared;
    }

    public boolean getIsBeingShared() {
        if (mVideoPlayer != null) {
            return mIsBeingShared;
        }
        return false;
    }

    @Override
    public void onTrackingEvent(@NonNull TrackingType type, @Nullable Object value) {
        Logger.d(TAG, "Tracking event=" + type.name() + " value=" + value);
        if (mVideoPlayer != null && mVideoPlayer.getVideo() != null
                && mAppContext instanceof PostTvApplication) {
            Object result = null;
            switch (type) {
                case ON_PLAY_STARTED: {
                    mIsPlaying = true;
                    break;
                }
                case ON_PLAY_COMPLETED: {
                    mIsPlaying = false;
                    break;
                }
                case VIDEO_PERCENTAGE_WATCHED: {
                    if (value instanceof Integer) {
                        result = value;
                    }
                    break;
                }
            }
            final Activity activity = ((PostTvApplication) mAppContext).getCurrentActivity();
            if (activity instanceof PostTvActivity) {
                ((PostTvActivity) activity).onTrackingEvent(type, mVideoPlayer.getVideo(), result);
            }
        }
    }

    @Override
    public void setSavedPosition(String id, long value) {
        if (value >= 0) {
            mIdToPosition.put(id, value);
        } else {
            mIdToPosition.put(id, (long) 0);
        }
    }

    @Override
    public long getSavedPosition(String id) {
        Long position = mIdToPosition.get(id);
        if (position != null && position >= 0) {
            return position;
        }
        return NO_POSITION;
    }

    @Override
    public void setSavedAdStatus(String id, long value) {
        if (value >= 0) {
            mIdToAdStatus.put(id, value);
        }
    }

    @Override
    public long getSavedAdStatus(String id) {
        Long adStatus = mIdToAdStatus.get(id);
        if (adStatus != null && adStatus >= 0) {
            return adStatus;
        }
        return Video.AD_NOT_STARTED;
    }

    @NonNull
    public FrameLayout getPlayerFrame() {
        return mVideoFrameLayout;
    }

    @Override
    public void addVideoView(View view) {
        mVideoFrameLayout.addView(view);
    }

    @Override
    public void addVideoFragment(Fragment fragment, boolean shouldSaveState) {
        final Activity activity = ((PostTvApplication) mAppContext).getCurrentActivity();
        if (activity instanceof PostTvActivity) {

            ((PostTvActivity) activity).addFragment(mVideoFrameLayout.getId(), fragment, shouldSaveState);
        }
    }

    @Override
    public void removeVideoFragment(Fragment fragment, boolean shouldSaveState) {
        final Activity activity = ((PostTvApplication) mAppContext).getCurrentActivity();
        if (activity instanceof PostTvActivity) {
            ((PostTvActivity) activity).removeFragment(fragment, shouldSaveState);
        }
    }

    @Override
    public void setIsLoading(boolean isLoading) {
        if (isLoading) {
            if (mProgressLayout.getParent() == null) {
                mVideoFrameLayout.addView(mProgressLayout);
            }
        } else {
            mVideoFrameLayout.removeView(mProgressLayout);
        }
    }

    @Override
    public void release() {
        if (mVideoPlayer != null && !mIsInPIP) {
            mVideoPlayer.release();
        }
        mIsPlaying = false;
    }

    @Override
    public void onError(String message) {
        ViewParent parent = mVideoFrameLayout.getParent();
        if (!mIsInPIP && parent instanceof ViewGroup) {
            release();
            mMessageText.setText(message);
            if (mMessageOverlay.getParent() != parent) {
                if (mMessageOverlay.getParent() instanceof ViewGroup) {
                    ((ViewGroup) mMessageOverlay.getParent()).removeView(mMessageOverlay);
                }
                ((ViewGroup) parent).addView(mMessageOverlay);
            }
        }
    }

    @Override
    public void logError(String log) {
        if (mAppContext instanceof  PostTvApplication) {
            EventLog.Builder builder = new EventLog.Builder();
            builder.setMessage("VideoManager Error");
            builder.setErrorMessage(log);
            ((PostTvApplication) mAppContext).logPostTvError(builder);
        }
    }

    @Nullable
    public String getId() {
        if (mVideoPlayer != null) {
            return mVideoPlayer.getId();
        }
        return null;
    }

    @Override
    public void onActivityResume() {
        if (mVideoPlayer != null) {
            mVideoPlayer.onActivityResume();
        }
    }

    @Nullable
    public String getVideoUrl() {
        if (mVideoPlayer == null || mVideoPlayer.getVideo() == null) {
            return null;
        }
        return mVideoPlayer.getVideo().getId();
    }

    @Nullable
    public String getContentUrl() {
        if (mVideoPlayer == null || mVideoPlayer.getVideo() == null) {
            return null;
        }
        return mVideoPlayer.getVideo().getContentUrl();
    }

    @Nullable
    public Video getVideo() {
        if (mVideoPlayer == null || mVideoPlayer.getVideo() == null) {
            return null;
        }
        return mVideoPlayer.getVideo();
    }

    @Override
    public boolean isInPIP() {
        return mIsInPIP;
    }

    public void setmIsInPIP(boolean isPIP) {
        mIsInPIP = isPIP;
    }

    @Override
    public boolean shouldSuppressAds() {
        return ((PostTvApplication) mAppContext).shouldSuppressAds();
    }

    @Override
    public void onAdEvent(VideoListener.AdEvent adEvent) {
        if (mVideoPlayer != null) {
            switch (adEvent) {
                case STARTED:
                    onTrackingEvent(TrackingType.AD_PLAY_STARTED, null);
                    mVideoPlayer.onAdEvent(AdEvent.STARTED);
                    break;
                case COMPLETED:
                    onTrackingEvent(TrackingType.AD_PLAY_COMPLETED, null);
                    mVideoPlayer.onAdEvent(AdEvent.COMPLETED);
                    break;
            }
        }
    }

    @Override
    public void onAdError(AdError adError, Video video) {
        logAdError(adError, video);
    }

    /**
     * Remote Logs a video ad error.
     * Error codes currently handled by this function:
     * 1005: FAILED_TO_REQUEST_ADS
     */
    @Override
    public void logAdError(AdError adError, Video video) {
        if (adError != null) {
            String errorInfo = "error_type=\"" + adError.getErrorType() + "\" error_code=\"" + adError.getErrorCode() + "\" error_message=\"" + adError.getMessage() + "\" content_url=\"" + video.getContentUrl() + "\" ad_tag_url=\"" + video.getAdTagUrl() + "\"";
            if (adError.getErrorCodeNumber() == 1005) {
                String errorCause = "error_cause=\"malformed URL\"";
                String errorLog = errorCause + " " + errorInfo;
                ((PostTvApplication) mAppContext).logVideoAdError(errorLog);
            } // Can add cases for other causes in the future
        }
    }

    @Override
    public void openYoutubeWeb(String videoId) {
        final Activity activity = ((PostTvApplication) mAppContext).getCurrentActivity();
        if (activity instanceof PostTvActivity) {
            String youtubeURL = mAppContext.getString(R.string.youtube_base_url) + videoId;
            ((PostTvActivity) activity).openWeb(youtubeURL);
        }
    }

    public static boolean isAmazonDevice() {
        return "Amazon".equals(Build.MANUFACTURER);
    }
}