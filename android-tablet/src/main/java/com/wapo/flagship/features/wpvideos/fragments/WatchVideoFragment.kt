package com.wapo.flagship.features.wpvideos.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.wapo.android.domain.repository.LoadRenderMetrics
import com.wapo.android.domain.repository.LoadRenderMetricsEvent
import com.wapo.flagship.features.grid.GridActivity
import com.wapo.flagship.features.grid.GridEnvironment
import com.wapo.flagship.features.grid.Tracking
import com.wapo.flagship.features.posttv.PostTvPlayer2Coordinator
import com.wapo.flagship.features.posttv.PostTvPlayer2Manager
import com.wapo.flagship.features.posttv.VideoTracker2
import com.wapo.flagship.features.posttv.model.PlaybackState
import com.wapo.flagship.features.sections.BaseSectionFragment
import com.wapo.flagship.features.sections.SectionActivity
import com.wapo.flagship.features.splash.SplashViewModel
import com.wapo.flagship.features.wpvideos.data.WatchVideosApi
import com.wapo.flagship.features.wpvideos.models.WatchVideosEvent
import com.wapo.flagship.features.wpvideos.ui.WatchVideoScreen
import com.wapo.flagship.features.wpvideos.viewmodel.WatchVideosViewModel
import com.wapo.flagship.navigation.viewmodel.sectionnav.SectionNavViewModel
import com.washingtonpost.userhistory.viewmodel.UserHistoryViewModel
import com.wpds.theme.wpdsColors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WatchVideoFragment : BaseSectionFragment() {

    @Inject
    lateinit var loadRenderMetrics: LoadRenderMetrics

    private var watchVideosApi by mutableStateOf<WatchVideosApi?>(null)
    private var player2Manager: PostTvPlayer2Manager? = null
    lateinit var gridEnvironment: GridEnvironment
    private val sectionNavViewModel: SectionNavViewModel by activityViewModels()
    private val _scrollToTopTrigger = MutableStateFlow(0)
    val scrollToTopTrigger: StateFlow<Int> get() = _scrollToTopTrigger

    private val userHistoryViewModel: UserHistoryViewModel by viewModels()
    private val watchVideosViewModel: WatchVideosViewModel by activityViewModels()
    private val splashViewModel: SplashViewModel by activityViewModels()

    private val _activeIndexFlow = MutableStateFlow(0)
    val activeIndexFlow: StateFlow<Int> get() = _activeIndexFlow

    fun create(bundleName: String?, displayName: String?): WatchVideoFragment {
        val arg = arguments ?: Bundle()
        arg.putString(ARG_BUNDLE_NAME, bundleName)
        arg.putString(ARG_DISPLAY_NAME, displayName)
        arguments = arg
        return this
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadRenderMetrics.startLoadRenderMetrics(LoadRenderMetricsEvent.WatchRenderEvent)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        gridEnvironment = (activity as GridActivity).getGridEnvironment()
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = wpdsColors.surface
                    ) {
                        splashViewModel.dismissSplashScreen()
                        WatchVideoScreen(
                            bundleName = getBundleName(),
                            player2Manager = player2Manager,
                            gridEnvironment = gridEnvironment,
                            reload = { loadInitialData() },
                            loadMoreVideos = { offset ->
                                watchVideosViewModel.fetchVideos(offset)
                            },
                            onActiveIndexChanged = { newIndex ->
                                _activeIndexFlow.value = newIndex
                            },
                            activeIndexFlow = activeIndexFlow,
                            sectionNavViewModel = sectionNavViewModel, // <-- pass to composable
                            sectionTitle = sectionDisplayName,
                            scrollToTopTrigger = scrollToTopTrigger,
                            watchVideosViewModel = watchVideosViewModel,
                            userHistoryViewModel = userHistoryViewModel,
                            eventSection = WATCH_VIDEOS_EVENT,
                            onFullScreenView = {
                                enableStopTrack = false
                                startEngagementTrace(TRACK_KEY, "FULL_VIDEO")
                            }
                        )
                    }

                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        enableStopTrack = true
        startEngagementTrace(TRACK_KEY, TAG)
    }

    override fun onPause() {
        super.onPause()
        stopAndTrackEngagementTrace(TRACK_KEY, TAG, false)
        player2Manager?.pauseMedia()
    }

    override fun onStop() {
        super.onStop()
        player2Manager?.resetTrackedEvents(scope = VideoTracker2.TrackScope.WATCH_PAGE)
    }

    override fun onDestroy() {
        if (!enableStopTrack) {
            stopAndTrackEngagementTrace(TRACK_KEY, TAG, true)
        }
        super.onDestroy()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        createAndObservePlayer()
        observeWatchVideosEvent()
        loadInitialData()
    }

    override fun onPageSelected() {
        super.onPageSelected()
        player2Manager?.resumeMedia()
    }

    override fun onPageUnselected() {
        super.onPageUnselected()
        player2Manager?.resetTrackedEvents(scope = VideoTracker2.TrackScope.WATCH_PAGE)
        player2Manager?.pauseMedia()
    }

    private fun observeWatchVideosEvent() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                watchVideosViewModel.watchVideosEvent.collect { event ->
                    when (event) {
                        WatchVideosEvent.WatchVideosReady -> {
                            loadRenderMetrics.stopLoadRenderMetrics(LoadRenderMetricsEvent.WatchRenderEvent)
                        }
                    }
                }
            }
        }
    }

    /**
     * Loads the WP Videos feed data by subscribing to the content manager observable.
     *
     */

    private fun loadInitialData() {
        watchVideosViewModel.clearVideos()
        watchVideosViewModel.fetchVideos(0)
    }

    /**
     * Initializes the `PostTvPlayer2Manager`, configures the player UI, and observes playback state.
     */

    private fun createAndObservePlayer() {
        val activity = requireActivity()
        player2Manager = PostTvPlayer2Coordinator.getOrCreatePlayer("sectionName", activity)
        player2Manager?.apply {
            mute()
            val containerResId = (activity as SectionActivity).player2ContainerResId
            updatePlayerContainerView(containerResId)
            showController()
            setRespectAudioFocus(false)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                player2Manager?.playbackState?.asFlow()?.collect { state ->
                    when (state) {
                        is PlaybackState.Ended -> {
                            val totalItems = watchVideosApi?.items?.size ?: 0
                            val currentIndex = _activeIndexFlow.value
                            if (currentIndex + 1 < totalItems) {
                                _activeIndexFlow.value = currentIndex + 1
                            }
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    override fun getBundleName(): String? = arguments?.getString(ARG_BUNDLE_NAME)
    override fun getSectionDisplayName(): String? = arguments?.getString(ARG_DISPLAY_NAME)
    override fun getAdKey(): String? = null
    override fun scrollToTop() {
        _scrollToTopTrigger.value += 1
    }

    override fun smoothScrollToTop() = Unit

    override fun getTracking(): Tracking? =
        sectionDisplayName?.run {
            Tracking(
                pageName = "front - $bundleName",
                platform = "",
                site = "",
                pageType = "",
                section = "",
                channel = "",
                subsection = "",
                hierarchy = "",
                contentType = "front",
                storyType = "",
                headline = this,
                author = "",
                source = "",
                contentID = "",
                pageNum = "",
                opRanking = "",
                columnName = "",
                blogName = "",
                published = "",
                newsOrCommercial = "",
                commercialNode = "",
                contentCategory = "",
                sectionFront = "",
                trackScrolling = "",
                contentTopics = "",
                pageTitle = "",
                pagePath = "",
            )
        }

    companion object {
        private const val TAG = "WatchVideoFragment"
        const val ARG_BUNDLE_NAME = "ARG_BUNDLE_NAME"
        const val ARG_DISPLAY_NAME = "ARG_DISPLAY_NAME"
        const val WP_VIDEO_BUNDLE_NAME = "/video"
        const val TRACK_KEY = "WatchVideoFragmentTrackKey"
        const val WATCH_VIDEOS_EVENT = "front-watch"
    }
}