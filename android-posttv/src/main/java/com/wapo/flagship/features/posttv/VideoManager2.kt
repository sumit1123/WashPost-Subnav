/* Copyright (c) 2022 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.posttv

import android.content.Context
import android.graphics.Color
import android.os.Build
import com.wapo.android.commons.util.Logger
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import com.google.ads.interactivemedia.v3.api.AdError
import com.wapo.android.commons.logs.EventLog
import com.wapo.flagship.features.posttv.listeners.PostTvActivity
import com.wapo.flagship.features.posttv.listeners.PostTvApplication
import com.wapo.flagship.features.posttv.listeners.VideoListener
import com.wapo.flagship.features.posttv.listeners.VideoPlayer
import com.wapo.flagship.features.posttv.model.TrackingType
import com.wapo.flagship.features.posttv.model.Video
import com.wapo.flagship.features.posttv.players.VimeoPlayerImpl
import com.wapo.flagship.features.posttv.players.YouTubePlayerImpl
import com.wapo.flagship.features.posttv.util.Player2ViewModel
import com.washingtonpost.android.config.domain.manager.ConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.LinkedList

/**
 * Class to manage [VideoPlayer] objects and their player frames.
 */
class VideoManager2(private val mAppContext: Context) {

    private val playerFrames = hashMapOf<String, PlayerFrame>()
    // Pool of PlayerFrame objects to create them in advance on the background thread.
    // It helps to reduce object creation time at runtime.
    // "ensurePlayerFramesPoolCapacity" method ensures pool has enough preloaded objects.
    private val playerFramesPool = LinkedList<PlayerFrame>()
    private var activeAutoplayCount = 0

    init {
        ensurePlayerFramesPoolCapacity()
    }

    private fun ensurePlayerFramesPoolCapacity(capacity: Int = 5) {
        // Creating PlayerFrame objects on the Default pool if Android API version allows.
        // Otherwise fallback to the Main pool.
        // Amazon 5 devices are not supporting View objects to be created on threads without a Looper.
        // (https://arcpublishing.atlassian.net/browse/AWA-8912).
        GlobalScope.launch(Dispatchers.Default) {
            try {
                while (playerFramesPool.size < capacity) {
                    playerFramesPool.add(PlayerFrame())
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    while (playerFramesPool.size < capacity) {
                        playerFramesPool.add(PlayerFrame())
                    }
                }
            }
        }
    }

    inner class PlayerFrame : VideoListener {
        private val videoFrameLayout: FrameLayout
        private val progressLayout: RelativeLayout
        private val messageText: TextView
        private val messageOverlay: RelativeLayout
        var videoPlayer: VideoPlayer? = null
            private set
        private var isPlaying = false
        private var hasStartedLooping = false

        init {
            // Prepare Video frame layout
            videoFrameLayout = FrameLayout(mAppContext)
            videoFrameLayout.id = ViewCompat.generateViewId()
            videoFrameLayout.setBackgroundColor(Color.BLACK)
            // Prepare Progress layout
            progressLayout = RelativeLayout(mAppContext)
            val progressBar = ProgressBar(mAppContext, null, android.R.attr.progressBarStyleSmall)
            val params1 = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params1.addRule(RelativeLayout.CENTER_IN_PARENT)
            progressLayout.addView(progressBar, params1)
            // Prepare Message overlay
            messageText = TextView(mAppContext)
            messageText.setTextColor(Color.WHITE)
            messageText.gravity = Gravity.CENTER
            messageOverlay = RelativeLayout(mAppContext)
            messageOverlay.setBackgroundColor(Color.BLACK)
            val params2 = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params2.addRule(RelativeLayout.CENTER_IN_PARENT)
            messageOverlay.addView(messageText, params2)
        }

        @Synchronized
        @Throws(IllegalStateException::class)
        fun initMedia(video: Video, isPlayerClickable: Boolean = false, onPlayerClicked: (() -> Unit)? = {}) {
            try {
                isPlaying = false
                val activity = (mAppContext as PostTvApplication).currentActivity
                if (activity is PostTvActivity && video.playType == Video.PLAY_TYPE_NORMAL) {
                    (activity as PostTvActivity).onVideoStarted()
                }
                if (video.isYouTube) {
                    if (isAmazonDevice) {
                        val youtubeURL = mAppContext.getString(R.string.youtube_base_url) + video.id
                        (activity as PostTvActivity?)?.openWeb(youtubeURL)
                    } else {
                        videoPlayer = YouTubePlayerImpl(this)
                        videoPlayer?.playVideo(video)
                    }
                } else if (video.isVimeo) {
                    videoPlayer = VimeoPlayerImpl(mAppContext, this)
                    videoPlayer?.playVideo(video)
                    isPlaying = true
                } else {
                    activity?.let {
                        PostTvPlayer2Coordinator.getOrCreatePlayer(PLAYER_NAME + video.id, it,
                            isPlayerClickable = isPlayerClickable,
                            onPlayerClicked = onPlayerClicked,
                            ).run {
                            updateViewModel(Player2ViewModel())
                            setVideoListener(this@PlayerFrame)
                            this@PlayerFrame.videoPlayer = this.videoPlayer
                            this@PlayerFrame.videoPlayer?.playVideo(video)
                            isPlaying = true
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun removePlayerFrame() {
            try {
                if (videoFrameLayout.parent is ViewGroup) {
                    (videoFrameLayout.parent as ViewGroup).removeView(videoFrameLayout)
                    videoFrameLayout.tag = null
                }
            } catch (e: java.lang.Exception) {
                // Temp Error Log to debug https://arcpublishing.atlassian.net/browse/AWA-8916
                val builder = EventLog.Builder()
                builder.setMessage("VideoManager removePlayerFrame Exception")
                builder.setErrorMessage(e.message)
                builder.set("video_id", video?.id)
                builder.set("video_name", video?.videoName)
                builder.set("video_section", video?.videoSection)
                builder.set("is_yt", video?.isYouTube)
                builder.set("is_vimeo", video?.isVimeo)
                builder.set("is_live", video?.isLive)
                builder.set("is_looping", video?.isLooping)
                builder.set("autoplay", video?.autoplay)
                builder.set("is_promo_looping", video?.promoIsLooping)
                builder.set("thread", Thread.currentThread().name)
                (mAppContext as? PostTvApplication)?.logPostTvError(builder)
                videoFrameLayout.tag = null
            }
        }

        fun pausePlay(shouldPlay: Boolean) {
            videoPlayer?.pausePlay(shouldPlay)
        }

        fun toggleCaptions() {
            videoPlayer?.toggleCaptions()
        }

        fun onOffscreen() {
            if (isPlaying) {
                if (videoPlayer is YouTubePlayerImpl) {
                    release()
                }
            }
        }

        fun isPlaying(): Boolean {
            return videoPlayer?.isPlaying == true
        }

        fun hasStartedLooping(): Boolean {
            return hasStartedLooping
        }

        fun setHasStartedLooping(startedLooping: Boolean) {
            this.hasStartedLooping = startedLooping
        }

        override fun onTrackingEvent(type: TrackingType, value: Any?) {
            Logger.d(TAG, "Tracking event=" + type.name + " value=" + value)
            if (videoPlayer != null && videoPlayer?.video != null && mAppContext is PostTvApplication) {
                var result: Any? = null
                when (type) {
                    TrackingType.ON_PLAY_STARTED -> {
                        isPlaying = true
                    }
                    TrackingType.ON_PLAY_COMPLETED -> {
                        isPlaying = false
                    }
                    TrackingType.VIDEO_PERCENTAGE_WATCHED -> {
                        if (value is Int) {
                            result = value
                        }
                    }
                    else -> {
                        // no op
                    }
                }
                val activity = (mAppContext as PostTvApplication).currentActivity
                if (activity is PostTvActivity) {
                    videoPlayer?.video?.let { video ->
                        (activity as PostTvActivity).onTrackingEvent(type, video, result)
                    }
                }
            }
        }

        override fun setSavedPosition(id: String, value: Long) {
        }

        override fun getSavedPosition(id: String): Long {
            return NO_POSITION
        }

        override fun setSavedAdStatus(id: String, value: Long) {
        }

        override fun getSavedAdStatus(id: String): Long {
            return Video.AD_NOT_STARTED.toLong()
        }

        override fun getPlayerFrame(): FrameLayout {
            return videoFrameLayout
        }

        override fun addVideoView(view: View) {
            videoFrameLayout.addView(view)
        }

        override fun addVideoFragment(fragment: Fragment, shouldSaveState: Boolean) {
            val activity = (mAppContext as PostTvApplication).currentActivity
            if (activity is PostTvActivity) {
                (activity as PostTvActivity).addFragment(
                    videoFrameLayout.id,
                    fragment,
                    shouldSaveState
                )
            }
        }

        /*** Remove the FragmentActivity this fragment is currently associated with which activity **/
        override fun removeVideoFragment(fragment: Fragment, shouldSaveState: Boolean) {
            val activity = fragment.activity
            if (activity is PostTvActivity) {
                (activity as PostTvActivity).removeFragment(fragment, shouldSaveState)
            }
        }

        override fun setIsLoading(isLoading: Boolean) {
            if (isLoading) {
                if (progressLayout.parent == null) {
                    videoFrameLayout.addView(progressLayout)
                }
            } else {
                videoFrameLayout.removeView(progressLayout)
            }
        }

        override fun release() {
            videoPlayer?.release()
            videoPlayer = null
            isPlaying = false
        }

        override fun onError(message: String) {
            videoFrameLayout.removeAllViews()
            messageText.text = message
            if (messageOverlay.parent != null) {
                (messageOverlay.parent as ViewGroup).removeView(messageOverlay)
            }
            videoFrameLayout.addView(messageOverlay)
        }

        override fun logError(log: String) {
            if (mAppContext is PostTvApplication) {
                val builder = EventLog.Builder()
                builder.setMessage("VideoManager Error")
                builder.setErrorMessage(log)
                (mAppContext as? PostTvApplication)?.logPostTvError(builder)
            }
        }

        val id: String?
            get() { return videoPlayer?.id }

        override fun onActivityResume() {
            videoPlayer?.onActivityResume()
        }

        val videoUrl: String?
            get() { return videoPlayer?.video?.id }

        val contentUrl: String?
            get() { return videoPlayer?.video?.contentUrl }

        val video: Video?
            get() { return videoPlayer?.video }

        override fun isInPIP(): Boolean {
            return videoPlayer?.isInPiP == true
        }

        override fun shouldSuppressAds(): Boolean {
            return (mAppContext as PostTvApplication).shouldSuppressAds()
        }

        override fun onAdEvent(adEvent: VideoListener.AdEvent) {
        }

        override fun onAdError(adError: AdError, video: Video) {
            logAdError(adError, video)
        }

        /**
         * Remote Logs a video ad error.
         * Error codes currently handled by this function:
         * 1005: FAILED_TO_REQUEST_ADS
         */
        override fun logAdError(adError: AdError, video: Video) {
            if (adError != null) {
                val errorInfo =
                    "error_type=\"" + adError.errorType + "\" error_code=\"" + adError.errorCode + "\" error_message=\"" + adError.message + "\" content_url=\"" + video.contentUrl + "\" ad_tag_url=\"" + video.adTagUrl + "\""
                if (adError.errorCodeNumber == 1005) {
                    val errorCause = "error_cause=\"malformed URL\""
                    val errorLog = "$errorCause $errorInfo"
                    (mAppContext as PostTvApplication).logVideoAdError(errorLog)
                } // Can add cases for other causes in the future
            }
        }

        override fun openYoutubeWeb(videoId: String) {
            val activity = (mAppContext as PostTvApplication).currentActivity
            if (activity is PostTvActivity) {
                val youtubeURL = mAppContext.getString(R.string.youtube_base_url) + videoId
                (activity as PostTvActivity).openWeb(youtubeURL)
            }
        }

        fun isErrorDisplayed(): Boolean {
            return messageOverlay.parent != null && messageOverlay.visibility == View.VISIBLE
        }
    }

    @Synchronized
    @Throws(IllegalStateException::class)
    fun initMedia(video: Video, isPlayerClickable: Boolean = false, onPlayerClicked:()-> Unit = {}) {
        Logger.d("VideoManager2","initMedia(), playerFrameExists?=${playerFrames[video.id] != null}, videoId=${video.id}")
        // Release if player frame already exists and in a error state
        if (playerFrames[video.id] != null && playerFrames[video.id]?.isErrorDisplayed() == true) {
            release(video.id)
        }
        if (video.playType == Video.PLAY_TYPE_NORMAL) {
            resetAllVideos(currentVideoId = video.id)
        }
        if (playerFrames[video.id] == null) {
            playerFrames[video.id] = playerFramesPool.poll() ?: PlayerFrame()
            playerFrames[video.id]?.initMedia(video, isPlayerClickable, onPlayerClicked)
            ensurePlayerFramesPoolCapacity()
        }
    }

    fun getPlayerFrame(videoId: String?): PlayerFrame? {
        return playerFrames[videoId]
    }

    fun getPlayerFrameContainer(videoId: String?): FrameLayout? {
        return playerFrames[videoId]?.playerFrame
    }

    fun removePlayerFrame(videoId: String?) {
        playerFrames[videoId]?.removePlayerFrame()
    }

    fun removePlayerFrameFromPool(videoId: String?) {
        playerFrames.remove(videoId)
    }

    fun release(videoId: String?) {
        playerFrames[videoId]?.apply {
            removePlayerFrame()
            release()
        }
        removePlayerFrameFromPool(videoId)
    }

    /**
     * Method to release all playerFrames except the one that is in PiP.
     */
    fun releaseAllVideos() {
        val iterator = playerFrames.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            entry.value.video?.let { video ->
                if (playerFrames[video.id]?.isInPIP == false) {
                    playerFrames[video.id]?.apply {
                        removePlayerFrame()
                        release()
                    }
                    iterator.remove()
                }
            }
        }
    }

    /**
     * Method to reset playerFrames. It releases [Video.PLAY_TYPE_NORMAL] videos and the currentVideoId and
     * mutes [Video.PLAY_TYPE_NORMAL_MUTED] videos.
     * The App can call this method before starting playing any other audio focus required by players.
     */
    fun resetAllVideos(currentVideoId: String? = null, skipVideoIds: List<String?> = emptyList()) {
        val iterator = playerFrames.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            entry.value.video?.let { video ->
                if (!skipVideoIds.contains(video.id)) {
                    if (video.id == currentVideoId || video.playType == Video.PLAY_TYPE_NORMAL) {
                        playerFrames[video.id]?.apply {
                            removePlayerFrame()
                            release()
                        }
                        iterator.remove()
                    } else if (video.playType == Video.PLAY_TYPE_NORMAL_MUTED) {
                        playerFrames[video.id]?.videoPlayer?.apply {
                            mute()
                            respectAudioFocus(false)
                        }
                    }
                }
            }
        }
    }

    fun onVideoUnmute(videoId: String?) {
        resetAllVideos(skipVideoIds = listOf(videoId))
    }

    fun onVideoOffscreen(videoId: String?) {
        playerFrames[videoId]?.video?.let { video ->
            if (video.playType != Video.PLAY_TYPE_NORMAL
                || video.isYouTube || video.isVimeo
                || playerFrames[video.id]?.isErrorDisplayed() == true) {
                release(videoId)
            }
        }
    }

    fun hasStartedLooping(videoId: String?): Boolean {
        return playerFrames[videoId]?.hasStartedLooping() == true
    }

    fun setHasStartedLooping(videoId: String?, startedLooping: Boolean) {
        playerFrames[videoId]?.setHasStartedLooping(startedLooping)
    }

    /**
     * Method to release any PiP player if one exists in the playerFrames pool.
     */
    fun releasePiPPlayerIfAny() {
        val iterator = playerFrames.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            entry.value.let { playerFrame ->
                if (playerFrame.isInPIP) {
                    playerFrame.removePlayerFrame()
                    playerFrame.release()
                    iterator.remove()
                }
            }
        }
    }

    fun getPlayerManager(videoId: String?): PostTvPlayer2Manager? {
        videoId ?: return null
        return try {
            PostTvPlayer2Coordinator.getPlayer(PLAYER_NAME + videoId)
        } catch (e: Exception) {
            null
        }
    }

    fun canAddAutoplay(): Boolean {
        return activeAutoplayCount < MAX_CONCURRENT_AUTOPLAYS
    }

    fun incrementAutoplayCount() { activeAutoplayCount++ }
    fun decrementAutoplayCount() { activeAutoplayCount-- }

    companion object {
        val TAG = VideoManager2::class.java.simpleName
        const val NO_POSITION: Long = -1
        val isAmazonDevice: Boolean
            get() = ("Amazon" == Build.MANUFACTURER)
        const val PLAYER_NAME = "INLINE_PLAYER"
        val MAX_CONCURRENT_AUTOPLAYS = ConfigManager.getInstance().config.videosConfig.maxConcurrentAutoplays
    }
}
