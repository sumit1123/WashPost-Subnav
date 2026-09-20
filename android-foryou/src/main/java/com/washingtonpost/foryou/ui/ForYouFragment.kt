package com.washingtonpost.foryou.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.wapo.flagship.features.grid.viewmodel.EllipsisHelperViewModel
import com.wapo.flagship.features.grid.Tracking
import com.wapo.flagship.features.grid.viewmodel.EllipsisHelperAction
import com.wapo.flagship.features.sections.BaseSectionFragment
import com.wapo.flagship.features.sections.viewmodels.sectionsribbon.SectionsRibbonViewModel
import com.washingtonpost.foryou.ForYouActivity
import com.washingtonpost.foryou.R
import com.washingtonpost.foryou.repo.ForYouFeedRepositoryImpl
import com.washingtonpost.foryou.viewmodel.ForYouViewModel
import com.washingtonpost.userhistory.ForYouViewedAction
import com.washingtonpost.userhistory.models.RecommendationsHelperItem
import com.wpds.theme.AndroidClassicTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.wapo.android.domain.repository.LoadRenderMetrics
import com.wapo.android.domain.repository.LoadRenderMetricsEvent
import com.wapo.android.remotelog.logger.EventTimerLog
import com.wapo.flagship.features.grid.GridActivity
import com.wapo.flagship.features.posttv.PostTvPlayer2Coordinator
import com.wapo.flagship.features.posttv.PostTvPlayer2Manager
import com.wapo.flagship.features.sections.SectionActivity
import com.wapo.flagship.features.sections.viewmodels.sectionsribbon.SectionsRibbonEvents
import com.washingtonpost.foryou.data.ForYouContentType
import com.washingtonpost.userhistory.viewmodel.UserHistoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ForYouFragment : BaseSectionFragment() {

    @Inject
    lateinit var loadRenderMetrics: LoadRenderMetrics

    private val forYouViewModel: ForYouViewModel by viewModels()
    private val userHistoryViewModel: UserHistoryViewModel by viewModels()
    private val sectionsRibbonViewModel: SectionsRibbonViewModel by activityViewModels()
    private val ellipsisHelperViewModel: EllipsisHelperViewModel by activityViewModels()
    private var defaultForYouLauched = false

    private val _scrollToTopTrigger = MutableStateFlow(0)
    val scrollToTopTrigger: StateFlow<Int> get() = _scrollToTopTrigger

    var player2Manager: PostTvPlayer2Manager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadRenderMetrics.startLoadRenderMetrics(LoadRenderMetricsEvent.ForYouRenderEvent)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        observeLaunchSection()
        val activity = requireActivity()

        return ComposeView(requireContext()).apply {
            setContent {
                AndroidClassicTheme {

                    val uiState by forYouViewModel.uiState.collectAsState()

                    LaunchedEffect(uiState) {
                        if (uiState is ForYouUiState.Feed || uiState is ForYouUiState.Error) {
                            loadRenderMetrics.stopLoadRenderMetrics(
                                LoadRenderMetricsEvent.ForYouRenderEvent,
                                mapOf(
                                    EventTimerLog.IS_CURRENTLY_VIEWED_SECTION_FIELD to
                                        (((activity as? SectionActivity)?.pager?.currentFragment === this@ForYouFragment).toString())
                                )
                            )
                        }
                    }

                    val items =
                        if (uiState is ForYouUiState.Feed) (uiState as ForYouUiState.Feed).items else emptyList()

                    player2Manager = PostTvPlayer2Coordinator.getOrCreatePlayer(sectionDisplayName, activity)
                    player2Manager?.apply {
                        mute()
                        val containerResId = (activity as SectionActivity).player2ContainerResId
                        updatePlayerContainerView(containerResId)
                        hideController()
                        setRespectAudioFocus(false)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colorResource(id = R.color.foryou_fragment_bg))
                    ) {

                        val gridEnvironment = (activity as GridActivity).getGridEnvironment()

                        ForYouScreen(
                            items = items,
                            forYouViewModel = forYouViewModel,
                            userHistoryViewModel = userHistoryViewModel,
                            onItemClicked = { position, item ->
                                (activity as? ForYouActivity)?.onItemClicked(
                                    position,
                                    item,
                                    items,
                                    defaultForYouLauched
                                )
                                defaultForYouLauched = false

                            },
                            onEllipsisClicked = { position, item ->
                                if (item.contentType == ForYouContentType.VIDEO.type) {
                                    ellipsisHelperViewModel.handleEllipsisClick(item.toEllipsisVideoActionItem())
                                } else {
                                    ellipsisHelperViewModel.handleEllipsisClick(item.toEllipsisActionItem())
                                }
                                ellipsisHelperViewModel.choiceClickEvent.observe(viewLifecycleOwner) {
                                    val action = when (it) {
                                        is EllipsisHelperAction.ActionSaveStory -> {
                                            ForYouViewedAction.SAVED_STORY
                                        }

                                        is EllipsisHelperAction.ActionRemoveSavedStory -> {
                                            ForYouViewedAction.REMOVED_SAVED_STORY
                                        }

                                        is EllipsisHelperAction.ActionGift -> {
                                            ForYouViewedAction.GIFTED
                                        }

                                        is EllipsisHelperAction.ActionAddToPlayList -> {
                                            ForYouViewedAction.ADDED_PLAYLIST
                                        }

                                        is EllipsisHelperAction.ActionRemoveFromPlaylist -> {
                                            ForYouViewedAction.REMOVED_FROM_PLAYLIST
                                        }

                                        is EllipsisHelperAction.ActionRead -> {
                                            // This shouldn't fire with the current for you ellipsis menu setup, but added this branch for future proofing
                                            ForYouViewedAction.CLICKED
                                        }

                                        is EllipsisHelperAction.ActionShare -> {
                                            // This shouldn't fire with the current for you ellipsis menu setup, but added this branch for future proofing
                                            ForYouViewedAction.SHARED
                                        }
                                    }
                                    val uiState = forYouViewModel.uiState.value
                                    if (uiState is ForYouUiState.Feed) {
                                        userHistoryViewModel.captureForYouViewAction(
                                            action = action,
                                            recommendationsItem = RecommendationsHelperItem(
                                                item.articleId,
                                                item.recReason,
                                                uiState.requestId,
                                                uiState.recipeId,
                                                uiState.testId,
                                                item.contentType
                                            ),
                                            adapterPosition = position,
                                            surface = ForYouFeedRepositoryImpl.SURFACE_FEED
                                        )
                                    }
                                }
                            },
                            onAudioClicked = { item, updateLoading ->
                                (activity as? ForYouActivity)?.playForYouAudio(
                                    item,
                                    isActionAudio = false,
                                    isAudioCarousel = false,
                                    feed = null,
                                    isFlexAudio = false,
                                    isActionButton = true,
                                    onLoadingChange = updateLoading
                                )
                                updateLoading(false)
                            },
                            onCommentClicked = {
                                (activity as? ForYouActivity)?.onCommentClicked(it)
                            },
                            onSummaryClicked = {
                                (activity as? ForYouActivity)?.onSummaryIconClicked(it)
                            },
                            scrollToTopTrigger = scrollToTopTrigger,
                            sectionTitle = sectionDisplayName,
                            player2Manager = player2Manager,
                            gridEnvironment = gridEnvironment
                        )
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                forYouViewModel.loadMoreRecs.collect {
                    (activity as ForYouActivity).onLoadMore(it)
                    userHistoryViewModel.updateFYSessionId()
                }
            }
        }

    }

    private fun observeLaunchSection() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sectionsRibbonViewModel.sectionsRibbonEvent.collect { event ->
                    when(event) {
                        is SectionsRibbonEvents.OpenSectionOnLaunchEvent -> {
                            if (event.open) {
                                defaultForYouLauched = true
                                sectionsRibbonViewModel.sendTrackingData(tracking)
                            } else {
                                defaultForYouLauched = false
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    override fun onPageSelected() {
        super.onPageSelected()
        forYouViewModel.setIsTabActive(true)
        forYouViewModel.attachPlayer(requireActivity(), sectionDisplayName)
    }

    override fun onPageUnselected() {
        super.onPageUnselected()
        forYouViewModel.setIsTabActive(false)
        forYouViewModel.releasePlayer()
    }

    override fun getSectionDisplayName(): String {
        return arguments?.getString(ARG_DISPLAY_NAME) ?: FOR_YOU_DISPLAY_NAME
    }

    override fun getAdKey(): String {
        return arguments?.getString(ARG_BUNDLE_NAME) ?: ""
    }

    override fun scrollToTop() {
        _scrollToTopTrigger.value += 1
    }

    override fun smoothScrollToTop() {
    }

    override fun getTracking(): Tracking {
        return sectionDisplayName.run {
            Tracking(
                pageName = "front - $bundleName",
                platform = "",
                site = "",
                pageType = "",
                section = "For You",
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
                pageTitle = this,
                pagePath = FOR_YOU_PATH
            )
        }
    }

    override fun getBundleName(): String {
        return arguments?.getString(ARG_BUNDLE_NAME) ?: FOR_YOU_BUNDLE_NAME
    }

    fun create(bundleName: String?, displayName: String?): ForYouFragment {
        val arg = arguments ?: Bundle()
        arg.putString(ARG_BUNDLE_NAME, bundleName)
        arg.putString(ARG_DISPLAY_NAME, displayName)
        arguments = arg
        return this
    }

    companion object {
        const val ARG_BUNDLE_NAME = "ARG_BUNDLE_NAME"
        const val ARG_DISPLAY_NAME = "ARG_DISPLAY_NAME"
        const val FOR_YOU_DISPLAY_NAME = "For You"
        const val FOR_YOU_BUNDLE_NAME = "for-you"
        const val FOR_YOU_PATH = "/for-you/"
        const val FOR_YOU_SECTION_NAME_LOWERCASE = "for you"
    }

}
