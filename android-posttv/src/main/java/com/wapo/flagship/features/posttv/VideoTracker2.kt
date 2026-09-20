/* Copyright (c) 2022 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.posttv

import androidx.media3.exoplayer.ExoPlayer
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.posttv.model.TrackingType
import com.wapo.flagship.features.posttv.model.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Tracker class to track video events (start, percentage and completion events)
 */
class VideoTracker2(private val exoPlayer: ExoPlayer, private val listener: VideoEventListener2) {

    enum class VideoType {
        STANDARD, STANDARD_AUTOPLAY, VERTICAL_CAROUSEL, VERTICAL_FULLSCREEN, WATCH_AUTOPLAY
    }

    data class Tracking(
        val progressThreshold: Int = 0,
        val videoType: VideoType = VideoType.STANDARD,
        val video: Video,
        val swipeDirection: SwipeDirection = SwipeDirection.NONE
    )

    var tracking: Tracking? = null
        private set

    private var highestPercent: Double = 0.0

    private var isPaused: Boolean = false

    // This job is used to start tracking after 1/10 second of start of play
    private var startJob: Job? = null

    // This job is used to continue repeating tracking every 5 seconds
    private var repeatJob: Job? = null

    private var mostRecentPositionMs: Long = 0L

    fun userScrubbedForward(currentPosition: Long?): Boolean {
        currentPosition ?: return false
        return currentPosition > mostRecentPositionMs
    }

    private var engagedTimeMs: Long = 0L
    var currentVideoStartId: String? = null
        private set
    private val scopedTrackedEvents: MutableMap<TrackScope, MutableSet<String>> = mutableMapOf()

    /**
     * Starts tracking jobs
     */
    fun startTracking() {
        startJob = GlobalScope.launch(Dispatchers.IO) {
            delay(START_DELAY_IN_MILLISECONDS)
            withContext(Dispatchers.Main) {
                trackProgress()
            }
        }
        repeatJob = GlobalScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(REPEAT_DELAY_IN_MILLISECONDS)
                withContext(Dispatchers.Main) {
                    trackProgress()
                }
            }
        }
    }

    /**
     * Function that actually tracks the progress of the playback
     */
    private fun trackProgress() {
        // requirements:
        // seeks backwards = no analytics
        // seek forward = fire all events which were passed by
        // if we ever add multiple tracks, I think we should use MediaSource setTag
        if (!exoPlayer.isPlayingAd) {
            val duration = exoPlayer.duration.toDouble()
            val currentPosition = exoPlayer.currentPosition.toDouble()

            if (!isPaused) {
                if (exoPlayer.currentPosition > mostRecentPositionMs) {
                    engagedTimeMs += exoPlayer.currentPosition - mostRecentPositionMs
                }
                Logger.d(TAG, "engagedTime: ${engagedTimeMs/1000}")
                mostRecentPositionMs = exoPlayer.currentPosition
                if (engagedTimeMs >= FIVE_SECONDS_IN_MILLISECONDS) {
                    trackProgressEvent()
                }
            }

            val currentPercent = (currentPosition / duration) * 100
            val valueMap = mutableMapOf<String, Any?>()
            valueMap[PROGRESS_THRESHOLD] = tracking?.progressThreshold
            if (currentPercent > 0 && highestPercent == 0.0) {
                currentVideoStartId = UUID.randomUUID().toString()
                onVideoEvent(TrackingType.ON_PLAY_STARTED, tracking?.progressThreshold)
            }
            val isResumed = isPaused && (highestPercent > 0 && currentPercent > highestPercent)
            if (isResumed) {
                onVideoEvent(TrackingType.ON_RESUME, tracking?.progressThreshold)
            }
            val isCarouselVideo = tracking?.videoType == VideoType.VERTICAL_CAROUSEL
            val isWatchAutoplay = tracking?.videoType == VideoType.WATCH_AUTOPLAY
            if (!isCarouselVideo && !isWatchAutoplay) {
                if (currentPercent >= 25 && highestPercent < 25) {
                    onVideoEvent(TrackingType.VIDEO_PERCENTAGE_WATCHED, 25)
                }
                if (currentPercent >= 50 && highestPercent < 50) {
                    onVideoEvent(TrackingType.VIDEO_PERCENTAGE_WATCHED, 50)
                }
                if (currentPercent >= 75 && highestPercent < 75) {
                    onVideoEvent(TrackingType.VIDEO_PERCENTAGE_WATCHED, 75)
                }
                if (currentPercent >= 100 && highestPercent < 100) {
                    onVideoEvent(TrackingType.ON_PLAY_COMPLETED)
                }
            }
            isPaused = highestPercent == currentPercent && highestPercent > 0
            highestPercent = when {
                currentPercent > highestPercent -> currentPercent
                else -> highestPercent
            }
        }
    }

    fun trackProgressEvent() {
        val engagedTimeSeconds = engagedTimeMs / 1000
        mostRecentPositionMs = exoPlayer.currentPosition
        engagedTimeMs = 0L
        if (engagedTimeSeconds == 0L || engagedTimeSeconds > 5L) {
            /* Covers race conditions with scrubbing events */
            return
        }
        onVideoEvent(TrackingType.VIDEO_PROGRESS, engagedTimeSeconds)
    }

    @Synchronized
    private fun onVideoEvent(trackingType: TrackingType, value: Any? = null) {
        val eventProps = tracking?.let {
            EventProps(it.video, it.videoType, trackingType)
        }
        val shouldTrack = eventProps?.let { shouldTrack(it) } ?: true
        if (!shouldTrack) {
            Logger.d(TAG, "Event ${eventProps?.eventName} already tracked. Video=${eventProps?.video}")
            return
        }
        listener.onVideoEvent(trackingType, value).also {
            eventProps?.let { markTracked(eventProps) }
        }
    }

    private fun shouldTrack(props: EventProps): Boolean {
        val trackScope = props.trackScope ?: return true
        val currentEvents = scopedTrackedEvents[trackScope].orEmpty()
        return !currentEvents.contains(props.eventId)
    }

    private fun markTracked(props: EventProps) {
        val trackScope = props.trackScope ?: return
        val events = scopedTrackedEvents[trackScope] ?: mutableSetOf()
        events.add(props.eventId)
        scopedTrackedEvents[trackScope] = events
    }

    fun resetTrackedEvents(scope: TrackScope) {
        scopedTrackedEvents[scope]?.clear()
    }

    fun resetHighestPercent() {
        highestPercent = 0.0
    }

    fun stopTracking() {
        startJob?.cancel()
        repeatJob?.cancel()
        startJob = null
        repeatJob = null
        // finalize by tracking current progress (e.g. they scrubbed straight to the end)
        trackProgress()
    }

    fun setTracking(tracking: Tracking?) {
        this.tracking = tracking
    }

    data class EventProps(
        val video: Video,
        val videoType: VideoType,
        val trackingType: TrackingType,
    ) {
        val isAutoplayEvent = videoType == VideoType.WATCH_AUTOPLAY &&
                (trackingType in setOf(TrackingType.ON_PLAY_STARTED, TrackingType.ON_RESUME))

        val trackScope: TrackScope? = when {
            isAutoplayEvent -> TrackScope.WATCH_PAGE
            else -> null
        }

        val eventId: String = video.id
        val eventName: String = if (isAutoplayEvent) "video_autoplay" else "unknown"
    }

    /**
     * Scopes where an event should only be tracked once.
     */
    enum class TrackScope { WATCH_PAGE }

    interface VideoEventListener2 {
        fun onVideoEvent(trackingType: TrackingType, value: Any? = null)
        fun onAdEvent(trackingType: TrackingType)
    }

    companion object {
        private val TAG: String = VideoTracker2::class.java.simpleName
        private const val START_DELAY_IN_MILLISECONDS = 100L
        private const val REPEAT_DELAY_IN_MILLISECONDS = 1000L
        const val TRACKING_VALUE = "tracking_value"
        const val PROGRESS_THRESHOLD = "progress_threshold"
        const val PERCENTAGE_WATCHED = "percentage_watched"
        const val VIDEO_MUTED = "video_muted"
        const val VIDEO_UNMUTED = "video_unmuted"
        const val VIDEO_CAPTION_ON = "video_caption_on"
        const val VIDEO_CAPTION_OFF = "video_caption_off"
        const val VIDEO_SOCIAL_SHARE_START = "video_social_share_start"
        const val VIDEO_PAUSE = "video_pause"
        const val VIDEO_UNPAUSE = "video_unpause"
        const val VIDEO_PLAY_NEXT = "video_play_next"
        const val VIDEO_PLAY_PREVIOUS = "video_play_previous"
        const val CAROUSEL = "carousel"
        const val SWIPE_DIRECTION = "swipe_direction"
        const val CAROUSEL_FORWARD = "carousel_forward"
        const val CAROUSEL_BACK = "carousel_back"
        const val AV_NAME = "av_name"

        // Fronts
        const val AUTOPLAY_FRONT = "autoplay_front"
        const val LOOPING_FRONT = "looping_front"
        const val LOOPING_PROMO_FRONT = "click_front"
        const val AV_PLAYER_TYPE_FRONT = "app-embed-front"

        // Articles
        const val AUTOPLAY_ARTICLE = "autoplay_embed"
        const val LOOPING_ARTICLE = "looping_embed"
        const val LOOPING_PROMO_ARTICLE = "click_embed"
        const val AV_PLAYER_TYPE_ARTICLE = "app-embed"
        const val AV_PLAYER_TYPE_LOOPING_ARTICLE = "app-embed-LoopingVideo"

        //Watch Section
        const val AUTOPLAY_FEED = "autoplay-feed"
        const val AV_PLAYER_TYPE_WATCH = "feed_wp_universe"
        const val AV_EXP = "av_exp"
        const val CLICK_FEED= "click_feed"
        const val SWIPE_FEED= "swipe_feed"
        const val ROLLTHROUGH_FEED= "rollthrough_feed"
        const val ENGAGED_TIME = "engaged_time"
        const val VIDEO_START_ID = "videostart_id"
        const val AV_TYPE = "vertical"
        private const val FIVE_SECONDS_IN_MILLISECONDS = 5000L
    }

    enum class SwipeDirection {
        NONE,
        FORWARD,
        BACK
    }
}
