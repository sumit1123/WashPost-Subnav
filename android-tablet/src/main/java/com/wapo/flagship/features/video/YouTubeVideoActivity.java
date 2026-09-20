package com.wapo.flagship.features.video;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;
import com.wapo.flagship.Utils;
import com.wapo.flagship.features.shared.activities.WebViewActivity;
import com.wapo.android.commons.util.Logger;
import com.washingtonpost.android.R;

import java.net.URL;

public class YouTubeVideoActivity extends YouTubeWapoBaseActivity {
    public static final String videoUrlParam = YouTubeVideoActivity.class.getSimpleName() + ".videoUrl";
    public static final String DEVELOPER_KEY = "AI39si41y7cCk9VuIKIltADGm_d0XgY0J1raL_07KmMUPNSqMOS96DbTaj9QxDwJh9aXsWtL2-9u5fAp5Nmy3r5srQWX9iZ7fg";
    private static final String TAG = YouTubeVideoActivity.class.getName();
    private View errorCurtain;
    YouTubePlayerView youTubeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_you_tube_video);

        final String videoUrl =  getIntent().getStringExtra(videoUrlParam);

        if(videoUrl==null)
        {
            shouldShowErrorCurtain(true);
           return;
        }

        final String videoId = extractYoutubeId(videoUrl);
        if(videoId==null || videoId.trim().equals(""))
        {
            Utils.startWeb(videoUrl, this);
            finish();
        }

        youTubeView = (YouTubePlayerView) findViewById(R.id.youtube_view);
        shouldShowErrorCurtain(false);
        youTubeView.initialize(DEVELOPER_KEY, new YouTubePlayer.OnInitializedListener() {
            @Override
            public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean restored) {
                youTubePlayer.setFullscreenControlFlags(YouTubePlayer.FULLSCREEN_FLAG_CONTROL_ORIENTATION);
                youTubePlayer.addFullscreenControlFlag(YouTubePlayer.FULLSCREEN_FLAG_ALWAYS_FULLSCREEN_IN_LANDSCAPE);
                youTubePlayer.addFullscreenControlFlag(YouTubePlayer.FULLSCREEN_FLAG_CONTROL_SYSTEM_UI);

                if (restored) {
                    youTubePlayer.play();
                } else {
                    youTubePlayer.loadVideo(videoId);
                    shouldShowErrorCurtain(false);
                }
            }

            @Override
            public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult result) {
                youTubeView.setVisibility(View.GONE);
                shouldShowErrorCurtain(true);

                Intent intent = new Intent(YouTubeVideoActivity.this, WebViewActivity.class);
                Bundle extras = getIntent().getExtras();
                if (extras != null) {
                    intent.putExtras(extras);
                }

                intent.putExtra(WebViewActivity.urlParam, videoUrl);

                YouTubeVideoActivity.this.startActivity(intent);
                YouTubeVideoActivity.this.finish();
            }
        });
    }

    //for fix android bug with AppCompat
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ( keyCode == KeyEvent.KEYCODE_MENU ) {
            // do nothing
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    private void initializeViews()
    {
        if(errorCurtain==null)
        {
            errorCurtain = findViewById(R.id.video_error_curtain);
        }
    }

    private void shouldShowErrorCurtain(boolean shouldShow) {
        initializeViews();
        if(errorCurtain!=null)
        {
        errorCurtain.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
        }
    }

    private String extractYoutubeId(String videoUrl) {
        String id = null;
        try
        {
            String query = new URL(videoUrl).getQuery();
            if (query != null)
            {
                String[] param = query.split("&");
                for (String row : param)
                {
                    String[] param1 = row.split("=");
                    if (param1[0].equals("v"))
                    {
                        id = param1[1];
                    }
                }
            }
            else
            {
                if (videoUrl.contains("embed"))
                {
                    id = videoUrl.substring(videoUrl.lastIndexOf("/") + 1);
                }
            }
        }
        catch (Exception ex)
        {
            Logger.e(TAG, ex.toString());
        }
        return id;
    }

}
