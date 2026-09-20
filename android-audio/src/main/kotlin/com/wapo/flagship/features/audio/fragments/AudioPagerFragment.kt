/* Copyright (c) 2018 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.audio.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.wapo.flagship.features.audio.AudioActivity
import com.wapo.flagship.features.audio.R
import com.wapo.flagship.features.audio.models.AudioPlaybackState
import com.wapo.flagship.features.audio.models.UserClickEvent
import com.wapo.flagship.features.audio.viewmodels.ArticleData
import com.wapo.flagship.features.audio.viewmodels.AudioArticleEvent
import com.wapo.flagship.features.audio.viewmodels.AudioFeatureArticleStateViewModel
import com.wapo.flagship.features.audio.viewmodels.AudioMediaActivityViewModel
import com.wapo.flagship.features.audio.viewmodels.PlaybackSpeedViewModel
import com.wapo.flagship.features.audio.viewmodels.PlaylistActivityViewModel
import com.wapo.flagship.features.feedback.viewmodel.FeedbackViewModel
import com.wapo.flagship.features.personalizedpodcasts.events.UserEvent
import com.wapo.flagship.features.personalizedpodcasts.fragments.PodcastCreationFragment
import com.wapo.flagship.features.personalizedpodcasts.model.PersonalizedPodcastFeedbackProviderImpl
import com.wapo.flagship.features.personalizedpodcasts.viewmodel.PersonalizedPodcastViewModel
import com.wapo.flagship.features.tts.domain.OnTtsEvent
import com.wapo.flagship.features.tts.viewmodel.TtsAudioPlayerViewModel
import com.wapo.flagship.features.utils.PersonalizedPodcastHelper
import com.wapo.flagship.features.utils.PersonalizedPodcastHelper.PersonalizedPodcastItemType.PODCAST
import com.wapo.fragment.BaseBottomSheetDialogFragment
import com.wapo.view.habittiles.ArticleSource
import com.wpds.theme.AndroidClassicTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AudioPagerFragment : BaseBottomSheetDialogFragment() {

    private val audioMediaActivityViewModel: AudioMediaActivityViewModel by activityViewModels()
    private val audioFeatureArticleStateViewModel: AudioFeatureArticleStateViewModel by activityViewModels()
    private val playListViewModel: PlaylistActivityViewModel by activityViewModels()
    private val playbackSpeedViewModel: PlaybackSpeedViewModel by activityViewModels()
    private val personalizedPodcastViewModel: PersonalizedPodcastViewModel by activityViewModels()
    private val feedbackViewModel: FeedbackViewModel by activityViewModels()

    private val ttsAudioPlayerViewModel: TtsAudioPlayerViewModel by activityViewModels()

    private var isNavigatingAwayFromAudioForArticleFlow: Boolean = false
    private var lastKnownPodcastSources: List<ArticleSource> = emptyList()

    private val _isTranscriptVisible = MutableLiveData(false)
    val isTranscriptVisible: LiveData<Boolean> = _isTranscriptVisible

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener {
            val bottomSheet = (it as BottomSheetDialog).findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.isFitToContents = true
                behavior.skipCollapsed = true
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AudioPagerScreen()
            }
        }
    }

    @Composable
    fun AudioPagerScreen() {
        val mediaItems by audioMediaActivityViewModel.mediaItems.observeAsState(emptyList())
        val nowPlayingItem by audioMediaActivityViewModel.nowPlayingAudioItem.observeAsState()
        val isPlaylistVisible by playListViewModel.playListVisibilityState.observeAsState(false)
        val persoUiState by personalizedPodcastViewModel.uiState.collectAsState()
        val generationState = persoUiState.generationState
        val transcript = persoUiState.transcript
        val playbackSpeed by playbackSpeedViewModel.selectedPlaybackSpeed.observeAsState()
        val sharedLockedHeight by playListViewModel.sharedLockedHeight.observeAsState()
        val playbackState by audioMediaActivityViewModel.audioPlaybackState.observeAsState(AudioPlaybackState.None)
        val percentage by audioMediaActivityViewModel.position.observeAsState(0f)
        val adUiState by audioMediaActivityViewModel.adUiState.collectAsStateWithLifecycle(AudioAdUiState())
        val isFeedbackEnabled by audioMediaActivityViewModel.isFeedbackEnabled
            .collectAsStateWithLifecycle(audioMediaActivityViewModel.currentFeedbackEnabled)
        val ttsState by ttsAudioPlayerViewModel.ttsState.collectAsStateWithLifecycle()

        val isTranscriptVisibleState by isTranscriptVisible.observeAsState(false)

        LaunchedEffect(playbackSpeed) {
            playbackSpeed?.let {
                audioMediaActivityViewModel.setPlaybackSpeed(it)
            }
        }

        LaunchedEffect(persoUiState.persoPodTrackingInfo) {
            val firstSources = persoUiState.persoPodTrackingInfo?.first?.sources
            val secondSources = persoUiState.persoPodTrackingInfo?.second?.sources
            val sources = firstSources ?: secondSources
            if (!sources.isNullOrEmpty()) {
                lastKnownPodcastSources = sources.toList()
                globalPodcastSourcesCache = sources.toList()
                personalizedPodcastViewModel.cachePodcastSources(sources.toList())
            }
        }

        val pagerState = rememberPagerState(
            initialPage = nowPlayingItem?.nowPlayingItemIndex?.coerceAtLeast(0) ?: 0,
            pageCount = { mediaItems.size }
        )

        // Sync pager with ViewModel's now playing index
        LaunchedEffect(nowPlayingItem?.nowPlayingItemIndex) {
            val index = nowPlayingItem?.nowPlayingItemIndex ?: -1
            if (index >= 0 && index < mediaItems.size && index != pagerState.currentPage) {
                pagerState.scrollToPage(index)
            }
            // Dismiss transcript when track changes
            setTranscriptVisibility(false)
        }

        // Hide playlist when generating
        val currentItem = mediaItems.getOrNull(pagerState.currentPage)
        val isGenerating = currentItem?.audioType == PersonalizedPodcastHelper.PersonalizedPodcastItemType.PLACEHOLDER
        LaunchedEffect(isGenerating) {
            if (isGenerating) {
                playListViewModel.setPlayListVisibility(false)
            }
        }

        // Sync ViewModel when user swipes manually
        LaunchedEffect(pagerState.currentPage) {
            if (nowPlayingItem?.nowPlayingItemIndex != pagerState.currentPage) {
                audioMediaActivityViewModel.dispatchPageChangeEvent(pagerState.currentPage)
                audioMediaActivityViewModel.playMediaAtIndex(pagerState.currentPage)
            }
        }

        AndroidClassicTheme {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(colorResource(id = R.color.podcast_background))
            ) {
                if (mediaItems.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        userScrollEnabled = !isPlaylistVisible && !isTranscriptVisibleState // Disable swipe when sub-views are open
                    ) { pageIndex ->
                        val item = mediaItems.getOrNull(pageIndex)
                        val isCurrentPage = pageIndex == pagerState.currentPage
                        
                        AudioPlayerScreen(
                            state = AudioPlayerUiState(
                                mediaItemData = item,
                                nowPlayingItem = if (isCurrentPage) nowPlayingItem else null,
                                isPlaylistVisible = if (isCurrentPage) isPlaylistVisible else false,
                                isTranscriptVisible = if (isCurrentPage) isTranscriptVisibleState else false,
                                transcript = if (isCurrentPage) transcript else null,
                                generationState = if (isCurrentPage) generationState else null,
                                playbackSpeedText = playbackSpeed?.text ?: stringResource(R.string.speed_1_0x),
                                upcomingItems = if (isCurrentPage) {
                                    mediaItems.drop(pageIndex + 1)
                                } else emptyList(),
                                lockedHeightPx = sharedLockedHeight,
                                percentage = if (isCurrentPage) { if (ttsState.isSpeaking || ttsState.isPause) ttsState.progress else percentage} else 0f,
                                playbackState = if (isCurrentPage) playbackState else AudioPlaybackState.None,
                                isPersonalizedPodcast = PersonalizedPodcastHelper.isPersonalizedPodcastItem(item?.audioType),
                                adUiState = adUiState,
                                ttsState = ttsState,
                                isFeedbackEnabled = isFeedbackEnabled,
                            ),
                            onEvent = { handleUiEvent(it) },
                            player = if (isCurrentPage) audioMediaActivityViewModel.getPlayer() else null,
                        )
                    }
                }

                if (isPlaylistVisible && nowPlayingItem?.audioPlaybackState == AudioPlaybackState.Buffering) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }

    private fun handleUiEvent(event: AudioPlayerUiEvent) {
        val persoUiState = personalizedPodcastViewModel.uiState.value
        when (event) {
            AudioPlayerUiEvent.PlaylistToggle -> {
                playListViewModel.togglePlayListVisibility()
                playListViewModel._playlistClickTrackEvent.value = true
            }
            AudioPlayerUiEvent.TranscriptToggle -> {
                setTranscriptVisibility(false)
            }
            is AudioPlayerUiEvent.ItemClicked -> {
                val index = audioMediaActivityViewModel.mediaItems.value?.indexOf(event.mediaItemData) ?: -1
                if (index != -1) {
                    audioMediaActivityViewModel.playMediaAtIndex(index)
                }
            }
            is AudioPlayerUiEvent.EllipsisClicked -> {
                val config = audioMediaActivityViewModel.getConfigFromMediaItem(event.mediaItemData)
                if (config != null) {
                    audioMediaActivityViewModel.audioPlayerEllipsisClickEvent.value = config
                }
            }
            AudioPlayerUiEvent.CreateClicked -> {
                personalizedPodcastViewModel.getPodcastConfig()
                PodcastCreationFragment.newInstance().show(childFragmentManager, PodcastCreationFragment.TAG)
            }
            AudioPlayerUiEvent.SpeedClicked -> {
                PlaybackSpeedDialogFragment().show(childFragmentManager, PlaybackSpeedDialogFragment.TAG)
            }
            AudioPlayerUiEvent.AskSamClicked -> {
                if (personalizedPodcastViewModel.wasMainPlayerPlaying()) {
                    personalizedPodcastViewModel.setPausedPodcast(true)
                }
                val pauseTimestamp = audioMediaActivityViewModel.getPosition()
                (activity as? AudioActivity)?.onAskSamEvent(
                    UserEvent.TryToOpenTalkToThePost(
                        pauseTimestamp / 1000f,
                        persoUiState.persoPodTrackingInfo?.first?.transcriptUrl
                            ?: persoUiState.persoPodTrackingInfo?.second?.transcriptUrl ?: ""
                    )
                )
            }
            is AudioPlayerUiEvent.PodcastMenuAction -> {
                handlePodcastMenuEvent(event.event)
            }
            is AudioPlayerUiEvent.HeightMeasured -> {
                playListViewModel.setSharedLockedHeight(event.height)
            }
            AudioPlayerUiEvent.TrackChanged -> {
                // Dismiss transcript when buttons are used
                setTranscriptVisibility(false)
                // Only reset height if we are NOT in a locked sub-view state.
                // If playlist is visible, we want to keep the current locked height
                // even if the track changes, to prevent jumping/expansion.
                if (playListViewModel.playListVisibilityState.value != true) {
                    playListViewModel.setSharedLockedHeight(null)
                }
            }
            is AudioPlayerUiEvent.TitleClicked -> {
                val contentUrl = event.mediaItemData.contentUrl
                val sectionName = event.mediaItemData.sectionName
                contentUrl?.let {
                    markArticleOrPaywallNavigationStart()
                    audioFeatureArticleStateViewModel.articleTitleClick(
                        ArticleData(contentUrl, sectionName)
                    )
                }
            }
            AudioPlayerUiEvent.PauseOrPlay -> {
                val ttsState = ttsAudioPlayerViewModel.ttsState.value
                if (ttsState.isSpeaking) {
                    ttsAudioPlayerViewModel.pauseTts()
                } else if (ttsState.isPause) {
                    ttsAudioPlayerViewModel.resumeTts()
                } else {
                    audioMediaActivityViewModel.pauseOrPlay()
                }
            }

            AudioPlayerUiEvent.SkipAdClicked -> {
                audioMediaActivityViewModel.skipAd()
            }
            else -> {}
        }
    }

    private fun handlePodcastMenuEvent(event: PodcastMenuUIEvent) {
        val persoUiState = personalizedPodcastViewModel.uiState.value
        when (event) {
            PodcastMenuUIEvent.MenuOpened -> {
           if (audioMediaActivityViewModel.nowPlayingAudioItem.value?.audioMediaConfig?.audioType == PODCAST) {
                        personalizedPodcastViewModel.checkAndAdvanceRollThrough()
                        personalizedPodcastViewModel.trackMenuOpen(audioMediaActivityViewModel.nowPlayingAudioItem.value?.mediaItemData?.primaryLabel)
                    }
            }
            PodcastMenuUIEvent.Share -> {
                personalizedPodcastViewModel.sharePodcast(
                    context = requireContext(),
                    podcastId = personalizedPodcastViewModel.getPodcastEpisodeId(),
                    kicker = audioMediaActivityViewModel.nowPlayingAudioItem.value?.mediaItemData?.primaryLabel
                )
            }
            PodcastMenuUIEvent.UpNext -> {
                playListViewModel.togglePlayListVisibility()
            }
            PodcastMenuUIEvent.Sources -> {
                val firstSources = persoUiState.persoPodTrackingInfo?.first?.sources
                val secondSources = persoUiState.persoPodTrackingInfo?.second?.sources
                val sources = firstSources ?: secondSources
                val vmCachedSources = personalizedPodcastViewModel.getCachedPodcastSources()
                val fallbackSources = when {
                    !sources.isNullOrEmpty() -> sources
                    lastKnownPodcastSources.isNotEmpty() -> lastKnownPodcastSources
                    globalPodcastSourcesCache.isNotEmpty() -> globalPodcastSourcesCache
                    else -> vmCachedSources
                }
                if (fallbackSources.isNotEmpty()) {
                    markArticleOrPaywallNavigationStart()
                    (activity as? AudioActivity)?.showSources(fallbackSources)
                }
            }
            PodcastMenuUIEvent.Transcript -> {
                personalizedPodcastViewModel.retrieveTranscript()
                setTranscriptVisibility(true)
            }
            PodcastMenuUIEvent.Feedback -> {
                val provider = PersonalizedPodcastFeedbackProviderImpl(
                    contentId = personalizedPodcastViewModel.getPodcastEpisodeId(),
                    position = audioMediaActivityViewModel.getPosition()
                )
                com.wapo.flagship.features.feedback.ui.FeedbackFragment(provider)
                    .show(parentFragmentManager, "feedback")
            }
        }
    }

    fun setTranscriptVisibility(isVisible: Boolean) {
        _isTranscriptVisible.value = isVisible
        if (!isVisible && playListViewModel.playListVisibilityState.value != true) {
            playListViewModel.setSharedLockedHeight(null)
        }
        setBottomSheetDraggable(shouldBeDraggable())
    }

    private fun shouldBeDraggable(): Boolean {
        val isPlaylistVisible = playListViewModel.playListVisibilityState.value ?: false
        val isTranscriptVisible = isTranscriptVisible.value ?: false
        return !isPlaylistVisible && !isTranscriptVisible
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setBottomSheetDraggable(shouldBeDraggable())
        observeUserClicks()
        observePlayListVisibility()
        observeFeedbackSubmittedEvent()
        observeTtsEvents()
    }

    private fun observeUserClicks() {
        audioFeatureArticleStateViewModel.userClickEvent.observe(viewLifecycleOwner) { clickEvent ->
            if (clickEvent is UserClickEvent.ClickArticle) {
                val section = clickEvent.articleData.section ?: ""
                val openArticle = audioFeatureArticleStateViewModel.getCurrentArticleUrl() ?: ""
                if (openArticle == clickEvent.articleData.contentUrl) {
                    dismiss()
                } else {
                    clickEvent.articleData.contentUrl?.let {
                        markArticleOrPaywallNavigationStart()
                        audioMediaActivityViewModel.dispatchOpenArticleEvent(
                            AudioArticleEvent(
                                it,
                                section,
                                section
                            )
                        )
                        dismiss()
                    }
                }
            }
        }
    }

    private fun observePlayListVisibility() {
        playListViewModel.playListVisibilityState.observe(viewLifecycleOwner) { isVisible ->
            if (!isVisible && isTranscriptVisible.value != true) {
                playListViewModel.setSharedLockedHeight(null)
            }
            setBottomSheetDraggable(shouldBeDraggable())
        }
    }

    private fun observeFeedbackSubmittedEvent() {
        feedbackViewModel.feedbackSubmittedEvent.observe(viewLifecycleOwner) {
            com.wapo.flagship.features.feedback.ui.FeedbackStatusFragment().show(
                requireActivity().supportFragmentManager,
                "feedback_status_fragment",
            )
        }
    }

    private fun observeTtsEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                ttsAudioPlayerViewModel.ttsEvents.collect { event ->
                    when (event) {
                        is OnTtsEvent.OnTtsError -> {
                            Toast.makeText(requireContext(), resources.getString(event.errorId), Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    override fun onStop() {
        if (!shouldSkipPlayerFragmentRefreshOnStop()) {
            (activity as? AudioActivity)?.updatePlayerFragment()
        }
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        isNavigatingAwayFromAudioForArticleFlow = false
    }

    override fun onDismiss(dialog: DialogInterface) {
        personalizedPodcastViewModel.getListOfPodcasts()
        super.onDismiss(dialog)
    }

    private fun markArticleOrPaywallNavigationStart() {
        isNavigatingAwayFromAudioForArticleFlow = true
    }

    private fun shouldSkipPlayerFragmentRefreshOnStop(): Boolean {
        val playbackState = audioMediaActivityViewModel.audioPlaybackState.value
        val isActivePlayback =
            playbackState is AudioPlaybackState.Playing || playbackState == AudioPlaybackState.Buffering
        val shouldSkip = isNavigatingAwayFromAudioForArticleFlow || isActivePlayback
        return shouldSkip
    }

    companion object {
        private var globalPodcastSourcesCache: List<ArticleSource> = emptyList()
        const val FRAGMENT_TAG = "audio_pager_fragment"
        @JvmStatic
        fun newInstance() = AudioPagerFragment()
    }
}
