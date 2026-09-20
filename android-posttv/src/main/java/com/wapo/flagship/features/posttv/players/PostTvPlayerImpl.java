package com.wapo.flagship.features.posttv.players;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import com.wapo.android.commons.util.Logger;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.CaptioningManager;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.DefaultDataSourceFactory;
import androidx.media3.datasource.FileDataSource;
import androidx.media3.exoplayer.ExoPlaybackException;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.ima.ImaAdsLoader;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.MediaSourceFactory;
import androidx.media3.exoplayer.source.MergingMediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.exoplayer.source.SingleSampleMediaSource;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.source.ads.AdsMediaSource;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.MappingTrackSelector;
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy;
import androidx.media3.ui.PlayerControlView;
import androidx.media3.ui.PlayerView;

import com.wapo.flagship.features.posttv.R;
import com.wapo.flagship.features.posttv.VideoManager;
import com.wapo.flagship.features.posttv.VideoTracker;
import com.wapo.flagship.features.posttv.listeners.AdErrorListener;
import com.wapo.flagship.features.posttv.listeners.AdEventListener;
import com.wapo.flagship.features.posttv.listeners.PostTvActivity;
import com.wapo.flagship.features.posttv.listeners.PostTvApplication;
import com.wapo.flagship.features.posttv.listeners.VideoListener;
import com.wapo.flagship.features.posttv.listeners.VideoPlayer;
import com.wapo.flagship.features.posttv.model.TrackingType;
import com.wapo.flagship.features.posttv.model.Video;
import com.wapo.flagship.features.posttv.util.PrefManager;

import org.jetbrains.annotations.NotNull;

import java.util.Date;

import rx.Subscription;
import rx.schedulers.Schedulers;

public class PostTvPlayerImpl implements Player.Listener, VideoPlayer,
        MediaSourceFactory, VideoTracker.VideoEventListener {
    private static final String TAG = PostTvPlayerImpl.class.getSimpleName();
    static final String ID_SUBTITLE_URL = "ID_SUBTITLE_URL";

    @NonNull
    private final Context mAppContext;
    @NonNull
    private final VideoListener mListener;
    @NonNull
    private final DataSource.Factory mMediaDataSourceFactory;
    @Nullable
    private ExoPlayer mPlayer;
    @Nullable
    private PlayerView mPlayerView;
    @Nullable
    private String mVideoId;
    private boolean mIsFullScreen = false;
    @Nullable
    private Dialog mFullScreenDialog;
    @Nullable
    protected Video mVideo;
    @Nullable
    private DefaultTrackSelector mTrackSelector;
    @Nullable
    private String mShareUrl;
    @Nullable
    private String mHeadline;
    private float mCurrentVolume = 0f;
    @Nullable
    private VideoTracker mVideoTracker;
    @Nullable
    private ImaAdsLoader mAdsLoader;
    private boolean mIsLive;
    private View ccButton;
    @Nullable
    private Subscription videoTrackingSub;
    private DefaultTrackFilter defaultTrackFilter = new DefaultTrackFilter();

    public static final String PREF_PIP_STATUS = "pref.PREF_PIP_STATUS";

    public PostTvPlayerImpl(@NonNull Context context, @NonNull VideoListener listener) {
        mAppContext = context;
        mListener = listener;
        mMediaDataSourceFactory = new DefaultDataSourceFactory(mAppContext, Util.getUserAgent(mAppContext, mAppContext.getResources().getString(R.string.app_name)));
    }

    @Override
    public void playVideo(@NonNull final Video video) {
        release();
        if (video.getId() == null) {
            if (video.getFallbackUrl() != null) {
                mVideoId = video.getFallbackUrl();
            } else {
                mVideoId = "";
            }
        } else {
            mVideoId = video.getId();
        }
        mVideo = video;
        mIsLive = mVideo.isLive();
        mHeadline = mVideo.getHeadline();
        mShareUrl = mVideo.getShareUrl();
        mTrackSelector = new DefaultTrackSelector(mAppContext);
        mPlayer = new ExoPlayer.Builder(mAppContext)
                .setTrackSelector(mTrackSelector)
                .build();
        final long savedPosition = mListener.getSavedPosition(mVideoId);
        final long savedAdStatus = mListener.getSavedAdStatus(mVideoId);
        mVideo.setAdStatus((int) savedAdStatus);
        mPlayer.addListener(this);
        mPlayerView = new PlayerView(mAppContext);
        mPlayerView.setId(R.id.wapo_player_view);
        mPlayerView.setPlayer(mPlayer);
        mPlayerView.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING);
        final MediaSource contentMediaSource = createMediaSourceWithCaptions(Uri.parse(mVideoId));
        MediaSource adsMediaSource = null;
        DataSpec adTagDataSpec = null;
        if (video.shouldPlayAd() && !mListener.shouldSuppressAds() && !TextUtils.isEmpty(video.getAdTagUrl())) {
            try {
                mAdsLoader = new ImaAdsLoader.Builder(mAppContext)
                        .setAdEventListener(new AdEventListener(mListener))
                        .setAdErrorListener(new AdErrorListener(mListener, video))
                        .build();
                mAdsLoader.setPlayer(mPlayer);
                adTagDataSpec = new DataSpec.Builder()
                        .setUri(Uri.parse(video.getAdTagUrl().replaceAll("\\[(?i)timestamp]", Long.toString(new Date().getTime()))))
                        .build();
                adsMediaSource = new AdsMediaSource(contentMediaSource, adTagDataSpec, video.getAdTagUrl(),this, mAdsLoader, mPlayerView);
            } catch (Exception e) {
                Logger.d(TAG, "Error preparing ad for video " + mVideoId, e);
            }
        }
        if (adsMediaSource != null) {
            mPlayer.setMediaSource(adsMediaSource);
        } else {
            mPlayer.setMediaSource(contentMediaSource);
        }
        mPlayer.prepare();
        if (!mIsFullScreen) {
            mListener.addVideoView(mPlayerView);
        } else {
            addPlayerToFullScreen();
        }
        mVideoTracker = VideoTracker.getInstance(this, mPlayer);
        setUpPlayerControlListeners();
        setAudioAttributes();
        pausePlay(true);
        if (savedPosition >= 0) {
            mPlayer.seekTo(savedPosition);
        }
    }

    @Override
    public void pausePlay(boolean shouldPlay) {
            if (mPlayer != null && mPlayerView != null) {
                mPlayer.setPlayWhenReady(shouldPlay);
                mPlayerView.hideController();
            }
    }

    @Override
    public void toggleCaptions() {
        showCaptionsSelectionDialog();
    }

    @Override
    public boolean isPlaying() {
        if (mPlayer != null) {
            return ((mPlayer.getPlaybackState() == Player.STATE_READY) ) && mPlayer.getPlayWhenReady();
        }
        return false;
    }

    @Override
    public boolean isFullScreen() {
        if (mPlayer != null) {
            return mIsFullScreen;
        }
        return false;
    }

    @Override
    public boolean isInPiP() {
        return false;
    }

    @Override
    public void onAdEvent(VideoListener.AdEvent adEvent) {
        if (mPlayerView == null) {
            return;
        }
        ImageButton pipButton = mPlayerView.findViewById(R.id.exo_pip);
        if (pipButton != null) {
            switch (adEvent) {
                case STARTED:
                    pipButton.setEnabled(false);
                    mVideo.setAdStatus( mVideo.AD_IN_PROGRESS);
                    break;
                case COMPLETED:
                    pipButton.setEnabled(true);
                    mVideo.setAdStatus(mVideo.AD_COMPLETED);
                    break;
            }
        }
    }

    @Override
    public void mute() {

    }

    @Override
    public void respectAudioFocus(Boolean respectAudioFocus) {

    }

    private void setAudioAttributes() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.CONTENT_TYPE_MOVIE)
                .build();
        mPlayer.setAudioAttributes(audioAttributes, true);
    }

    private void setUpPlayerControlListeners() {
        if (mPlayer == null || mPlayerView == null) {
            return;
        }
        PlayerControlView playerControlView = mPlayerView.findViewById(androidx.media3.ui.R.id.exo_controller);
        View exoDuration = playerControlView.findViewById(androidx.media3.ui.R.id.exo_duration);
        ImageButton fullscreenButton = playerControlView.findViewById(R.id.exo_fullscreen);
        fullscreenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFullScreenDialog(mIsFullScreen);
            }
        });
        ImageButton shareButton = playerControlView.findViewById(R.id.exo_share);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Activity activity = ((PostTvApplication) mAppContext).getCurrentActivity();
                if (activity instanceof PostTvActivity && !activity.isFinishing()) {
                    ((PostTvActivity) activity).shareVideo(mHeadline, mShareUrl);
                }
            }
        });
        shareButton.setVisibility(TextUtils.isEmpty(mShareUrl) ? View.GONE : View.VISIBLE);

        ImageButton pipButton = playerControlView.findViewById(R.id.exo_pip);
        pipButton.setImageDrawable(ContextCompat.getDrawable(mAppContext, R.drawable.ic_picture_in_picture_alt_white_24dp));
        if (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)) {
            pipButton.setVisibility(View.GONE);
        }

        pipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Activity activity = ((PostTvApplication) mAppContext).getCurrentActivity();
                if (activity instanceof PostTvActivity && ((PostTvActivity) activity).isPIPEnabled()) {
                   toggleFullScreenDialog(true);
                    if (mPlayerView != null && mIsFullScreen) {
                        if (mPlayerView.getParent() instanceof ViewGroup) {
                            ((ViewGroup) mPlayerView.getParent()).removeView(mPlayerView);
                        }
                        mListener.getPlayerFrame().addView(mPlayerView);
                        ImageButton fullScreenButton = mPlayerView.findViewById(R.id.exo_fullscreen);
                        fullScreenButton.setImageDrawable(ContextCompat.getDrawable(mAppContext, R.drawable.ic_full_screen_expand));
                        mIsFullScreen = false;
                        if (mFullScreenDialog != null) {
                            mFullScreenDialog.dismiss();
                        }
                    }
                    if (mPlayerView != null) {
                        mPlayerView.hideController();
                    }
                    VideoManager videoManager = ((PostTvApplication) mAppContext).getVideoManager();
                    if (videoManager.hasPIPInitiated()) {
                        activity.onBackPressed();
                    } else if ((mVideo) != null) {
                        videoManager.setSavedPosition(mVideoId, mPlayer.getCurrentPosition());
                        videoManager.setHasPIPInitiated(true);
                        ((PostTvActivity) activity).startPIP(mVideo);
                    }
                } else {
                    openPIPSettings();
                }
            }
        });
        final ImageButton volumeButton = playerControlView.findViewById(R.id.exo_volume);
        volumeButton.setImageDrawable(ContextCompat.getDrawable(mAppContext, mPlayer.getVolume() != 0 ? R.drawable.mute_off : R.drawable.mute));
        volumeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Activity activity = ((PostTvApplication) mAppContext).getCurrentActivity();
                if (mPlayer != null && mVideo != null && activity instanceof PostTvActivity
                        && !activity.isFinishing()) {
                    if (mPlayer.getVolume() != 0) {
                        mCurrentVolume = mPlayer.getVolume();
                        mPlayer.setVolume(0f);
                        volumeButton.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.mute));
                        ((PostTvActivity) activity).onTrackingEvent(TrackingType.ON_MUTE, mVideo, true);
                    } else {
                        mPlayer.setVolume(mCurrentVolume);
                        volumeButton.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.mute_off));
                        ((PostTvActivity) activity).onTrackingEvent(TrackingType.ON_MUTE, mVideo, false);
                    }
                }
            }
        });
        ccButton = playerControlView.findViewById(R.id.exo_cc);
        ccButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAppContext instanceof PostTvApplication) {
                    showCaptionsSelectionDialog();
                }
            }
        });
        View exoPosition = playerControlView.findViewById(androidx.media3.ui.R.id.exo_position);
        View exoProgress = playerControlView.findViewById(androidx.media3.ui.R.id.exo_progress);
        if (mIsLive) {
            exoPosition.setVisibility(View.GONE);
            exoDuration.setVisibility(View.GONE);
            exoProgress.setVisibility(View.GONE);
        } else {
            exoPosition.setVisibility(View.VISIBLE);
            exoDuration.setVisibility(View.VISIBLE);
            exoProgress.setVisibility(View.VISIBLE);
        }
    }

    private void openPIPSettings () {
        final Activity activity = ((PostTvApplication) mAppContext).getCurrentActivity();
        new AlertDialog.Builder(activity)
                .setTitle("Picture-in-Picture functionality is disabled")
                .setMessage("Would you like to enable Picture-in-Picture?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", mAppContext.getPackageName(), null);
                        intent.setData(uri);
                        activity.startActivity(intent);
                    }

                })
                .setNegativeButton(android.R.string.cancel, null)
                .setCancelable(true)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    /**
     * Enable/Disable captions rendering according to user preferences
     */
    private void initCaptions() {
        boolean captionsEnabled = isVideoCaptionsEnabled();
        final Activity activity = ((PostTvApplication) mAppContext).getCurrentActivity();
        if (activity instanceof PostTvActivity && !activity.isFinishing() && mTrackSelector != null) {
            final MappingTrackSelector.MappedTrackInfo mappedTrackInfo = mTrackSelector.getCurrentMappedTrackInfo();
            if (mappedTrackInfo != null) {
                final int textRendererIndex = getTextRendererIndex(mappedTrackInfo);
                if (textRendererIndex != -1) {
                    DefaultTrackSelector.Parameters.Builder parametersBuilder = mTrackSelector.buildUponParameters();
                    parametersBuilder.setRendererDisabled(textRendererIndex, !captionsEnabled);
                    mTrackSelector.setParameters(parametersBuilder);
                }
            }
        }
    }

    /**
     * Returns a number of caption tracks that have a non-null language
     */
    private int hasAvailableSubtitlesTracks(MappingTrackSelector.MappedTrackInfo mappedTrackInfo, int textRendererIndex) {
        int result = 0;
        TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(textRendererIndex);
        for (int groupIndex = 0; groupIndex < trackGroups.length; groupIndex++) {
            TrackGroup group = trackGroups.get(groupIndex);
            for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
                Format format = group.getFormat(trackIndex);
                Logger.d(TAG, "format: " + format);
                if (defaultTrackFilter.filter(format, trackGroups)) {
                    result++;
                }
            }
        }
        return result;
    }

    private void showCaptionsSelectionDialog() {
        final Activity activity = ((PostTvApplication) mAppContext).getCurrentActivity();
        if (activity instanceof PostTvActivity && !activity.isFinishing() && mTrackSelector != null) {
            final MappingTrackSelector.MappedTrackInfo mappedTrackInfo = mTrackSelector.getCurrentMappedTrackInfo();
            if (mappedTrackInfo != null) {
                final int textRendererIndex = getTextRendererIndex(mappedTrackInfo);
                if (textRendererIndex != -1) {
                    Pair<AlertDialog, WaPoTrackSelectionView> dialogPair =
                            WaPoTrackSelectionView.getDialog(((PostTvApplication) mAppContext).getCurrentActivity(),
                                    mAppContext.getString(R.string.captions_dialog_title),
                                    mTrackSelector,
                                    textRendererIndex,
                                    defaultTrackFilter);
                    dialogPair.second.setShowDisableOption(true);
                    dialogPair.second.setAllowAdaptiveSelections(false);
                    dialogPair.second.setShowDefault(false);
                    dialogPair.first.show();

                    dialogPair.first.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            // save the chosen option to preferences
                            DefaultTrackSelector.Parameters parameters = mTrackSelector.getParameters();
                            boolean isDisabled = parameters.getRendererDisabled(textRendererIndex);
                            setVideoCaptionsEnabled(!isDisabled);
                        }
                    });
                }
            }
        }
    }

    private int getTextRendererIndex(MappingTrackSelector.MappedTrackInfo mappedTrackInfo) {
        int count = mappedTrackInfo.getRendererCount();
        for (int i = 0; i < count; i++) {
            if (mappedTrackInfo.getRendererType(i) == C.TRACK_TYPE_TEXT) {
                return i;
            }
        }
        return -1;
    }

    private synchronized void toggleFullScreenDialog(boolean isFullScreen) {
        if (!isFullScreen) {
            if (((PostTvApplication) mAppContext).getCurrentActivity() instanceof PostTvActivity && !(((PostTvApplication) mAppContext).getCurrentActivity().isFinishing())) {
                mFullScreenDialog = new Dialog(((PostTvApplication) mAppContext).getCurrentActivity(), android.R.style.Theme_Black_NoTitleBar_Fullscreen) {
                    public void onBackPressed() {
                        toggleFullScreenDialog(true);
                    }
                };
            }
        }
        if (mFullScreenDialog != null) {
            if (!isFullScreen) {
                if (mPlayerView != null && mPlayerView.getParent() instanceof ViewGroup) {
                    ((ViewGroup) mPlayerView.getParent()).removeView(mPlayerView);
                }
                addPlayerToFullScreen();
                ImageButton fullScreenButton = mPlayerView.findViewById(R.id.exo_fullscreen);
                fullScreenButton.setImageDrawable(ContextCompat.getDrawable(mAppContext, R.drawable.ic_full_screen_collapse));
                mFullScreenDialog.show();
                mIsFullScreen = true;
                mListener.onTrackingEvent(TrackingType.ON_OPEN_FULL_SCREEN, null);
            } else {
                if (mPlayerView != null) {
                    if (mPlayerView.getParent() instanceof ViewGroup) {
                        ((ViewGroup) mPlayerView.getParent()).removeView(mPlayerView);
                    }
                    mListener.getPlayerFrame().addView(mPlayerView);
                    ImageButton fullScreenButton = mPlayerView.findViewById(R.id.exo_fullscreen);
                    fullScreenButton.setImageDrawable(ContextCompat.getDrawable(mAppContext, R.drawable.ic_full_screen_expand));
                }
                mIsFullScreen = false;
                if (((PostTvApplication) mAppContext).getCurrentActivity() != null && !(((PostTvApplication) mAppContext).getCurrentActivity().isFinishing())) {
                    mFullScreenDialog.dismiss();
                }
                mFullScreenDialog = null;
            }
        }
    }

    private void addPlayerToFullScreen() {
        if (mFullScreenDialog != null && mPlayerView != null) {
            mFullScreenDialog.addContentView(mPlayerView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }

    @Override
    public void onActivityResume() {
        if (mIsFullScreen && mVideo != null && mPlayerView == null) {
            playVideo(mVideo);
        }
    }

    @Override
    public void release() {
        if (mPlayerView != null) {
            if (mPlayerView.getParent() instanceof ViewGroup) {
                ((ViewGroup) mPlayerView.getParent()).removeView(mPlayerView);
            }
            mPlayerView = null;
        }
        if (videoTrackingSub != null) {
            videoTrackingSub.unsubscribe();
            videoTrackingSub = null;
        }
        if (mPlayer != null) {
            if (!mPlayer.isPlayingAd()) {
                if (mPlayer.getPlaybackState() == Player.STATE_ENDED) {
                    mListener.setSavedPosition(mVideoId, VideoManager.NO_POSITION);
                    mListener.setSavedAdStatus(mVideoId, mVideo.getAdStatus());
                } else {
                    mListener.setSavedPosition(mVideoId, mPlayer.getCurrentPosition());
                    mListener.setSavedAdStatus(mVideoId, mVideo.getAdStatus());
                }
            }
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }
        if (mTrackSelector != null) {
            mTrackSelector = null;
        }
        if (mAdsLoader != null) {
            mAdsLoader.release();
            mAdsLoader = null;
        }
        if (!mIsFullScreen) {
            mListener.removePlayerFrame();
        }
        mVideo = null;
    }

    @Nullable
    @Override
    public String getId() {
        return mVideoId;
    }

    @Nullable
    @Override
    public Video getVideo() {
        return mVideo;
    }

    @Override
    public void onTracksChanged(Tracks tracks) {
        updateSubtitlesControlVisibility();
    }

    private void updateSubtitlesControlVisibility() {
        final Activity activity = ((PostTvApplication) mAppContext).getCurrentActivity();
        if (activity instanceof PostTvActivity && !activity.isFinishing() && ccButton != null && mTrackSelector != null) {
            final MappingTrackSelector.MappedTrackInfo mappedTrackInfo = mTrackSelector.getCurrentMappedTrackInfo();
            if (mappedTrackInfo != null) {
                final int textRendererIndex = getTextRendererIndex(mappedTrackInfo);
                if (textRendererIndex != -1) {
                    int availableTracksCount = hasAvailableSubtitlesTracks(mappedTrackInfo, textRendererIndex);
                    if (availableTracksCount > 0) {
                        ccButton.setVisibility(View.VISIBLE);
                    } else {
                        ccButton.setVisibility(View.GONE);
                    }
                }
            }
        }
    }

    @Override
    public void onPlaybackStateChanged(int playerState) {
        if (mPlayer == null || mVideoTracker == null || mPlayerView == null) {
            return;
        }
        if (playerState != Player.STATE_BUFFERING) {
            if (playerState != Player.STATE_IDLE
                    && playerState != Player.STATE_ENDED) {
                mPlayerView.setKeepScreenOn(true);
                if (!mPlayer.isPlayingAd()) {
                    initCaptions();
                }
                if (mVideoTracker != null && (videoTrackingSub == null
                        || videoTrackingSub.isUnsubscribed())) {
                    videoTrackingSub = mVideoTracker
                            .getObs()
                            .subscribeOn(Schedulers.io())
                            .observeOn(Schedulers.io())
                            .subscribe();
                }
            } else if (mVideoId != null) {
                mPlayerView.setKeepScreenOn(false);
                if (((PostTvApplication) mAppContext).getVideoManager().isInPIP()) {
                    ((PostTvApplication) mAppContext).pausePIP();
                }
                if (playerState == Player.STATE_ENDED) {
                    if (mIsFullScreen) {
                        toggleFullScreenDialog(true);
                    }
                    mListener.onTrackingEvent(TrackingType.ON_PLAY_COMPLETED, null);
                    mListener.release();
                } else {
                    mListener.setSavedPosition(mVideoId, mPlayer.getCurrentPosition());
                }
                if (videoTrackingSub != null) {
                    videoTrackingSub.unsubscribe();
                    videoTrackingSub = null;
                }
            }
        }
    }

    @Override
    public void onPlayerError(PlaybackException error) {
        ExoPlaybackException e = error instanceof ExoPlaybackException? (ExoPlaybackException) error : null;
        if (e != null && e.type == ExoPlaybackException.TYPE_SOURCE) {
            mListener.onError(mAppContext.getString(R.string.source_error));

            if (e.getSourceException() instanceof FileDataSource.FileDataSourceException) {
                // no url passed from backend
                mListener.logError("Exoplayer Source Error: No url passed from backend. Caused by:\n" + e.getSourceException());
            }
        } else if (e != null && e.type == ExoPlaybackException.TYPE_RENDERER) {
            mListener.onError(mAppContext.getString(R.string.render_error));
        } else {
            mListener.onError(mAppContext.getString(R.string.unknown_error));
        }
        Logger.d(TAG, "ExoPlayer Error", e);
    }

    @Nullable
    private MediaSource createMediaSourceWithCaptions(Uri uri) {
        MediaSource videoMediaSource = createMediaSource(MediaItem.fromUri(uri));
        if (videoMediaSource != null) {
            if (mVideo != null && mVideo.getSubtitleUrl() != null) {
                Format format = new Format.Builder()
                        .setId(ID_SUBTITLE_URL)
                        .setSampleMimeType(MimeTypes.TEXT_VTT)
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .setLanguage("en")
                        .build();
                SingleSampleMediaSource singleSampleSource = new SingleSampleMediaSource.Factory(mMediaDataSourceFactory)
                        .createMediaSource(new MediaItem.Subtitle(Uri.parse(mVideo.getSubtitleUrl()), format.sampleMimeType, format.language, format.selectionFlags), C.TIME_UNSET);
                return new MergingMediaSource(videoMediaSource, singleSampleSource);
            }
        }
        return videoMediaSource;
    }

    private boolean isVideoCaptionsEnabled() {
        boolean defaultValue = false;
        if (Build.VERSION.SDK_INT >= 19) {
            Object service = mAppContext.getSystemService(Context.CAPTIONING_SERVICE);
            if (service instanceof CaptioningManager) {
                defaultValue = ((CaptioningManager) service).isEnabled();
            }
        }
        return PrefManager.getBoolean(mAppContext, PrefManager.IS_CAPTIONS_ENABLED, defaultValue);
    }

    private void setVideoCaptionsEnabled(boolean value) {
        PrefManager.saveBoolean(mAppContext, PrefManager.IS_CAPTIONS_ENABLED, value);
    }

    @Override
    public MediaSourceFactory setDrmSessionManagerProvider(@Nullable DrmSessionManagerProvider drmSessionManagerProvider) {
        return null;
    }

    @NotNull
    @Override
    public MediaSourceFactory setLoadErrorHandlingPolicy(@Nullable @org.jetbrains.annotations.Nullable LoadErrorHandlingPolicy loadErrorHandlingPolicy) {
        return null;
    }

    @NotNull
    @Override
    public int[] getSupportedTypes() {
        // IMA does not support Smooth Streaming ads.
        return new int[]{C.TYPE_HLS, C.TYPE_OTHER};
    }

    @NotNull
    @Override
    public MediaSource createMediaSource(@NotNull MediaItem mediaItem) {
        return buildMediaSource(mediaItem);
    }

    @SuppressLint("SwitchIntDef")
    private MediaSource buildMediaSource(MediaItem mediaItem) {
        String url = mediaItem.mediaId;
        if (TextUtils.isEmpty(url) && mediaItem.localConfiguration != null) {
            url = mediaItem.localConfiguration.uri.toString();
        }
        if (TextUtils.isEmpty(url)) return null;
        @C.ContentType int type = Util.inferContentType(Uri.parse(url));
        switch (type) {
            case C.TYPE_HLS:
                return new HlsMediaSource.Factory(mMediaDataSourceFactory).createMediaSource(mediaItem);
            case C.TYPE_OTHER:
                return new ProgressiveMediaSource.Factory(mMediaDataSourceFactory).createMediaSource(mediaItem);
            default:
                return null;
        }
    }

    @Override
    public void onVideoEvent(@NotNull TrackingType trackingType, @Nullable Object value) {
        mListener.onTrackingEvent(trackingType, value);
    }
}