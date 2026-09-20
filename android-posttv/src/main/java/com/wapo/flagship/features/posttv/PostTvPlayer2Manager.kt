/* Copyright (c) 2022 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.posttv

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.annotation.LayoutRes
import androidx.annotation.OptIn
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.media3.common.Player.DISCONTINUITY_REASON_AUTO_TRANSITION
import androidx.media3.common.Player.DISCONTINUITY_REASON_REMOVE
import androidx.media3.common.Player.DISCONTINUITY_REASON_SEEK
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlaybackException
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.EventLog.*
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.LiveEvent
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.posttv.listeners.AudioFocusListener
import com.wapo.flagship.features.posttv.listeners.PiPActivity
import com.wapo.flagship.features.posttv.listeners.PostTvActivity
import com.wapo.flagship.features.posttv.listeners.PostTvApplication
import com.wapo.flagship.features.posttv.listeners.VideoListener
import com.wapo.flagship.features.posttv.listeners.VideoPlayer
import com.wapo.flagship.features.posttv.model.AdPlaybackState
import com.wapo.flagship.features.posttv.model.ControllerViewEvent
import com.wapo.flagship.features.posttv.model.PlaybackState
import com.wapo.flagship.features.posttv.model.TrackingType
import com.wapo.flagship.features.posttv.model.Video
import com.wapo.flagship.features.posttv.players.PostTvPlayer2
import com.wapo.flagship.features.posttv.util.Player2ViewModel
import com.wapo.flagship.features.posttv.util.PrefManager
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Class to manage player operations and related views (PlayerView and StyledPlayerControllerView).
 */
class PostTvPlayer2Manager: VideoTracker2.VideoEventListener2 {

    private val tag = PostTvPlayer2Manager::class.java.simpleName

    // playerName from [PostTvPlayer2Coordinator] class to identify the player from its pool.
    var playerName: String? = null
        private set

    // FocusListener to create player with or without AudioFocus feature.
    // Player can request for Audio focus gain or can mix with other media items there in the app.
    private var audioFocusListener: AudioFocusListener? = null

    // Member to respect audio focus or not. Applications classes can set while creating the
    // manager class
    private var respectAudioFocus: Boolean = true

    // Member to control pre roll ads
    private var playAds: Boolean = true

    // Member to maintain promoUrl for autoplay videos.
    private var promoUrl: String? = null

    private var playerContainerView: ViewGroup? = null
    private var progressBar: ProgressBar? = null
    private var player2: PostTvPlayer2? = null

    // Activity ref to maintain owner of this manager.
    // [com.wapo.flagship.features.posttv.PostTvPlayer2ActivityLifecycleObserver] uses this for handling
    // life cycle methods.
    private var activityRef: WeakReference<Activity>? = null
    private val appContext: Context?
        get() = activityRef?.get()?.applicationContext

    // Player's View Model to maintain its state (playWhenReady etc.,)
    var player2ViewModel: Player2ViewModel? = null
        private set

    private var currentWindow: Int = player2ViewModel?.currentWindow ?: 0

    // Video data class
    var video: Video? = null
        private set

    private var fullScreenPlayerFragment: WeakReference<FullScreenPlayerFragment>? = null

    // ParentView object to hold player's parent container details.
    // Using when switching to FullScreen.
    var parentView: ParentView? = null

    // LiveEvent for PlaybackState states set by the [PostTvPlayer2]
    private val _playbackState: LiveEvent<PlaybackState> = LiveEvent()
    val playbackState: LiveData<PlaybackState> = _playbackState

    // LiveEvent for AdPlaybackState states set by the [PostTvPlayer2]
    private val _adPlaybackState: LiveEvent<AdPlaybackState> = LiveEvent()
    val adPlaybackState: LiveData<AdPlaybackState> = _adPlaybackState

    // LiveEvent for ControllerViewEvent states set by the [PostTvPlayer2]
    private val _controllerViewEvent: LiveEvent<ControllerViewEvent> = LiveEvent()
    val controllerViewEvent: LiveData<ControllerViewEvent> = _controllerViewEvent

    // Tracking-related fields
    private var videoTracker: VideoTracker2? = null
    private val startTrackingWhenPlayerIsReady = AtomicBoolean(false)

    // Member to perform common video operations and is owned by the [com.wapo.flagship.features.posttv.VideoManager2] class
    var videoListener: VideoListener? = null
        private set

    // Member to perform player operations from [com.wapo.flagship.features.posttv.VideoManager2] class
    var videoPlayer: Player2VideoPlayer? = null
        private set

    private var lastAvExp : String? = null

    fun resetTrackedEvents(scope: VideoTracker2.TrackScope) {
        videoTracker?.resetTrackedEvents(scope)
    }

    /**
     * Method to instantiate [PostTvPlayer2] with the given playerContainerViewResId and [Player2ViewModel].
     * playerContainerViewResId and [Player2ViewModel] can be nulls while initiating the player and can be updated
     * later by using updatePlayerContainerView() and updateViewModel() methods  for flexibility.
     * @param activity  to know the owner activity of this manager class for handling in the life cycle methods.
     * @param viewModel to main player state in the life cycle methods.
     * @param playerContainerViewResId layout res id of the view group that has [androidx.media3.ui.PlayerView]
     */
    fun initPlayer(
        activity: Activity,
        viewModel: Player2ViewModel? = null,
        @LayoutRes playerContainerViewResId: Int = R.layout.posttv2_exo_player_view,
        fetchPercentage: Float? = null,
        isPlayerClickable: Boolean,
        onPlayerClicked: (() -> Unit)?
    ) {
        Logger.d(tag, "initPlayer(), activity=${activity.javaClass.simpleName}")
        activityRef = WeakReference(activity)
        player2ViewModel = viewModel
        initializePlayerContainerView(activity.applicationContext, playerContainerViewResId)
        initializePlayer2(activity.applicationContext, viewModel, fetchPercentage, isPlayerClickable, onPlayerClicked)
        initializeVideoPlayer()
        observeControllerViewEvents()
        observePlaybackStateEvents()
        observeAdPlaybackStateEvents()
        initializeVideoTracker()
    }

    /**
     * Method to instantiate playerContainerViewResId
     */
    private fun initializePlayerContainerView(context: Context, @LayoutRes playerViewResId: Int) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        playerContainerView = inflater.inflate(playerViewResId, null, false) as ViewGroup
        playerContainerView?.tag = playerViewResId
        progressBar = playerContainerView?.findViewById(R.id.progress_bar)
    }

    /**
     * Method to instantiate [PostTvPlayer2]
     */
    private fun initializePlayer2(context: Context, viewModel: Player2ViewModel? = null,
                                  fetchPercentage: Float?,
                                  isPlayerClickable: Boolean,
                                  onPlayerClicked: (() -> Unit)?
                                  ) {
        player2 = PostTvPlayer2().also {
            it.initialize(context, playerContainerView, _playbackState, _adPlaybackState, _controllerViewEvent, viewModel, fetchPercentage, isPlayerClickable, onPlayerClicked)
        }
    }

    private fun initializeVideoPlayer() {
        videoPlayer = Player2VideoPlayer(appContext,this)
    }

    /**
     * Method to instantiate [VideoTracker2]
     */
    private fun initializeVideoTracker() {
        videoTracker = player2?.initializeTracker(this)
    }

    fun stopTracking() {
        videoTracker?.stopTracking()
    }

    private fun updateTrackingValues(tracking: VideoTracker2.Tracking?) {
        videoTracker?.let {
            it.stopTracking()
            it.setTracking(tracking)
            it.resetHighestPercent()
            startTrackingWhenPlayerIsReady.set(true)
        }
    }

    private fun updatePlayer2ContainerView() {
        player2?.updateContainerView(playerContainerView, _controllerViewEvent)
    }

    private fun updatePlayer2ViewModel() {
        player2?.updateViewModel(player2ViewModel)
    }

    /**
     * Method to update [Player2ViewModel].
     * Application level classes can update [Player2ViewModel] anytime in the life span of the player.
     */
    fun updateViewModel(viewModel: Player2ViewModel?) {
        if (viewModel != player2ViewModel) {
            player2ViewModel = viewModel
            updatePlayer2ViewModel()
        }
    }

    /**
     * Method to update [activityRef]
     * Player can be transferred between activities and the actvities can call this method to update
     * their ownership.
     */
    fun updateOwnerActivity(activity: Activity) {
        if (activityRef?.get() != activity) {
            activityRef = WeakReference(activity)
        }
    }

    /**
     * [VideoManager2] sets this when it creates a [PostTvPlayer2Manager]
     */
    fun setVideoListener(videoListener: VideoListener) {
        this.videoListener = videoListener
    }

    /**
     * Method to update Player Container View.
     * Application level classes can update anytime in the life span of the player.
     */
    fun updatePlayerContainerView(@LayoutRes playerContainerViewResId: Int) {
        if (playerContainerViewResId != playerContainerView?.tag ) {
            appContext?.let {
                initializePlayerContainerView(it, playerContainerViewResId)
                updatePlayer2ContainerView()
            }
        }
    }

    @OptIn(UnstableApi::class)
    fun updatePlayerAutoHide(enableAutoHide: Boolean) {
        player2?.setEnableAutoHide(enableAutoHide)
    }

    /**
     * Currently [VideoManager2] sets this when it creates a [PostTvPlayer2Manager]
     */
    fun setPlayerName(name: String) {
        playerName = name
        // fullScreenPlayerFragment can get the player manager from the coordinator by using the
        // playerName.
        getFullScreenPlayerFragment().setPlayerName(playerName)
    }

    /**
     * Method to start playing the url from the given video object.
     * @param video [Video] object that contains the video data.
     * @param playWhenReady flag whether to start playing the video once player is ready
     * @param tracking [VideoTracker2.Tracking] object for tracking video events.
     */
    fun playMedia(video: Video,
                  playWhenReady: Boolean = true,
                  tracking: VideoTracker2.Tracking? = VideoTracker2.Tracking(video = video)
    ) {
        Logger.d(tag, "playMedia(), id=${video.id}")
        updateTrackingValues(tracking)
        requestAudioFocus()
        _adPlaybackState.value  = AdPlaybackState.NoAd
        this.video = video
        if ((activityRef?.get() as? PostTvActivity)?.isPIPEnabled == false) {
            player2?.hideControllerOption(R.id.exo_pip)
        }
        player2?.play(video, playWhenReady, promoUrl, playAds)
    }

    /**
     * Method to resume the current video. It restores the state that is saved in the last pauseMedia() call.
     * It starts playing the video when there is no last known view model Otherwise it respects the state.
     */
    fun resumeMedia() {
        player2?.restorePlayerState(player2ViewModel, video?.isLive == true)
        player2?.setPlayWhenReady(player2ViewModel == null || player2ViewModel?.playWhenReady == true)
    }

    /**
     * Method to pause the current video. It also saves state in the given view model.
     */
    fun pauseMedia() {
        player2?.savePlayerState(player2ViewModel)
        player2?.pause()
    }

    /**
     * Method to stop player as well as the tracker.
     * Caller should use a playMedia() call to start any video using the same player.
     */
    fun stopMedia() {
        videoTracker?.stopTracking()
        player2?.stop()
    }

    fun isPlaying(): Boolean {
        return player2?.isPlaying() == true
    }

    fun isInitialized(): Boolean {
        return player2 != null
    }

    fun mute() {
        player2?.mute()
    }

    fun unmute() {
        player2?.unmute()
    }

    fun loop() {
        player2?.loop()
    }

    fun isLooping(): Boolean {
        return player2?.isLooping() == true
    }

    fun isPiPActivity(): Boolean {
        return activityRef?.get() is PiPActivity
    }

    fun hideControllerOptions(vararg ids: Int) {
        ids.forEach { id -> player2?.hideControllerOption(id) }
    }

    fun showControllerOptions(vararg ids: Int) {
        ids.forEach { id -> player2?.showControllerOption(id) }
    }

    fun isPlayingAds() : Boolean{
        return player2?.isAdPlaying() == true
    }

    /**
     * Callback method from [AudioFocusListener] to control play state based on the Audio Focus events.
     */
    fun pauseMediaOnLostFocus() {
        if (respectAudioFocus) {
            pauseMedia()
        }
    }

    /**
     * Method to control a AudioFocus flag.
     * Manager will register for Audio Focus events based on this flag before start playing the video.
     */
    fun setRespectAudioFocus(respectAudioFocus: Boolean) {
        this.respectAudioFocus = respectAudioFocus
    }

    fun setPromoUrl(promoUrl: String) {
        this.promoUrl = promoUrl
    }

    fun setPlayAds(playAds: Boolean) {
        this.playAds = playAds
    }

    /**
     * Method to set a callback listener for the [androidx.media3.ui.PlayerControlView]
     * visibility events.
     */
    fun setControllerVisibilityListener(listener: (Int) -> Unit) {
        player2?.setControllerVisibilityListener(listener)
    }

    /**
     * Method to set playWhenReady flag.
     */
    fun setPlayWhenReady(playWhenReady: Boolean) {
        player2?.setPlayWhenReady(playWhenReady)
    }

    /**
     * Method to know whether this manager is owned by the given activity.
     * [PostTvPlayer2Coordinator] class uses this to maintain manager's life cycle.
     */
    fun isOwnedBy(activity: Activity): Boolean {
        return activityRef?.get() == activity
    }

    fun hideController() {
        player2?.hideController()
    }

    fun showController() {
        player2?.showController()
    }

    fun useController(useController: Boolean) {
        player2?.useController(useController)
    }

    /**
     * Method to detach [VideoManager2]'s playerFrame from its parent and to remove a playerFrame
     * from the pool.
     * [PostTvPlayer2Coordinator] calls this method when owner activity is being destroyed.
     */
    fun releasePlayerFrame() {
        videoListener?.removePlayerFrame()
        val videoManager2 = (appContext as? PostTvApplication)?.videoManager2
        videoManager2?.removePlayerFrameFromPool(video?.id)
    }

    private fun onUnmute() {
        if (video?.playType == Video.PLAY_TYPE_NORMAL_MUTED) {
            (appContext as? PostTvApplication)?.videoManager2?.onVideoUnmute(video?.id)
            setRespectAudioFocus(true)
            requestAudioFocus()
        }
    }

    /**
     * Resets player state and its tracker state once it ends.
     */
    fun resetPlayerIfEnded() {
        if (playbackState.value == PlaybackState.Ended) {
            player2?.reset()
            videoTracker?.resetHighestPercent()
            startTrackingWhenPlayerIsReady.set(true)
        }
    }

    /**
     * Method to release the manager resources.
     */
    fun releasePlayer() {
        Logger.d(tag, "releasePlayer(), id=${video?.id}")
        if (!isInitialized()) return
        // Track final progress event with any leftover engaged time
        videoTracker?.trackProgressEvent()
        videoPlayer = null
        videoListener = null
        player2ViewModel?.id = video?.id
        player2?.releasePlayer()
        player2 = null
        activityRef?.get()?.let {
            // Finish PiP activity also in any case when player is released.
            if (!it.isFinishing && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && it.isInPictureInPictureMode) {
                it.finish()
            }
        }
        activityRef = null
        detachContainerViewFromParent(playerContainerView)
        playerContainerView = null
        progressBar = null
        controllerViewEvent.removeObservers(ProcessLifecycleOwner.get())
        video = null
        audioFocusListener?.abandonAudioFocus()
        audioFocusListener = null
        videoTracker?.stopTracking()
        videoTracker = null
        parentView = null
        player2ViewModel = null
        playbackState.removeObservers(ProcessLifecycleOwner.get())
        adPlaybackState.removeObservers(ProcessLifecycleOwner.get())
        fullScreenPlayerFragment = null
    }

    fun getPlaybackPosition(): Long? {
        return player2?.getCurrentPosition()
    }

    fun getDuration(): Long? {
        return player2?.getDuration()
    }

    private fun getFullScreenPlayerFragment(): FullScreenPlayerFragment {
        return fullScreenPlayerFragment?.get()
            ?: FullScreenPlayerFragment().also {
                it.setPlayerName(playerName)
                fullScreenPlayerFragment = WeakReference(it)
            }
    }

    /**
     * Method to observe [PlaybackState] events
     */
    private fun observePlaybackStateEvents() {
        playbackState.observe(ProcessLifecycleOwner.get()) {
            Logger.d(tag,"observePlaybackStateEvents(), playbackState=$it, id=${video?.id}")
            when (it) {
                PlaybackState.Idle -> {}
                PlaybackState.Buffering -> {
                    player2ViewModel?.errorState = false
                    progressBar?.visibility = View.VISIBLE
                }
                PlaybackState.Ready -> {
                    player2ViewModel?.errorState = false
                    progressBar?.visibility = View.GONE
                    val videoManager2 = (appContext as? PostTvApplication)?.videoManager2
                    videoManager2?.setHasStartedLooping(video?.id, false)
                    if (startTrackingWhenPlayerIsReady.get()) {
                        startTrackingWhenPlayerIsReady.set(false)
                        videoTracker?.startTracking()
                    }
                    currentWindow = player2ViewModel?.currentWindow ?: 0
                }
                PlaybackState.ShowCountdownTimer, PlaybackState.CancelCountdownTimer -> {}
                PlaybackState.Ended -> {
                    videoTracker?.stopTracking()
                }
                is PlaybackState.Error -> {
                    player2ViewModel?.errorState = true
                    videoTracker?.stopTracking()
                    // log error
                    activityRef?.get()?.let { activity ->
                        val msg = "error_type=Video, msg=${it.error?.message}, cause=${it.error?.cause?.message}," +
                                " url=${video?.id}, owner=${activity.javaClass.simpleName}"
                        Builder().apply {
                            setMessage("Video Error")
                            setModule(LogModules.VIDEO)
                            setErrorMessage(it.error?.message)
                            set("cause", it.error?.cause?.message)
                            set("url", video?.id)
                            set("activity", activity.javaClass.simpleName)
                        }.run {
                            (activity as? PostTvActivity)?.logVideoError(this)
                        }
                    }
                    val exoPlaybackException = it.error as? ExoPlaybackException
                    when (exoPlaybackException?.type) {
                        ExoPlaybackException.TYPE_SOURCE -> {
                            videoListener?.onError(appContext?.getString(R.string.source_error))
                        }
                        ExoPlaybackException.TYPE_RENDERER -> {
                            videoListener?.onError(appContext?.getString(R.string.render_error))
                        }
                        else -> {
                            videoListener?.onError(appContext?.getString(R.string.unknown_error))
                        }
                    }
                }
                PlaybackState.TrackChanged -> {
                }
                is PlaybackState.PositionDiscontinuity -> {
                    updateLoopingStatus(it.reason)
                    Logger.d(tag,"observePlaybackStateEvents(), reason=${it.reason}, id=${video?.id}")
                }
                PlaybackState.Paused -> {
                }
                PlaybackState.Resumed -> {
                }
                is PlaybackState.FetchDurationReached -> {

                }
            }
        }
    }

    private fun observeAdPlaybackStateEvents() {
        adPlaybackState.observe(ProcessLifecycleOwner.get()) {
            when(it) {
                AdPlaybackState.Started -> {
                    onAdEvent(TrackingType.AD_PLAY_STARTED)
                }
                AdPlaybackState.Completed -> {
                    onAdEvent(TrackingType.AD_PLAY_COMPLETED)
                }
                is AdPlaybackState.Error -> {
                    videoListener?.onAdError(it.errorEvent.error, video)
                }
                else -> {

                }
            }
        }
    }

    /**
     * Method to observe [ControllerViewEvent] events
     */
    private fun observeControllerViewEvents() {
        controllerViewEvent.observe(ProcessLifecycleOwner.get()) {
            Logger.d(tag, "observeControllerViewEvents(), event=$it")
            when (it) {
                ControllerViewEvent.Captions -> {
                    player2?.performCaptionsEvent(activityRef?.get())
                    activityRef?.get()?.applicationContext?.let {
                        val enabled = PrefManager.getBoolean(it, PrefManager.IS_CAPTIONS_ENABLED, false)
                        onVideoEvent(TrackingType.ON_CAPTION_TOGGLE, enabled)
                    }
                }
                ControllerViewEvent.Volume -> player2?.performVolumeEvent()
                ControllerViewEvent.Share -> {
                    player2?.performVideoShare(activityRef?.get(), video)
                    onVideoEvent(TrackingType.ON_SHARE)
                }
                ControllerViewEvent.Mute -> {
                    player2?.performVolumeEvent()
                    onVideoEvent(TrackingType.ON_MUTE, false)
                    onUnmute()
                }
                ControllerViewEvent.Unmute -> {
                    player2?.performVolumeEvent()
                    onVideoEvent(TrackingType.ON_MUTE, true)
                }
                ControllerViewEvent.Play -> {
                    resetPlayerIfEnded()
                    player2?.resume()
                    onVideoEvent(TrackingType.ON_PAUSE, false)
                }
                ControllerViewEvent.Pause -> {
                    player2?.pause()
                    onVideoEvent(TrackingType.ON_PAUSE, true)
                }
                ControllerViewEvent.FullScreen -> {
                    onFullScreenEvent(playerName)
                }
                ControllerViewEvent.PictureInPicture -> {
                    exitFullScreen()
                    player2?.performPictureInPictureEvent(activityRef?.get(), video, playerName)
                }
                else -> {

                }
            }
            player2?.updateControls()
        }
    }

    /**
     * Method to return the player container view
     */
    fun getPlayerContainerView(): ViewGroup? {
        return playerContainerView
    }

    /**
     * Application level classes can utilize this method to add player container view to the
     * given itemView container.
     * This method detaches the player container view from its existing parent before adding
     * it to the itemView container.
     * If playerFrame exists for the current playerContainerView, then process the whole playerFrame
     */
    fun addPlayerContainerViewToItemView(itemView: ViewGroup) {
        if (videoListener != null) {
            if (videoListener?.playerFrame?.parent != itemView) {
                detachContainerViewFromParent(videoListener?.playerFrame)
                itemView.addView(videoListener?.playerFrame)
            }
        } else if (itemView != playerContainerView?.parent) {
            detachContainerViewFromParent(playerContainerView)
            itemView.addView(playerContainerView)
        }
    }

    private fun detachContainerViewFromParent(containerView: ViewGroup?) {
        if (containerView?.parent != null) {
            val previousParent = containerView.parent as ViewGroup
            val index = previousParent.indexOfChild(containerView)
            if (index >= 0) {
                previousParent.removeViewAt(index)
            }
        }
    }

    private fun storeParentView() {
        if (videoListener != null) {
            // If playerFrame exists for the current playerContainerView, then process the playerFrame's parent
            if (videoListener?.playerFrame?.parent != null) {
                val parent = videoListener?.playerFrame?.parent as ViewGroup
                val index = parent.indexOfChild(videoListener?.playerFrame)
                if (index >= 0) {
                    parentView = ParentView(parent, index)
                }
            }
        } else if (playerContainerView?.parent != null) {
            val parent = playerContainerView?.parent as ViewGroup
            val index = parent.indexOfChild(playerContainerView)
            if (index >= 0) {
                parentView = ParentView(parent, index)
            }
        }
    }

    private fun requestAudioFocus() {
        if (!respectAudioFocus) {
            return
        }
        appContext?.let {
            if (audioFocusListener == null) {
                audioFocusListener = AudioFocusListener(it, this)
            }
            audioFocusListener?.requestAudioFocus()
        }
    }

    private fun onFullScreenEvent(playerName: String?) {
        val viewModel = player2ViewModel ?: return
        //Start Video activity
        video?.let {
            (activityRef?.get() as? PostTvActivity)?.startFullScreen(it, playerName)
        }
    }

    fun enterFullScreen() {
        if (player2ViewModel?.isInFullScreen == false) {
            val activity = (activityRef?.get() as? FragmentActivity) ?: return
            player2ViewModel?.isInFullScreen = true
            storeParentView()
            getFullScreenPlayerFragment().showNow(activity.supportFragmentManager, null)
            player2?.performFullScreenEvent(true)
        }
    }

    fun exitFullScreen() {
        if (player2ViewModel?.isInFullScreen == true) {
            player2ViewModel?.isInFullScreen = false
            // player container can be added at it.playerViewIndex.
            // But then player container is usually added as a direct child of the item view.
            // We can revisit if there is any such case and then need to duplicate
            // addPlayerContainerViewToItemView code for adding then.
            parentView?.let { addPlayerContainerViewToItemView(it.itemView) }
            parentView = null
            // onDetach call of the fragment calls this exitFullScreen method and it is able
            // to dismiss the fragment. But there are rare cases when activity/fragment states are saved
            // already, then this check will bypass the dismiss and then fragment dismisses itself when
            // restored. It is up to the caller to handle any such cases.
            if (!getFullScreenPlayerFragment().isStateSaved) {
                getFullScreenPlayerFragment().dismiss()
            }
            player2?.performFullScreenEvent(false)
        }
        (activityRef?.get() as? PiPActivity)?.let {
            it.enterPiPMode()
        }
    }

    /**
     * Overridden method from [VideoTracker2.VideoEventListener2] for tracking analytics events.
     */
    override fun onVideoEvent(trackingType: TrackingType, value: Any?) {
        if (isPromoVideo()) return
        val videoManager2 = (appContext as? PostTvApplication)?.videoManager2
        if (videoManager2?.hasStartedLooping(video?.id) == true) return

        val videoType = videoTracker?.tracking?.videoType ?: VideoTracker2.VideoType.STANDARD
        val valueMap = mutableMapOf<String, Any>()
        videoTracker?.let { tracker ->
            tracker.tracking?.let { tracking ->
                valueMap[VideoTracker2.PROGRESS_THRESHOLD] = tracking.progressThreshold
                valueMap[VideoTracker2.AV_NAME] = tracking.video.videoName ?: ""
            }
        }
        when (trackingType) {
            TrackingType.ON_PLAY_STARTED -> {
                valueMap[VideoTracker2.VIDEO_START_ID] = videoTracker?.currentVideoStartId ?: ""
            }
            TrackingType.VIDEO_PERCENTAGE_WATCHED -> {
                if (video?.playType != Video.PLAY_TYPE_NORMAL) { return }
                // Contains progress threshold and percentage watched
                value?.let { valueMap[VideoTracker2.TRACKING_VALUE] = it as Int }
            }
            TrackingType.ON_PLAY_COMPLETED -> {
                valueMap[VideoTracker2.VIDEO_START_ID] = videoTracker?.currentVideoStartId ?: ""
                if (video?.playType != Video.PLAY_TYPE_NORMAL) { return }
            }
            TrackingType.ON_MUTE -> {
                value?.let {
                    valueMap[VideoTracker2.TRACKING_VALUE] =
                        if (it as Boolean) {
                            VideoTracker2.VIDEO_MUTED
                        } else {
                            VideoTracker2.VIDEO_UNMUTED
                        }
                }
            }
            TrackingType.ON_CAPTION_TOGGLE -> {
                value?.let {
                    valueMap[VideoTracker2.TRACKING_VALUE] =
                        if (it as Boolean) {
                            VideoTracker2.VIDEO_CAPTION_ON
                        } else {
                            VideoTracker2.VIDEO_CAPTION_OFF
                        }
                }
            }
            TrackingType.ON_SHARE -> {
                valueMap[VideoTracker2.TRACKING_VALUE] = VideoTracker2.VIDEO_SOCIAL_SHARE_START
            }
            TrackingType.ON_PAUSE -> {
                value?.let {
                    valueMap[VideoTracker2.TRACKING_VALUE] =
                        if (it as Boolean) {
                            VideoTracker2.VIDEO_PAUSE
                        } else {
                            VideoTracker2.VIDEO_UNPAUSE
                        }
                }
            }
            TrackingType.ON_PLAY_NEXT -> {
                valueMap[VideoTracker2.TRACKING_VALUE] = VideoTracker2.VIDEO_PLAY_NEXT
                lastAvExp = VideoTracker2.VIDEO_PLAY_NEXT
            }
            TrackingType.ON_PLAY_PREVIOUS -> {
                valueMap[VideoTracker2.TRACKING_VALUE] = VideoTracker2.VIDEO_PLAY_PREVIOUS
                lastAvExp = VideoTracker2.VIDEO_PLAY_PREVIOUS
            }
            TrackingType.ON_AUTO_SCROLLED -> {
                lastAvExp = VideoTracker2.ROLLTHROUGH_FEED
            }
            TrackingType.VIDEO_PROGRESS -> {
                valueMap[VideoTracker2.ENGAGED_TIME] = value.toString()
                valueMap[VideoTracker2.VIDEO_START_ID] = videoTracker?.currentVideoStartId ?: ""
            }
            else -> {
                // no op
            }
        }
        videoTracker?.tracking?.let {
            valueMap[VideoTracker2.VIDEO_START_ID] = videoTracker?.currentVideoStartId ?: ""
            if (it.videoType == VideoTracker2.VideoType.VERTICAL_CAROUSEL) {
                valueMap[VideoTracker2.CAROUSEL] = true
                if (it.swipeDirection == VideoTracker2.SwipeDirection.BACK) {
                    valueMap[VideoTracker2.SWIPE_DIRECTION] = VideoTracker2.CAROUSEL_BACK
                } else if (it.swipeDirection == VideoTracker2.SwipeDirection.FORWARD) {
                    valueMap[VideoTracker2.SWIPE_DIRECTION] = VideoTracker2.CAROUSEL_FORWARD
                }
            } else if (it.videoType == VideoTracker2.VideoType.VERTICAL_FULLSCREEN) {
                valueMap[VideoTracker2.CAROUSEL] = false
                when (lastAvExp) {
                    VideoTracker2.VIDEO_PLAY_NEXT, VideoTracker2.VIDEO_PLAY_PREVIOUS -> valueMap[VideoTracker2.AV_EXP] = VideoTracker2.SWIPE_FEED
                    VideoTracker2.ROLLTHROUGH_FEED -> valueMap[VideoTracker2.AV_EXP] = VideoTracker2.ROLLTHROUGH_FEED
                    null -> valueMap[VideoTracker2.AV_EXP] = VideoTracker2.CLICK_FEED
                }
            }
            else if (it.videoType == VideoTracker2.VideoType.WATCH_AUTOPLAY) {
                valueMap[VideoTracker2.CAROUSEL] = false
            }
        }
        video?.let {
            (activityRef?.get() as? PostTvActivity)?.onTrackingEvent(videoType, trackingType, it, valueMap)
        }
    }

    override fun onAdEvent(trackingType: TrackingType) {
        val videoType = videoTracker?.tracking?.videoType ?: VideoTracker2.VideoType.STANDARD
        val postTvActivity = (activityRef?.get() as? PostTvActivity)
        val valueMap = mutableMapOf<String, Any>()
        videoTracker?.let { tracker ->
            tracker.tracking?.let { tracking ->
                valueMap[VideoTracker2.PROGRESS_THRESHOLD] = tracking.progressThreshold
            }
        }
        video?.let {
            postTvActivity?.onTrackingEvent(videoType, trackingType, it, valueMap)
        }
    }

    /**
     * Checks if the current video has started looping,
     * and updates its looping status in the video manager
     */
    private fun updateLoopingStatus(reason: Int) {
        val videoManager2 = (appContext as? PostTvApplication)?.videoManager2
        /* Check that:
        * 1. The player auto-transitioned from one period in the timeline to the next
        *    (reason 0 is DISCONTINUITY_REASON_AUTO_TRANSITION)
        * 2. The playback position is back to 0
        * 3. The current window has not changed, meaning the player is still on the same video */
        player2ViewModel?.let { viewModel ->
            if (reason == DISCONTINUITY_REASON_AUTO_TRANSITION && viewModel.playbackPosition == 0L && currentWindow == viewModel.currentWindow) {
                videoManager2?.let { videoManager ->
                    if (!videoManager.hasStartedLooping(video?.id) && isLooping()) {
                        videoManager.setHasStartedLooping(video?.id, true)
                    }
                }
                /* Reset highest percentage so that ON_PLAY_STARTED case will get hit again
                * in VideoTracker2.trackProgress() */
                videoTracker?.resetHighestPercent()
            }

            val scrubbedForward = (reason == DISCONTINUITY_REASON_SEEK) &&
                    videoTracker?.userScrubbedForward(getPlaybackPosition()) == true
            val videoStopped = reason == DISCONTINUITY_REASON_REMOVE
            if (scrubbedForward || videoStopped) {
                videoTracker?.trackProgressEvent()
            }
        }
    }

    fun isPromoVideo(): Boolean {
        return !promoUrl.isNullOrEmpty()
    }

    /**
     * Data class to store Parent container details temporarily for any sub operations.
     * Flows that are using this class:
     * 1. FullScreen Event
     */
    data class ParentView(val itemView: ViewGroup, val playerViewIndex: Int)

    /**
     * [VideoManager2] class maintains the [Player2VideoPlayer] objects to perform unique operations on each player.
     */
    class Player2VideoPlayer(val appContext: Context?, val postTvPlayer2Manager: PostTvPlayer2Manager): VideoPlayer {

        val videoManager2 = (appContext as? PostTvApplication)?.videoManager2

        private fun observePlaybackStateEvents(video: Video) {
            postTvPlayer2Manager.playbackState.observe(ProcessLifecycleOwner.get()) {
                when(it) {
                    PlaybackState.Ended -> {
                        if (!postTvPlayer2Manager.isLooping()
                            && !postTvPlayer2Manager.isPiPActivity()
                            && postTvPlayer2Manager.player2ViewModel?.isInFullScreen == false
                        ) {
                            videoManager2?.release(video.id)
                        }
                    }
                    PlaybackState.Ready -> {
                        postTvPlayer2Manager.videoListener?.setIsLoading(false)
                    }
                    PlaybackState.Buffering -> {
                        postTvPlayer2Manager.videoListener?.setIsLoading(true)
                    }
                    else -> {
                        // no op
                    }
                }
            }
        }

        private fun attachContainerToPlayerFrame(video: Video) {
            postTvPlayer2Manager.videoListener?.addVideoView(postTvPlayer2Manager.playerContainerView)
            observePlaybackStateEvents(video)
        }

        override fun release() {
            postTvPlayer2Manager.releasePlayer()
        }

        override fun getId(): String? {
            return postTvPlayer2Manager.video?.id
        }

        override fun getVideo(): Video? {
            return postTvPlayer2Manager.video
        }

        override fun onActivityResume() {

        }

        override fun playVideo(video: Video) {
            postTvPlayer2Manager.apply {
                // Set AudioFocus, mute/unmute, controller and ads visibility
                when (video.playType) {
                    Video.PLAY_TYPE_NORMAL -> {
                        setRespectAudioFocus(true)
                        unmute()
                        if (video.aspectRatio < 1){
                            useController(false)

                        } else {
                            useController(true)
                        }
                        setPlayAds(true)
                    }
                    Video.PLAY_TYPE_NORMAL_MUTED -> {
                        setRespectAudioFocus(false)
                        mute()
                        if (video.aspectRatio < 1){
                            useController(false)
                        } else {
                            useController(true)
                        }
                        hideControllerOptions(R.id.exo_share, R.id.exo_cc, R.id.exo_fullscreen, R.id.exo_pip)
                        setPlayAds(false)
                    }
                    Video.PLAY_TYPE_AUTOPLAY -> {
                        setRespectAudioFocus(false)
                        mute()
                        useController(false)
                        setPlayAds(false)
                    }
                }
                // Handle loop and promo for not normal types
                if (video.playType != Video.PLAY_TYPE_NORMAL) {
                    if (video.isLooping) loop()
                    if (!video.autoplay && !video.promoUrl.isNullOrEmpty()) {
                        setPromoUrl(video.promoUrl)
                        if (video.promoIsLooping == true) loop()
                    }
                }
            }

            attachContainerToPlayerFrame(video)
            postTvPlayer2Manager.apply {
                playMedia(video)
                hideController()
                player2?.updateControls()
            }
        }

        override fun pausePlay(shouldPlay: Boolean) {
            postTvPlayer2Manager.setPlayWhenReady(shouldPlay)
        }

        override fun toggleCaptions() {
        }

        override fun isPlaying(): Boolean {
            return postTvPlayer2Manager.isPlaying()
        }

        override fun isFullScreen(): Boolean {
            return postTvPlayer2Manager.player2ViewModel?.isInFullScreen == true
        }

        override fun isInPiP(): Boolean {
            return postTvPlayer2Manager.isPiPActivity()
        }

        override fun onAdEvent(adEvent: VideoListener.AdEvent?) {
        }

        override fun mute() {
            postTvPlayer2Manager.apply {
                mute()
                player2?.updateControls()
            }
        }

        override fun respectAudioFocus(flag: Boolean) {
            postTvPlayer2Manager.setRespectAudioFocus(flag)
        }
    }
}
