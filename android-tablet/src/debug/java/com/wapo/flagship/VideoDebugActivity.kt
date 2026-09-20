// Copyright (c) 2019 The Washington Post. All rights reserved.

package com.wapo.flagship

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.wapo.flagship.features.grid.Tracking
import com.wapo.flagship.features.personalizedpodcasts.viewmodel.PersonalizedPodcastViewModel
import com.wapo.flagship.features.video.VideoActivity
import com.wapo.flagship.wapomain.MainActivity
import com.washingtonpost.android.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VideoDebugActivity : MainActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val videoIntent =
            VideoActivity.createIntent(
                this,
                VideoActivity::class.java,
                "https://d21rhj7n383afu.cloudfront.net/washpost-production/The_Washington_Post/20190718/5d30b8c946e0fb00094408d5/5d30c022cff47e00098383bc_1439412153584-wn5qra_t_1563476006079_640_360_600.mp4",
                null,
                "https://www.washingtonpost.com/video/politics/trump-falsely-says-he-stopped-send-her-back-chants/2019/07/18/35474a2b-072b-4133-9351-eb307eea482d_video.html",
                "Trump falsely says he stopped ‘send her back’ chants",
                "https://closedcaptions.posttv.com/2019/07/22/4d1abccd-4c19-40c4-995b-f98943e58ab9/3_1563820401929/4d1abccd-4c19-40c4-995b-f98943e58ab9_5d35fbb852faff000938cc27.vtt",
                null,
                "https://www.washingtonpost.com/video/politics/trump-falsely-says-he-stopped-send-her-back-chants/2019/07/18/35474a2b-072b-4133-9351-eb307eea482d_video.html",
                "Trump falsely says he stopped ‘send her back’ chants",
                null,
                null,
                null,
            )

        videoIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(videoIntent)
        overridePendingTransition(R.anim.slide_up, R.anim.slide_down)
    }

    override fun onSectionLoadStart(
        context: Context?,
        sectionBundleName: String?,
    ) {
    }

    override fun getPersoPodcastViewModel(): PersonalizedPodcastViewModel? {
        return null
    }
}
