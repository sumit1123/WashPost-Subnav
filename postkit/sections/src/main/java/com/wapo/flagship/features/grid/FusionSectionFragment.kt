package com.wapo.flagship.features.grid

import SharedScrollViewModel
import android.annotation.SuppressLint
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearSmoothScroller
import com.wapo.adsinf.models.AdsModel
import androidx.recyclerview.widget.RecyclerView
import com.wapo.adsinf.policy.AdService
import com.wapo.android.commons.util.toDateLong
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.audio.viewmodels.AudioMediaActivityViewModel
import com.wapo.flagship.features.grid.events.ActionButtonEvent
import com.wapo.flagship.features.grid.model.CardSegmentType
import com.wapo.flagship.features.grid.model.CarouselAudioItem
import com.wapo.flagship.features.grid.model.CarouselAudioPlaylist
import com.wapo.flagship.features.grid.model.Grid
import com.wapo.flagship.features.grid.viewmodel.ActionsHelperViewModel
import com.wapo.flagship.features.grid.viewmodel.EllipsisHelperViewModel
import com.wapo.flagship.features.grid.viewmodel.sectionsHabitTiles.SectionsHabitTilesViewModel
import com.wapo.flagship.features.inlineoffer.model.SectionInlineOfferViewModel
import com.wapo.flagship.features.lowdatamodelbanner.viewmodel.LowDataBannerViewModel
import com.wapo.flagship.features.newsprint.NewsprintHelper
import com.wapo.flagship.features.newsprint.NewsprintHelper.NEWSPRINT_TOP_CARD_ITID
import com.wapo.flagship.features.newsprint.NewsprintState
import com.wapo.flagship.features.newsprint.NewsprintViewModel
import com.wapo.flagship.features.nightmode.NightModeManager
import com.wapo.flagship.features.nightmode.NightModeProvider
import com.wapo.flagship.features.personalizedpodcasts.model.PersoPodTrackingInfo
import com.wapo.flagship.features.personalizedpodcasts.viewmodel.PersonalizedPodcastViewModel
import com.wapo.flagship.features.sections.BaseSectionFragment
import com.wapo.flagship.features.sections.ConnectivityActivity
import com.wapo.flagship.features.sections.SectionActivity
import com.wapo.flagship.features.sections.tracking.SectionTrackEvent
import com.wapo.flagship.features.sections.tracking.SectionTrackerFactory
import com.wapo.flagship.features.sections.tracking.SectionsTracker
import com.wapo.flagship.features.sections.utils.AnimationHelper
import com.wapo.flagship.features.sections.viewmodels.SectionNavigation
import com.wapo.flagship.features.sections.viewmodels.SectionTrackingViewModel
import com.wapo.flagship.features.splash.SplashViewModel
import com.wapo.flagship.features.sections.viewmodels.SectionVideoActivityViewModel
import com.wapo.flagship.features.subscribebanner.state.BannerEvent
import com.wapo.flagship.features.subscribebanner.viewmodel.GlobalBannerViewModel
import com.wapo.flagship.features.utils.PersonalizedPodcastHelper
import com.wapo.flagship.features.utils.PersonalizedPodcastHelper.PersonalizedPodcastItemType.ONBOARDING
import com.wapo.flagship.features.utils.PersonalizedPodcastHelper.PersonalizedPodcastItemType.PLACEHOLDER
import com.wapo.flagship.features.utils.PersonalizedPodcastHelper.PersonalizedPodcastItemType.PODCAST
import com.wapo.view.habittiles.PersonalizedPodcast
import com.washingtonpost.android.androidlive.cache.AndroidLiveCache
import com.washingtonpost.android.recirculation.carousel.viewmodels.CarouselAudioMediaActivityViewModel
import com.washingtonpost.android.sections.R
import com.washingtonpost.android.sections.databinding.FragmentFusionSectionBinding
import com.washingtonpost.android.volley.toolbox.ImageLoaderProvider
import com.washingtonpost.userhistory.viewmodel.UserHistoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import rx.Subscription
import rx.subjects.BehaviorSubject
import javax.inject.Inject

@AndroidEntryPoint
open class FusionSectionFragment : BaseSectionFragment(), MarginUpdatable {
    private var _binding: FragmentFusionSectionBinding? = null
    private val binding get() = _binding!!

    private var grid: Grid? = null

    //JTid is a time based identifier for section page view that will be send in ad requests as well.
    private lateinit var pageViewModel: PageViewModel
    private lateinit var gridEnvironment: GridEnvironment

    private val globalBannerViewModel: GlobalBannerViewModel by activityViewModels()

    private val sectionInlineOfferViewModel: SectionInlineOfferViewModel by activityViewModels()

    private val lowDataBannerViewModel: LowDataBannerViewModel by activityViewModels()

    private val splashViewModel: SplashViewModel by activityViewModels()

    private val ellipsisHelperViewModel: EllipsisHelperViewModel by activityViewModels()

    private val actionsHelperViewModel: ActionsHelperViewModel by activityViewModels()
    private val audioViewModel: AudioMediaActivityViewModel by activityViewModels()
    private val personalizedPodcastViewModel: PersonalizedPodcastViewModel by activityViewModels()
    private val sectionsHabitTilesViewModel: SectionsHabitTilesViewModel by activityViewModels()

    private val sectionTrackingViewModel: SectionTrackingViewModel by activityViewModels()

    private val newsprintViewModel: NewsprintViewModel by activityViewModels()

    private val userHistoryViewModel: UserHistoryViewModel by activityViewModels()

    private val videoActivityViewModel: SectionVideoActivityViewModel by activityViewModels()
    private val popupBannerCounter = BehaviorSubject.create<Int>()

    private var subscribeButtonSubscription: Subscription? = null

    private val breakingNewsInflater = BreakingNewsInflater()

    private var pageManagerSubscription: Subscription? = null

    private var lastReportedScrollThreshold = 0

    private val sharedScrollViewModel: SharedScrollViewModel by activityViewModels()

    private val carouselAudioMediaActivityViewModel: CarouselAudioMediaActivityViewModel by activityViewModels()

    @Inject lateinit var adService: AdService

    fun create(bundleName: String?, displayName: String?): FusionSectionFragment {
        val arg = arguments ?: Bundle()
        arg.putString(ARG_BUNDLE_NAME, bundleName)
        arg.putString(ARG_DISPLAY_NAME, displayName)
        arguments = arg
        return this
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val subscriberStatusProvider = {
            val gridEnv = (activity as GridActivity).getGridEnvironment()
            gridEnv.isLoggedInUser()
        }
        pageViewModel = ViewModelProvider(
            this,
            PageViewModelFactory(
                getNightModeManager(),
                subscriberStatusProvider,
                { (activity as GridActivity).getGridEnvironment().getPageConfig() }
            )
        )[PageViewModel::class.java]
    }

    private fun getNightModeManager(): NightModeManager {
        return (activity as NightModeProvider).nightModeManager
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFusionSectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.gridView.setImageLoader(getImageLoader())
        binding.gridView.setShowGrid(false)
        gridEnvironment = (activity as GridActivity).getGridEnvironment()
        binding.gridView.setEnvironment(gridEnvironment)
        binding.gridView.adapter?.newsprintViewModel = newsprintViewModel

        binding.gridView.adapter?.sectionsHabitTilesRequestId = sectionsHabitTilesViewModel.getRequestId()
        binding.gridView.adapter?.sectionsHabitTilesTestGroup = sectionsHabitTilesViewModel.getTestGroup()

        binding.gridView.adapter?.userHistoryViewModel = userHistoryViewModel
        binding.gridView.adapter?.videoActivityViewModel = videoActivityViewModel
        binding.gridView.adapter?.onStoryViewClicked = { story, pos ->
            grid?.let {
                gridEnvironment.openArticle(
                    story,
                    it,
                    sectionDisplayName,
                    getArgKey(),
                    pos
                )
            }
        }
        binding.gridView.adapter?.onRelatedLinkClicked = { story, relatedLink ->
            grid?.let {
                gridEnvironment.openRelatedLink(
                    relatedLink,
                    story,
                    it,
                    sectionDisplayName,
                    getArgKey()
                )
            }
        }
        binding.gridView.adapter?.onLabelClicked = { label, itId ->
            gridEnvironment.openLabel(label, itId)
        }
        binding.gridView.adapter?.onLiveBlogClicked = { link, itId ->
            grid?.let {
                gridEnvironment.openLiveBlog(
                    link,
                    it,
                    sectionDisplayName,
                    getArgKey(),
                    itId
                )
            }
        }
        binding.gridView.adapter?.onMediaClicked = { story, link ->
            grid?.let {
                gridEnvironment.openMedia(
                    story,
                    link,
                    it,
                    sectionDisplayName,
                    getArgKey()
                )
            }
        }
        binding.gridView.adapter?.onCarouselCardClicked = { links, pos ->
            grid?.let { gridEnvironment.openCarouselCard(links, sectionDisplayName, pos) }
        }
        binding.gridView.adapter?.onCarouselImmersionCardClicked = { links, pos ->
            grid?.let { gridEnvironment.openCarouselImmersionCard(links, sectionDisplayName, pos) }
        }
        binding.gridView.adapter?.onCarouselSevenLiveCardClicked = { links, pos ->
            grid?.let { gridEnvironment.openCarouselSevenLiveCard(links, sectionDisplayName, pos) }
        }
        binding.gridView.adapter?.onCarouselRecipeCardClicked = { links, pos ->
            grid?.let { gridEnvironment.openCarouselRecipeCard(links, sectionDisplayName, pos) }
        }
        binding.gridView.adapter?.onStackCardClicked = { links, pos ->
            grid?.let { gridEnvironment.openStackCard(links, sectionDisplayName, pos) }
        }
        binding.gridView.adapter?.onCarouselVideoCardClicked = { postTvVideos, pos ->
            grid?.let {
                gridEnvironment.openCarouselVideoCard(
                    postTvVideos, it,
                    sectionDisplayName, pos
                )
            }
        }
        binding.gridView.adapter?.onBookmarkClick = { actionItem, view, isStatusChecked ->
            gridEnvironment.bookMarkClicked(actionItem, view, isStatusChecked)
        }
        binding.gridView.adapter?.onHabitTileClicked = { tile ->
            context?.let {
                requireActivity().apply {
                    if (PersonalizedPodcastHelper.isPersonalizedPodcastItem(tile?.persoPodcastMetadata?.itemType)) {
                        gridEnvironment.trackHabitTileClicked(tile?.tileLink)
                        when (tile?.persoPodcastMetadata?.itemType) {
                            PLACEHOLDER -> {
                                personalizedPodcastViewModel.setIsCurrentPlaceholder(true)
                                personalizedPodcastViewModel.generatePodcast(null)
                                createPersoPodTrackingInfo(tile.persoPodcastMetadata,
                                    getString(R.string.hp_tile_homepage), PLACEHOLDER)
                                sectionTrackingViewModel.trackEvent(SectionTrackEvent.AudioInteraction(
                                    "perso-$PLACEHOLDER: ${tile.persoPodcastMetadata?.kicker}-${tile.persoPodcastMetadata?.createdAt}",
                                    getString(R.string.hp_tile_homepage),
                                    getString(R.string.perso_podcast_generate), tile.persoPodcastMetadata?.itemType)
                                )
                            }
                            ONBOARDING -> {
                                personalizedPodcastViewModel.generatePodcast(null)
                                createPersoPodTrackingInfo(tile.persoPodcastMetadata, getString(R.string.hp_tile_homepage), "intro")
                            }
                            PODCAST -> {
                                createPersoPodTrackingInfo(tile.persoPodcastMetadata, getString(R.string.hp_tile_homepage), PODCAST)
                            }
                        }

                        lifecycleScope.launch {
                            val persoPodTrackingInfo = personalizedPodcastViewModel.uiState.value.persoPodTrackingInfo
                            gridEnvironment.playAudioIfHasAccess(
                                AudioMediaConfig(
                                    mediaId = tile?.persoPodcastMetadata?.id,
                                    title = tile?.persoPodcastMetadata?.title,
                                    streamUrl = tile?.persoPodcastMetadata?.audioFilePath ?: "null",
                                    contentUrl = null,
                                    imageUrl = tile?.persoPodcastMetadata?.image,
                                    primaryLabel = tile?.persoPodcastMetadata?.kicker,
                                    sectionName = tile?.tileCategory,
                                    audioType = tile?.persoPodcastMetadata?.itemType,
                                    date = toDateLong(tile?.persoPodcastMetadata?.createdAt),
                                    audioTracking = personalizedPodcastViewModel.createPodcastTracker(tile?.persoPodcastMetadata?.audioDuration?.toLong() ?: 0L, persoPodTrackingInfo)
                                )
                            )
                        }
                    } else {
                        personalizedPodcastViewModel.setPersoPodTrackingInfo(PersoPodTrackingInfo())
                        gridEnvironment.openHabitTileLink(
                            tile?.tileLink,
                            sectionDisplayName,
                            grid?.tracking?.section,
                            grid?.tracking?.subsection
                        )
                    }
                }
            }
        }
        binding.gridView.adapter?.onNewsprintButtonClicked = {
            when (newsprintViewModel.topCardState.value) {
                NewsprintState.COMPLETED_NEWSPRINT -> {
                    newsprintViewModel.readerType.value?.let { readerType ->
                        val pos = readerType.ordinal
                        val anchor = binding.gridView.adapter?.items?.find {
                            it.cardIndex == pos && it.cardSegmentType == CardSegmentType.TOP_CARD
                        }
                        if (anchor != null) {
                            anchor.adapterPosition?.let {
                                sectionTrackingViewModel.trackEvent(
                                    SectionTrackEvent.OnpageTap(grid?.tracking),
                                    pos,
                                    SectionTrackingViewModel.NEWSPRINT_TOP_CARD
                                )
                                binding.gridView.scrollToPosition(it)
                            }
                        } else { // non-cardified
                            sectionTrackingViewModel.trackEvent(
                                SectionTrackEvent.OnpageTap(grid?.tracking),
                                pos,
                                SectionTrackingViewModel.NEWSPRINT_TOP_CARD
                            )
                            val position = if (pos == 0) 1 else pos * 3
                            binding.gridView.scrollToPosition(position)
                        }
                    }
                }

                else -> {
                    gridEnvironment.setNavigationBehaviorInDefaultMap(NEWSPRINT_TOP_CARD_ITID)
                    gridEnvironment.openLink(
                        NewsprintHelper.getNewsprintUrlWithItId(
                            NEWSPRINT_TOP_CARD_ITID
                        )
                    )
                }
            }
        }
        binding.gridView.adapter?.onNewsprintSpanClicked = {
            gridEnvironment.setNavigationBehaviorInDefaultMap(NEWSPRINT_TOP_CARD_ITID)
            gridEnvironment.openLink(NewsprintHelper.getNewsprintUrlWithItId(NEWSPRINT_TOP_CARD_ITID))
        }
        binding.gridView.adapter?.onStoryViewBlurbsLinkClicked = { story, link ->
            grid?.let {
                gridEnvironment.openArticleUrl(
                    link,
                    story,
                    it,
                    sectionDisplayName,
                    getArgKey(),
                    story.adapterPosition
                )
            }
        }
        binding.gridView.adapter?.onCarouselCommentsCardClicked = { link, pos ->
            link?.let {
                gridEnvironment.openCarouselCommentsCard(it, sectionDisplayName, pos)
            }
        }
        binding.gridView.adapter?.onCarouselAudioArticleCardClicked = { carouselAudio, pos ->
            grid?.let {
                val persoPodTrackingInfo = personalizedPodcastViewModel.uiState.value.persoPodTrackingInfo
                when (carouselAudio.items[pos].carouselItemType) {
                    PLACEHOLDER -> {
                        personalizedPodcastViewModel.setIsCurrentPlaceholder(true)
                        personalizedPodcastViewModel.generatePodcast(null)
                        createPersoPodTrackingInfo(carouselAudio.items[pos],
                            getString(R.string.audio_carousel), PLACEHOLDER)
                        gridEnvironment.playAudioCarouselAudioArticleItem(
                            carouselAudio,
                            pos,
                            sectionDisplayName,
                            personalizedPodcastViewModel.createPodcastTracker(carouselAudio.items[pos].audio?.duration ?: 0L, persoPodTrackingInfo)
                        )
                        sectionTrackingViewModel.trackEvent(SectionTrackEvent.AudioInteraction(
                            "perso-$PLACEHOLDER: ${carouselAudio.items[pos].audio?.displayLabel}-${carouselAudio.items[pos].audio?.displayDate}",
                            getString(R.string.audio_carousel), getString(R.string.perso_podcast_generate), carouselAudio.items[pos].carouselItemType)
                        )
                    }
                    ONBOARDING -> {
                        personalizedPodcastViewModel.generatePodcast(null)
                        createPersoPodTrackingInfo(carouselAudio.items[pos], getString(R.string.audio_carousel), "intro")
                        gridEnvironment.playAudioCarouselAudioArticleItem(
                            carouselAudio,
                            pos,
                            sectionDisplayName,
                            personalizedPodcastViewModel.createPodcastTracker(carouselAudio.items[pos].audio?.duration ?: 0L, persoPodTrackingInfo)
                        )
                    }
                    PODCAST -> {
                        createPersoPodTrackingInfo(carouselAudio.items[pos], getString(R.string.audio_carousel), PODCAST)
                        gridEnvironment.playAudioCarouselAudioArticleItem(
                            carouselAudio,
                            pos,
                            sectionDisplayName,
                            personalizedPodcastViewModel.createPodcastTracker(carouselAudio.items[pos].audio?.duration ?: 0L, persoPodTrackingInfo)
                        )
                    }
                    else -> {
                        personalizedPodcastViewModel.setPersoPodTrackingInfo(PersoPodTrackingInfo())
                        gridEnvironment.playAudioCarouselAudioArticleItem(
                            carouselAudio,
                            pos,
                            sectionDisplayName
                        )
                    }
                }
            }
        }
        binding.gridView.adapter?.onCarouselAudioPlaylistArticleCardClicked =
            { carouselAudioPlaylist, playListItems, pos ->
                grid?.let {
                    gridEnvironment.playAudioCarouselAudioPlaylistArticleItem(
                        carouselAudioPlaylist,
                        playListItems,
                        pos,
                        sectionDisplayName
                    )
                }
            }
        binding.gridView.adapter?.onCarouselExternalCardClicked = { links, pos ->
            grid?.let { gridEnvironment.openCarouselExternalCard(links, sectionDisplayName, pos) }
        }
        binding.gridView.adapter?.onImpressionEvent = {
            globalBannerViewModel.setBannerEvent(BannerEvent.ImpressionEvent(it))
        }
        binding.gridView.adapter?.generateAudioMediaConfig = { carouselAudioItem ->
            gridEnvironment.generateAudioMediaConfig(carouselAudioItem, sectionDisplayName)
        }
        binding.gridView.adapter?.onMessageBannerClicked = {
            globalBannerViewModel.setBannerEvent(it)
        }
        binding.gridView.adapter?.onEllipsisClick = {
            ellipsisHelperViewModel.handleEllipsisClick(it)
        }
        binding.gridView.adapter?.onActionButtonClicked = {
            // Handle clicks here directly
            handleActionClick(it)
            // Post livedata to be handled in MainActivity.
            actionsHelperViewModel.handleActionClick(it)
        }
        binding.gridView.adapter?.onSectionThresholdScrolled = ::onSectionThresholdScrolled
        binding.gridView.adapter?.onLowDataModeDisable = {
            lowDataBannerViewModel.updateLowDataBanner(false)
            val sectionTracker: SectionsTracker? = SectionTrackerFactory.get(context)
            sectionTracker?.trackLowDataModeTurnedOff(grid?.tracking?.pageName)
        }
        binding.retry.setOnClickListener {
            showLoading()
            onRefresh()
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            onRefresh()
        }
        globalBannerViewModel.globalBannerState.observe(viewLifecycleOwner) {
            binding.gridView.adapter?.updateGlobalBannerItem(it)
        }
        lowDataBannerViewModel.lowDataBannerState.observe(viewLifecycleOwner) {
            binding.gridView.adapter?.updateLowDataBannerItem(it)
            audioViewModel.setIsLowDataMode(it.isLowDataBannerEnable)
            showLoading()
            registerNewPageSubscription()
        }

        observeSectionInlineOfferData()
        observeBackToFront()
        observeSectionsHabitTilesRefreshEvent()
        collectNewsprintData()
        observePlaylistScrollTrigger()
        observeAdsMode()
    }

    private var lastAdsMode: AdsModel? = null
    private fun observeAdsMode() {
        var isFirstEmission = true
        adService.adsMode.onEach { mode ->
            if (isFirstEmission) {
                isFirstEmission = false
                lastAdsMode = mode
                return@onEach
            }
            // If we are transitioning from Disabled to Enabled, we MUST refresh to restore physically removed items.
            if (lastAdsMode is AdsModel.Disabled &&
                mode is AdsModel.Enabled && grid != null) {
                onRefresh()
            } else if (lastAdsMode != mode) {
                binding.gridView.refreshAds()
            }
            lastAdsMode = mode
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun createPersoPodTrackingInfo(personalizedPodcast: PersonalizedPodcast?, touchpoint: String?, podcastType: String?) {
        setPersoPodTrackingInfo(PersoPodTrackingInfo(date = personalizedPodcast?.createdAt, touchpoint = touchpoint,
            podcastType = podcastType, id = personalizedPodcast?.id, title = personalizedPodcast?.title, transcriptUrl = personalizedPodcast?.transcript, sources = personalizedPodcast?.articlesUsed)
        )
    }

    private fun createPersoPodTrackingInfo(audioItem: CarouselAudioItem, touchpoint: String?, podcastType: String?) {
        setPersoPodTrackingInfo(PersoPodTrackingInfo(date = audioItem.audio?.displayDate, touchpoint = touchpoint,
            podcastType = podcastType, id = audioItem.audio?.mediaId, title = audioItem.headline, transcriptUrl = audioItem.audio?.transcriptUrl, sources = audioItem.audio?.sources)
        )
    }

    private fun setPersoPodTrackingInfo(persoPodTrackingInfo: PersoPodTrackingInfo) {
        personalizedPodcastViewModel.setPersoPodTrackingInfo(persoPodTrackingInfo)
    }

    private fun registerNewPageSubscription() {
        pageManagerSubscription?.unsubscribe()
        pageManagerSubscription = pageManagerObs
            .take(1)
            .subscribe(
                {
                    pageViewModel.onPageManagerReady(
                        it,
                        getArgKey(),
                        gridEnvironment.isLowDataModeEnable()
                    )
                },
                { showError() }
            )
    }

    private fun observeBackToFront() {
        ellipsisHelperViewModel.backToFrontEvent.observe(viewLifecycleOwner) {
            binding.gridView.adapter?.backToFront = true
        }
    }

    private fun observeSectionInlineOfferData() {
        sectionInlineOfferViewModel.sectionMessageData.observe(viewLifecycleOwner) { offerData ->
            if (offerData == null) {
                binding.gridView.adapter?.removeInlineOfferItem()
            }
        }
    }

    private fun observeSectionsHabitTilesRefreshEvent() {
        viewLifecycleOwner.lifecycleScope.launch {
            sectionsHabitTilesViewModel.uiState.collect { _ ->
                binding.gridView.adapter?.refreshHabitTiles(binding.gridView)
            }
        }
    }

    private fun observePlaylistScrollTrigger() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedScrollViewModel.shouldScrollToPlaylist.collectLatest { shouldScroll ->
                    if (shouldScroll) {
                        checkScrollToAudioPlaylist()
                    }
                }
            }
        }
    }

    private fun observePageData() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                pageViewModel.pageData.asFlow().collect {
                    onScreenStateUpdated(it)
                }
            }
        }
    }

    private fun observeNightModeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                pageViewModel.nightModeData.asFlow().collect {
                    onNightModeChanged(it)
                }
            }
        }
    }

    private fun observeBreakingNewsData() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                pageViewModel.breakingNewsData.asFlow().collect {
                    showBreakingNews(it)
                }
            }
        }
    }

    private fun observeNowPlayingAudioItem() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                carouselAudioMediaActivityViewModel.nowPlayingAudioItem.collect {
                    binding.gridView.onNowPlayingAudioItem(it)
                }
            }
        }
    }

    private fun collectNewsprintData() {
        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                gridEnvironment.getNewsprintEngagedStatus(),
                gridEnvironment.getNewsprintHasViewed(),
                gridEnvironment.getNewsprintReaderType()
            ) { engagedStatus, hasViewed, readerType ->
                Triple(engagedStatus, hasViewed, readerType)
            }.collect {
                newsprintViewModel.updateData(it.first, it.second, it.third)
            }
        }
    }

    private fun handleActionClick(event: ActionButtonEvent) {
        when (event) {
            is ActionButtonEvent.Listen -> {
                gridEnvironment.playAudioIfHasAccess(event.audioMediaConfig)
            }

            is ActionButtonEvent.Menu -> {
                ellipsisHelperViewModel.handleEllipsisClick(event.ellipsisActionItem)
            }

            else -> {
                //no-op
            }
        }
    }

    private fun getImageLoader() = (activity as ImageLoaderProvider).imageLoader

    override fun onStart() {
        super.onStart()
        val activity = activity
        if (activity is SectionActivity && !activity.isFinishing) {
            activity.onStartSFFragment(getArgKey())
        }
    }

    override fun onPause() {
        super.onPause()
        subscribeButtonSubscription?.unsubscribe()
        subscribeButtonSubscription = null
        pageManagerSubscription?.unsubscribe()
        pageManagerSubscription = null
        pageViewModel.onPageStop(getArgKey())
    }

    override fun onDestroyView() {
        binding.gridView.releaseResources()
        binding.swipeRefreshLayout.let {
            it.isRefreshing = false
            it.setOnRefreshListener(null)
            it.clearAnimation()
        }
        grid = null
        _binding = null
        super.onDestroyView()
    }

    private fun onRefresh() {
        AndroidLiveCache.clearCache()
        val activity = activity
        if (activity is SectionActivity && !activity.isFinishing) {
            activity.onRefreshSFPage(getArgKey())
        }
        this.grid = null
        sectionTrackingViewModel.setNavigating(SectionNavigation.REFRESH)
        pageViewModel.refreshPage(isLowDataModeEnable = lowDataBannerViewModel.lowDataBannerState.value?.isLowDataBannerEnable == true)
        gridEnvironment.onRefresh(getArgKey(), sectionDisplayName)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        observePageData()
        observeNightModeData()
        observeBreakingNewsData()
        observeNowPlayingAudioItem()
    }

    override fun onResume() {
        super.onResume()
        registerNewPageSubscription()
        binding.gridView.refreshAds()
        binding.gridView.refreshHabitTiles()
    }

    private fun showBreakingNews(items: List<BarEntity>) {
        binding.breakingNewsContainer.removeAllViews()
        items
            .filter { gridEnvironment.shouldShowBreakingNewsBar(it) }
            .forEach {
                val view = makeBreakingNewsVew(it)
                if (view != null) {
                    binding.breakingNewsContainer.addView(view)
                }
            }
        popupBannerCounter.onNext(binding.breakingNewsContainer.childCount)
    }

    private fun makeBreakingNewsVew(item: BarEntity): View? {
        val link = item.link?.url
        val onCloseListener = { view: View ->
            gridEnvironment.onBreakingNewsBarClosed(item)
            binding.breakingNewsContainer.removeView(view)
            popupBannerCounter.onNext(binding.breakingNewsContainer.childCount)
        }
        return when (item) {
            is BreakingNewsBarEntity -> {
                val onClickListener = View.OnClickListener {
                    if (link != null) {
                        gridEnvironment.openBreakingNewsBar(link)
                    }
                }
                breakingNewsInflater.createBreakingNewsBar(
                    item,
                    requireContext(),
                    binding.breakingNewsContainer,
                    onClickListener,
                    onCloseListener
                )
            }

            is LiveVideoBarEntity -> {
                val onClickListener = View.OnClickListener {
                    if (link != null) {
                        gridEnvironment.openLiveVideoBar(link)
                    }
                }

                breakingNewsInflater.createLiveVideoBar(
                    item,
                    requireContext(),
                    binding.breakingNewsContainer,
                    onClickListener,
                    onCloseListener,
                    getImageLoader()
                )
            }

            else -> null
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun onNightModeChanged(status: NightModeStatus) {
        when (status) {
            NightModeStatus.ON -> {
                binding.gridView.setNightModeEnabled(true)
            }

            NightModeStatus.OFF -> {
                binding.gridView.setNightModeEnabled(false)
            }
        }
        binding.gridView.adapter?.notifyDataSetChanged()
    }

    private fun onScreenStateUpdated(pageState: PageState) {
        when (pageState) {
            is PageState.Content -> {
                activity?.let { trackerActivity ->
                    SectionTrackerFactory.get(trackerActivity).onSectionLoadSuccess(
                        trackerActivity,
                        sectionDisplayName, true
                    )
                }
                if (this.grid == null || pageState.refreshPage) {
                    showPage(pageState.page)
                } else if (binding.gridView.visibility == View.GONE) {
                    makePageVisible()
                }
                onPageReady()
            }

            is PageState.Loading -> {
                activity?.let { trackerActivity ->
                    SectionTrackerFactory.get(trackerActivity).onSectionLoadStart(
                        trackerActivity,
                        sectionDisplayName
                    )
                }
                if (pageState.showProgress) {
                    showLoading()
                }
                if (pageState.dropPage) {
                    this.grid = null
                }
            }

            is PageState.Error -> {
                activity?.let { trackerActivity ->
                    SectionTrackerFactory.get(trackerActivity).onSectionLoadError(
                        trackerActivity,
                        sectionDisplayName,
                        pageViewModel.lastReceivedPage != null,
                        pageState.error,
                        true
                    )
                }
                this.grid = null
                showError()
                onPageReady()
            }
            else -> {}
        }
        activity?.invalidateOptionsMenu()
    }

    open fun onPageReady() {}

    private fun showPage(page: Grid) {
        this.grid = page
        binding.gridView.setGrid(page)
        binding.gridView.setDisplayContext(getDisplayContext())
        binding.gridView.setSectionDisplayName(sectionDisplayName)
        AnimationHelper.fadeOut(binding.statusContainer, null)
        binding.anchor.visibility = View.GONE
        stopAsyncLoadingAnim()
        AnimationHelper.fadeIn(binding.gridView, null)
        dismissSplashScreen()

        binding.swipeRefreshLayout.isRefreshing = false

        if (page.tracking != null) {
            page.tracking.contentType = "front"
            if (sectionTrackingViewModel.shouldTrackSection(bundleName)) {
                sectionTrackingViewModel.trackEvent(
                    SectionTrackEvent.PageView(
                        sectionDisplayName,
                        page.tracking
                    ), "FusionSectionFragment"
                )
            }
        }

        PostTvWarmUp().createPostTvPlayersForSection(page, requireActivity())
    }

    private fun makePageVisible() {
        binding.statusContainer.visibility = View.GONE
        binding.anchor.visibility = View.GONE
        stopAsyncLoadingAnim()
        AnimationHelper.fadeIn(binding.gridView, null)
        dismissSplashScreen()
    }

    private fun stopAsyncLoadingAnim() {
        binding.asyncAnimImageView.clearAnimation()
        binding.swipeRefreshLayout.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.section_front_background
            )
        )
        binding.asyncAnimImageView.visibility = View.GONE
    }

    fun dismissSplashScreen() {
        lifecycleScope.launch {
            splashViewModel.dismissSplashScreen()
        }
    }

    private fun showLoading() {
        binding.swipeRefreshLayout.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.sf_loading_spinner
            )
        )
        binding.asyncAnimImageView.visibility = View.VISIBLE
        val loadingAnimation = AnimationUtils.loadAnimation(context, R.anim.horizontal_anim)
        binding.asyncAnimImageView.startAnimation(loadingAnimation)
        binding.gridView.visibility = View.GONE
    }

    private fun showError() {
        stopAsyncLoadingAnim()
        binding.swipeRefreshLayout.isRefreshing = false
        checkConnectivity()
        val ssb = SpannableStringBuilder(getString(R.string.articles_unable_to_load_a_content_msg))
        binding.anchor.visibility = View.VISIBLE
        binding.statusContainer.visibility = View.VISIBLE
        binding.blogFrontStatusCurtain.text = ssb
        binding.gridView.visibility = View.GONE
        dismissSplashScreen()
    }

    private fun checkConnectivity() {
        if (activity is ConnectivityActivity) {
            (activity as ConnectivityActivity).checkConnectivity()
        }
    }

    override fun getAdKey(): String? {
        return null
    }

    override fun getSectionDisplayName(): String {
        return arguments?.getString(ARG_DISPLAY_NAME) ?: ""
    }

    private fun getArgKey(): String {
        return arguments?.getString(ARG_BUNDLE_NAME) ?: ""
    }

    private fun getDisplayContext(): String {
        return arguments?.getString(ARG_DISPLAY_CONTEXT) ?: "front"
    }

    override fun scrollToTop() {
        binding.gridView.scrollToPosition(0)
    }

    override fun smoothScrollToTop() {
        val scrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                // On Animation complete requestLayout to update RV state
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    recyclerView.requestLayout();
                    recyclerView.removeOnScrollListener(this)
                }
            }
        }

        with(binding.gridView) {
            addOnScrollListener(scrollListener)
            smoothScrollToPosition(0)
        }
    }

    private fun checkScrollToAudioPlaylist() {
        val adapter = binding.gridView.adapter ?: return
        val index = adapter.items.indexOfFirst { it is CarouselAudioPlaylist }

        if (index != -1) {
            // subtract 1 to get header of audio playlist instead
            scrollToPlaylist(maxOf(index - 1, 0))
            sharedScrollViewModel.clearScrollRequest()
        } else {
            adapter.onCarouselAudioPlaylistAdded = { addedIndex ->
                scrollToPlaylist(maxOf(addedIndex - 1, 0))
                sharedScrollViewModel.clearScrollRequest()
                adapter.onCarouselAudioPlaylistAdded = null
            }
        }
    }

    private fun scrollToPlaylist(index: Int) {
        val layoutManager = binding.gridView.layoutManager ?: return

        val scroller = object : LinearSmoothScroller(binding.gridView.context) {
            override fun getVerticalSnapPreference(): Int = SNAP_TO_START
        }.apply {
            targetPosition = index
        }

        layoutManager.scrollToPosition(index)
        binding.gridView.post {
            layoutManager.startSmoothScroll(scroller)
        }
    }

    override fun getTracking(): Tracking? {
        return grid?.tracking
    }

    override fun getBundleName(): String {
        return arguments?.getString(ARG_BUNDLE_NAME) ?: ""
    }

    override fun onPageSelected() {
        super.onPageSelected()
        binding.gridView.onPageSelected()
    }

    companion object {
        const val ARG_BUNDLE_NAME = "ARG_BUNDLE_NAME"
        const val ARG_DISPLAY_NAME = "ARG_DISPLAY_NAME"
        const val ARG_DISPLAY_CONTEXT = "ARG_DISPLAY_CONTEXT"
    }

    private fun onSectionThresholdScrolled(percentage: Int, totalFeatureItems: Int) {
        if (lastReportedScrollThreshold != percentage) {
            lastReportedScrollThreshold = percentage
            val sectionTracker: SectionsTracker? = SectionTrackerFactory.get(activity)
            sectionTracker?.trackSectionPercentage(
                percentage,
                totalFeatureItems,
                sectionDisplayName,
                getArgKey(),
                _binding?.gridView?.adapter?.grid
            )
        }
    }

    override fun updateBottomMargin(bottomMargin: Int) {
        _binding?.gridView?.updateBottomMargin(bottomMargin)
    }
}
