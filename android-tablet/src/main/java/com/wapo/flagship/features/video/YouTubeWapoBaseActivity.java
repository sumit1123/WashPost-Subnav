package com.wapo.flagship.features.video;

import com.google.android.youtube.player.YouTubeBaseActivity;
import com.wapo.flagship.util.tracking.Measurement;

/**
 * Created by a.glyzin on 17.03.14.
 */
public class YouTubeWapoBaseActivity extends YouTubeBaseActivity {

    @Override
    protected void onResume() {
        super.onResume();
        Measurement.resumeCollection(this);
    }
}
