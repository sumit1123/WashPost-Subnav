package com.wapo.flagship.features.video;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.ActionBar;
import android.text.TextUtils;
import com.wapo.android.commons.util.Logger;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.VideoView;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wapo.android.commons.retrofit.DefaultHeadersInterceptor;
import com.wapo.flagship.features.shared.activities.BaseActivity;
import com.wapo.flagship.features.shared.fragments.TopBarFragment;
import com.wapo.flagship.json.VimeoMeta;
import com.wapo.flagship.util.tracking.Measurement;
import com.wapo.view.MediaControllerEx;
import com.washingtonpost.android.R;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SimpleVideoActivity extends BaseActivity {
    public static final String videoHostParam = SimpleVideoActivity.class.getSimpleName() + ".videoHost";
    public static final String videoExtraParam = SimpleVideoActivity.class.getSimpleName() + ".videoUrl";
    private static final String TAG = SimpleVideoActivity.class.getSimpleName();
    private static final String TOP_BAR_FRAGMENT_TAG = "top-bar-fragment";
    private View errorCurtain;
    private ProgressBar progressBar;
    private MediaControllerEx mc;
    private VideoView videoView;

    private static final String VIDEO_SOURCE_VIMEO = "vimeo";
    private TopBarFragment _topBarFragment;
    private boolean _isTopBarInitialized;

    private TopBarFragment.OnClickListener _topBarListener = new TopBarFragment.OnClickListener() {
        @Override
        public void onClick() {
            videoView.stopPlayback();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Use *.NoActionBar theme in the Manifest
        //and setTheme() during onCreate()
        //to avoid actionbar lagging
        setTheme(R.style.WaPo_ActionBarOverlay);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_video);

        mc = new MediaControllerEx(SimpleVideoActivity.this);
        mc.setOnVisibilityChangedListener(new MediaControllerEx.OnVisibilityChangedListener() {
            @Override
            public void onVisibilityChanged(boolean isShowing) {
                ActionBar ab = getSupportActionBar();
                if (ab != null) {
                    if (isShowing) {
                        ab.show();
                    } else {
                        ab.hide();
                    }
                }
            }
        });

        String videoHost = getIntent().getStringExtra(videoHostParam);
        String videoParam = getIntent().getStringExtra(videoExtraParam);

        if (videoParam == null || videoParam.trim().equals("")) {
            showErrorCurtain(true);
            return;
        }

        showErrorCurtain(false);
        showProgressSpinner(true);

        if (VIDEO_SOURCE_VIMEO.equals(videoHost)) {

            handleVimeoVideos(videoParam);

        } else {
            playVideoWithUrl(videoParam);
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction transaction = fm.beginTransaction();

            _topBarFragment = (TopBarFragment) fm.findFragmentByTag(TOP_BAR_FRAGMENT_TAG);
            if (_topBarFragment == null) {
                _topBarFragment = new TopBarFragment();
                transaction.add(_topBarFragment, TOP_BAR_FRAGMENT_TAG);
                _isTopBarInitialized = false;
                _topBarFragment.setMode(true);
            }

            _topBarFragment.setOnClickListener(_topBarListener);

            transaction.commit();
            actionBar.hide();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (_topBarFragment != null) {
            if (!_isTopBarInitialized) {
                View view = _topBarFragment.getView();
                ActionBar actionBar = getSupportActionBar();
                if (view != null && actionBar != null) {
                    actionBar.setCustomView(view);
                    actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
                    _isTopBarInitialized = true;
                }
            }
        }
        Measurement.resumeCollection(this);
    }

    //for fix android bug with AppCompat
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // do nothing
        return keyCode == KeyEvent.KEYCODE_MENU || super.onKeyDown(keyCode, event);
    }

    private void handleVimeoVideos(String videoId) {
        //No caching of the token needed
        new GetVimeoTokenAndPlay(videoId).execute();
    }

    private void showErrorCurtain(boolean shouldShow) {
        errorCurtain = findViewById(R.id.video_error_curtain);
        errorCurtain.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
    }

    private void showProgressSpinner(boolean shouldShow) {
        progressBar = (ProgressBar) findViewById(R.id.progress_spinner);
        progressBar.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
    }

    private void playVideoWithUrl(String videoUrl) {
        videoView = (VideoView) findViewById(R.id.video_view);
        videoView.setVideoURI(Uri.parse(videoUrl));
        videoView.setMediaController(mc);
        videoView.requestFocus();
        showProgressSpinner(false);
        videoView.start();
    }

    private class GetVimeoTokenAndPlay extends AsyncTask<Void, Void, Void> {
        private VimeoMeta videoMeta = null;
        private String vimeoId = null;

        GetVimeoTokenAndPlay(String vimeoid) {
            this.vimeoId = vimeoid;
        }

        @Override
        protected void onPostExecute(Void result) {
            String finalUrl = getVimeoUrl();
            showProgressSpinner(false);
            if (TextUtils.isEmpty(finalUrl)) {
                showErrorCurtain(true);
            } else {
                showErrorCurtain(false);
                playVideoWithUrl(finalUrl);
            }
            super.onPostExecute(result);
        }

        @Override
        protected Void doInBackground(Void... params) {
            HttpURLConnection urlConnection = null;
            boolean redirect = false;

            try {
                URL url = new URL(String.format(getResources().getString(R.string.vimeo_url), vimeoId));
                urlConnection = (HttpURLConnection) url.openConnection();
                Map<String, String> defaultHeaders = DefaultHeadersInterceptor.Companion.getHeaders();
                Set<String> keys = defaultHeaders.keySet();
                for (String key : keys) {
                    urlConnection.setRequestProperty(key, defaultHeaders.get(key));
                }
                urlConnection.setInstanceFollowRedirects(true);
                HttpURLConnection.setFollowRedirects(true);

                // Handling Redirection for 3xx
                int status = urlConnection.getResponseCode();
                if (status != HttpURLConnection.HTTP_OK) {
                    if (status == HttpURLConnection.HTTP_MOVED_TEMP
                            || status == HttpURLConnection.HTTP_MOVED_PERM
                            || status == HttpURLConnection.HTTP_SEE_OTHER)
                        redirect = true;
                }

                if (redirect) {
                    // getting redirect url from "Location" header field
                    String newUrl = urlConnection.getHeaderField("Location");

                    // Opening the new connection 
                    urlConnection = (HttpURLConnection) new URL(newUrl).openConnection();
                    for (String key : keys) {
                        urlConnection.setRequestProperty(key, defaultHeaders.get(key));
                    }
                }

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                Reader reader = new InputStreamReader(in);
                readAndProcessStream(reader);
            } catch (MalformedURLException e) {
                Logger.e(TAG, Log.getStackTraceString(e));
            } catch (IOException e) {
                Logger.e(TAG, Log.getStackTraceString(e));
            } finally {
                if (urlConnection != null)
                    urlConnection.disconnect();
            }

            return null;
        }

        private void readAndProcessStream(Reader reader) {
            if (reader != null) {
                Gson gson = new GsonBuilder().create();
                videoMeta = gson.fromJson(reader, VimeoMeta.class);
            }
        }

        private String getVimeoUrl() {

            if (videoMeta == null)
                return null;
            if (videoMeta.request == null)
                return null;
            if (videoMeta.request.files == null)
                return null;

            VimeoMeta.H264 h264 = videoMeta.request.files.h264;


            String finalUrl = null;
            if (h264 != null) {


                if (h264.hd != null && h264.hd.url != null) {

                    finalUrl = h264.hd.url;
                    if (!finalUrl.trim().equals(""))
                        return finalUrl;
                }
                if (h264.sd != null && h264.sd.url != null) {

                    finalUrl = h264.sd.url;
                    if (!finalUrl.trim().equals(""))
                        return finalUrl;
                }
                if (h264.mobile != null && h264.mobile.url != null) {

                    finalUrl = h264.mobile.url;
                    if (!finalUrl.trim().equals(""))
                        return finalUrl;
                }
            }
            final List<VimeoMeta.MetaProfile> progressive = videoMeta.request.files.progressive;
            if (progressive != null) {
                Collections.sort(progressive, new Comparator<VimeoMeta.MetaProfile>() {
                    @Override
                    public int compare(VimeoMeta.MetaProfile lhs, VimeoMeta.MetaProfile rhs) {
                        return rhs.width - lhs.width;
                    }
                });
                for (VimeoMeta.MetaProfile profile : progressive) {
                    if (profile != null && profile.url != null) {
                        finalUrl = profile.url;
                        if (!finalUrl.trim().equals(""))
                            return finalUrl;
                    }
                }

            }
            return finalUrl;
        }
    }

    @Override
    public void onDestroy() {

        if (videoView != null)
            videoView.stopPlayback();

        super.onDestroy();
    }

    @Override
    protected boolean ignoreNightMode() {
        return true;
    }
}
