package com.wapo.flagship.features.video;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Rational;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.wapo.android.commons.logs.EventLog;
import com.wapo.flagship.FlagshipApplication;
import com.wapo.flagship.Utils;
import com.wapo.flagship.features.posttv.VideoManager;
import com.wapo.flagship.features.posttv.VideoTracker2;
import com.wapo.flagship.features.posttv.listeners.PostTvActivity;
import com.wapo.flagship.features.posttv.model.TrackingType;
import com.wapo.flagship.features.posttv.model.Video;
import com.wapo.flagship.features.shared.activities.BaseActivity;
import com.wapo.flagship.json.AdConfig;
import com.wapo.android.commons.util.Logger;
import com.wapo.flagship.util.PrefUtils;
import com.wapo.flagship.util.Share;
import com.wapo.flagship.util.UIUtil;
import com.wapo.flagship.util.tracking.Measurement;
import com.wapo.flagship.wrappers.CrashWrapper;
import com.washingtonpost.android.R;
import com.washingtonpost.android.paywall.features.ccpa.CCPAUtils;
import com.washingtonpost.android.volley.Cache;
import com.washingtonpost.android.volley.NetworkResponse;
import com.washingtonpost.android.volley.Response;
import com.washingtonpost.android.volley.VolleyError;
import com.washingtonpost.android.volley.toolbox.HttpHeaderParser;
import com.washingtonpost.android.volley.toolbox.JsonRequest;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VideoActivity extends BaseActivity implements PostTvActivity {
    public static final String VideoInfoUrlExtraParamName = VideoActivity.class.getSimpleName() + ".videoInfoUrl";

    public final static int PRE_ROLL_TIMEOUT = 1;
    public static final String PARAM_MEDIA_URL = VideoActivity.class.getSimpleName() + ".mediaUrl";
    public static final String PARAM_TITLE = VideoActivity.class.getSimpleName() + ".title";
    public static final String PARAM_SHARE_URL = VideoActivity.class.getSimpleName() + ".shareUrl";
    public static final String PARAM_DESCRIPTION = VideoActivity.class.getSimpleName() + ".desc";
    public static final String PARAM_SUBTITLES_URL = VideoActivity.class.getSimpleName() + ".subsUrl";
    public static final String PARAM_AD_CONFIG = VideoActivity.class.getSimpleName() + ".adConfig";
    public static final String PARAM_FALLBACK_URL = VideoActivity.class.getSimpleName() + ".fallbackURL";
    public static final String PARAM_FORCE_LANDSCAPE = VideoActivity.class.getSimpleName() + ".forceLandscape";
    public static final String PARAM_VIDEO_NAME = VideoActivity.class.getSimpleName() + ".videoName";
    public static final String PARAM_VIDEO_SECTION = VideoActivity.class.getSimpleName() + ".videoSection";
    public static final String PARAM_VIDEO_SOURCE = VideoActivity.class.getSimpleName() + ".videoSource";
    public static final String PARAM_CONTENT_ID = VideoActivity.class.getSimpleName() + ".contentId";
    public static final String VIDEO_HOST_YOUTUBE = "youtube";
    public static final String IsPIPRequest = "isPIPRequest";
    public static final String IsCaptionsAvailable = "isCaptionsAvailable";

    private static final String EXTRA_START_WITH = "EXTRA_START_WITH";

    //    private static Handler preRollHandler = new Handler() {
//        @Override
//        public void handleMessage(Message msg) {
//            switch (msg.what) {
//                case PRE_ROLL_TIMEOUT:
//                    assert msg.obj != null;
//                    ((Runnable) msg.obj).run();
//                    break;
//            }
//        }
//    };
    private static final String TAG = VideoActivity.class.getName();
    private static final String PlayheadTimeParamName = VideoActivity.class.getSimpleName() + ".playheadTime";

    public static final String ACTION_MEDIA_CONTROL = "media_control";

    public static final String EXTRA_CONTROL_TYPE = "control_type";

    private static final int REQUEST_PLAY = 1;

    private static final int REQUEST_PAUSE = 2;

    private static final int REQUEST_CAPTIONS = 3;

    private static final int CONTROL_TYPE_CAPTIONS = 3;

    private static final int CONTROL_TYPE_PLAY = 1;

    public static final int CONTROL_TYPE_PAUSE = 2;


    private String _mediaUrl;
    private String _title;
    private String _shareUrl;
    private String _description;
    private String _subtitlesUrl;
    private AdConfig _adConfig;

    private ViewGroup _descPanel;
    private ViewGroup _curtain;
    private ImageButton _shareButton;
    private FrameLayout _video_container;
    private boolean shouldReopenOmni = false;
    private String mediaName;
    private double mediaLength;
    private String playerName = "Android_Sample_Player";
    private ViewGroup _topPanel;
    private ViewGroup _videoLayoutContainer;
    private boolean wasPlayingBeforeShare;
    private DisplayMetrics displaymetrics = new DisplayMetrics();
    private String fallBackURL;
    private String videoName;
    private String videoSection;
    private String videoSource;
    private String contentId;
    private Boolean isInPIP;
    private BroadcastReceiver mReceiver;
    private boolean mAdjustViewBounds;
    private boolean hasIniated = false;
    private BroadcastReceiver broadcastReceiver;
    private boolean isPIPStopRequest = false;
    private PictureInPictureParams.Builder mPictureInPictureParamsBuilder;
    private boolean isCaptionsAvailable = false;
    private VideoManager videoManager = FlagshipApplication.getInstance().getVideoManager();

    public static Intent createIntent(
            Context context,
            Class<?> activityClass,
            String mediaUrl,
            String title,
            String shareUrl,
            String description,
            String subtitlesUrl,
            AdConfig adConfig,
            String fallbackURL,
            String videoName,
            String videoSection,
            String videoSource,
            String contentId
    ) {
        return createIntent(
                context,
                activityClass,
                mediaUrl,
                title,
                shareUrl,
                description,
                subtitlesUrl,
                adConfig,
                fallbackURL,
                videoName,
                videoSection,
                videoSource,
                contentId,
                false
        );
    }

    public static Intent createIntent(
            Context context,
            Class<?> activityClass,
            String mediaUrl,
            String title,
            String shareUrl,
            String description,
            String subtitlesUrl,
            AdConfig adConfig,
            String fallbackURL,
            String videoName,
            String videoSection,
            String videoSource,
            String contentId,
            boolean forceLandscape
    ) {
        return new Intent(context, activityClass)
                .putExtra(PARAM_MEDIA_URL, mediaUrl)
                .putExtra(PARAM_TITLE, title)
                .putExtra(PARAM_SHARE_URL, shareUrl)
                .putExtra(PARAM_DESCRIPTION, description)
                .putExtra(PARAM_SUBTITLES_URL, subtitlesUrl)
                .putExtra(PARAM_FALLBACK_URL, fallbackURL)
                .putExtra(PARAM_FORCE_LANDSCAPE, forceLandscape)
                .putExtra(PARAM_VIDEO_NAME, videoName)
                .putExtra(PARAM_VIDEO_SECTION, videoSection)
                .putExtra(PARAM_VIDEO_SOURCE, videoSource)
                .putExtra(PARAM_CONTENT_ID, contentId);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        broadcastReceiver = new BroadcastReceiver() {

            @SuppressLint("NewApi")
            @Override
            public void onReceive(Context arg0, Intent intent) {
                String action = intent.getAction();
                if (action != null && action.equals("finish_PIPVideoActivity")) {
                    if (UIUtil.isPIPSupported()) {
                        isPIPStopRequest = true;
                        VideoActivity.this.finishAndRemoveTask();
                    }
                }
            }
        };
        ContextCompat.registerReceiver(this, broadcastReceiver, new IntentFilter("finish_PIPVideoActivity"), ContextCompat.RECEIVER_NOT_EXPORTED);

        if (getIntent().getBooleanExtra(PARAM_FORCE_LANDSCAPE, false)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        hideActionBar();

        final String videoArticleUrl = getIntent().getStringExtra(VideoInfoUrlExtraParamName);
        extractVideoParams(savedInstanceState);
        if (videoArticleUrl == null && _mediaUrl == null) {
            finish();
            return;
        }

        if (UIUtil.isPIPSupported() && mPictureInPictureParamsBuilder == null) {
            mPictureInPictureParamsBuilder = new PictureInPictureParams.Builder();
        }

        _video_container = (FrameLayout) findViewById(R.id.video_container);
        _videoLayoutContainer = (ViewGroup) findViewById(R.id.video_layout_container);
        _descPanel = (ViewGroup) findViewById(R.id.video_description_panel);
        _curtain = (ViewGroup) findViewById(R.id.video_curtain);
        _shareButton = (ImageButton) findViewById(R.id.video_share);
        _topPanel = (ViewGroup) findViewById(R.id.video_top_panel);
        if (getIntent().getExtras() != null) {
            if (getIntent().getExtras().getBoolean(VideoActivity.IsPIPRequest)) {
                Measurement.trackPIPEnter("inline");
                videoManager.release();
                videoManager.setmIsInPIP(true);
                minimize();

            }
            if (getIntent().getExtras().getBoolean(VideoActivity.IsCaptionsAvailable)) {
                isCaptionsAvailable = true;
            }
        } else {
            overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
        }

        CrashWrapper.logExtras("Start Video Activity with URL" + videoArticleUrl);

        configureShare();
        configurePrimaryLayout(_topPanel);
    }

    @SuppressLint("NewApi")
    void updatePictureInPictureActions(
            @DrawableRes int iconId, String title, int controlType, int requestCode) {
        final ArrayList<RemoteAction> actions = new ArrayList<>();
        if (UIUtil.isPIPSupported()) {
            if (isCaptionsAvailable) {
                actions.add(new RemoteAction(Icon.createWithResource(VideoActivity.this, com.wapo.flagship.features.posttv.R.drawable.ic_cc), "caption", "Toggle captions", PendingIntent.getBroadcast(
                        VideoActivity.this,
                        REQUEST_CAPTIONS,
                        new Intent(ACTION_MEDIA_CONTROL).putExtra(EXTRA_CONTROL_TYPE, CONTROL_TYPE_CAPTIONS),
                        PendingIntent.FLAG_IMMUTABLE)));
            }
            final PendingIntent intent =
                    PendingIntent.getBroadcast(
                            VideoActivity.this,
                            requestCode,
                            new Intent(ACTION_MEDIA_CONTROL).putExtra(EXTRA_CONTROL_TYPE, controlType),
                            PendingIntent.FLAG_IMMUTABLE);
            final Icon icon = Icon.createWithResource(VideoActivity.this, iconId);
            actions.add(new RemoteAction(icon, title, title, intent));

            mPictureInPictureParamsBuilder.setActions(actions);
            setPictureInPictureParams(mPictureInPictureParamsBuilder.build());
        }
    }

    @Override
    public void onPictureInPictureModeChanged(
            boolean isInPictureInPictureMode, Configuration configuration) {
        adjustFullScreen(configuration);
        if (isInPictureInPictureMode) {
            isInPIP = true;

            videoManager.setmIsInPIP(true);
            videoManager.setHasPIPInitiated(true);
            videoManager.pausePlay(true);
            updatePictureInPictureActions(
                    R.drawable.gallery_pause, "pause", CONTROL_TYPE_PAUSE, REQUEST_PAUSE);

            mReceiver =
                    new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            if (intent == null
                                    || !ACTION_MEDIA_CONTROL.equals(intent.getAction())) {
                                return;
                            }
                            final int controlType = intent.getIntExtra(EXTRA_CONTROL_TYPE, 0);
                            switch (controlType) {
                                case CONTROL_TYPE_PLAY:
                                    videoManager.pausePlay(true);
                                    updatePictureInPictureActions(
                                            R.drawable.gallery_pause, "pause", CONTROL_TYPE_PAUSE, REQUEST_PAUSE);
                                    break;
                                case CONTROL_TYPE_PAUSE:
                                    videoManager.pausePlay(false);
                                    updatePictureInPictureActions(
                                            R.drawable.gallery_play, "play", CONTROL_TYPE_PLAY, REQUEST_PLAY);
                                    break;
                                case CONTROL_TYPE_CAPTIONS:
                                    videoManager.toggleCaptions();
                                    break;
                            }

                        }

                    };
            ContextCompat.registerReceiver(this, mReceiver, new IntentFilter(ACTION_MEDIA_CONTROL), ContextCompat.RECEIVER_NOT_EXPORTED);
        } else {
            isInPIP = false;
            if (mReceiver != null)
                unregisterReceiver(mReceiver);
            mReceiver = null;

        }
        hasIniated = true;
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, configuration);
    }


    private void hideActionBar() {
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.hide();
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onBackPressed() {
        if (UIUtil.isPIPSupported()) {
            if (!isInPictureInPictureMode()) {
                Measurement.trackPIPEnter("fullscreen");
                minimize();
            } else {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

    @SuppressLint("NewApi")
    @Override
    protected void onUserLeaveHint() {
        if (UIUtil.isPIPSupported()) {
            if (!isInPictureInPictureMode())
                minimize();
            else {
                super.onUserLeaveHint();
            }
        } else {
            super.onUserLeaveHint();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        final String videoArticleUrl = getIntent().getStringExtra(VideoInfoUrlExtraParamName);
        if (_mediaUrl == null) {
            //this case should not happen any longer because feeds will never return a video article
            //but for safety, forward to a webview
            if (videoArticleUrl != null) {
                launchWebView(videoArticleUrl);
            } else {
                finish();
            }
        } else {
            startCurrentVideo();
        }
    }

    private void extractVideoParams(Bundle saved) {
        Bundle bundle = saved == null ? getIntent().getExtras() : saved;

        if (bundle == null) {
            return;
        }

        _mediaUrl = bundle.getString(PARAM_MEDIA_URL);
        _title = bundle.getString(PARAM_TITLE);
        _shareUrl = bundle.getString(PARAM_SHARE_URL);
        _description = bundle.getString(PARAM_DESCRIPTION);
        _subtitlesUrl = bundle.getString(PARAM_SUBTITLES_URL);
        fallBackURL = bundle.getString(PARAM_FALLBACK_URL);
        String json = bundle.getString(PARAM_AD_CONFIG);
        videoName = bundle.getString(PARAM_VIDEO_NAME);
        videoSection = bundle.getString(PARAM_VIDEO_SECTION);
        videoSource = bundle.getString(PARAM_VIDEO_SOURCE);
        contentId = bundle.getString(PARAM_CONTENT_ID);
    }
    private void configurePrimaryLayout(ViewGroup topPanel) {
        this.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int rectWidth = displaymetrics.widthPixels;
        int rectHeight = displaymetrics.heightPixels;
        int topMargin = 0;

        topPanel.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.EXACTLY));
        _descPanel.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.AT_MOST));

        ViewGroup.LayoutParams curtainParams = _curtain.getLayoutParams();
        if (curtainParams == null) {
            curtainParams = new ViewGroup.LayoutParams(rectWidth, rectHeight - topPanel.getMeasuredHeight());
            _curtain.setLayoutParams(curtainParams);
        } else {
            curtainParams.height = rectHeight - topPanel.getMeasuredHeight();
        }

        _video_container.setTop(topPanel.getMeasuredHeight());

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) _video_container.getLayoutParams();
        if (params == null || getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            params = new RelativeLayout.LayoutParams(rectWidth, rectWidth / 16 * 9);
            params.topMargin = topMargin;
            _video_container.setLayoutParams(params);
        } else {
            params.width = rectWidth;
            params.height = rectWidth / 16 * 9;
            params.topMargin = topMargin;
        }
    }

    //for fix android bug with AppCompat
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return keyCode == KeyEvent.KEYCODE_MENU || super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Measurement.resumeCollection(this);
        if (videoManager.getPlayerFrame().getParent() == null) {
            startCurrentVideo();
        }
    }

    @SuppressLint("NewApi")
    @Override
    protected void onStop() {
        CrashWrapper.logExtras("Stop Video Activity");
        if (UIUtil.isPIPSupported()) {
            videoManager.setHasPIPInitiated(false);
            if (isInPIP != null && isInPIP) {
                Measurement.trackPIPExit("pip");
            }
            if (isInPIP != null && broadcastReceiver != null)
                if (isInPIP && !broadcastReceiver.isInitialStickyBroadcast() && UIUtil.isPIPSupported()) {
                    this.finishAndRemoveTask();
                }
            if(!isPIPStopRequest) {
                videoManager.setmIsInPIP(false);
                if (!videoManager.isInPIP()) {
                    videoManager.release();
                }
            }

        }
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }

        super.onStop();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }

    @SuppressLint("NewApi")
    private void minimize() {
        if (_video_container == null) {
            return;
        }
        if (UIUtil.isPIPSupported()) {
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int screenHeight = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, getResources().getConfiguration().screenHeightDp, getResources().getDisplayMetrics()));
            int screenWidth = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, getResources().getConfiguration().screenWidthDp, getResources().getDisplayMetrics()));
            int height, width;
            width = screenHeight;
            height = screenHeight / 16 * 9;
            Rational aspectRatio = new Rational(width, height);

            mPictureInPictureParamsBuilder.setAspectRatio(aspectRatio).build();
            enterPictureInPictureMode(mPictureInPictureParamsBuilder.build());
        }
    }

    private void setAdjustViewBounds(boolean adjustViewBounds) {
        if (mAdjustViewBounds == adjustViewBounds) {
            return;
        }
        mAdjustViewBounds = adjustViewBounds;
        if (adjustViewBounds) {
            _video_container.setBackgroundColor(Color.RED);
        } else {
            _video_container.setBackgroundColor(Color.BLUE);
        }
        _videoLayoutContainer.requestLayout();
    }

    private void adjustFullScreen(Configuration config) {
        final View decorView = getWindow().getDecorView();
        View descPanel = findViewById(R.id.video_description_panel);
        View topLayout = findViewById(R.id.video_layout_container);
        View curtain = findViewById(R.id.video_curtain);
        View errCurtain = findViewById(R.id.video_error_curtain);
        View topPanel = findViewById(R.id.video_top_panel);

        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE || isInPIP == null || isInPIP) {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);
            descPanel.setVisibility(View.GONE);
            curtain.setVisibility(View.GONE);
            errCurtain.setVisibility(View.GONE);
            topPanel.setVisibility(View.GONE);
            setAdjustViewBounds(false);
        } else {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            descPanel.setVisibility(View.VISIBLE);
            topLayout.setVisibility(View.VISIBLE);
            curtain.setVisibility(View.VISIBLE);
            errCurtain.setVisibility(View.VISIBLE);
            topPanel.setVisibility(View.VISIBLE);
            setAdjustViewBounds(true);
        }
    }

    @Override
    protected void onDestroy() {
        videoManager.setmIsInPIP(false);
//        if (preRollHandler != null) {
//            preRollHandler.removeMessages(PRE_ROLL_TIMEOUT);
//        }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(PlayheadTimeParamName, 0);
        outState.putString(PARAM_MEDIA_URL, _mediaUrl);
        outState.putString(PARAM_TITLE, _title);
        outState.putString(PARAM_SHARE_URL, _shareUrl);
        outState.putString(PARAM_DESCRIPTION, _description);
        outState.putString(PARAM_SUBTITLES_URL, _subtitlesUrl);
        outState.putString(PARAM_VIDEO_NAME, videoName);
        outState.putString(PARAM_VIDEO_SECTION, videoSection);
        outState.putString(PARAM_VIDEO_SOURCE, videoSource);
        outState.putString(PARAM_CONTENT_ID, contentId);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        final int offset = savedInstanceState.getInt(PlayheadTimeParamName, 0);
        if (offset > 0) {
            videoManager.setSavedPosition(_mediaUrl, offset);
        }
    }

    private void launchWebView(String videoURL) {
        if (TextUtils.isEmpty(videoURL)) return;
        Utils.startWeb(videoURL, this);
        finish();
    }

    private void configureShare() {
        _shareButton.setEnabled(false);
        _shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                videoManager.release();
                share();
            }
        });
    }

    private void share(){
        new Share.Builder()
                .shareUrl(_shareUrl)
                .headline(videoName)
                .fromPush(false)
                .arcId("")
                .title(getString(R.string.share_dialog_title))
                .isVideoShare(true)
                .build().shareItem(this);
    }

    private void resetWasPlayingBeforeShare() {
        wasPlayingBeforeShare = false;
    }

    private void startCurrentVideo() {
        _shareButton.setEnabled(true);
        TextView title = (TextView) findViewById(R.id.video_title);
        title.setText(_title);

        TextView desc = (TextView) findViewById(R.id.video_description);
        desc.setMovementMethod(new ScrollingMovementMethod());
        if (_description != null) {
            desc.setText(Html.fromHtml(_description));
        }

        revealVideo();

        Logger.i(TAG, "the current video URL: " + _mediaUrl);

        String adTagUrl = "";
        if (_adConfig != null && _adConfig.getAdSetConfig() != null && _adConfig.getAdSetConfig().getAdSetUrls() != null) {
            adTagUrl = _adConfig.getAdSetConfig().getAdSetUrls().getApps();
            adTagUrl = CCPAUtils.appendCCPAQueryParameterToUrl(adTagUrl);
        }
        long startPos = getIntent().getLongExtra(EXTRA_START_WITH, 0L);

        final Video video = new Video.Builder()
                .setId(_mediaUrl)
                .setIsYouTube(false)
                .setIsLive(false)
                .setShareUrl(null)
                .setHeadline(null)
                .setPageName(null)
                .setVideoName(null)
                .setVideoSection(null)
                .setVideoSource(null)
                .setVideoCategory(null)
                .setSubtitleUrl(_subtitlesUrl)
                .setAdTagUrl(adTagUrl)
                .setStartPos(startPos)
                .build();
        videoManager.setmIsInPIP(false);
        videoManager.initMedia(video);
        videoManager.setmIsInPIP(true);
        if (videoManager.getPlayerFrame().getParent() == null) {
            _video_container.addView(videoManager.getPlayerFrame());
        }


    }

    private void revealVideo() {
        if (Thread.currentThread().getId() != getMainLooper().getThread().getId()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    revealVideo();
                }
            });
            return;
        }

        _curtain.setVisibility(View.GONE);
        _video_container.setVisibility(View.VISIBLE);
        _descPanel.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        ViewGroup errorCurtain = (ViewGroup) findViewById(R.id.video_error_curtain);

        Toast.makeText(VideoActivity.this, message, Toast.LENGTH_LONG).show();

        _curtain.setVisibility(View.GONE);
        errorCurtain.setVisibility(View.VISIBLE);
    }

    @Override
    public void onConfigurationChanged(@NotNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        resizeVideoContainer();
    }

    private void resizeVideoContainer() {
        this.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int width = displaymetrics.widthPixels;
        int height = width / 16 * 9;

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, height);

        initVideoContainerLayout(lp);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            lp.topMargin = (int) UIUtil.dip2Px(50, this);
        }

        _video_container.setLayoutParams(lp);
    }

    public static void withStartPosition(Intent intent, long startWith) {
        intent.putExtra(EXTRA_START_WITH, startWith);
    }

    @Override
    public void shareVideo(String headline, String shareUrl) {
        //not supported
    }

    @Override
    public void startPIP(Video mVideo) {
        Measurement.trackPIPEnter("inline");
        videoManager.release();
        videoManager.setmIsInPIP(true);
        minimize();
    }

    @Override
    public boolean isPIPEnabled() {
        return PrefUtils.getPIPEnabled(this);
    }

    @Override
    public void onTrackingEvent(@NonNull TrackingType type, @NonNull Video video, @Nullable Object value) {
        switch (type) {
            case ON_PLAY_STARTED:
                Measurement.playVideo(video.getVideoName(), video.getPageName(), video.getVideoSection(), video.getVideoSource(), video.getVideoCategory(), video.getContentId(), null, null);
                break;
            case AD_PLAY_STARTED:
                Measurement.trackVideoAdStart(video.getVideoName(), video.getPageName(), video.getVideoSection(), video.getVideoSource(), video.getContentId(), video.getVideoCategory(), 0,"","","");
                break;
            case AD_PLAY_COMPLETED:
                Measurement.trackVideoAdComplete(video.getVideoName(), video.getPageName(), video.getVideoSection(), video.getVideoSource(), video.getVideoCategory(), video.getContentId(), 0,"","","");
                break;
            case VIDEO_PERCENTAGE_WATCHED:
                if (value instanceof Integer) {
                    Measurement.trackCurrentVideoPercentage(video.getVideoName(), video.getPageName(), video.getVideoSection(), video.getVideoSource(), video.getVideoCategory(), video.getContentId(), (int) value, null, 0, null);
                }
                break;
            case ON_PLAY_COMPLETED:
                Measurement.stopVideo(video.getVideoName(), video.getPageName(), video.getVideoSection(), video.getVideoSource(), video.getVideoCategory(), video.getContentId(), null, null);
                break;
        }
    }

    @Override
    public void onTrackingEvent(@NonNull VideoTracker2.VideoType videoType, @NonNull TrackingType type, @NonNull Video video, @Nullable Object value) {

    }

    @Override
    public void openWeb(String url) {
        if (url == null) return;
        Utils.startWeb(url, this);
    }

    @Override
    public void logVideoError(EventLog.Builder eventLogBuilder) {

    }

    @Override
    public void startPIP(Video video, String playerName) {

    }

    @Override
    public void startFullScreen(Video video, String playerName) {

    }

    private class VideoJsonRequest extends JsonRequest<VideoJson> {
        public VideoJsonRequest(String url, Response.Listener<VideoJson> listener, Response.ErrorListener errorListener) {
            super(Method.GET, url, null, listener, errorListener);
        }

        @Override
        protected Response<VideoJson> parseNetworkResponse(NetworkResponse response) {
            try {
                Gson gson = new GsonBuilder().create();
                VideoJson videoMeta = gson.fromJson(new String(response.data, "UTF-8"), VideoJson.class);
                Cache.Entry entry = HttpHeaderParser.parseCacheHeaders(response);
                if (entry.softTtl == 0) {
                    entry.softTtl = System.currentTimeMillis() + 3600000;
                }
                return Response.success(videoMeta, entry);
            } catch (Exception e) {
                return Response.error(new VolleyError(e));
            }
        }
    }

    private class VideoJson {
        private String headline;
        private String byline;
        private String pubdate;
        private String blurb;
        private String embed_code;
        private String adkey;
        private String shareUrl;
        private String shareThumbnail;
        @SerializedName("subtitlesurl")
        private String subtitlesURL;
        @SerializedName("omniture")
        private String omnitureJson;
        @SerializedName("adconfig")
        private String adConfigJson;
    }

    private void initVideoContainerLayout(RelativeLayout.LayoutParams viewGroupLayoutParams) {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewGroupLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
            _topPanel.setVisibility(View.INVISIBLE);
            _descPanel.setVisibility(View.INVISIBLE);
            _videoLayoutContainer.setBackgroundColor(getResources().getColor(R.color.black));
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            _topPanel.setVisibility(View.VISIBLE);
            _descPanel.setVisibility(View.VISIBLE);
            _videoLayoutContainer.setBackgroundColor(getResources().getColor(R.color.transparent));
        }
    }
}
