package com.wapo.flagship.wrappers;

import android.content.Context;
import android.content.Intent;

import com.wapo.flagship.features.video.SimpleVideoActivity;
import com.wapo.flagship.features.video.VideoActivity;
import com.wapo.flagship.features.video.YouTubeVideoActivity;
import com.wapo.flagship.json.AdConfig;
import com.wapo.flagship.json.VideoSource;
import com.wapo.flagship.util.UIUtil;
import com.wapo.flagship.util.Util;

/**
 * Created by kilarib on 2/6/14.
 */
public class VideoActivityWrapper {

    public static Intent getVideoIntent(
            Context context,
            String host,
            String mediaUrl,
            String videoId,
            String title,
            String shareUrl,
            String caption,
            String subtitlesUrl,
            AdConfig adConfig,
            long startWith,
            String fallbackURL,
            String videoName,
            String videoSection,
            String videoSource,
            String contentId
    ) {

        Intent intent;

        final VideoSource providerSource = VideoSource.valueOf(host.toUpperCase());
        switch (providerSource) {
            case POSTTV:
                if (Util.isFirePhone()) {
                    intent = new Intent(context, SimpleVideoActivity.class);
                    intent.putExtra(SimpleVideoActivity.videoHostParam, host);
                    intent.putExtra(SimpleVideoActivity.videoExtraParam, mediaUrl);
                } else {
                    intent = VideoActivity.createIntent(
                            context,
                            VideoActivity.class,
                            mediaUrl,
                            title,
                            shareUrl,
                            caption,
                            subtitlesUrl,
                            adConfig,
                            fallbackURL,
                            videoName,
                            videoSection,
                            videoSource,
                            contentId,
                            false
                    );
                    VideoActivity.withStartPosition(intent, startWith);
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                return intent;
            case METHODE:
                intent = new Intent(context, VideoActivity.class);
                intent.putExtra(VideoActivity.VideoInfoUrlExtraParamName, mediaUrl);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                return intent;
            case YOUTUBE:
                intent = new Intent(context, YouTubeVideoActivity.class);
                intent.putExtra(YouTubeVideoActivity.videoUrlParam, mediaUrl);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                return intent;
            case VIMEO:
                intent = new Intent(context, SimpleVideoActivity.class);
                intent.putExtra(SimpleVideoActivity.videoHostParam, host);
                intent.putExtra(SimpleVideoActivity.videoExtraParam, videoId);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                return intent;
            case INSTAGRAM:
            default:
                intent = new Intent(context, SimpleVideoActivity.class);
                intent.putExtra(SimpleVideoActivity.videoHostParam, host);
                intent.putExtra(SimpleVideoActivity.videoExtraParam, mediaUrl);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                return intent;
        }

    }

}
