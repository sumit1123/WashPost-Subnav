// Copyright (c) 2022 The Washington Post. All rights reserved.

package com.wapo.flagship.features.video

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.google.android.material.snackbar.Snackbar
import com.wapo.adsinf.models.AdsModel
import com.wapo.adsinf.policy.AdService
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.util.DeviceUtils
import com.wapo.android.commons.util.Logger
import com.wapo.android.commons.util.Utils.isConnectedOrConnecting
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.features.articles2.activities.ArticlesParcel
import com.wapo.flagship.features.conversations.ui.CommentBottomSheetFragment
import com.wapo.flagship.features.posttv.PostTvPlayer2Coordinator
import com.wapo.flagship.features.posttv.PostTvPlayer2Manager
import com.wapo.flagship.features.posttv.VideoTracker2
import com.wapo.flagship.features.posttv.listeners.PostTvActivity
import com.wapo.flagship.features.posttv.model.PlaybackState
import com.wapo.flagship.features.posttv.model.TrackingType
import com.wapo.flagship.features.posttv.model.Video
import com.wapo.flagship.features.posttv.util.Player2ViewModel
import com.wapo.flagship.features.shared.activities.BaseActivity
import com.wapo.flagship.features.video.models.VerticalVideoAdItem
import com.wapo.flagship.features.video.models.VideoAdResponse
import com.wapo.flagship.features.video.viewmodels.NativeVideoAdViewModel
import com.wapo.flagship.features.video.viewmodels.VideoActivityViewModel
import com.wapo.flagship.features.wpvideos.data.WatchVideoMapper
import com.wapo.flagship.features.wpvideos.models.WatchVideoAdItem
import com.wapo.flagship.features.wpvideos.ui.VerticalVideoCommentButton
import com.wapo.flagship.util.ReachabilityUtil
import com.wapo.flagship.util.Share
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.util.tracking.states.NavigationBehavior
import com.wapo.flagship.views.SnackbarFactory
import com.wapo.flagship.views.VerticalVideosPlayerName
import com.wapo.flagship.views.VerticalVideosRecyclerViewAdapter
import com.washingtonpost.android.R
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.databinding.ActivityVerticalVideosBinding
import com.washingtonpost.userhistory.models.VideoConclusionState
import com.washingtonpost.userhistory.viewmodel.UserHistoryViewModel
import com.wpds.theme.AndroidClassicTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class VerticalVideosActivity :
    BaseActivity(),
    PostTvActivity {

    private val videoActivityViewModel: VideoActivityViewModel by viewModels()

    private val userHistoryViewModel: UserHistoryViewModel by viewModels()

    @Inject
    lateinit var adService: AdService

    private val tag = "VerticalVideosActivity"
    private lateinit var binding: ActivityVerticalVideosBinding
    private val playerViewModel: Player2ViewModel by viewModels()
    private val videosAdViewModel: NativeVideoAdViewModel by viewModels()
    private var errorSnackbar: Snackbar? = null
    private lateinit var player2Manager: PostTvPlayer2Manager
    private lateinit var adapter: VerticalVideosRecyclerViewAdapter
    private var isAutoScrolled: Boolean = false
    private var lastTrackedPosition: Int = RecyclerView.NO_POSITION
    private var isLoading: Boolean = false
    private var tabName: String? = null
    private val commentCountState = mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.initPaywallIfNeeded()

        requestedOrientation =
            if (DeviceUtils.isTablet(applicationContext)) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            }

        binding = ActivityVerticalVideosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hideSystemBars()

        setupCommentButton()

        VectorDrawableCompat
            .create(resources, R.drawable.vertical_videos_back_arrow, theme)
            ?.apply {
                binding.backButton.setCompoundDrawablesWithIntrinsicBounds(this, null, null, null)
            }

        binding.backButton.setOnClickListener {
            Measurement.setNavigationBehavior(NavigationBehavior.BACK_TO_FRONT)
            writeVideoViewedToStorage(VideoConclusionState.VIDEO_BACK)
            onBackPressed()
        }

        binding.appBarLayout.bringToFront()

        val verticalVideosParcel = VerticalVideosParcel(intent)
        val videos = verticalVideosParcel.getVideos()
        val position = verticalVideosParcel.getPosition()
        val config = verticalVideosParcel.getConfig()
        val sourceScreen = verticalVideosParcel.getSourceScreen()
        tabName = verticalVideosParcel.getTabName()
        videoActivityViewModel.setOffset(verticalVideosParcel.getOffset())
        adapter =
            VerticalVideosRecyclerViewAdapter(this, videosAdViewModel, sourceScreen) { pos ->
                binding.recyclerView.smoothScrollToPosition(pos)
                isAutoScrolled = true
                lastTrackedPosition = pos
            }

        binding.recyclerView.adapter = adapter

        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.recyclerView)

        val nextVideoConfig = ConfigManager.getInstance().config.nextVideoConfig
        player2Manager = PostTvPlayer2Coordinator.getOrCreatePlayer(
            VerticalVideosPlayerName,
            this,
            fetchPercentage = nextVideoConfig.percentageLoad
        )
        player2Manager.apply {
            updatePlayerContainerView(R.layout.vertical_video_player_view)
            updateViewModel(playerViewModel)
            setRespectAudioFocus(true)
            setControllerVisibilityListener {
                when (it) {
                    View.VISIBLE -> binding.appBarLayout.visibility = View.VISIBLE
                    else -> {
                        if (!playerViewModel.errorState) {
                            binding.appBarLayout.visibility = View.GONE
                        }
                    }
                }
            }
        }

        ViewCompat.setAccessibilityDelegate(binding.recyclerView, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.setCollectionInfo(null)
            }
        })

        disablePlayerAutoHideForAccessibility()

        observePlayerState()
        observeWaitCompletionEvent(verticalVideosParcel)
        observeFetchVideo()
        observeAdsModeRefresh(verticalVideosParcel)

        // videosAdViewModel holds the updated list. Get the list from it.
        val items = videosAdViewModel.itemsList
        // Add Videos to the list
        if (videos.size == 1) {
            videoActivityViewModel.fetchVideo()
        }
        val isAdFree = adService.currentAdsMode == AdsModel.Disabled
        val sanitizedSeedVideos = if (isAdFree) {
            videos.map { item ->
                item.copy(adTagUrl = null)
            }
        } else {
            videos
        }
        items.addAll(sanitizedSeedVideos)
        // Inject Ads to the list
        if (config != null && config.enabled && adService.currentAdsMode == AdsModel.Enabled) {
            videosAdViewModel.injectAds(
                config.firstItemDelay,
                config.adItemInterval,
                config.requestsUiTimeoutMillis,
                position,
                sourceScreen
            )
        }

        // Submit List to start playing the videos when
        // 1. ads are disabled
        // 2. no ads are scheduled (for ex: when items list size is small to inserts ads)
        if (config == null || !config.enabled || !videosAdViewModel.isTimerRunning()) {
            submitList(verticalVideosParcel)
        }
    }

    private fun addScrollListener(
        items: MutableList<Any>,
        itemPosition: Int,
    ) {
        binding.recyclerView.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                private var previousViewHolder: VerticalVideosRecyclerViewAdapter.VerticalVideoBaseViewHolder? =
                    null

                var pageName: String? = null
                var arcId: String? = null

                init {
                    // Scroll to the position that was selected in the carousel
                    binding.recyclerView.apply {
                        scrollToPosition(itemPosition)
                        post {
                            previousViewHolder =
                                binding.recyclerView.findViewHolderForAdapterPosition(
                                    itemPosition,
                                )
                                        as? VerticalVideosRecyclerViewAdapter.VerticalVideoViewHolder
                            val video = items[itemPosition] as Video
                            pageName = video.pageName
                            arcId = video.arcId
                            commentCountState.value = video.commentCount
                            videoActivityViewModel.shouldShowTooltip()
                            setRelatedLink(video.relatedLink, video.relatedLinkLastModified)
                            (previousViewHolder as? VerticalVideosRecyclerViewAdapter.VerticalVideoViewHolder)?.playVideo(
                                video,
                            )
                            lastTrackedPosition = itemPosition
                        }
                    }
                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    videoActivityViewModel.shouldShowTooltip()
                    when (newState) {
                        RecyclerView.SCROLL_STATE_IDLE -> {
                            val llm = recyclerView.layoutManager as LinearLayoutManager
                            val pos = llm.findFirstCompletelyVisibleItemPosition()
                            val currentViewHolder =
                                recyclerView.findViewHolderForAdapterPosition(pos)

                            if (previousViewHolder != null && currentViewHolder != previousViewHolder) { // Scrolled to new view holder
                                previousViewHolder?.unbind()
                                if (currentViewHolder is VerticalVideosRecyclerViewAdapter.VerticalVideoViewHolder) {
                                    currentViewHolder.updateControls()
                                    if (currentViewHolder is VerticalVideosRecyclerViewAdapter.VerticalVideosAdCardViewHolder) {
                                        binding.commentButton.visibility = View.GONE
                                        commentCountState.value = null
                                        currentViewHolder.playVideo()
                                    } else {
                                        binding.commentButton.visibility = View.VISIBLE
                                        val video = (items[currentViewHolder.absoluteAdapterPosition] as Video).copy(
                                            pageName = pageName,
                                            avPlayerType = null,
                                            arcId = arcId
                                        )
                                        commentCountState.value = video.commentCount
                                        setRelatedLink(video.relatedLink, video.relatedLinkLastModified)
                                        writeVideoViewedToStorage(VideoConclusionState.VIDEO_NEXT)
                                        currentViewHolder.playVideo(
                                            video,
                                        )
                                        if (!isLoading) {
                                            isLoading = true
                                            videoActivityViewModel.fetchVideo()
                                        }
                                    }

                                    previousViewHolder?.let {
                                        if (it.absoluteAdapterPosition < currentViewHolder.absoluteAdapterPosition) {
                                            if (isAutoScrolled) {
                                                isAutoScrolled = false
                                                player2Manager.onVideoEvent(TrackingType.ON_AUTO_SCROLLED)
                                            } else {
                                                player2Manager.onVideoEvent(TrackingType.ON_PLAY_NEXT)
                                            }
                                        } else {
                                            player2Manager.onVideoEvent(
                                                TrackingType.ON_PLAY_PREVIOUS,
                                            )
                                        }
                                    }
                                }
                            }
                            if (currentViewHolder != null) {
                                previousViewHolder =
                                    currentViewHolder as VerticalVideosRecyclerViewAdapter.VerticalVideoBaseViewHolder
                            }
                            lastTrackedPosition = pos
                        }
                    }
                }
            },
        )
    }

    private fun writeVideoViewedToStorage(videoConclusionState: VideoConclusionState) {
        val playerPosition = player2Manager.getPlaybackPosition()
        val totalSec = player2Manager.getDuration()
        val id = player2Manager.video?.contentId
        userHistoryViewModel.writeVideoViewedEvent(
            id,
            VIDEOS_SECTION,
            null,
            playerPosition,
            totalSec,
            videoConclusionState
        )
    }

    private fun setRelatedLink(relatedLink: String?, lastModified: String?) {
        binding.relatedLinkButton.apply {
            isVisible = relatedLink != null
            relatedLink?.let {
                binding.relatedLinkButton.setOnClickListener {
                    val intent = ArticlesParcel.builder()
                        .setArticleSingleUrl(articleUrl = relatedLink, lastModified = lastModified)
                        .setIsFromRelatedArticle(true)
                        .buildIntent(this@VerticalVideosActivity)
                    this@VerticalVideosActivity.startActivity(intent)
                }
            }
        }
    }

    private fun observeFetchVideo() {
        videoActivityViewModel.fetchNextVideo.observe(this) {
            val config = ConfigManager.getInstance().config.videosConfig
            val video = WatchVideoMapper.getWpVideoItemList(
                it.items,
                adService.currentAdsMode == AdsModel.Disabled,
                config.mobileMaxBitRate,
                config.tabletMaxBitRate
            )
            loadMore(video)
        }
    }

    private fun loadMore(videos: List<Video>) {
        val isAdFree = adService.currentAdsMode == AdsModel.Disabled

        if (isAdFree) {
            videosAdViewModel.itemsList.removeAll { it is VerticalVideoAdItem || it is WatchVideoAdItem }
        }

        val filteredItems = if (isAdFree) {
            videos.map { it.copy(adTagUrl = null) }
        } else {
            videos
        }

        videosAdViewModel.itemsList.addAll(filteredItems)
        adapter.submitList(videosAdViewModel.itemsList.toList())
        isLoading = false
    }

    private fun observePlayerState() {
        player2Manager.playbackState.observe(this) { state ->
            when (state) {
                is PlaybackState.Buffering, PlaybackState.Ready -> {
                    playerViewModel.errorState = false
                    hideError()
                }

                is PlaybackState.Error -> {
                    playerViewModel.errorState = true
                    showError()
                }

                is PlaybackState.Ended -> {
                    //Scroll to next video
                    val recyclerView = binding.recyclerView
                    val llm = recyclerView.layoutManager as LinearLayoutManager
                    val pos = llm.findFirstVisibleItemPosition()
                    recyclerView.smoothScrollToPosition(pos + 1)
                    isAutoScrolled = true
                    writeVideoViewedToStorage(VideoConclusionState.AUTO_COMPLETE)
                }

                is PlaybackState.FetchDurationReached -> {
                    //Fetch new video
                    videoActivityViewModel.durationFetchNextVideo(
                        state.id
                    )
                }

                else -> {
                    // no op
                }
            }
        }
    }

    /**
     * Observer for the WaitCompletion Event.
     * Event is fired once after UI timeout is fired or once all ads are returned.
     * Handling only the first event whichever occurs first and ignoring the rest of the events.
     * For example, o 1 of 2 are loaded within timeout, then _timerCompletion gets fired and rest of the
     * events are ignored. If all ads are loaded within timeout, then _itemsListReadyEvent gets fired
     * and the rest of the events are ignored here.
     */
    private fun observeWaitCompletionEvent(verticalVideosParcel: VerticalVideosParcel) {
        videosAdViewModel.waitCompletion.observe(this) { completion ->
            Logger.d(
                tag,
                "observeWaitCompletionEvent(), completion=$completion, timerRunning=${videosAdViewModel.isTimerRunning()}",
            )
            if (completion) {
                if (videosAdViewModel.isTimerRunning()) {
                    videosAdViewModel.stopAdTimeoutTimer()
                }
                videosAdViewModel.insertSuccessfulAdsAndUpdateFinalAdPositions()
                submitList(verticalVideosParcel)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupCommentButton() {
        binding.commentButton.setContent {
            AndroidClassicTheme {

                val showTooltip = videoActivityViewModel.tooltipState.collectAsStateWithLifecycle()

                binding.tooltipHelp.apply {
                    if (showTooltip.value) {
                        visibility = View.VISIBLE
                        setOnTouchListener { _, _ ->
                            videoActivityViewModel.shouldShowTooltip()
                            false
                        }
                    } else {
                        setOnTouchListener { _, _ -> false }
                        visibility = View.GONE
                    }
                }

                VerticalVideoCommentButton(
                    onClick = {
                        videoActivityViewModel.shouldShowTooltip()
                        openComments()
                    },
                    onTooltipClosed = {
                        videoActivityViewModel.shouldShowTooltip()
                    },
                    commentCount = commentCountState.value,
                    showTooltip = showTooltip.value
                )
            }
        }
    }

    private fun observeAdsModeRefresh(verticalVideosParcel: VerticalVideosParcel) {
        videoActivityViewModel.refreshVideosForAdsModeEvent.observe(this) {
            refreshVideosForAdsModeChange(verticalVideosParcel)
        }
    }

    private fun refreshVideosForAdsModeChange(verticalVideosParcel: VerticalVideosParcel) {
        val config = verticalVideosParcel.getConfig()
        val sourceScreen = verticalVideosParcel.getSourceScreen()
        val position = verticalVideosParcel.getPosition()

        if (videosAdViewModel.isTimerRunning()) {
            videosAdViewModel.stopAdTimeoutTimer()
        }

        videosAdViewModel.itemsList.clear()
        adapter.submitList(videosAdViewModel.itemsList)

        videoActivityViewModel.setOffset(0)
        isLoading = false

        if (config != null && config.enabled && adService.currentAdsMode == AdsModel.Enabled) {
            videosAdViewModel.injectAds(
                config.firstItemDelay,
                config.adItemInterval,
                config.requestsUiTimeoutMillis,
                position,
                sourceScreen
            )
        }

        videoActivityViewModel.fetchVideo()
    }

    private fun submitList(verticalVideosParcel: VerticalVideosParcel) {
        val position = verticalVideosParcel.getPosition()
        with(videosAdViewModel.itemsList) {
            val pos = videosAdViewModel.getItemPositionAfterAddingAds(this, position)
            adapter.submitList(this)
            addScrollListener(this, pos)
            binding.progressSpinner.alpha = 0f
        }
    }


    /**
     * Pauses media playback and opens the comments bottom sheet for the current video.
     * Playback automatically resumes when the comments sheet is dismissed.
     */
    private fun openComments() {
        val currentVideo = player2Manager.video
        player2Manager.pauseMedia()
        CommentBottomSheetFragment.showComments(
            fragmentManager = supportFragmentManager,
            storyID = "",
            storyUrl = currentVideo?.shareUrl,
            storyTitle = "",
            commentSource = "video"
        )
        resumeOnCommentDismiss()
    }

    /**
     * Observes the [CommentBottomSheetFragment] lifecycle and resumes media playback
     * once the fragment is destroyed (i.e., the comments bottom sheet is dismissed).
     */
    private fun resumeOnCommentDismiss() {
        supportFragmentManager.executePendingTransactions()
        val fragment = supportFragmentManager.findFragmentByTag(CommentBottomSheetFragment.TAG) ?: return
        fragment.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_DESTROY) {
                    player2Manager.resumeMedia()
                    source.lifecycle.removeObserver(this)
                }
            }
        })
    }

    private fun showError() {
        errorSnackbar =
            SnackbarFactory.verticalVideoError(
                !ReachabilityUtil.isConnected(this.applicationContext),
                binding.root.findViewById(R.id.error_snackbar_container),
                this.applicationContext,
            )
        binding.errorBackground.visibility = View.VISIBLE
        binding.errorBackground.bringToFront()
        binding.appBarLayout.bringToFront()
        errorSnackbar?.show()
    }

    private fun hideError() {
        errorSnackbar?.dismiss()
        binding.errorBackground.visibility = View.INVISIBLE
    }

    /**
     * Hides the system bar at the top of the screen and the nav bar at the bottom of the screen
     * Per the docs: for SDKs < 30, [WindowInsetsControllerCompat] "aims to behave as close as possible to the original implementation,"
     * but support is shaky on APIs below 21, hence the else cases that use the deprecated system UI visibility flags.
     */
    private fun hideSystemBars() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                WindowInsetsControllerCompat(window, binding.root).let { controller ->
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                window.decorView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_IMMERSIVE // This flag keeps bars hidden when swiping, but is not supported below API 19
                                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_FULLSCREEN
                        )
            }

            else -> {
                window.decorView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_LOW_PROFILE // Rather than hiding the status bar, this flag "dims" it making it less prominent
                                or View.SYSTEM_UI_FLAG_VISIBLE
                        )
            }
        }
    }

    override fun shareVideo(
        headline: String?,
        shareUrl: String?,
    ) {
        Share
            .Builder()
            .shareUrl(shareUrl)
            .headline(headline)
            .fromPush(false)
            .isVerticalVideoShare(true)
            .build()
            .shareItem(this)
    }

    override fun startPIP(mVideo: Video?) {
        TODO("Not yet implemented")
    }

    override fun startPIP(
        video: Video?,
        playerName: String?,
    ) {
    }

    override fun startFullScreen(video: Video?, playerName: String?) {
    }

    override fun isPIPEnabled(): Boolean = false

    /**
     * Full screen vertical video tracking events
     */
    @Deprecated("Deprecated in Java")
    override fun onTrackingEvent(
        type: TrackingType,
        video: Video,
        value: Any?,
    ) {
        onTrackingEvent(VideoTracker2.VideoType.VERTICAL_FULLSCREEN, type, video, value)
    }

    override fun onTrackingEvent(
        videoType: VideoTracker2.VideoType,
        type: TrackingType,
        video: Video,
        value: Any?,
    ) {
        var avPlayerType: String? = null
        val valueMap = value as MutableMap<*, *>
        val adResponse = video.source as? VideoAdResponse
        val videoStartId =
            if (valueMap.containsKey(VideoTracker2.VIDEO_START_ID)) {
                valueMap[VideoTracker2.VIDEO_START_ID] as String
            } else {
                ""
            }
        val aspectRatio =
            if (video.height != 0F) {
                video.width / video.height
            } else {
                video.aspectRatio ?: 0f
            }
        if (video.isLooping) {
            avPlayerType = VideoTracker2.AV_PLAYER_TYPE_FRONT
        } else if (video.autoplay) {
            avPlayerType = VideoTracker2.AV_PLAYER_TYPE_FRONT
        } else if (video.promoIsLooping == true && !video.promoUrl.isNullOrEmpty()) {
            avPlayerType = VideoTracker2.AV_PLAYER_TYPE_FRONT
        }
        val avType = if (aspectRatio < 1) VideoTracker2.AV_TYPE else null
        when (type) {
            TrackingType.ON_PLAY_STARTED -> {
                videoActivityViewModel.tooltipViewed()
                val progressThreshold =
                    if (valueMap.containsKey(VideoTracker2.PROGRESS_THRESHOLD)) {
                        valueMap[VideoTracker2.PROGRESS_THRESHOLD] as Int
                    } else {
                        0
                    }
                val isCarousel =
                    if (valueMap.containsKey(VideoTracker2.CAROUSEL)) {
                        valueMap[VideoTracker2.CAROUSEL] as Boolean
                    } else {
                        false
                    }
                val miscellany =
                    if (valueMap.containsKey(VideoTracker2.SWIPE_DIRECTION)) {
                        valueMap[VideoTracker2.SWIPE_DIRECTION] as String
                    } else {
                        ""
                    }
                val avExp =
                    if (valueMap.containsKey(VideoTracker2.AV_EXP)) {
                        valueMap[VideoTracker2.AV_EXP] as String
                    } else {
                        ""
                    }
                val avName =
                    if (valueMap.containsKey(VideoTracker2.AV_NAME)) {
                        valueMap[VideoTracker2.AV_NAME] as String
                    } else {
                        ""
                    }
                Measurement.playVerticalVideo(
                    video.videoName ?: avName,
                    video.pageName,
                    video.videoSection,
                    video.videoSource,
                    video.videoCategory,
                    video.contentId,
                    progressThreshold,
                    isCarousel,
                    miscellany, // carousel swipe direction
                    adResponse?.gamCreativeId,
                    adResponse?.gamLineItemId,
                    video.arcId,
                    avExp,
                    videoStartId,
                    if (isCarousel) avPlayerType else null,
                    tabName
                )
            }

            TrackingType.AD_PLAY_STARTED -> {
                runOnUiThread { binding.commentButton.visibility = View.GONE }
                val progressThreshold =
                    if (valueMap.containsKey(VideoTracker2.PROGRESS_THRESHOLD)) {
                        valueMap[VideoTracker2.PROGRESS_THRESHOLD] as Int
                    } else {
                        0
                    }
                Measurement.trackVideoAdStart(
                    video.videoName,
                    video.pageName,
                    video.videoSection,
                    video.videoSource,
                    video.contentId,
                    video.videoCategory,
                    progressThreshold,
                    adResponse?.gamCreativeId,
                    adResponse?.gamLineItemId,
                    "preroll"
                )
            }

            TrackingType.AD_PLAY_COMPLETED -> {
                runOnUiThread { binding.commentButton.visibility = View.VISIBLE }
                val progressThreshold =
                    if (valueMap.containsKey(VideoTracker2.PROGRESS_THRESHOLD)) {
                        valueMap[VideoTracker2.PROGRESS_THRESHOLD] as Int
                    } else {
                        0
                    }
                Measurement.trackVideoAdComplete(
                    video.videoName,
                    video.pageName,
                    video.videoSection,
                    video.videoSource,
                    video.videoCategory,
                    video.contentId,
                    progressThreshold,
                    adResponse?.gamCreativeId,
                    adResponse?.gamLineItemId,
                    "preroll"
                )
            }

            TrackingType.VIDEO_PERCENTAGE_WATCHED -> {
                val progressThreshold =
                    if (valueMap.containsKey(VideoTracker2.PROGRESS_THRESHOLD)) {
                        valueMap[VideoTracker2.PROGRESS_THRESHOLD] as Int
                    } else {
                        0
                    }
                val percentageWatched =
                    if (valueMap.containsKey(VideoTracker2.TRACKING_VALUE)) {
                        valueMap[VideoTracker2.TRACKING_VALUE] as Int
                    } else {
                        0
                    }
                val avExp =
                    if (valueMap.containsKey(VideoTracker2.AV_EXP)) {
                        valueMap[VideoTracker2.AV_EXP] as String
                    } else {
                        ""
                    }
                val avName =
                    if (valueMap.containsKey(VideoTracker2.AV_NAME)) {
                        valueMap[VideoTracker2.AV_NAME] as String
                    } else {
                        ""
                    }
                val isCarousel =
                    if (valueMap.containsKey(VideoTracker2.CAROUSEL)) {
                        valueMap[VideoTracker2.CAROUSEL] as Boolean
                    } else {
                        false
                    }
                Measurement.trackCurrentVerticalVideoPercentage(
                    video.videoName ?: avName,
                    video.pageName,
                    video.videoSection,
                    video.videoSource,
                    video.videoCategory,
                    video.contentId,
                    progressThreshold,
                    percentageWatched,
                    adResponse?.gamCreativeId,
                    adResponse?.gamLineItemId,
                    video.arcId,
                    avExp,
                    if (isCarousel) avPlayerType else null,
                    tabName,
                    videoStartId
                )
            }

            TrackingType.ON_PLAY_COMPLETED -> {
                val progressThreshold =
                    if (valueMap.containsKey(VideoTracker2.PROGRESS_THRESHOLD)) {
                        valueMap[VideoTracker2.PROGRESS_THRESHOLD] as Int
                    } else {
                        0
                    }
                val avExp =
                    if (valueMap.containsKey(VideoTracker2.AV_EXP)) {
                        valueMap[VideoTracker2.AV_EXP] as String
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
                val isCarousel =
                    if (valueMap.containsKey(VideoTracker2.CAROUSEL)) {
                        valueMap[VideoTracker2.CAROUSEL] as Boolean
                    } else {
                        false
                    }
                Measurement.stopVerticalVideo(
                    video.videoName ?: avName,
                    video.pageName,
                    video.videoSection,
                    video.videoSource,
                    video.videoCategory,
                    video.contentId,
                    progressThreshold,
                    adResponse?.gamCreativeId,
                    adResponse?.gamLineItemId,
                    video.arcId,
                    avExp,
                    videoStartId,
                    if (isCarousel) avPlayerType else null,
                    tabName
                )
            }

            TrackingType.ON_MUTE, TrackingType.ON_CAPTION_TOGGLE, TrackingType.ON_SHARE,
            TrackingType.ON_PAUSE, TrackingType.ON_PLAY_NEXT, TrackingType.ON_PLAY_PREVIOUS,
                -> {
                val progressThreshold =
                    if (valueMap.containsKey(VideoTracker2.PROGRESS_THRESHOLD)) {
                        valueMap[VideoTracker2.PROGRESS_THRESHOLD] as Int
                    } else {
                        0
                    }
                val trackingValue =
                    if (valueMap.containsKey(VideoTracker2.TRACKING_VALUE)) {
                        valueMap[VideoTracker2.TRACKING_VALUE] as String
                    } else {
                        ""
                    }
                val avExp =
                    if (valueMap.containsKey(VideoTracker2.AV_EXP)) {
                        valueMap[VideoTracker2.AV_EXP] as String
                    } else {
                        ""
                    }
                val avName =
                    if (valueMap.containsKey(VideoTracker2.AV_NAME)) {
                        valueMap[VideoTracker2.AV_NAME] as String
                    } else {
                        ""
                    }
                val isCarousel =
                    if (valueMap.containsKey(VideoTracker2.CAROUSEL)) {
                        valueMap[VideoTracker2.CAROUSEL] as Boolean
                    } else {
                        false
                    }
                Measurement.trackVerticalVideoInteraction(
                    video.videoName ?: avName,
                    video.pageName,
                    video.videoSection,
                    video.videoSource,
                    video.videoCategory,
                    video.contentId,
                    progressThreshold,
                    trackingValue, // miscellany
                    adResponse?.gamCreativeId,
                    adResponse?.gamLineItemId,
                    video.arcId,
                    avExp,
                    if (isCarousel) avPlayerType else null,
                    tabName
                )
            }

            TrackingType.VIDEO_PROGRESS -> {
                val progressThreshold =
                    if (valueMap.containsKey(VideoTracker2.PROGRESS_THRESHOLD)) {
                        valueMap[VideoTracker2.PROGRESS_THRESHOLD] as Int
                    } else {
                        0
                    }
                val avExp =
                    if (valueMap.containsKey(VideoTracker2.AV_EXP)) {
                        valueMap[VideoTracker2.AV_EXP] as String
                    } else {
                        ""
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
                val isCarousel =
                    if (valueMap.containsKey(VideoTracker2.CAROUSEL)) {
                        valueMap[VideoTracker2.CAROUSEL] as Boolean
                    } else {
                        false
                    }
                Measurement.trackVideoProgress(
                    video.videoName ?: avName,
                    video.pageName,
                    video.videoSection,
                    video.videoSource,
                    video.videoCategory,
                    video.arcId,
                    video.contentId,
                    progressThreshold,
                    adResponse?.gamCreativeId,
                    adResponse?.gamLineItemId,
                    avExp,
                    if (isCarousel) avPlayerType else null,
                    engagedTime,
                    videoStartId,
                    true,
                    tabName,
                    avType
                )
            }

            else -> {
                // no op
            }
        }
    }

    override fun onPause() {
        super.onPause()
        writeVideoViewedToStorage(VideoConclusionState.VIDEO_END)
    }

    override fun onResume() {
        super.onResume()
        disablePlayerAutoHideForAccessibility()
    }

    override fun openWeb(url: String?) {
        TODO("Not yet implemented")
    }

    override fun logVideoError(eventLogBuilder: EventLog.Builder?) {
        // log non n/w errors
        val isNetworkError = !isConnectedOrConnecting(applicationContext)
        if (eventLogBuilder != null && !isNetworkError) {
            RemoteLog.e(applicationContext, eventLogBuilder.build())
        }
    }

    private fun isTalkBackEnabled(): Boolean {
        val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return accessibilityManager.isEnabled && accessibilityManager.isTouchExplorationEnabled
    }

    private fun disablePlayerAutoHideForAccessibility() {
        val isTalkBackEnabled = isTalkBackEnabled()
        player2Manager.updatePlayerAutoHide(enableAutoHide = !isTalkBackEnabled)
    }

    companion object {
        const val VIDEOS_SECTION = "videos"
    }
}
