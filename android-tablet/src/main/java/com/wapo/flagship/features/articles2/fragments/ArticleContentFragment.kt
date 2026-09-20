package com.wapo.flagship.features.articles2.fragments

import android.animation.Animator
import android.animation.LayoutTransition
import android.animation.ObjectAnimator
import com.wapo.flagship.FlagshipApplication
import android.animation.PropertyValuesHolder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.wapo.android.commons.extensions.toUri
import com.wapo.adsinf.policy.AdService
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.URLParser
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.Utils
import com.wapo.flagship.features.articles.ArticleLinkType
import com.wapo.flagship.features.articles.models.ArticleModel
import com.wapo.flagship.features.articles2.activities.AUDIO_ARTICLE_POSITION
import com.wapo.flagship.features.articles2.activities.Articles2Activity
import com.wapo.flagship.features.articles2.activities.FAILOVER_ORIGINATED
import com.wapo.flagship.features.articles2.activities.PUSH_TOPIC
import com.wapo.flagship.features.articles2.activities.SHOULD_PLAY_AUDIO_ARTICLE
import com.wapo.flagship.features.articles2.activities.toEllipsisActionItem
import com.wapo.flagship.features.articles2.adapters.ArticlesPagerAdapter
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.interfaces.ExternalEventsCoordinator
import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.articles2.models.Author
import com.wapo.flagship.features.articles2.models.Item
import com.wapo.flagship.features.articles2.models.deserialized.*
import com.wapo.flagship.features.articles2.models.deserialized.podcast.Podcast
import com.wapo.flagship.features.articles2.navigation_models.ActionsOnIndividualArticles
import com.wapo.flagship.features.articles2.navigation_models.ShareContent
import com.wapo.flagship.features.articles2.navigation_models.UserBehaviorTrackingModel
import com.wapo.flagship.features.articles2.states.ArticleContentState
import com.wapo.flagship.features.articles2.tracking.ArticleMetricsEvent
import com.wapo.flagship.features.articles2.tracking.FirebaseAnalyticsTrackingEvent
import com.wapo.flagship.features.articles2.tracking.FirebaseAnalyticsTrackingEvent.*
import com.wapo.flagship.features.articles2.tracking.FirebaseTrackingInfo
import com.wapo.flagship.features.articles2.utils.InlineAlertToggleHelper
import com.wapo.flagship.features.articles2.utils.appendTrackingParams
import com.wapo.flagship.features.articles2.utils.getUrlWithoutParameters
import com.wapo.flagship.features.articles2.utils.toTrackingInfo
import com.wapo.flagship.features.articles2.viewmodels.*
import com.wapo.flagship.features.articles2.viewmodels.Articles2ViewModel.Companion.ARTICLE_BASE_URL
import com.wapo.flagship.features.articles3.models.ui.ArticleItemUiModel
import com.wapo.flagship.features.articles3.models.ui.ListUiModel
import com.wapo.flagship.features.articles3.models.ui.SanitizedHtmlUiModel
import com.wapo.flagship.features.articles3.models.ui.CarouselUiModel
import com.wapo.flagship.features.articles3.models.ui.ForYouCarouselItemUiModel
import com.wapo.flagship.features.articles3.models.ui.RecircCarouselItemUiModel
import com.wapo.flagship.features.articles3.views.ArticleContentView
import com.wapo.flagship.features.articles3.views.ProvideWebViewPool
import com.wapo.flagship.features.articles3.views.CarouselUiStyle
import com.wapo.flagship.features.audio.*
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.audio.config2.NowPlayingAudioItem
import com.wapo.flagship.features.audio.models.AudioPlaybackState
import com.wapo.flagship.features.audio.playlist.toAudioMediaConfig
import com.wapo.flagship.features.audio.utils.AudioPreferences
import com.wapo.flagship.features.audio.viewmodels.AudioMediaActivityViewModel
import com.wapo.flagship.features.comments.CommentsViewModel
import com.wapo.flagship.features.conversations.ui.CommentBottomSheetFragment
import com.wapo.flagship.features.gifting.tracking.GiftTrackingDetails
import com.wapo.flagship.features.gifting.viewmodels.GiftCollaborationViewModel
import com.wapo.flagship.features.gifting.views.GiftArticleSenderFragment
import com.wapo.flagship.features.grid.viewmodel.EllipsisHelperViewModel
import com.wapo.flagship.features.articles2.viewmodels.ArticleInlineMessageViewModel
import com.wapo.flagship.features.articles2.views.DisclaimerBottomSheetFragment
import com.wapo.flagship.features.articles3.views.WebEmbedSettings
import com.wapo.flagship.features.lowdatamodelbanner.viewmodel.LowDataBannerViewModel
import com.wapo.flagship.features.settings.AppPreferences
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor
import com.wapo.flagship.features.notification.AlertsSettings
import com.wapo.flagship.features.shared.activities.BaseActivity
import com.wapo.flagship.features.shared.activities.SimpleWebViewActivity
import com.wapo.flagship.features.subscribebanner.viewmodel.GlobalBannerViewModel
import com.wapo.flagship.features.topicfollow.fragments.TopicFollowArticleBottomSheetFragment
import com.wapo.flagship.features.topicfollow.viewmodels.TopicFollowCollaborationViewModel
import com.wapo.flagship.features.video.PostTvWarmUp
import com.wapo.flagship.features.video.viewmodels.VideoActivityViewModel
import com.wapo.flagship.model.ArticleMeta
import com.wapo.flagship.util.Share
import com.wapo.flagship.util.WPUrlAnalyser
import com.wapo.flagship.util.tracking.Events
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.view.web_embeds.EmbedJSInterface
import com.washingtonpost.android.BuildConfig
import com.washingtonpost.android.R
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.databinding.FragmentArticleContentBinding
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.util.PaywallConstants
import com.washingtonpost.android.save.database.model.MetadataModel
import com.washingtonpost.foryou.viewmodel.ForYouActivityViewModel
import com.washingtonpost.foryou.repo.ForYouFeedRepositoryImpl
import com.washingtonpost.userhistory.models.AutoRecircHelperItem
import com.washingtonpost.userhistory.models.UserHistoryArticleItem
import com.washingtonpost.userhistory.models.RecommendationsHelperItem
import com.washingtonpost.userhistory.viewmodel.UserHistoryViewModel
import com.wpds.theme.AndroidClassicTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import rx.Subscription

@AndroidEntryPoint
class ArticleContentFragment :
    Fragment(),
    ArticlesInteractionHelper,
    ExternalEventsCoordinator {
    private var _binding: FragmentArticleContentBinding? = null
    private val binding get() = _binding!!

    private val articles2ViewModel: Articles2ViewModel by viewModels()

    private val inlineTopicFollowViewModel: InlineTopicFollowViewModel by viewModels()

    private val articlesPagerCollaborationViewModel: ArticlesPagerCollaborationViewModel by activityViewModels()

    private val giftCollaborationViewModel: GiftCollaborationViewModel by activityViewModels()

    private val pageViewTimeTrackerViewModel: PageViewTimeTrackerViewModel by activityViewModels()

    private val articleWallHelperViewModel: ArticleWallHelperViewModel by activityViewModels()

    private val forYouActivityViewModel: ForYouActivityViewModel by activityViewModels()

    private val videoActivityViewModel: VideoActivityViewModel by activityViewModels()

    private val topicFollowCollaborationViewModel: TopicFollowCollaborationViewModel by activityViewModels()

    private val ellipsisHelperViewModel: EllipsisHelperViewModel by activityViewModels()

    private val article2ItemsViewModel: Article2ItemsViewModel by viewModels()

    private val userHistoryViewModel: UserHistoryViewModel by viewModels()

    private val lowDataBannerViewModel: LowDataBannerViewModel by activityViewModels()

    private val articleInlineMessageViewModel: ArticleInlineMessageViewModel by activityViewModels()

    private val commentsViewModel: CommentsViewModel by activityViewModels()

    private val audioMediaActivityViewModel: AudioMediaActivityViewModel by activityViewModels()

    private val globalBannerViewModel: GlobalBannerViewModel by activityViewModels()

    private val toolbarActionsLiveData = MutableLiveData<ActionsOnIndividualArticles>()
    private var readingStartTime: Long = 0L

    private var isPushOriginated: Boolean = false
    private var mostReadSubscription: Subscription? = null
    private var pageExpandState: Boolean? = null

    @Inject
    lateinit var adService: AdService

    @Inject
    lateinit var embedJSInterface: EmbedJSInterface

    private val webEmbedSettings by lazy {
        WebEmbedSettings(embedJSInterface = embedJSInterface)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentArticleContentBinding.inflate(inflater, container, false)
        return binding.root
    }

    private var audioArticlePosition: Int? = null
    private var shouldPlayAudioArticle: Boolean = false

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        binding.nativeItem.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
        )

        // Fix for https://arcpublishing.atlassian.net/browse/AWA-9829
        binding.containerView.layoutTransition?.setAnimateParentHierarchy(false)

        val meta = arguments?.getParcelable<ArticleMeta>(ArticlesPagerAdapter.ARTICLE_METADATA_KEY)
        val pushTopic = arguments?.getString(PUSH_TOPIC)
        shouldPlayAudioArticle = arguments?.getBoolean(SHOULD_PLAY_AUDIO_ARTICLE) ?: false
        audioArticlePosition = arguments?.getInt(AUDIO_ARTICLE_POSITION)
        isPushOriginated = pushTopic != null
        if (isPushOriginated) {
            articles2ViewModel.setPushTopic(pushTopic!!)
        }

        val pageViewTrackerUrl = pageViewTimeTrackerViewModel.articleTimeStamp.articleUrl
        val currentArticleUrl =
            articlesPagerCollaborationViewModel.currentPage.value?.articleMeta?.id
        if (pageViewTrackerUrl == currentArticleUrl) {
            pageViewTimeTrackerViewModel.articleTimeStamp.timeStamp?.let {
                articles2ViewModel.setJTid(it)
            }
        }

        val position = arguments?.getInt(ArticlesPagerAdapter.ARTICLE_POSITION) ?: -1
        setupLufAnimation()

        meta?.let { articleMeta ->
            articles2ViewModel.startLoadingArticle(articleMeta)
            bindToolbarActions(articleMeta.id)
            observeContentStateEvents(articleMeta, savedInstanceState, position)
            observePageChangeEvents(articleMeta)
            observeTopicFollowStateUpdate()
            observeAvailableVoicesRequestUpdates()
            observeVideoItems(articleMeta)
            observeSourceAnnotations()
            observeForYouRecircUpdates()
            observeMostReadRecircUpdates()

            binding.nativeItem.setContent {
                AndroidClassicTheme {
                    val contentState by articles2ViewModel.articleContentState.observeAsState(
                        ArticleContentState.Loading,
                    )
                    val articleInlineMessage =
                        articleInlineMessageViewModel.articleInlineMessage.observeAsState().value
                    val adsMode by adService.adsMode.collectAsState()
                    ProvideWebViewPool {
                        ArticleContentView(
                            uiState = contentState,
                            articleInlineMessage = articleInlineMessage,
                            articlesInteractionHelper = this@ArticleContentFragment,
                            onPageExpandChanged = { shouldExpand ->
                                onPageExpandChanged(articleMeta.id, shouldExpand)
                            },
                            onScrollStarted = {
                                if (articleMeta.id == articlesPagerCollaborationViewModel.currentPage.value?.articleMeta?.id) {
                                    onEventFired(ArticleInteractionEvent.ArticleScrollStartedEvent)
                                }
                            },
                            onScrollStopped = { fullyVisibleIndices ->
                                if (articleMeta.id == articlesPagerCollaborationViewModel.currentPage.value?.articleMeta?.id) {
                                    onEventFired(ArticleInteractionEvent.ArticleScrollStoppedEvent)
                                    updateScrollDepth(fullyVisibleIndices)
                                }
                            },
                            activeVideoIds = articles2ViewModel.activeVideoIds,
                            videoManager2 = videoActivityViewModel.getVideoManager2(),
                            resolveVideoStreamId = { arcId ->
                                resolveVideoStreamId(arcId)
                            },
                            userHistoryViewModel = userHistoryViewModel,
                            forYouActivityViewModel = forYouActivityViewModel,
                            onCarouselVisibilityChanged = { carouselIndex, isVisible ->
                                onCarouselVisibilityChanged(
                                    carouselIndex,
                                    isVisible,
                                    articleMeta.id
                                )
                            },
                            onScrollProgressChanged = { progress ->
                                if (articleMeta.id == articlesPagerCollaborationViewModel.currentPage.value?.articleMeta?.id) {
                                    articlesPagerCollaborationViewModel.updateScrollProgress(
                                        progress
                                    )
                                }
                            },
                            initialScrollId = articleMeta.deepestScrollId,
                            adsMode = adsMode,
                            isCurrentPage = articleMeta.id == articlesPagerCollaborationViewModel.currentPage.observeAsState().value?.articleMeta?.id,
                            webEmbedSettings = webEmbedSettings,
                            onContentReady = {
                                onArticleContentReady(articleMeta.id)
                            },
                        )
                    }
                }
            }

            updateContentLinkType(
                meta,
                articlesPagerCollaborationViewModel.currentPage.value
                    ?.articleMeta
                    ?.id,
            )

            binding.retry.setOnClickListener {
                val meta =
                    arguments?.getParcelable<ArticleMeta>(ArticlesPagerAdapter.ARTICLE_METADATA_KEY)
                if (meta != null) {
                    articles2ViewModel.startLoadingArticle(meta)
                }
            }
        }

        val url = articlesPagerCollaborationViewModel.currentPage.value?.articleMeta?.id
        url?.let {
            commentsViewModel.fetchCommentsData(it, isPushOriginated)
        }

        observeArticleContentState()
        observeAudioPlaybackUpdates()

        observeLowDataBannerEvent {
        }

        updateLowDataMode()

        updateInlineOfferData()
        observeVideoEvent()
    }

    private fun bindToolbarActions(articleId: String) {
        observeLiveMapEvents(toolbarActionsLiveData)
        articlesPagerCollaborationViewModel.setLiveMapLiveData(articleId, toolbarActionsLiveData)
    }

    private fun observeVideoEvent() {
        videoActivityViewModel.openVideoEvent.observe(viewLifecycleOwner) { _ ->
            // In the Compose flow the inline player frame is managed declaratively via
            // activeVideoIds + AndroidView. Nothing to manually rebind here.
        }
    }

    private fun updateInlineOfferData() {
        articleInlineMessageViewModel.dispatchMessageUpdateEvent()
    }

    override fun onPause() {
        super.onPause()
        userHistoryViewModel.finalizeScrollDepthItem(readingStartTime)
//        contentView?.onVisibilityChanged(false) TODO still needed?
    }

    override fun onResume() {
        super.onResume()
//        contentView?.onVisibilityChanged(true) TODO still needed?
    }

    /**
     * Do nothing. We just need to make sure that there is an observer subscribed to listen to updates from [articles2ViewModel.availableVoices]
     * We're currently not using this for any UI updates.
     */
    private fun observeAvailableVoicesRequestUpdates() {
        articles2ViewModel.availableVoices.observe(viewLifecycleOwner) {
            /*
                Do nothing - View model handles the response internally.
             */
        }
    }

    /**
     * Prepare video player and its views as soon as video items are prepared from article items.
     */
    private fun observeVideoItems(meta: ArticleMeta) {
        articles2ViewModel.videoItems.observe(viewLifecycleOwner) {
            // Load only current article videos. They will be unloaded when closing current article
            // or when switching to a different article.
            if (meta.id ==
                articlesPagerCollaborationViewModel.currentPage.value
                    ?.articleMeta
                    ?.id
            ) {
                warmUpVideoItems(meta)
            }
        }
    }

    private fun observeTopicFollowStateUpdate() {
        topicFollowCollaborationViewModel.reloadTopicState.observe(viewLifecycleOwner) {
            inlineTopicFollowViewModel.reload(it, requireContext())
        }
    }

    private fun observeLowDataBannerEvent(onChangeClick: () -> Unit) {
        lowDataBannerViewModel.lowDataBannerState.observe(viewLifecycleOwner) {
            topicFollowCollaborationViewModel.setIsLowDataMode(it.isLowDataBannerEnable)
        }
    }

    private fun observeArticleContentState() {
        articles2ViewModel.articleContentState.observe(viewLifecycleOwner) { state ->
            if (state is ArticleContentState.Success &&
                articlesPagerCollaborationViewModel.isVisibleArticle(state.article.contenturl)
            ) {
                val article = state.article
                val articleArcId = article.arcId
                if (articleArcId != null) {
                    val userHistoryArticleItems =
                        article.items?.map { item ->
                            UserHistoryArticleItem(item.arcId, item.type)
                        }
                    userHistoryViewModel.initializeScrollDepth(
                        articleArcId,
                        userHistoryArticleItems,
                        articlesPagerCollaborationViewModel.currentPage.value?.articleMeta?.deepestScrollId
                    )
                }
            }
        }
    }

    private fun observeAudioPlaybackUpdates() {
        articles2ViewModel.nowPlayingAudioItem.observe(viewLifecycleOwner) { nowPlayingAudioItem ->
            val playbackState = nowPlayingAudioItem?.audioPlaybackState ?: AudioPlaybackState.None
            articles2ViewModel.setAudioPlaybackState(playbackState)
            articles2ViewModel.updateAudioPlaybackUi(nowPlayingAudioItem)
        }
    }

    private fun updateScrollDepth(fullyVisibleIndices: List<Int>) {
        if (fullyVisibleIndices.isEmpty()) return
        val state =
            articles2ViewModel.articleContentState.value as? ArticleContentState.Success ?: return
        val uiItems = state.uiItems

        for (index in fullyVisibleIndices.asReversed()) {
            val item = uiItems.getOrNull(index) ?: continue
            val arcId = getTrackableItemArcId(item) ?: continue
            userHistoryViewModel.updateScrollDepth(arcId)
            articleWallHelperViewModel.updateScrollDepth(
                calculateScrollDepthPercentage(
                    index,
                    uiItems.size
                )
            )
            return
        }
    }

    private fun getTrackableItemArcId(item: ArticleItemUiModel): String? =
        when (item) {
            is SanitizedHtmlUiModel -> item.arcId
            is ListUiModel -> item.arcId
            else -> null
        }

    private fun calculateScrollDepthPercentage(
        itemIndex: Int,
        itemCount: Int,
    ): Int {
        if (itemCount <= 0) return 0
        return ((itemIndex.toDouble() / itemCount) * 100).toInt()
    }

    private fun onCarouselVisibilityChanged(
        carouselIndex: Int,
        isVisible: Boolean,
        articleId: String
    ) {
        // Only process if this is the currently displayed article
        if (articleId != articlesPagerCollaborationViewModel.currentPage.value?.articleMeta?.id) {
            return
        }

        val state =
            articles2ViewModel.articleContentState.value as? ArticleContentState.Success ?: return
        val carousel = state.uiItems.getOrNull(carouselIndex) as? CarouselUiModel ?: return

        // Only track For You carousels
        when (carousel.uiStyle) {
            CarouselUiStyle.FOR_YOU -> {
                if (isVisible) {
                    // Start timers for carousel items if carousel is ≥50% visible
                    val recommendationsItems = carousel.items
                        .filterIsInstance<ForYouCarouselItemUiModel>()
                        .mapIndexed { idx, item ->
                            RecommendationsHelperItem(
                                item.articleId,
                                item.recReason,
                                carousel.requestId,
                                carousel.recipeId,
                                carousel.testId,
                                item.contentType
                            )
                        }
                    if (recommendationsItems.isNotEmpty()) {
                        userHistoryViewModel.startOrStopForYouViewedTimers(
                            recommendationsItems = recommendationsItems,
                            visibleItemIndices = (0 until recommendationsItems.size).toList(),
                            surface = ForYouFeedRepositoryImpl.SURFACE_RECIRC
                        )
                    }
                } else {
                    // Stop all timers if carousel is <50% visible
                    userHistoryViewModel.stopAllForYouViewedTimers()
                }
            }

            CarouselUiStyle.AUTO_RECIRC,
            CarouselUiStyle.SEVEN_LIVE -> {
                if (isVisible) {
                    val recommendationsItems = carousel.items
                        .filterIsInstance<RecircCarouselItemUiModel>()
                        .mapIndexed { index, item ->
                            AutoRecircHelperItem(
                                articleId = item.articleId.orEmpty(),
                                requestId = carousel.requestId.orEmpty(),
                                currentUrl = item.url.orEmpty(),
                                collectionCategory = carousel.category.orEmpty(),
                                positionInModule = index,
                            )
                        }
                    if (recommendationsItems.isNotEmpty()) {
                        userHistoryViewModel.startOrStopAutoRecircViewedTimers(
                            recommendationsItems = recommendationsItems,
                            visibleItemIndices = (0 until recommendationsItems.size).toList(),
                        )
                    }
                } else {
                    userHistoryViewModel.stopAllAutoRecircViewedTimers()
                }
            }

            else -> {/* no-op */
            }
        }
    }

    private fun updateLowDataMode() {
        lowDataBannerViewModel.updateLowDataBanner(
            AppPreferences.isLowDataModeEnabled() && ConfigManager.getInstance().config.lowDataModeConfig.enable,
        )
    }

    private fun setupLufAnimation() {
        val navBarHeight = resources.getDimension(R.dimen.luf_bar_height)

        val scaleDown: Animator =
            ObjectAnimator.ofPropertyValuesHolder(
                binding.stickyNav,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, -navBarHeight, 0f),
            )

        val scaleUp: Animator =
            ObjectAnimator.ofPropertyValuesHolder(
                binding.stickyNav,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, -navBarHeight),
            )
        val itemLayoutTransition = LayoutTransition()
        itemLayoutTransition.setAnimator(LayoutTransition.APPEARING, scaleDown)
        itemLayoutTransition.setAnimator(LayoutTransition.DISAPPEARING, scaleUp)
        binding.containerView.layoutTransition = itemLayoutTransition
    }

    private fun constructAudioMediaConfig(
        audio: Audio?,
        isActionAudio: Boolean = false,
    ): AudioMediaConfig? {
        val contentState = articles2ViewModel.articleContentState.value
        return if (contentState is ArticleContentState.Success) {
            val article = contentState.article
            val audio = audio ?: article.audio
            if (audio?.subtype == Audio.SubType.STANDALONE.value) {
                val audioTracker =
                    articlesPagerCollaborationViewModel.getAudioTracker(
                        isActionAudio,
                        AudioPreferences.getAudioPlaybackSpeed(requireContext()),
                        audio.tracking?.id,
                        audio.tracking?.name,
                    )
                audio.toAudioMediaConfig(
                    playerType = PlayerType.STANDALONE,
                    article = article,
                    audioTracker = audioTracker,
                )
            } else if (audio?.children != null || audio?.transitions != null) {
                articlesPagerCollaborationViewModel.assembleConfigWithChildren(
                    audio,
                    article,
                    requireContext(),
                    isActionAudio
                )
            } else {
                val omnitureX =
                    articlesPagerCollaborationViewModel.getCurrentTrackingInfo()?.omnitureX
                val audioTracker =
                    articlesPagerCollaborationViewModel.getAudioTracker(
                        isActionAudio,
                        AudioPreferences.getAudioPlaybackSpeed(requireContext()),
                        audio?.tracking?.id ?: omnitureX?.arcId,
                        audio?.tracking?.name ?: omnitureX?.pageName,
                    )
                audio?.toAudioMediaConfig(article = article, audioTracker = audioTracker)
            }
        } else {
            null
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // No legacy view-holder state to persist in the Compose-hosted path.
        super.onSaveInstanceState(outState)
    }

    private fun observeContentStateEvents(
        meta: ArticleMeta,
        savedInstanceState: Bundle?,
        position: Int,
    ) {
        articles2ViewModel.articleContentState.observe(viewLifecycleOwner) { state ->
            articlesPagerCollaborationViewModel.apply {
                if (meta.id == currentPage.value?.articleMeta?.id) {
                    setContentState(state)
                }
                addToScreenState(meta.id, state)
            }
            val url = meta.id ?: ""
            when (state) {
                is ArticleContentState.Loading -> {
                    articlesPagerCollaborationViewModel.dispatchArticleMetricsEvent(
                        ArticleMetricsEvent.StartLoading(url),
                    )
                }
                is ArticleContentState.Processing -> {
                    articlesPagerCollaborationViewModel.dispatchArticleMetricsEvent(
                        ArticleMetricsEvent.StopLoading(url),
                    )
                    articlesPagerCollaborationViewModel.dispatchArticleMetricsEvent(
                        ArticleMetricsEvent.StartProcessing(url),
                    )
                }
                is ArticleContentState.Success -> {
                    // The logic in this case is only needed on initial content load
                    if (state.isUpdate) return@observe

                    articlesPagerCollaborationViewModel.dispatchArticleMetricsEvent(
                        ArticleMetricsEvent.StopProcessing(url),
                    )

                    articlesPagerCollaborationViewModel.dispatchArticleMetricsEvent(
                        ArticleMetricsEvent.StartDrawing(url),
                    )

                    addToUserBehaviorTrackingMap(state)
                    addNativeArticleToMetaDataModelMap(state)
                    addToFirebaseAnalyticsTrackingMap(state.article, position)
                    if (shouldPlayAudioArticle && position == audioArticlePosition) {
                        val audioConfig = constructAudioMediaConfig(state.article.audio)
                        audioConfig?.let {
                            audioConfig.positionToStartInMs = meta.listenDepthSec * 1000
                            audioMediaActivityViewModel.playMedia(audioConfig)
                        }
                    }

                    // Dispatch delayed paywall if current article is a Native Article
                    if (articlesPagerCollaborationViewModel.isVisibleArticle(
                            state.article.contenturl,
                        )
                    ) {
                        articleWallHelperViewModel.dispatchArticleForPaywall(state.article, meta.id)
                    }

                    // Check audio items availability here and for every page change event.
                    performAudioAvailabilityCheck(meta, state.article)
                    // Check summary availability here and for every page change event.
                    performSummaryAvailabilityCheck(meta, state.article)
                    // Check comments availability here and for every page change event.
                    performCommentsAvailabilityCheck(meta, state.article)
                }

                is ArticleContentState.Unsupported, ArticleContentState.UITimedOut -> {
                    if (state is ArticleContentState.Unsupported) {
                        state.article?.let {
                            addToFirebaseAnalyticsTrackingMap(it, position)
                        }
                    }
                    // Fallback to the webview only in the Unsupported case.
                    articlesPagerCollaborationViewModel.apply {
                        if (state is ArticleContentState.Unsupported) {
                            // if this is the current page in the list (i.e. someone clicks on a webview article), start SimpleWebviewActivity
                            if (meta.id == currentPage.value?.articleMeta?.id && activity?.isFinishing != true) {
                                val isFailoverOriginated =
                                    activity?.intent?.getBooleanExtra(FAILOVER_ORIGINATED, false)
                                Utils.startWeb(
                                    meta.id,
                                    context,
                                    mapOf(
                                        SimpleWebViewActivity.IS_FAILOVER_ORIGINATED to isFailoverOriginated
                                    )
                                )
                                (activity as? Articles2Activity)?.finish()
                            } else {
                                // If this is not the current page, we can remove it from the screen state
                                // and hide the view.
                                binding.nativeItem.visibility = View.GONE
                                _binding = null
                                articlesPagerCollaborationViewModel.removeFromScreenState(meta)
                                setPageRemoved(position)
                            }
                        }
                    }
                    EventLog
                        .Builder()
                        .apply {
                            setMessage("Article is natively unsupported")
                            setModule(LogModules.ARTICLES)
                            setContentUrl(meta.id)
                            set(
                                "article_type",
                                if (state is ArticleContentState.Unsupported) {
                                    if (state.article != null) {
                                        "WebRenderer"
                                    } else if (state.article415 != null) {
                                        "FourFifteen"
                                    } else {
                                        ""
                                    }
                                } else if (state is ArticleContentState.UITimedOut) {
                                    "UITimedOut"
                                } else {
                                    ""
                                },
                            )
                        }.run {
                            RemoteLog.d(context, build())
                        }
                }

                else -> {
                    // no-op
                }
            }
        }
    }

    private fun onArticleContentReady(url: String) {
        _binding?.nativeItem?.doOnPreDraw {
            val state = articles2ViewModel.articleContentState.value
            if (state is ArticleContentState.Success) {
                articlesPagerCollaborationViewModel.dispatchArticleMetricsEvent(
                    ArticleMetricsEvent.StopDrawing(url, state.source),
                )
            }
        }
    }

    /**
     * Adds the metadata of the article that has been loaded to the map so that it can be used in future
     * for adding it to the reading history list as soon as the timer of 5 sec has elapsed.
     */
    private fun addNativeArticleToMetaDataModelMap(state: ArticleContentState.Success) {
        val urlKey = getUrlWithoutParameters(state.article.contenturl)
        val article = state.article
        val metadataModel =
            MetadataModel(
                article.contenturl,
                System.currentTimeMillis()
            ).apply {
                imageURL = article.socialImage
                headline = article.title
                blurb = article.blurb
                // get byline and date from items list
                article.items?.forEach {
                    when (it) {
                        is ByLine -> {
                            byline = it.content ?: ""
                        }

                        is Date -> {
                            publishedTime = it.content
                        }
                    }
                }
            }
        articlesPagerCollaborationViewModel.metaDataModelMap[urlKey] = metadataModel
        /*
              Checks if this article is currently being viewed by user/visible on the screen.
         */
        val isThisCurrentPage =
            articlesPagerCollaborationViewModel.currentPage.value?.articleMeta?.id?.let { url ->
                getUrlWithoutParameters(url)
            } == urlKey
        if (isThisCurrentPage) {
            articlesPagerCollaborationViewModel.startTimer(urlKey)
        }
    }

    /**
     * Adds the current article data to the user behavior tracking map so that it can be used when user swipes to this specific article.
     * [state] This provides all the necessary information required to perform adding tracking data [UserBehaviorTrackingModel] to the map.
     */
    private fun addToUserBehaviorTrackingMap(state: ArticleContentState.Success) {
        val urlKey = getUrlWithoutParameters(state.article.contenturl)
        state.article.sourcesubsection?.let {
            /*
                Checks if this article is currently being viewed by user/visible on the screen.
             */
            val isThisCurrentPage =
                articlesPagerCollaborationViewModel.currentPage.value?.articleMeta?.id?.let { url ->
                    getUrlWithoutParameters(url)
                } == urlKey

            /*
                Checks if the tracking event has already been dispatched for this article in current instance of an activity.
             */
            val hasBeenTracked =
                articlesPagerCollaborationViewModel.trackingMap[urlKey]?.hasBeenTracked ?: false

            /*
                Only if this is the current page and has not been tracked in the current instance of articles activity, we proceed with dispatching the tracking event.
             */
            if (isThisCurrentPage && !hasBeenTracked) {
                articlesPagerCollaborationViewModel.dispatchTrackUserBehaviorEvent(it)
                articlesPagerCollaborationViewModel.trackingMap[urlKey] =
                    UserBehaviorTrackingModel(urlKey, state.article.sourcesubsection, true)
            }
            /*
                Else make sure that map doesn't already have the tracking model added to it before adding.
             */
            else if (articlesPagerCollaborationViewModel.trackingMap[urlKey] == null) {
                articlesPagerCollaborationViewModel.trackingMap[urlKey] =
                    UserBehaviorTrackingModel(urlKey, state.article.sourcesubsection, false)
            }
        }
    }

    private fun observePageChangeEvents(meta: ArticleMeta) {
        articlesPagerCollaborationViewModel.currentPage.observe(viewLifecycleOwner) {
            if (meta.id == it.articleMeta.id) {
                // Force first scroll callback on each page activation.
                pageExpandState = null
            }
            updateContentLinkType(meta, it.articleMeta.id)
            // Check if current article is a webview article and
            // stops the delayed paywall from the previously focused article
            if (meta.id == it.articleMeta.id) {
                if (articlesPagerCollaborationViewModel.contentLinkType.value == ArticleLinkType.WEB) {
                    articleWallHelperViewModel.dispatchStopDelayedPaywall()
                }
            }
            when (val articleContentState = articles2ViewModel.articleContentState.value) {
                is ArticleContentState.Success -> {
                    // Check if current article is a Native Article and dispatch Delayed Paywall
                    if (articlesPagerCollaborationViewModel.isVisibleArticle(
                            articleContentState.article.contenturl,
                        )
                    ) {
                        commentsViewModel.fetchCommentsData(
                            articleContentState.article.contenturl,
                            isPushOriginated
                        )
                        // Internally this will stop the delayed paywall call of the previous article in the
                        // the ViewPager
                        articleWallHelperViewModel.dispatchArticleForPaywall(
                            articleContentState.article,
                            meta.id,
                        )
                        // Check audio items for every page event.
                        performAudioAvailabilityCheck(meta, articleContentState.article)
                        // Check summary availability for every page event.
                        performSummaryAvailabilityCheck(meta, articleContentState.article)
                        // Check comments availability for every page event.
                        performCommentsAvailabilityCheck(meta, articleContentState.article)
                        // Warmup Video items
                        if (meta.id == it.articleMeta.id) {
                            warmUpVideoItems(meta)
                        }
                    }
                }

                else -> {
                }
            }
        }
    }

    /**
     * Checks whether audio or podcast item is available in an article items.
     * Headphones Audio icon will be updated based on its state.
     */
    private fun performAudioAvailabilityCheck(
        meta: ArticleMeta,
        article2: Article2,
    ) {
        val isCurrentPage =
            meta.id ==
                    articlesPagerCollaborationViewModel.currentPage.value
                        ?.articleMeta
                        ?.id
        if (!isCurrentPage) return
        val hasAudio = article2.items?.firstOrNull { item -> item is Audio } != null
        val hasPodcast = article2.items?.firstOrNull { item -> item is Podcast } != null
        val audioAvailable = hasAudio || hasPodcast
        articlesPagerCollaborationViewModel.dispatchCurrentPageAudioAvailability(audioAvailable)
    }

    private fun performSummaryAvailabilityCheck(
        meta: ArticleMeta,
        article2: Article2,
    ) {
        val isCurrentPage =
            meta.id ==
                    articlesPagerCollaborationViewModel.currentPage.value
                        ?.articleMeta
                        ?.id
        if (!isCurrentPage) return
        articlesPagerCollaborationViewModel.dispatchCurrentPageSummaryAvailability(
            article2.summary != null,
            article2.showSummary == true,
        )
    }

    private fun performCommentsAvailabilityCheck(
        meta: ArticleMeta,
        article2: Article2,
    ) {
        val isCurrentPage =
            meta.id ==
                    articlesPagerCollaborationViewModel.currentPage.value
                        ?.articleMeta
                        ?.id
        if (!isCurrentPage) return
        articlesPagerCollaborationViewModel.dispatchCurrentPageCommentsAvailability(
            article2.comments != null,
        )
    }

    private fun warmUpVideoItems(meta: ArticleMeta) {
        if (articles2ViewModel.videoItems.value?.isNotEmpty() == true) {
            PostTvWarmUp(videoActivityViewModel).createPostTvPlayersForArticle(
                meta.id,
                articles2ViewModel.videoItems.value,
                requireActivity(),
            )
        }
    }

    private fun updateContentLinkType(
        meta: ArticleMeta,
        currentPageMetaId: String?,
    ) {
        if (meta.id == currentPageMetaId) {
            val contentState = articles2ViewModel.articleContentState.value
            val linkType = when {
                contentState is ArticleContentState.Unsupported -> ArticleLinkType.WEB
                else -> meta.articleLinkType
            }
            articlesPagerCollaborationViewModel.setContentLinkType(linkType)
        }
    }

    /**
     * These live map events basically listens to all of the toolbar click events that are performed in the scope of Articles2Activity.
     */
    private fun observeLiveMapEvents(liveData: LiveData<ActionsOnIndividualArticles>) {
        liveData.observe(viewLifecycleOwner) {
            when (it) {
                ActionsOnIndividualArticles.ActionGift -> performGifting()
                ActionsOnIndividualArticles.ActionShare -> performShare()
                ActionsOnIndividualArticles.ActionHeadphonesClick -> playAudio()
                ActionsOnIndividualArticles.ActionBookmarkClick -> handleBookmarkClick()
                ActionsOnIndividualArticles.ActionEllipsisClick -> handleEllipsisClick()
                ActionsOnIndividualArticles.ActionSummaryClick -> handleSummaryClick()
                ActionsOnIndividualArticles.ActionLUFRefresh -> articles2ViewModel.refreshWithNewContext()
                ActionsOnIndividualArticles.ActionCommentClick -> onEventFired(
                    ArticleInteractionEvent.ViewCommentsClickEvent
                )

                ActionsOnIndividualArticles.ActionTalkToThePost -> handleTalkToThePostClick()
            }
        }
    }

    /**
     * When Ellipsis is clicked from the Sections, Articles and Playlist Adapter converting the to Ellipsis action
     */
    private fun handleEllipsisClick() {
        val article = articles2ViewModel.shouldProceedToBookmark()
        val articleContentState = articles2ViewModel.shouldProceedToEllipsisMenu()
        article?.let {
            ellipsisHelperViewModel.handleEllipsisClick(
                it.toEllipsisActionItem(
                    url = it.contenturl,
                    arcId = it.arcId ?: "",
                    contentType = it.contentType ?: "",
                    imageUrl = it.socialImage ?: "",
                    headline = it.title ?: ""
                ),
            )
        }
        articleContentState?.let { articlesPagerCollaborationViewModel.dispatchArticleTapEvent(it) }
    }

    private fun handleSummaryClick() {
        val article = articles2ViewModel.shouldProceedToSummary()
        article?.let { articlesPagerCollaborationViewModel.dispatchSummaryClickEvent(it) }
    }

    private fun handleTalkToThePostClick() {
        articlesPagerCollaborationViewModel.dispatchTalkToThePostClickEvent()
    }

    /**
     * Show Gift Fragment when gift icon has been clicked.
     */
    private fun performGifting() {
        activity?.supportFragmentManager?.let {
            /* Override the standard gift sheet with pause wall if subscription is paused.
             * A bit of a workaround, as gifting is the only wall that isn't actually a paywall or regwall,
             * it's its own fragment type */
            if (PaywallService.getInstance().isSubscriptionPaused) {
                articleWallHelperViewModel.dispatchShowPaywallNow(
                    PaywallConstants.WallType.PAUSEWALL,
                )
            } else {
                val giftFragment = GiftArticleSenderFragment()
                giftFragment.show(it, "GiftBottomSheet")
            }
        }
        articlesPagerCollaborationViewModel.currentPage.value?.articleMeta?.id?.let {
            giftCollaborationViewModel.giftArticleUrl = it
        }
        giftCollaborationViewModel.dispatchGiftTrackingEvent(GiftTrackingDetails.GIFT_CLICK)
    }

    /**
     * Show Share dialog when share icon has been clicked
     */
    private fun performShare() {
        articles2ViewModel.getShareContent()?.run {
            Share
                .Builder()
                .headline(content)
                .byline(byLine)
                .shareUrl(url)
                .build()
                .shareItem(requireContext())
        }
    }

    private fun playAudio() {
        val article = articles2ViewModel.shouldProceedToAudio()
        article?.audio?.mediaId?.let {
            onEventFired(ArticleInteractionEvent.AudioItemClicked(it, true))
        }
    }

    private fun handleBookmarkClick() {
        val article = articles2ViewModel.shouldProceedToBookmark()
        article?.let { articlesPagerCollaborationViewModel.dispatchBookmarkTapEvent(it) }
    }

    /**
     * This function is specifically used to share content in text selection mode.
     */
    private fun performShareWithTextSelection(shareContent: ShareContent) {
        shareContent.run {
            Share
                .Builder()
                .headline(url)
                .shareUrl(content)
                .build()
                .shareItem(requireContext())
        }
    }

    private fun tryOpenSpecificComment(commentIdUrl: String): Boolean {
        val fm = activity?.supportFragmentManager ?: return false
        val deepLinkData =
            DeepLinksProcessor.getCommentDeepLinkData(URLParser(commentIdUrl)) ?: return false
        val (storyUrl, commentId) = deepLinkData
        val articleId =
            articlesPagerCollaborationViewModel.currentPage.value?.articleMeta?.id.orEmpty()

        CommentBottomSheetFragment.showComments(
            fm,
            storyID = articleId,
            storyUrl = storyUrl,
            storyTitle = null,
            commentSource = CommentBottomSheetFragment.COMMENT_SOURCE_DEEPLINK,
            commentID = commentId,
        )
        return true
    }

    override fun onEventFired(event: ArticleInteractionEvent) {
        when (event) {
            is ArticleInteractionEvent.ViewCommentsClickEvent -> {
                val currentPage = articlesPagerCollaborationViewModel.currentPage.value
                val fbTrackingInfo = articlesPagerCollaborationViewModel.getCurrentTrackingInfo()
                val arcId =
                    fbTrackingInfo?.omnitureX?.arcId ?: currentPage?.articleMeta?.id ?: return
                val storyUrl = (currentPage?.articleMeta?.id)?.let { getUrlWithoutParameters(it) }
                val storyTitle =
                    (articles2ViewModel.articleContentState.value as? ArticleContentState.Success)?.article?.title
                        ?: ""
                val trackingInfo = fbTrackingInfo?.let {
                    it.omnitureX?.toTrackingInfo()?.apply {
                        contentURL = it.contentUrl
                    }
                }
                activity?.supportFragmentManager?.let { fm ->
                    CommentBottomSheetFragment.showComments(
                        fm,
                        arcId,
                        storyUrl,
                        storyTitle,
                        "bottom-article",
                        trackingInfo
                    )
                }
                fbTrackingInfo?.let {
                    articlesPagerCollaborationViewModel.dispatchFirebaseAnalyticsTrackingEvent(
                        ViewCommentsTracking(it)
                    )
                }
            }

            is ArticleInteractionEvent.FromTheSourceTapEvent -> {
                val trackedUrl = appendTrackingParams(event.commentUrl, event.miscellany, null)

                val openedNatively = tryOpenSpecificComment(trackedUrl)
                if (!openedNatively) {
                    val deepLink =
                        trackedUrl
                            .toUri()
                            .buildUpon()
                            .appendQueryParameter(DeepLinksProcessor.NO_NAV, true.toString())
                            .build()
                            .toString()
                    WPUrlAnalyser.getWPUrlAnalyser()
                        .analyseAndStartIntent(requireContext(), deepLink, "")
                }

                articlesPagerCollaborationViewModel.getCurrentTrackingInfo()?.let {
                    articlesPagerCollaborationViewModel.dispatchFirebaseAnalyticsTrackingEvent(
                        OnPageTap(
                            it,
                            event.miscellany,
                            event.sourceName
                        ),
                    )
                }
            }

            is ArticleInteractionEvent.AuthorNameClickEvent -> {
                val author = mapAuthorIdToAuthor(event.authorId) ?: return
                articlesPagerCollaborationViewModel.authorClicked(
                    Author(
                        id = author.id,
                        url = null,
                        name = author.name,
                        bio = author.bio,
                        expertise = null,
                        image = author.image,
                    ),
                )
            }

            is ArticleInteractionEvent.AskThePostClickEvent -> {
                val contentState =
                    articles2ViewModel.articleContentState.value as? ArticleContentState.Success
                        ?: return
                val selectedQuestions = contentState
                    .article.atpQuestions
                    ?.questionSet
                    ?.firstOrNull { it.id == event.questionId }
                    ?.questions
                    .orEmpty()
                if (selectedQuestions.isNotEmpty()) {
                    articlesPagerCollaborationViewModel.askThePostClicked(selectedQuestions)
                }
            }

            is ArticleInteractionEvent.LinkClickEvent -> {
                articlesPagerCollaborationViewModel.linkClicked(event.url)
            }

            is ArticleInteractionEvent.LufOutcomePostClickEvent -> {
                articlesPagerCollaborationViewModel.lufOutcomePostClicked(event.url)
            }

            is ArticleInteractionEvent.ImageClickEvent -> {
                val contentState =
                    articles2ViewModel.articleContentState.value as? ArticleContentState.Success
                val image = contentState?.article?.items?.filterIsInstance<Image>()?.firstOrNull {
                    it.imageURL == event.imageUrl
                } ?: return
                articlesPagerCollaborationViewModel.imageTapped(image)
            }

            is ArticleInteractionEvent.TextSelectionShareEvent ->
                performShareWithTextSelection(
                    event.shareContent,
                )

            ArticleInteractionEvent.ArticleScrollStartedEvent -> {
                articlesPagerCollaborationViewModel.getCurrentTrackingInfo()?.let {
                    articlesPagerCollaborationViewModel.dispatchFirebaseAnalyticsTrackingEvent(
                        ArticleScrollTracking(
                            Events.EVENT_SCROLL_START,
                            it,
                        ),
                    )
                }
            }

            ArticleInteractionEvent.ArticleScrollStoppedEvent -> {
                articlesPagerCollaborationViewModel.getCurrentTrackingInfo()?.let {
                    articlesPagerCollaborationViewModel.dispatchFirebaseAnalyticsTrackingEvent(
                        ArticleScrollTracking(
                            Events.EVENT_SCROLL_END,
                            it,
                        ),
                    )
                }
            }

            is ArticleInteractionEvent.AudioItemClicked -> {
                val audioMediaConfig =
                    constructAudioMediaConfig(mapRawUrlToAudio(event.url), event.isActionAudio)
                if (audioMediaConfig != null) {
                    if (audioMediaConfig.getPlayerType() == PlayerType.PODCAST) {
                        // Skip paywall for podcast
                        handleAudioPlayback(event)
                    } else {
                        observePaywallStatusForAudio(event)
                        articleWallHelperViewModel.checkAudioPaywallStatus()
                    }
                    articles2ViewModel.setAudioPlaybackState(AudioPlaybackState.Buffering)
                }
            }

            is ArticleInteractionEvent.ArticleGalleryExpandCollapse -> {
                val contentState =
                    articles2ViewModel.articleContentState.value as? ArticleContentState.Success
                        ?: return
                val collapseCard =
                    contentState.article.items?.firstOrNull {
                        it is GalleryExpandCollapse && it.galleryId == (event.item as? GalleryExpandCollapse)?.galleryId
                    }
                if (collapseCard != null) {
                    onTruncate(event.item)
                }
            }

            is ArticleInteractionEvent.ArticleTruncateExpandCollapse -> {
                onTruncate(event.item)
            }

            is ArticleInteractionEvent.ArticleCardExpandCollapse -> {
                onTruncate(event.item)
            }

            is ArticleInteractionEvent.TopicFollowButtonClicked -> {
                val contentState = articles2ViewModel.articleContentState.value
                if (contentState is ArticleContentState.Success) {
                    Measurement.trackInlineTopicFollowClicked(contentState.article)
                }
                val item = event.inlineTopicFollowItem
                activity?.supportFragmentManager?.let { fm ->
                    inlineTopicFollowViewModel.getFollowable(item.topicKey)?.let {
                        item.topicKey ?: return@let
                        TopicFollowArticleBottomSheetFragment(
                            contentPackId = item.topicKey,
                            followable = it,
                            trackingPageName = articlesPagerCollaborationViewModel.getCurrentTrackingInfo()?.omnitureX?.pageName
                                ?: "",
                        ).apply {
                            show(fm, "topic-follow")
                        }
                    }
                }
            }

            is ArticleInteractionEvent.InlineMessageBannerEvent -> {
                globalBannerViewModel.setBannerEvent(event.bannerEvent)
            }

            is ArticleInteractionEvent.CarouselCardSwipeEvent -> {
                articlesPagerCollaborationViewModel.togglePagerScroll(event.shouldAllowPagerScroll)
            }

            is ArticleInteractionEvent.CarouselItemClick -> {
                articlesPagerCollaborationViewModel.linkClicked(event.url)
            }

            is ArticleInteractionEvent.VideoClickEvent -> {
                handleVideoClick(event.videoId)
            }

            is ArticleInteractionEvent.VideoAutoplayEvent -> {
                handleVideoAutoplay(event.videoId)
            }

            is ArticleInteractionEvent.VideoOffscreenEvent -> {
                handleVideoOffscreen(event.videoId)
            }

            is ArticleInteractionEvent.AlertToggled -> {
                FlagshipApplication.getInstance().alertsSettings.enableAlertsTopic(
                    event.topicKey,
                    event.isEnabled
                )
                Measurement.trackAlertTopicEnroll(
                    event.topicDisplayName,
                    AlertsSettings.EntryPoint.INLINE_TOGGLE.trackingName,
                    event.isEnabled,
                )
            }

            is ArticleInteractionEvent.KickerClickEvent -> {
                val deepLinkUrl = DeepLinksProcessor.sectionPathToDeepLink(event.path)
                DeepLinksProcessor.processAsync(
                    com.wapo.android.commons.util.URLParser(deepLinkUrl),
                    activityContext = requireContext()
                )
            }

            is ArticleInteractionEvent.ElementGroupExpandCollapse -> {
                articles2ViewModel.toggleElementGroupExpansion(event.group)
            }

            is ArticleInteractionEvent.DisclaimerInfoClicked -> {
                val endpoint = "${event.reference}.json"
                lifecycleScope.launch {
                    val disclaimerInfo = articles2ViewModel.getDisclaimerInfo(endpoint)
                    disclaimerInfo?.let {
                        DisclaimerBottomSheetFragment
                            .newInstance(it)
                            .show(childFragmentManager, DisclaimerBottomSheetFragment.TAG)
                    } ?: run {
                        //Show error message if disclaimer info is null
                        Toast.makeText(requireContext(), "Failed to load disclaimer info", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun onTruncate(item: Item) {
        val state =
            articles2ViewModel.articleContentState.value as? ArticleContentState.Success ?: return
        val currentItems = state.article.items?.toMutableList() ?: return
        article2ItemsViewModel.onItemClick(item, currentItems)
        // Write the mutated list back so Compose re-renders with expanded/collapsed items
        articles2ViewModel.updateItemsAfterToggle(currentItems)
    }

    /**
     * Observes whether or not polly usage should be paywalled.
     * [event] current inline polly item/ audio click event.
     */
    private fun observePaywallStatusForAudio(event: ArticleInteractionEvent.AudioItemClicked) {
        articleWallHelperViewModel.shouldPaywallAudio.observe(
            viewLifecycleOwner,
        ) { shouldShowPaywall ->
            if (!shouldShowPaywall) {
                handleAudioPlayback(event)
            }
            /*
                Need to do this since user can tap on the inline item multiple times.
             */
            articleWallHelperViewModel.shouldPaywallAudio.removeObservers(viewLifecycleOwner)
        }
    }

    /**
     * Called after chekcing for paywall status for Polly. This function takes care of
     * playing, pausing / stopping the polly playback based on the current state.
     * [event] current inline polly item/ audio click event.
     */
    private fun handleAudioPlayback(event: ArticleInteractionEvent.AudioItemClicked) {
        val handled = closePersistentPlayerIfNeeded(event.isActionAudio)
        if (!handled) {
            constructAudioMediaConfig(mapRawUrlToAudio(event.url), event.isActionAudio)?.also {
                it.isActionAudio = event.isActionAudio
                articlesPagerCollaborationViewModel.dispatchAudioClickEvent(it)
                (activity as? BaseActivity)?.onAudioStarted()
            }
        }
    }

    /**
     * Add the tracking info to the firebaseAnalyticsTrackingMap in [articlesPagerCollaborationViewModel] which can be used immediately or in the future.
     * [article2] the article with the tracking information.
     */
    private fun addToFirebaseAnalyticsTrackingMap(
        article2: Article2,
        position: Int,
    ) {
        val url = getUrlWithoutParameters(article2.contenturl)
        val currentPageUrl =
            articlesPagerCollaborationViewModel.currentPage.value?.articleMeta?.id?.let {
                getUrlWithoutParameters(it)
            }
        val articleWeight =
            if (ArticleModel.CONTENT_RESTRICTION_FREE == article2.contentRestrictionCode) {
                0f
            } else {
                getArticleWeightByUrl(
                    url,
                )
            }
        val inlinePushToggleFlag = InlineAlertToggleHelper().articleHasToggle(article2)
        val trackingInfo =
            FirebaseTrackingInfo(
                article2.title,
                article2.omniture,
                article2.blogname,
                article2.contenturl,
                article2.firstPublished,
                article2.lmt,
                position,
                articleWeight,
                inlinePushToggleFlag,
                article2.targeting,
                article2.commercialnode,
            )
        if (!articlesPagerCollaborationViewModel.firebaseAnalyticsTrackingMap.contains(url)) {
            articlesPagerCollaborationViewModel.firebaseAnalyticsTrackingMap[url] = trackingInfo
        }
        // For the first article that's in the list, the article's page change event will not be fired which internally fires an analytics
        // event.
        // Also allowing firebase events for debug articles.
        if (url == currentPageUrl ||
            (
                    BuildConfig.DEBUG &&
                            currentPageUrl?.startsWith(
                                "${ARTICLE_BASE_URL}/debug/",
                            ) == true
                    )
        ) {
            val jTid =
                if (pageViewTimeTrackerViewModel.articleTimeStamp.articleUrl == url) {
                    pageViewTimeTrackerViewModel.articleTimeStamp.timeStamp
                } else {
                    null
                }
            articlesPagerCollaborationViewModel.dispatchFirebaseAnalyticsTrackingEvent(
                FirebaseAnalyticsTrackingEvent.ArticleTracking(trackingInfo, jTid),
            )
        }
    }

    private fun getArticleWeightByUrl(url: String): Float? {
        val weightedArticles = PaywallService.getInstance().tetroManager.getWeightedArticles()
        return if (weightedArticles.containsKey(url)) {
            weightedArticles[url]
        } else {
            1f
        }
    }

    private fun closePersistentPlayerIfNeeded(isActionAudio: Boolean): Boolean =
        if (isActionAudio && articles2ViewModel.nowPlayingAudioItem.value?.audioPlaybackState is AudioPlaybackState.Playing) {
            (activity as? Articles2Activity)?.stopPersistentAudioPlayer()
            true
        } else {
            false
        }

    // ── Inline video playback helpers (Compose path) ─────────────────────────

    /**
     * Resolve the PostTV stream-URL key that [VideoManager2] uses for a given arc ID.
     * This mirrors [VideoViewHolder.getVideoId].
     */
    private fun resolveVideoStreamId(arcId: String): String? {
        val videoData = articles2ViewModel.findVideoItemById(arcId) ?: return null
        return when (videoData.host) {
            com.wapo.flagship.features.articles2.models.deserialized.video.Host.YOUTUBE ->
                android.net.Uri.parse(videoData.mediaURL).getQueryParameter("v")

            else -> videoData.getStreamUrl(
                videoActivityViewModel.pageConfig.videoMaxBitRateMobile,
                videoActivityViewModel.pageConfig.videoMaxBitRateTablet,
            ) ?: videoData.streamURL ?: videoData.mediaURL
        }
    }

    /**
     * Handle a user tap on a video thumbnail in the Compose article view.
     * Mirrors the legacy VideoViewHolder.bindUserInitiatedVideo path.
     */
    private fun handleVideoClick(arcId: String) {
        val videoData = articles2ViewModel.findVideoItemById(arcId) ?: return
        val contentState =
            articles2ViewModel.articleContentState.value as? ArticleContentState.Success ?: return
        val articleModel = contentState.article

        // Stop any currently-playing video before starting the new one.
        articles2ViewModel.clearActiveVideos()

        // User-initiated tap always uses PLAY_TYPE_NORMAL (even for autoplay+looping videos,
        // since the Compose flow doesn't yet have autoplay-on-scroll for those).
        val playType = com.wapo.flagship.features.posttv.model.Video.PLAY_TYPE_NORMAL

        val shouldSuppressAds = FlagshipApplication.getInstance().shouldSuppressAds()
        val adTagUrl = if (videoData.vertical == false && !shouldSuppressAds) {
            com.wapo.flagship.common.getAdTagUrl(videoData, articleModel, null)
        } else {
            null
        }

        val streamId = resolveVideoStreamId(arcId) ?: return
        val videoManager2 = videoActivityViewModel.getVideoManager2()
        val aspectRatio = computeAspectRatio(videoData)

        val video =
            buildPostTvVideo(videoData, streamId, playType, aspectRatio, articleModel, adTagUrl)

        if (videoData.vertical == true) {
            if (!shouldSuppressAds) {
                video.adTagUrl = com.wapo.flagship.common.getAdTagUrl(videoData, articleModel, null)
            }
            videoActivityViewModel.openWatchVideoCard(video)
            return
        }
        videoManager2.initMedia(video, isPlayerClickable = false)
        (activity as? BaseActivity)?.onVideoStarted()

        articles2ViewModel.setVideoActive(arcId)
        val isLoopingPlayback = videoData.isLooping == true ||
                (videoData.promo?.isLooping == true && !videoData.promo?.url.isNullOrEmpty())
        observeVideoPlaybackState(streamId, arcId, isLooping = isLoopingPlayback)
        videoActivityViewModel.dispatchVideoClickEvent(arcId)
    }

    /**
     * Handle autoplay: when an autoplay-eligible video scrolls into view.
     * Mirrors the legacy VideoViewHolder.bindAutoplayVideo eligibility checks:
     * Skip if autoplay is explicitly off AND there's no promo URL
     * Skip if both autoplay and isLooping are null and there's no promo URL
     */
    private fun handleVideoAutoplay(arcId: String) {
        // Don't re-init if already active
        if (articles2ViewModel.activeVideoIds.value.contains(arcId)) return
        // Don't re-trigger autoplay for videos that have already finished playing
        if (articles2ViewModel.isAutoplayCompleted(arcId)) return

        val videoData = articles2ViewModel.findVideoItemById(arcId) ?: return
        // Skip if autoplay is explicitly off AND there's no promo URL
        if (videoData.autoplay == false && videoData.promo?.url.isNullOrEmpty()) return
        // Skip if both autoplay and isLooping are null and there's no promo URL
        if (videoData.autoplay == null && videoData.isLooping == null && videoData.promo?.url.isNullOrEmpty()) return
        if (!videoActivityViewModel.canAutoPlayInlineVideo()) return
        if (videoData.host == com.wapo.flagship.features.articles2.models.deserialized.video.Host.YOUTUBE) return
        if (videoData.host == com.wapo.flagship.features.articles2.models.deserialized.video.Host.VIMEO) return

        val videoManager2 = videoActivityViewModel.getVideoManager2()
        if (!videoManager2.canAddAutoplay()) return

        val streamId = resolveVideoStreamId(arcId) ?: return

        // If the user already manually played this video, don't downgrade to muted autoplay
        val existingFrame = videoManager2.getPlayerFrame(streamId)
        if (existingFrame?.video?.playType == com.wapo.flagship.features.posttv.model.Video.PLAY_TYPE_NORMAL) return

        val contentState =
            articles2ViewModel.articleContentState.value as? ArticleContentState.Success ?: return
        val articleModel = contentState.article

        val playType = if (videoData.isLooping == true) {
            com.wapo.flagship.features.posttv.model.Video.PLAY_TYPE_NORMAL_MUTED
        } else {
            com.wapo.flagship.features.posttv.model.Video.PLAY_TYPE_AUTOPLAY
        }

        val aspectRatio = computeAspectRatio(videoData)
        val video = buildPostTvVideo(
            videoData,
            streamId,
            playType,
            aspectRatio,
            articleModel,
            adTagUrl = null
        )

        videoManager2.incrementAutoplayCount()
        videoManager2.initMedia(video, isPlayerClickable = false) {}

        articles2ViewModel.setVideoActive(arcId)
        val isLoopingPlayback = videoData.isLooping == true ||
                (videoData.promo?.isLooping == true && !videoData.promo?.url.isNullOrEmpty())
        observeVideoPlaybackState(streamId, arcId, isLooping = isLoopingPlayback)
    }

    /**
     * Handle a video scrolling off screen — pause/release autoplay videos to save resources.
     * User-initiated plays continue in the background.
     */
    private fun handleVideoOffscreen(arcId: String) {
        if (!articles2ViewModel.activeVideoIds.value.contains(arcId)) return

        val videoData = articles2ViewModel.findVideoItemById(arcId) ?: return
        val streamId = resolveVideoStreamId(arcId) ?: return
        val videoManager2 = videoActivityViewModel.getVideoManager2()

        // Only manage autoplay videos; user-initiated plays should continue in background
        val playType = videoManager2.getPlayerFrame(streamId)?.video?.playType
        if (playType == com.wapo.flagship.features.posttv.model.Video.PLAY_TYPE_NORMAL) return

        val isLoopingPlayback = videoData.isLooping == true ||
                (videoData.promo?.isLooping == true && !videoData.promo?.url.isNullOrEmpty())

        if (isLoopingPlayback) {
            // Looping autoplay / promo videos get fully released when offscreen (matching legacy)
            videoManager2.decrementAutoplayCount()
            videoManager2.release(streamId)
        } else {
            // Regular autoplay: just pause
            videoManager2.getPlayerManager(streamId)?.setPlayWhenReady(false)
            videoManager2.decrementAutoplayCount()
        }
        articles2ViewModel.setVideoInactive(arcId)
    }

    /**
     * Watch the PostTV player's playback state for a given video.
     * On Ended (non-looping) or Error, revert to thumbnail overlay.
     * Looping videos won't end — PostTV handles the loop — but we still watch for errors.
     */
    private fun observeVideoPlaybackState(streamId: String, arcId: String, isLooping: Boolean) {
        val videoManager2 = videoActivityViewModel.getVideoManager2()
        val playerManager = videoManager2.getPlayerManager(streamId) ?: return
        playerManager.playbackState.removeObservers(viewLifecycleOwner)
        playerManager.playbackState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is com.wapo.flagship.features.posttv.model.PlaybackState.Ended -> {
                    if (!isLooping) {
                        articles2ViewModel.markAutoplayCompleted(arcId)
                        articles2ViewModel.setVideoInactive(arcId)
                        playerManager.playbackState.removeObservers(viewLifecycleOwner)
                    }
                }

                is com.wapo.flagship.features.posttv.model.PlaybackState.Error -> {
                    articles2ViewModel.setVideoInactive(arcId)
                    playerManager.playbackState.removeObservers(viewLifecycleOwner)
                }

                else -> { /* playing / buffering / etc — keep showing player */
                }
            }
        }
    }

    private fun computeAspectRatio(videoData: com.wapo.flagship.features.articles2.models.deserialized.video.Video): Float {
        val w = videoData.imageWidth
        val h = videoData.imageHeight
        return if (h != null && w > 0 && h > 0) w / h.toFloat() else 16f / 9f
    }

    /**
     * Build a PostTV Video from the article data model.
     * Shared between user-initiated and autoplay paths.
     */
    private fun buildPostTvVideo(
        videoData: com.wapo.flagship.features.articles2.models.deserialized.video.Video,
        streamId: String,
        playType: Int,
        aspectRatio: Float,
        articleModel: com.wapo.flagship.features.articles2.models.Article2,
        adTagUrl: String?,
    ): com.wapo.flagship.features.posttv.model.Video {
        return com.wapo.flagship.features.posttv.model.Video.Builder()
            .setId(streamId)
            .setContentUrl(videoData.contenturl)
            .setShareUrl(videoData.shareurl)
            .setHeadline(null)
            .setIsYouTube(videoData.host == com.wapo.flagship.features.articles2.models.deserialized.video.Host.YOUTUBE)
            .setIsVimeo(videoData.host == com.wapo.flagship.features.articles2.models.deserialized.video.Host.VIMEO)
            .setDuration(videoData.duration ?: 0L)
            .setIsLive(
                videoData.isLive
                    ?: (videoData.duration == null && videoData.content?.duration == null)
            )
            .setPageName(videoData.omniture?.pageName)
            .setVideoName(videoData.title)
            .setVideoSection(videoData.omniture?.contentSubsection)
            .setVideoSource(videoData.omniture?.source)
            .setVideoCategory(null)
            .setShouldPlayAds(
                videoData.adconfig?.playAds == true && !FlagshipApplication.getInstance()
                    .shouldSuppressAds()
            )
            .setContentId(videoData.omniture?.contentId)
            .setSubtitleUrl(videoData.subtitlesURL)
            .setFallbackUrl(videoData.fallback)
            .setAspectRatio(aspectRatio)
            .setIsLooping(videoData.isLooping == true)
            .setAutoplay(videoData.autoplay == true)
            .setPromoIsLooping(videoData.promo?.isLooping == true)
            .setPromoUrl(videoData.promo?.url)
            .setPlayType(playType)
            .setSource(videoData)
            .setAdTagUrl(adTagUrl)
            .setPlaybackPosition(-1)
            .build()
    }

    override fun onDestroyView() {
        /**
         * To avoid mem. leaks
         */
//        contentView?.unbind() TODO still needed?
        super.onDestroyView()
        mostReadSubscription?.unsubscribe()
        mostReadSubscription = null
        _binding = null

        val meta = arguments?.getParcelable<ArticleMeta>(ArticlesPagerAdapter.ARTICLE_METADATA_KEY)
        articlesPagerCollaborationViewModel.removeFromScreenState(meta)
    }

    /**
     * provides livedata access to components that need it.
     */
    override fun provideAudioPlaybackStateLiveData(): LiveData<AudioPlaybackState> =
        articles2ViewModel.audioPlaybackState

    override fun provideNowPlayingAudioItemLiveData(): LiveData<NowPlayingAudioItem?> =
        articles2ViewModel.nowPlayingAudioItem

    private fun mapRawUrlToAudio(rawUrl: String): Audio? {
        val contentState =
            articles2ViewModel.articleContentState.value as? ArticleContentState.Success
        return contentState?.article?.items?.filterIsInstance<Audio>()?.firstOrNull {
            it.rawUrl == rawUrl
        }
    }

    private fun mapAuthorIdToAuthor(authorId: String): AuthorInfo? {
        val contentState =
            articles2ViewModel.articleContentState.value as? ArticleContentState.Success
        return contentState?.article?.items?.filterIsInstance<AuthorInfo>()?.firstOrNull {
            it.id == authorId
        }
    }

    private fun observeSourceAnnotations() {
        commentsViewModel.sourceAnnotations.observe(this.viewLifecycleOwner) { annotations ->
            articles2ViewModel.updateSourceAnnotations(annotations)
        }
    }

    private fun observeForYouRecircUpdates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                forYouActivityViewModel.feedData.collect { response ->
                    articles2ViewModel.setForYouResponse(response)
                }
            }
        }
    }

    private fun onPageExpandChanged(
        articleId: String,
        shouldExpand: Boolean,
    ) {
        val currentPageId =
            articlesPagerCollaborationViewModel.currentPage.value?.articleMeta?.id ?: return
        if (articleId != currentPageId) return
        if (pageExpandState == shouldExpand) return

        pageExpandState = shouldExpand
        if (shouldExpand) {
            articlesPagerCollaborationViewModel.expandPage()
        } else {
            articlesPagerCollaborationViewModel.collapsePage()
        }
    }

    private fun observeMostReadRecircUpdates() {
        mostReadSubscription?.unsubscribe()
        mostReadSubscription =
            FlagshipApplication
                .getInstance()
                .articleRecircCarouselCache
                .getCarouselItems(
                    RecirculationType.MOST_READ.sectionName,
                    RecirculationType.MOST_READ
                )
                .subscribe(
                    { items: List<com.washingtonpost.android.recirculation.carousel.models.CarouselViewItem> ->
                        articles2ViewModel.setMostReadRecircItems(items)
                    },
                    {
                        // no-op: Most Read is optional content
                    },
                )
    }
}
