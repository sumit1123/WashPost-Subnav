package com.wapo.flagship.features.articles2.models.deserialized.video

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.squareup.moshi.Moshi
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.Utils
import com.wapo.flagship.features.articles2.activities.Articles2Activity
import com.wapo.flagship.features.articles2.tracking.FirebaseTrackingInfo
import com.wapo.flagship.features.audio.ClassicAudioManager2
import com.wapo.flagship.features.posttv.VideoTracker2
import com.wapo.flagship.features.posttv.model.TrackingType
import com.wapo.flagship.features.posttv.model.Video
import com.wapo.flagship.features.video.VideoActivity
import com.wapo.flagship.features.video.models.VideoAdResponse
import com.wapo.flagship.util.PrefUtils
import com.wapo.flagship.util.Share
import com.wapo.flagship.util.UIUtil
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.R
import java.util.*
import javax.inject.Inject

/**
 * This class takes care of all the events that are coming from the video player.
 */
class InlineVideoPlayerEvents {
    @Inject
    lateinit var audioManager: ClassicAudioManager2

    private val playerErrorCountMap: HashMap<String?, Int?> =
        HashMap(5)

    fun trackStandardVideoEvent(
        type: TrackingType,
        video: Video,
        valueMap: MutableMap<*, *>,
        trackingInfo: FirebaseTrackingInfo?,
    ) {
        var eventLabel: String? = null
        var avExp: String? = null
        var avPlayerType: String? = null
        var avType: String? = null
        var aspectRatio: Float? = null
        if (video.isLooping) {
            eventLabel = VideoTracker2.LOOPING_ARTICLE
            avExp = VideoTracker2.LOOPING_ARTICLE
            avPlayerType = VideoTracker2.AV_PLAYER_TYPE_LOOPING_ARTICLE
        } else if (video.autoplay) {
            eventLabel = VideoTracker2.AUTOPLAY_ARTICLE
            avExp = VideoTracker2.AUTOPLAY_ARTICLE
            avPlayerType = VideoTracker2.AV_PLAYER_TYPE_ARTICLE
        } else if (video.promoIsLooping == true && !video.promoUrl.isNullOrEmpty()) {
            eventLabel = VideoTracker2.LOOPING_PROMO_ARTICLE
            avExp = VideoTracker2.LOOPING_PROMO_ARTICLE
            avPlayerType = VideoTracker2.AV_PLAYER_TYPE_ARTICLE
        } else {
            avExp = VideoTracker2.CLICK_FEED
        }
        val arcId = trackingInfo?.omnitureX?.arcId
        val videoStartId =
            if (valueMap.containsKey(VideoTracker2.VIDEO_START_ID)) {
                valueMap[VideoTracker2.VIDEO_START_ID] as String
            } else {
                ""
            }
        if (video.height != 0f && video.width != 0f) {
            aspectRatio = video.width / video.height
        }
        if (aspectRatio != null && aspectRatio < 1) {
            avType = VideoTracker2.AV_TYPE
        } else if (video.aspectRatio < 1) {
            avType = VideoTracker2.AV_TYPE
        }
        val avName =
            if (valueMap.containsKey(VideoTracker2.AV_NAME)) {
                valueMap[VideoTracker2.AV_NAME] as String
            } else {
                ""
            }
        when (type) {
            TrackingType.ON_PLAY_STARTED -> {
                if (video.playType == Video.PLAY_TYPE_NORMAL) {
                    Measurement.playVideo(
                        video.videoName ?: avName,
                        video.pageName,
                        video.videoSection,
                        video.videoSource,
                        video.videoCategory,
                        video.contentId,
                        eventLabel,
                        avExp,
                        avPlayerType,
                        video.arcId ?: arcId,
                        videoStartId,
                        avType
                    )
                } else {
                    Measurement.autoplayVideo(
                        video.videoName ?: avName,
                        video.pageName,
                        video.videoSection,
                        video.videoSource,
                        video.videoCategory,
                        video.contentId,
                        eventLabel,
                        avExp,
                        avPlayerType,
                        video.arcId ?: arcId,
                        videoStartId,
                        avType
                    )
                }
            }
            TrackingType.AD_PLAY_STARTED ->
                Measurement.trackVideoAdStart(
                    video.videoName,
                    video.pageName,
                    video.videoSection,
                    video.videoSource,
                    video.contentId,
                    video.videoCategory,
                    0,
                    "",
                    "",
                    ""
                )
            TrackingType.AD_PLAY_COMPLETED ->
                Measurement.trackVideoAdComplete(
                    video.videoName,
                    video.pageName,
                    video.videoSection,
                    video.videoSource,
                    video.videoCategory,
                    video.contentId,
                    0,
                    "",
                    "",
                    ""
                )
            TrackingType.VIDEO_PERCENTAGE_WATCHED -> {
                val percentageWatched =
                    if (valueMap.containsKey(VideoTracker2.TRACKING_VALUE)) {
                        valueMap[VideoTracker2.TRACKING_VALUE] as Int
                    } else {
                        0
                    }
                val avName =
                    if (valueMap.containsKey(VideoTracker2.AV_NAME)) {
                        valueMap[VideoTracker2.AV_NAME] as String
                    } else {
                        ""
                    }
                Measurement.trackCurrentVideoPercentage(
                    video.videoName ?: avName,
                    video.pageName,
                    video.videoSection,
                    video.videoSource,
                    video.videoCategory,
                    video.contentId,
                    percentageWatched,
                    eventLabel,
                    avExp,
                    avPlayerType,
                    video.arcId ?: arcId,
                    videoStartId,
                    0,
                    avType
                )
            }
            TrackingType.ON_PLAY_COMPLETED ->
                Measurement.stopVideo(
                    video.videoName ?: avName,
                    video.pageName,
                    video.videoSection,
                    video.videoSource,
                    video.videoCategory,
                    video.contentId,
                    eventLabel,
                    avExp,
                    avPlayerType,
                    video.arcId ?: arcId,
                    videoStartId,
                    avType
                )
            TrackingType.ON_MUTE,
            TrackingType.ON_CAPTION_TOGGLE,
            TrackingType.ON_PAUSE,
            ->
                Measurement.trackVideoInteraction(
                    video.videoName ?: avName,
                    video.pageName,
                    video.videoSection,
                    video.videoSource,
                    video.videoCategory,
                    video.contentId,
                    eventLabel,
                    avExp,
                    avPlayerType,
                    video.arcId ?: arcId,
                    valueMap[VideoTracker2.TRACKING_VALUE] as? String,
                )
            TrackingType.VIDEO_PROGRESS -> {
                val progressThreshold =
                    if (valueMap.containsKey(VideoTracker2.PROGRESS_THRESHOLD)) {
                        valueMap[VideoTracker2.PROGRESS_THRESHOLD] as Int
                    } else {
                        0
                    }
                val engagedTime =
                    if (valueMap.containsKey(VideoTracker2.ENGAGED_TIME)) {
                        valueMap[VideoTracker2.ENGAGED_TIME] as String
                    } else {
                        ""
                    }
                val videoStartId =
                    if (valueMap.containsKey(VideoTracker2.VIDEO_START_ID)) {
                        valueMap[VideoTracker2.VIDEO_START_ID] as String
                    } else {
                        ""
                    }
                val avName =
                    if (valueMap.containsKey(VideoTracker2.AV_NAME)) {
                        valueMap[VideoTracker2.AV_NAME] as String
                    } else {
                        ""
                    }
                val adResponse = video.source as? VideoAdResponse
                Measurement.trackVideoProgress(
                    video.videoName ?: avName,
                    video.pageName,
                    video.videoSection,
                    video.videoSource,
                    video.videoCategory,
                    video.arcId ?: arcId,
                    video.contentId,
                    progressThreshold,
                    adResponse?.gamCreativeId,
                    adResponse?.gamLineItemId,
                    avExp,
                    avPlayerType,
                    engagedTime,
                    videoStartId,
                    false,
                    null,
                    avType
                )
            }
            else -> {
                // no op
            }
        }
    }

    private fun handleVideoPlaybackError(
        video: Video?,
        context: Context,
    ) {
        if (video == null) {
            return
        }
        val currentCount: Int = playerErrorCountMap[video.id] ?: 0
        if (currentCount < Articles2Activity.NUMBER_OF_RETRY_ATTEMPTS) {
            playerErrorCountMap[video.id] = currentCount.plus(1)
        } else {
            playerErrorCountMap[video.id] = 0
            FlagshipApplication.getInstance().releaseVideoManager2()
            videoPlayerErrorOccurred(video.fallbackUrl, context)
        }
    }

    private fun videoPlayerErrorOccurred(
        url: String,
        context: Context,
    ) {
        if (TextUtils.isEmpty(url)) return
        Utils.startWeb(url, context)
    }

    fun shareVideo(
        headline: String?,
        shareUrl: String?,
        context: Context,
    ) {
        shareNativeContent(headline, shareUrl, context)
    }

    private fun shareNativeContent(
        title: String?,
        shareUrl: String?,
        context: Context,
    ) {
        val title = title ?: ""
        if (shareUrl.isNullOrEmpty()) {
            Toast.makeText(context, "Unable to share article", Toast.LENGTH_SHORT).show()
            return
        }

        Share
            .Builder()
            .fromPush(false)
            .headline(title)
            .shareUrl(shareUrl)
            .isVideoShare(true)
            .build()
            .shareItem(context)
    }

    fun startPIP(
        mVideo: Video?,
        context: Activity,
    ) {
        if (UIUtil.isPIPSupported() && PrefUtils.getPIPEnabled(context) && mVideo != null) {
            val content = mVideo.source as com.wapo.flagship.features.articles2.models.deserialized.video.Video
            val videoManager = FlagshipApplication.getInstance().videoManager
            val i =
                createIntent(
                    context,
                    VideoActivity::class.java,
                    content.streamURL ?: content.mediaURL,
                    content.id,
                    content.shareurl,
                    content.fullcaption,
                    content.subtitlesURL,
                    content.adconfig,
                    content.fallback,
                    content.fullcaption,
                    null,
                    null,
                    content.contenturl,
                )
            i?.putExtra(VideoActivity.IsPIPRequest, true)
            if (!TextUtils.isEmpty(
                    (mVideo.source as com.wapo.flagship.features.articles2.models.deserialized.video.Video).subtitlesURL,
                )
            ) {
                i?.putExtra(VideoActivity.IsCaptionsAvailable, true)
            }
            if (videoManager.getSavedPosition(mVideo.id) > 0) {
                VideoActivity.withStartPosition(
                    i,
                    videoManager.getSavedPosition(mVideo.id),
                )
            }
            i?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            stopPersistentPodcastPlayer(context.applicationContext)
            context.startActivity(i)
            context.overridePendingTransition(R.anim.slide_up, R.anim.slide_down)
        }
    }

    fun createIntent(
        context: Context?,
        activityClass: Class<*>?,
        mediaUrl: String?,
        title: String?,
        shareUrl: String?,
        description: String?,
        subtitlesUrl: String?,
        adConfig: Adconfig?,
        fallbackURL: String?,
        videoName: String?,
        videoSection: String?,
        videoSource: String?,
        contentId: String?,
    ): Intent? =
        Intent(context, activityClass)
            .putExtra(VideoActivity.PARAM_MEDIA_URL, mediaUrl)
            .putExtra(VideoActivity.PARAM_TITLE, title)
            .putExtra(VideoActivity.PARAM_SHARE_URL, shareUrl)
            .putExtra(VideoActivity.PARAM_DESCRIPTION, description)
            .putExtra(VideoActivity.PARAM_SUBTITLES_URL, subtitlesUrl)
            .putExtra(VideoActivity.PARAM_FALLBACK_URL, fallbackURL)
            .putExtra(
                VideoActivity.PARAM_AD_CONFIG,
                if (adConfig == null) {
                    null
                } else {
                    AdconfigJsonAdapter(
                        Moshi.Builder().build(),
                    ).toJson(adConfig)
                },
            ).putExtra(VideoActivity.PARAM_FORCE_LANDSCAPE, false)
            .putExtra(VideoActivity.PARAM_VIDEO_NAME, videoName)
            .putExtra(VideoActivity.PARAM_VIDEO_SECTION, videoSection)
            .putExtra(VideoActivity.PARAM_VIDEO_SOURCE, videoSource)
            .putExtra(VideoActivity.PARAM_CONTENT_ID, contentId)

    private fun stopPersistentPodcastPlayer(context: Context) {
        audioManager.stopMedia()
    }

    private fun stopPIPPlayer(context: Context) {
        val intent = Intent("finish_PIPVideoActivity")
        context.sendBroadcast(intent)
    }

    fun removeFragment(
        fragment: Fragment?,
        shouldSaveState: Boolean,
        supportFragmentManager: FragmentManager,
    ) {
        if (fragment != null) {
            if (shouldSaveState) {
                supportFragmentManager
                    .beginTransaction()
                    .remove(fragment)
                    .commit()
            } else {
                supportFragmentManager
                    .beginTransaction()
                    .remove(fragment)
                    .commitAllowingStateLoss()
            }
        }
    }

    fun openWeb(
        url: String?,
        context: Context,
    ) {
        if (url == null) return
        Utils.startWeb(url, context)
    }

    fun addFragment(
        viewID: Int,
        fragment: Fragment,
        shouldSaveState: Boolean,
        supportFragmentManager: FragmentManager,
    ) {
        supportFragmentManager
            .beginTransaction()
            .replace(viewID, fragment, null)
            .apply {
                if (shouldSaveState) {
                    commit()
                } else {
                    commitAllowingStateLoss()
                }
            }
    }

    fun isPIPEnabled(context: Context): Boolean = PrefUtils.getPIPEnabled(context)
}
