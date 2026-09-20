// Copyright (c) 2022 The Washington Post. All rights reserved.

package com.wapo.flagship.features.posttv.players

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.ima.ImaAdsLoader
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSourceFactory
import androidx.media3.exoplayer.source.ads.AdsLoader
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.PlayerView
import androidx.media3.ui.PlayerView.ControllerVisibilityListener
import com.google.ads.interactivemedia.v3.api.AdEvent
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.LiveEvent
import com.wapo.android.commons.util.Logger
import com.wapo.android.commons.util.subtractTimes
import com.wapo.flagship.features.posttv.BuildConfig
import com.wapo.flagship.features.posttv.ExoPlayerCache
import com.wapo.flagship.features.posttv.R
import com.wapo.flagship.features.posttv.VideoTracker2
import com.wapo.flagship.features.posttv.listeners.PostTvActivity
import com.wapo.flagship.features.posttv.listeners.PostTvApplication
import com.wapo.flagship.features.posttv.model.AdPlaybackState
import com.wapo.flagship.features.posttv.model.ControllerViewEvent
import com.wapo.flagship.features.posttv.model.PlaybackState
import com.wapo.flagship.features.posttv.model.Video
import com.wapo.flagship.features.posttv.util.Player2ViewModel
import com.wapo.flagship.features.posttv.util.PrefManager
import com.wapo.flagship.features.posttv.util.controllerViewClickListener
import com.wapo.flagship.features.posttv.util.playbackStateListener

@UnstableApi /**
 * Class to manage [ExoPlayer] and its [PlayerControlView], [DefaultTrackSelector],
 * [AdsLoader], ui elements and event listeners.
 */
class PostTvPlayer2 : Player.Listener {
    private val tag = "PostTvPlayer2"
    private var adsLoader: AdsLoader? = null
    private var playerView: PlayerView? = null
    private var playerControlView: PlayerControlView? = null
    private var adViewContainer: ViewGroup? = null
    private var player: ExoPlayer? = null
    private var shareButton: View? = null
    private var captionsButton: View? = null
    private var muteButton: View? = null
    private var unmuteButton: View? = null
    private var playButton: View? = null
    private var pauseButton: View? = null
    private var fullScreenButton: View? = null
    private var pipButton: View? = null
    private var position: View? = null
    private var duration: View? = null
    private var progress: View? = null
    private var remainingDuration: View? = null
    private var fetchPercentage: Float? = null
    private var isTalkBackEnable = false
    private var defaultTimer: Int? = null
    private lateinit var trackSelector: DefaultTrackSelector
    private val defaultTrackFilter = DefaultTrackFilter()
    private val _playbackState: LiveEvent<PlaybackState> = LiveEvent()
    private val eventListener by lazy { playbackStateListener(_playbackState, player, fetchPercentage) }
    private lateinit var cacheDataSourceFactory: CacheDataSource.Factory

    fun initialize(
        context: Context,
        playerContainerView: ViewGroup?,
        playbackState: MutableLiveData<PlaybackState>,
        adPlaybackState: MutableLiveData<AdPlaybackState>,
        controllerViewEvent: MutableLiveData<ControllerViewEvent>,
        viewModel: Player2ViewModel? = null,
        fetchPercentage: Float?,
        isPlayerClickable: Boolean,
        onPlayerClicked: (() -> Unit)?
    ) {
        Logger.d(tag, "initialize()")
        initializePlayerView(playerContainerView, controllerViewEvent, isPlayerClickable, onPlayerClicked)
        initializePlayer(context, viewModel, adPlaybackState, fetchPercentage)
        observePlaybackStateEvents(playbackState)
    }

    private fun initializePlayerView(
        playerContainerView: ViewGroup?,
        controllerViewEvent: MutableLiveData<ControllerViewEvent>,
        isPlayerClickable: Boolean = false,
        onPlayerClicked: (() -> Unit)?
    ) {
        Logger.d(tag, "initializePlayerView()")
        playerView =
            playerContainerView?.findViewById<PlayerView>(R.id.player_view)?.also {
                playerControlView = it.findViewById(androidx.media3.ui.R.id.exo_controller)
                shareButton = it.findViewById(R.id.exo_share)
                captionsButton = it.findViewById(R.id.exo_cc)
                muteButton = it.findViewById(R.id.exo_mute)
                unmuteButton = it.findViewById(R.id.exo_unmute)
                playButton = it.findViewById(androidx.media3.ui.R.id.exo_play)
                pauseButton = it.findViewById(androidx.media3.ui.R.id.exo_pause)
                fullScreenButton = it.findViewById(R.id.exo_fullscreen)
                pipButton = it.findViewById(R.id.exo_pip)
                position = it.findViewById(androidx.media3.ui.R.id.exo_position)
                duration = it.findViewById(androidx.media3.ui.R.id.exo_duration)
                progress = it.findViewById(androidx.media3.ui.R.id.exo_progress)
                progress?.visibility = View.VISIBLE
                remainingDuration = it.findViewById(R.id.exo_remaining_duration)
                if (isPlayerClickable) {
                    it.setOnClickListener {
                        onPlayerClicked?.invoke()
                    }
                }
                // No library method is available to show the remaining duration on the views.
                // Handling using duration TextWatcher to calculate and update the remaining duration view.
                (position as? TextView)?.addTextChangedListener(
                    object : TextWatcher {
                        val d: TextView? = duration as? TextView
                        val r: TextView? = remainingDuration as? TextView

                        override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int,
                        ) {}

                        override fun onTextChanged(
                            s: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int,
                        ) {
                            r?.text = subtractTimes(d?.text?.toString(), s?.toString())
                        }

                        override fun afterTextChanged(s: Editable?) {}
                    },
                )
                it.keepScreenOn = true
            }
        adViewContainer = playerContainerView?.findViewById(R.id.ad_view)
        handleControllerViewEvents(controllerViewEvent)
        updateControls()
    }

    private fun initializePlayer(
        context: Context,
        viewModel: Player2ViewModel? = null,
        adPlaybackState: MutableLiveData<AdPlaybackState>,
        fetchPercentage: Float?,
    ) {
        // Create an AdsLoader.
        adsLoader =
            ImaAdsLoader
                .Builder(context)
                .setDebugModeEnabled(false)
                .setAdEventListener {
                    when (it.type) {
                        AdEvent.AdEventType.LOADED -> adPlaybackState.value = AdPlaybackState.Loaded
                        AdEvent.AdEventType.CONTENT_PAUSE_REQUESTED -> adPlaybackState.value = AdPlaybackState.ContentPauseRequested
                        AdEvent.AdEventType.STARTED -> adPlaybackState.value = AdPlaybackState.Started
                        AdEvent.AdEventType.AD_PROGRESS -> adPlaybackState.value = AdPlaybackState.AdProgress
                        AdEvent.AdEventType.FIRST_QUARTILE -> adPlaybackState.value = AdPlaybackState.FirstQuartile
                        AdEvent.AdEventType.MIDPOINT -> adPlaybackState.value = AdPlaybackState.MidPoint
                        AdEvent.AdEventType.THIRD_QUARTILE -> adPlaybackState.value = AdPlaybackState.ThirdQuartile
                        AdEvent.AdEventType.COMPLETED -> adPlaybackState.value = AdPlaybackState.Completed
                        AdEvent.AdEventType.CONTENT_RESUME_REQUESTED -> adPlaybackState.value = AdPlaybackState.ContentResumeRequested
                        AdEvent.AdEventType.ALL_ADS_COMPLETED -> adPlaybackState.value = AdPlaybackState.AllAdsCompleted
                        else -> {
                            // no op
                        }
                    }
                }.setAdErrorListener {
                    Logger.d(tag, "AdError: type=${it.error.errorType}, code=${it.error.errorCode}")
                    // Listener gets called on a main thread in general.
                    // But there are instances reported in Crashlytics that listener is being called on a non main thread.
                    adPlaybackState.postValue(AdPlaybackState.Error(it))
                }.build()

        // Set up the factory for media sources, passing the ads loader and ad view providers.
        cacheDataSourceFactory = ExoPlayerCache.createCacheDataSourceFactory()

        val mediaSourceFactory: MediaSourceFactory =
            DefaultMediaSourceFactory(cacheDataSourceFactory)
                .setAdsLoaderProvider { adsLoader }
                .setAdViewProvider { adViewContainer }

        trackSelector = DefaultTrackSelector(context)
        updateTrackSelector(context)

        // Create a ExoPlayer and set it as the player for content and ads.
        // Controller show timeout is set to 2 sec
        this.fetchPercentage = fetchPercentage
        player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .build()
        player?.addListener(eventListener)
        playerView?.player = player
        adsLoader?.setPlayer(player)
        restorePlayerState(viewModel)
        updateControls()
    }

    /**
     * Method to update Closed Captions visibility
     */
    private fun updateTrackSelector(context: Context) {
        val captionsEnabled = areVideoCaptionsEnabled(context)
        val parameterBuilder =
            DefaultTrackSelector
                .ParametersBuilder(context)
                .setMaxVideoSizeSd()
                .setRendererDisabled(C.TRACK_TYPE_VIDEO, !captionsEnabled)
                .setSelectUndeterminedTextLanguage(captionsEnabled)
        trackSelector.setParameters(parameterBuilder)
    }

    fun updateContainerView(
        playerContainerView: ViewGroup?,
        controllerViewEvent: MutableLiveData<ControllerViewEvent>,
        onPlayerClicked: (() -> Unit)? = {}
    ) {
        Logger.d(tag, "updateContainerView()")
        initializePlayerView(playerContainerView, controllerViewEvent, onPlayerClicked = onPlayerClicked)
        playerView?.player = player
    }

    fun updateViewModel(viewModel: Player2ViewModel?) {
        Logger.d(tag, "updateViewModel()")
        viewModel?.let { vm ->
            player?.playWhenReady = vm.playWhenReady
            player?.seekTo(vm.currentWindow, vm.playbackPosition)
        }
    }

    private fun observePlaybackStateEvents(state: MutableLiveData<PlaybackState>) {
        _playbackState.observe(ProcessLifecycleOwner.get()) {
            when (it) {
                PlaybackState.Idle -> {
                    state.value = PlaybackState.Idle
                }
                PlaybackState.Buffering -> {
                    state.value = PlaybackState.Buffering
                }
                PlaybackState.Ready -> {
                    updateSubtitlesControlVisibility()
                    state.value = PlaybackState.Ready
                }
                PlaybackState.ShowCountdownTimer -> {
                    state.value = it
                }
                PlaybackState.CancelCountdownTimer -> {
                    state.value = it
                }
                PlaybackState.Ended -> {
                    state.value = PlaybackState.Ended
                }
                is PlaybackState.Error -> {
                    state.value = it
                }
                PlaybackState.TrackChanged -> {
                    state.value = PlaybackState.TrackChanged
                    updateSubtitlesControlVisibility()
                }
                is PlaybackState.PositionDiscontinuity -> {
                    state.value = it
                }
                is PlaybackState.Paused -> {
                    state.value = it
                }
                is PlaybackState.Resumed -> {
                    state.value = it
                }
                is PlaybackState.FetchDurationReached -> {
                    if (!isAdPlaying()) {
                        state.value = it
                    }
                }
            }
        }
    }

    private fun handleControllerViewEvents(controllerViewEvent: MutableLiveData<ControllerViewEvent>) {
        val clickListener = controllerViewClickListener(controllerViewEvent)
        shareButton?.setOnClickListener(clickListener)
        captionsButton?.setOnClickListener(clickListener)
        muteButton?.setOnClickListener(clickListener)
        unmuteButton?.setOnClickListener(clickListener)
        playButton?.setOnClickListener(clickListener)
        pauseButton?.setOnClickListener(clickListener)
        fullScreenButton?.setOnClickListener(clickListener)
        pipButton?.setOnClickListener(clickListener)
    }

    /**
     * Method to play the url from the given video object.
     */
    fun play(
        video: Video,
        playWhenReady: Boolean,
        overrideVideoUrl: String? = null,
        playAds: Boolean = true,
    ) {
        Logger.d(tag, "play()")
        if (video.isLive) {
            // Hide postion, duration and progress views on live videos
            position?.visibility = View.GONE
            duration?.visibility = View.GONE
            progress?.visibility = View.GONE
            // App doesn't set length to the cache's dataSpec objects for any videos.
            // So enabling flags here to ignore for any such requests without lengths.
            // Live videos serves steaming data from the n/w once sets this flag. Otherwise
            // HlsMediaSource throws an error after playing a few seconds.
            // https://github.com/google/ExoPlayer/issues/4188
            ExoPlayerCache.ignoreCacheUnsetLengthRequests(cacheDataSourceFactory)
        }
        if (player?.isPlayingAd == true) {
            // resets the current adViewContainer state when player has any previous video ad state
            // to play the next ad.
            // AdsLoader is allowing to play any ad only one time on the given existing player.
            // If caller wants to play the same ad everytime for every play() call, then the caller should
            // init and release the manager for every player.
            adViewContainer?.removeAllViews()
            adsLoader?.apply {
                release()
                setPlayer(player)
            }
        }
        // Create the MediaItem to play, specifying the content URI and ad tag URI.
        val contentUri: Uri? = Uri.parse(overrideVideoUrl ?: video.id)

        val adTagUri: Uri? = video.adTagUrl?.trim()?.takeIf { it.isNotEmpty() }?.toUri()

        if (BuildConfig.DEBUG) {
            Logger.d(tag, "contentUri=$contentUri, adTagUri=$adTagUri")
        }
        val mediaItem =
            MediaItem
                .Builder()
                .setAdsConfiguration(
                    adTagUri?.let { MediaItem.AdsConfiguration.Builder(it).build() }
                )
                .setUri(contentUri)
                .build()
        player?.apply {
            stop()   //It stops the playback and resets the player's state. Important to do before loading new media or reconfiguring the player to ensure a clean state. Any resources associated with the previous media are typically released.
            setMediaItem(mediaItem)
            prepare()
            if (video.playbackPosition > -1) seekTo(0, video.playbackPosition)
            this.playWhenReady = playWhenReady
        }
    }

    fun hideController() {
        if (!isTalkBackEnable) {
            playerView?.hideController()
        }
    }

    fun showController() {
        playerView?.showController()
    }

    fun useController(useController: Boolean) {
        playerView?.useController = useController
    }

    fun setEnableAutoHide(enabled: Boolean) {
        isTalkBackEnable = !enabled
        playerView?.let {
            defaultTimer = it.controllerShowTimeoutMs
            it.controllerAutoShow = enabled
            it.controllerHideOnTouch = enabled
            if (!enabled) {
                // If we're disabling auto-hide, set a very long timeout as a fallback
                // and ensure the controller is visible.
                it.controllerShowTimeoutMs = Int.MAX_VALUE
                it.showController()
            } else {
                // Restore default timeout
                defaultTimer?.let { timer ->
                    it.controllerShowTimeoutMs = timer
                }
            }
        }
    }

    fun setControllerHideDuringAds(){
        playerView?.setControllerHideDuringAds(false)
    }

    fun pause() {
        Logger.d(tag, "pause()")
        if (player?.isPlaying == true) {
            _playbackState.value = PlaybackState.Paused
        }
        player?.pause()
    }

    fun resume() {
        Logger.d(tag, "resume()")
        if (player?.isPlaying == false) {
            _playbackState.value = PlaybackState.Resumed
        }
        player?.play()
    }

    fun setPlayWhenReady(playWhenReady: Boolean) {
        player?.playWhenReady = playWhenReady
    }

    fun getCurrentPosition(): Long? = player?.currentPosition

    fun getDuration(): Long? = player?.duration

    fun reset() {
        player?.seekTo(C.TIME_UNSET)
    }

    fun stop() {
        player?.stop()
    }

    fun isPlaying(): Boolean = player?.isPlaying == true

    fun isAdPlaying(): Boolean = player?.isPlayingAd == true

    private fun releaseExoPlayer() {
        adsLoader?.apply {
            setPlayer(null)
            release()
        }
        playerView?.player = null
        playerControlView = null
        player?.apply {
            removeListener(eventListener)
            release()
        }
    }

    /**
     * Method to release [PostTvPlayer2] resources.
     * This should be called by the corresponding owner classes to avoid any leaks.
     */
    fun releasePlayer() {
        Logger.d(tag, "releasePlayer()")
        releaseExoPlayer()
        _playbackState.removeObservers(ProcessLifecycleOwner.get())
        player = null
        playerView = null
        playerControlView = null
        adViewContainer = null
        shareButton = null
        captionsButton = null
        muteButton = null
        unmuteButton = null
        playButton = null
        pauseButton = null
        fullScreenButton = null
        pipButton = null
        position = null
        duration = null
        remainingDuration = null
    }

    fun savePlayerState(viewModel: Player2ViewModel?) {
        player?.let {
            viewModel?.apply {
                playbackPosition = it.currentPosition
                currentWindow = it.currentWindowIndex
                playWhenReady = it.playWhenReady
                Logger.d(tag, "savePlayerState, pos=$playbackPosition, window=$currentWindow, playWhenReady=$playWhenReady")
            }
        }
    }

    fun restorePlayerState(
        viewModel: Player2ViewModel?,
        restoreToDefaultPosition: Boolean = false,
    ) {
        player?.apply {
            viewModel?.let { vm ->
                playWhenReady = vm.playWhenReady
                if (restoreToDefaultPosition) {
                    seekToDefaultPosition()
                } else {
                    seekTo(vm.currentWindow, vm.playbackPosition)
                }
                Logger.d(
                    tag,
                    "restorePlayerState, pos=${vm.playbackPosition}, window=${vm.currentWindow}, playWhenReady=${vm.playWhenReady}",
                )
            }
        }
    }

    fun performCaptionsEvent(activity: Activity?) {
        activity?.applicationContext?.let {
            val enabled = PrefManager.getBoolean(it, PrefManager.IS_CAPTIONS_ENABLED, false)
            setVideoCaptionsEnabled(it, !enabled)
            updateTrackSelector(it)
            updateControls()
        }
    }

    fun performVideoShare(
        activity: Activity?,
        video: Video?,
    ) {
        val currentActivity = (activity?.applicationContext as PostTvApplication).currentActivity
        if (currentActivity is PostTvActivity) {
            (currentActivity as PostTvActivity).shareVideo(video?.headline, video?.shareUrl)
        }
    }

    fun performVolumeEvent() {
        player?.let { if (it.volume > 0f) mute() else unmute() }
    }

    fun performPictureInPictureEvent(
        activity: Activity?,
        video: Video?,
        playerName: String?,
    ) {
        val currentActivity = (activity?.applicationContext as PostTvApplication).currentActivity as? PostTvActivity
        currentActivity?.startPIP(video, playerName)
    }

    fun mute() {
        player?.volume = 0f
    }

    fun unmute() {
        player?.volume = 1f
    }

    fun loop() {
        player?.repeatMode = Player.REPEAT_MODE_ALL
    }

    fun isLooping(): Boolean = player?.repeatMode == Player.REPEAT_MODE_ALL || player?.repeatMode == Player.REPEAT_MODE_ONE

    fun hideControllerOption(
        @IdRes id: Int,
    ) {
        when (id) {
            R.id.exo_share -> shareButton?.visibility = View.GONE
            R.id.exo_cc -> captionsButton?.visibility = View.GONE
            R.id.exo_fullscreen -> fullScreenButton?.visibility = View.GONE
            R.id.exo_pip -> pipButton?.visibility = View.GONE
        }
    }

    fun showControllerOption(
        @IdRes id: Int,
    ) {
        when (id) {
            R.id.exo_share -> shareButton?.visibility = View.VISIBLE
            R.id.exo_cc -> captionsButton?.visibility = View.VISIBLE
            R.id.exo_fullscreen -> fullScreenButton?.visibility = View.VISIBLE
            R.id.exo_pip -> pipButton?.visibility = View.VISIBLE
        }
    }

    fun performFullScreenEvent(expanded: Boolean) {
        (fullScreenButton as? ImageButton)?.let { button ->
            // Changing drawable here. Separate buttons can be created for on and off
            // (like for mute and unmute) states to have different image drawables for
            // different places in the app when required.
            val drawableResId =
                if (expanded) {
                    R.drawable.ic_baseline_fullscreen_exit_24
                } else {
                    R.drawable.ic_baseline_fullscreen_24
                }
            button.setImageDrawable(ContextCompat.getDrawable(button.context, drawableResId))
        }
    }

    fun setControllerVisibilityListener(listener: (Int) -> Unit) {
        playerView?.setControllerVisibilityListener(ControllerVisibilityListener {
            listener(it)
        })
    }

    /**
     * Method to update current ui based on the player state.
     */
    fun updateControls() {
        Logger.d(tag, "updateControls()")
        player?.let {
            if (it.volume > 0f) {
                muteButton?.visibility = View.GONE
                unmuteButton?.visibility = View.VISIBLE
            } else {
                unmuteButton?.visibility = View.GONE
                muteButton?.visibility = View.VISIBLE
            }
            (captionsButton as? ImageButton)?.let { button ->
                // Changing drawable here. Separate buttons can be created for on and off
                // (like for mute and unmute) states to have different image drawables for
                // different places in the app when required.
                val drawableResId =
                    if (areVideoCaptionsEnabled(button.context)) {
                        R.drawable.ic_baseline_closed_caption_24
                    } else {
                        R.drawable.ic_baseline_closed_caption_off_24
                    }
                button.setImageDrawable(ContextCompat.getDrawable(button.context, drawableResId))
            }
        }
    }

    private fun updateSubtitlesControlVisibility() {
        val tracksCount = WaPoTrackSelectionView.getNumberOfTracks(trackSelector, defaultTrackFilter)
        captionsButton?.visibility = if (tracksCount > 0) View.VISIBLE else View.GONE
    }

    private fun setVideoCaptionsEnabled(
        context: Context,
        value: Boolean,
    ) {
        PrefManager.saveBoolean(context, PrefManager.IS_CAPTIONS_ENABLED, value)
    }

    private fun areVideoCaptionsEnabled(context: Context): Boolean =
        PrefManager.getBoolean(context, PrefManager.IS_CAPTIONS_ENABLED, AppContextUtils.isVideoCaptionsEnabled())

    fun initializeTracker(listener: VideoTracker2.VideoEventListener2): VideoTracker2? = player?.let { VideoTracker2(it, listener) }
}
