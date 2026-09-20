package com.wapo.flagship.features.articles2.activities

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.animation.AccelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.children
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Slide
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.google.android.material.snackbar.Snackbar
import com.wapo.adsinf.policy.AdService
import com.wapo.android.commons.config.Constants
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.Logger
import com.wapo.android.commons.util.Utils.getAppVersionName
import com.wapo.android.commons.util.Utils.isConnectedOrConnecting
import com.wapo.android.commons.util.timePeriodString
import com.wapo.android.remotelog.logger.EventTimerLog
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.IntentHelper
import com.wapo.flagship.Utils
import com.wapo.flagship.features.articles2.views.ArticlesTopAppBar
import com.wapo.flagship.common.DialogFactory.getActiveSubscriberDialog
import com.wapo.flagship.common.tint
import com.wapo.flagship.content.WapoConfigManager
import com.wapo.flagship.di.app.modules.features.readinghistory.ReadingHistoryViewModel
import com.wapo.flagship.external.WidgetData
import com.wapo.flagship.external.storage.WidgetType
import com.wapo.flagship.features.ads.targeting.ui.ContentUIState
import com.wapo.flagship.features.ads.targeting.viewmodels.ContentViewModel
import com.wapo.flagship.features.aixp.SummariesFeatureFlag
import com.wapo.flagship.features.aixp.models.ArticleSummary
import com.wapo.flagship.features.aixp.ui.FeedbackFragment
import com.wapo.flagship.features.aixp.ui.FeedbackStatusFragment
import com.wapo.flagship.features.aixp.ui.SummaryFragment
import com.wapo.flagship.features.aixp.viewmodels.ArticleSummaryCollaborationViewModel
import com.wapo.flagship.features.aixp.viewmodels.ArticleSummaryCollaborationViewModel.Companion.MISCELLANY_SUFFIX_REALTIME
import com.wapo.flagship.features.aixp.viewmodels.FeedbackCollaborationViewModel
import com.wapo.flagship.features.articles.models.ArticlesTooltipsHelper.isTooltipShownInCurrentInstanceOfArticleActivity
import com.wapo.flagship.features.articles.models.ArticlesTooltipsHelper.resetTooltipShownInCurrentArticleActivityInstance
import com.wapo.flagship.features.articles.models.ArticlesTooltipsHelper.setTooltipShownInCurrentArticleActivityInstance
import com.wapo.flagship.features.articles2.ads.targeting.TargetingContent
import com.wapo.flagship.features.articles2.ads.targeting.TargetingContentUIState
import com.wapo.flagship.features.articles2.events.Article2Events
import com.wapo.flagship.features.articles2.fragments.ArticleTableOfContentsFragment
import com.wapo.flagship.features.articles2.interfaces.ArticleWebComponentHelper
import com.wapo.flagship.features.articles2.interfaces.ArticlesTopAppBarInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesTopAppBarInteractionHelper
import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.articles2.models.Author
import com.wapo.flagship.features.articles2.models.OmnitureX
import com.wapo.flagship.features.articles2.models.Question
import com.wapo.flagship.features.articles2.models.Summary
import com.wapo.flagship.features.articles2.models.deserialized.ByLine
import com.wapo.flagship.features.articles2.models.deserialized.Date
import com.wapo.flagship.features.articles2.models.deserialized.video.InlineVideoPlayerEvents
import com.wapo.flagship.features.articles2.navigation_models.ActionsOnIndividualArticles
import com.wapo.flagship.features.articles2.navigation_models.ArticlePage
import com.wapo.flagship.features.articles2.navigation_models.ArticleToolbarIconsState
import com.wapo.flagship.features.articles2.paywall.WallUiEvent
import com.wapo.flagship.features.articles2.states.ArticleContentState
import com.wapo.flagship.features.articles2.tracking.ArticleMetricsEvent
import com.wapo.flagship.features.articles2.tracking.FirebaseAnalyticsTracker
import com.wapo.flagship.features.articles2.tracking.FirebaseAnalyticsTrackingEvent
import com.wapo.flagship.features.articles2.tracking.FirebaseTrackingHelperData
import com.wapo.flagship.features.articles2.tracking.FirebaseTrackingHelperWidgetData
import com.wapo.flagship.features.articles2.tracking.PushArticleTrackingHelperData
import com.wapo.flagship.features.articles2.utils.getUrlWithoutParameters
import com.wapo.flagship.features.articles2.utils.toTrackingInfo
import com.wapo.flagship.features.articles2.viewmodels.ArticleTableOfContentsViewModel
import com.wapo.flagship.features.articles2.viewmodels.ArticleWallHelperViewModel
import com.wapo.flagship.features.articles2.viewmodels.ArticleWallHelperViewModel.MapDismissOrigin
import com.wapo.flagship.features.articles2.viewmodels.Articles2DestinationViewModel
import com.wapo.flagship.features.articles2.viewmodels.Articles2ViewModel
import com.wapo.flagship.features.articles2.viewmodels.ArticlesPagerCollaborationViewModel
import com.wapo.flagship.features.articles2.viewmodels.BottomCtaViewModel
import com.wapo.flagship.features.articles2.viewmodels.PageViewTimeTrackerViewModel
import com.wapo.flagship.features.ask.fragments.AskThePostBottomSheetFragment
import com.wapo.flagship.features.ask.fragments.TalkToThePostBottomSheetFragmentFactory
import com.wapo.flagship.features.ask.models.AskThePostEvent
import com.wapo.flagship.features.ask.viewmodels.AskThePostViewModel
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.audio.models.AudioPlaybackState
import com.wapo.flagship.features.audio.playlist.toAudioMediaConfig
import com.wapo.flagship.features.audio.playlist.toEllipsisActionItem
import com.wapo.flagship.features.audio.playlist.toPlaylistAudio
import com.wapo.flagship.features.audio.utils.AudioPreferences
import com.wapo.flagship.features.audio.viewmodels.AudioFeatureArticleStateViewModel
import com.wapo.flagship.features.audio.viewmodels.PlaylistActivityViewModel
import com.wapo.flagship.features.comments.CommentsViewModel
import com.wapo.flagship.features.conversations.ui.CommentBottomSheetFragment
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor
import com.wapo.flagship.features.fusion.fragments.EllipsisMenuFragment
import com.wapo.flagship.features.gifting.events.GiftCollabEvent
import com.wapo.flagship.features.gifting.viewmodels.GiftArticleRecipientViewModel
import com.wapo.flagship.features.gifting.viewmodels.GiftCollaborationViewModel
import com.wapo.flagship.features.articles2.models.ArticleInlineMessage
import com.wapo.flagship.features.grid.model.EllipsisMenu
import com.wapo.flagship.features.grid.viewmodel.EllipsisHelperViewModel
import com.wapo.flagship.features.articles2.viewmodels.ArticleInlineMessageViewModel
import com.wapo.flagship.features.lowdatamodelbanner.ui.LowDataBannerView
import com.wapo.flagship.features.lowdatamodelbanner.viewmodel.LowDataBannerViewModel
import com.wapo.flagship.features.map.models.MapWallType
import com.wapo.flagship.features.map.models.MapWallUiData
import com.wapo.flagship.features.map.views.MapWallView
import com.wapo.flagship.features.nightmode.NightModeController
import com.wapo.flagship.features.photos.NativePhotoActivity
import com.wapo.flagship.features.posttv.VideoTracker2
import com.wapo.flagship.features.posttv.listeners.PostTvActivity
import com.wapo.flagship.features.posttv.model.TrackingType
import com.wapo.flagship.features.posttv.model.Video
import com.wapo.flagship.features.search2.events.UserEvent
import com.wapo.flagship.features.search2.fragments.PostAnswersInfoBottomSheetFragment
import com.wapo.flagship.features.search2.viewmodel.PostAnswersFeedbackReaction
import com.wapo.flagship.features.settings.AppPreferences
import com.wapo.flagship.features.shared.activities.BaseActivity
import com.wapo.flagship.features.signin.LoginRegActivityViewModel
import com.wapo.flagship.features.signin.LoginRegHost
import com.wapo.flagship.features.subscribebanner.state.BannerEvent
import com.wapo.flagship.features.subscribebanner.viewmodel.GlobalBannerViewModel
import com.wapo.flagship.features.support.ABTests
import com.wapo.flagship.features.video.FullScreenVideoActivity
import com.wapo.flagship.features.video.FullScreenVideoParcel
import com.wapo.flagship.features.video.VerticalVideosParcel
import com.wapo.flagship.features.video.viewmodels.VideoActivityViewModel
import com.wapo.flagship.model.ArticleMeta
import com.wapo.flagship.sdk.iterable.IterablePlugin
import com.wapo.flagship.sdk.iterable.models.IamMessageType
import com.wapo.flagship.sdk.iterable.viewmodels.IterableActivityViewModel
import com.wapo.flagship.util.ChartbeatManager.pauseTracker
import com.wapo.flagship.util.PrefUtils
import com.wapo.flagship.util.WPUrlAnalyser
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.util.tracking.states.NavigationBehavior
import com.wapo.flagship.wapomain.MainConstants.ACTION_OPEN_SECTION
import com.wapo.flagship.wapomain.MainConstants.EXTRAS_SECTION_URL
import com.wapo.view.tooltip.TooltipData
import com.wapo.view.tooltip.TooltipPopupManager
import com.wapo.view.tooltip.TooltipPopupManager.Instance.getSpannableStringWithBoldText
import com.wapo.view.tooltip.TooltipPriority
import com.wapo.view.tooltip.TooltipProperties
import com.washingtonpost.android.R
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.databinding.ActivityArticlesBinding
import com.washingtonpost.android.databinding.CustomePlaylistSnackbarBinding
import com.washingtonpost.android.follow.helper.AuthorHelper
import com.washingtonpost.android.follow.helper.AuthorProvider
import com.washingtonpost.android.follow.model.AuthorItem
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.auth.AuthIntentBuilder
import com.washingtonpost.android.paywall.bottomsheet.ui.PaywallSheet2Fragment
import com.washingtonpost.android.paywall.bottomsheet.SubState
import com.washingtonpost.android.paywall.bottomsheet.model.BottomCtaType
import com.washingtonpost.android.paywall.events.GiftState
import com.washingtonpost.android.paywall.features.tetro.Prompt
import com.washingtonpost.android.paywall.features.tetro.WebTetroResponse
import com.washingtonpost.android.paywall.helper.PaywallPrefHelper
import com.washingtonpost.android.paywall.reminder.ReminderScreenFragment
import com.washingtonpost.android.paywall.util.PaywallConstants
import com.washingtonpost.android.paywall.util.PaywallConstants.WallType
import com.washingtonpost.android.save.database.model.ArticleAndMetadata
import com.washingtonpost.android.save.database.model.MetadataModel
import com.washingtonpost.android.save.database.model.ReadingHistoryModel
import com.washingtonpost.android.save.database.model.SavedArticleModel
import com.washingtonpost.android.save.views.ArticleListViewModel
import com.washingtonpost.android.wapocontent.ILoader
import com.washingtonpost.android.wapocontent.LoaderProvider
import com.washingtonpost.foryou.data.RecommendationsItem
import com.washingtonpost.foryou.data.byline
import com.washingtonpost.foryou.data.getURL
import com.washingtonpost.foryou.repo.ForYouFeedRepositoryImpl
import com.washingtonpost.foryou.viewmodel.ForYouActivityViewModel
import com.washingtonpost.userhistory.viewmodel.UserHistoryViewModel
import com.wpds.theme.AndroidClassicTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * This activity is the hosting activity that will display fully readable articles in it.
 */
@AndroidEntryPoint
class Articles2Activity :
    BaseActivity(),
    PostTvActivity,
    AuthorProvider,
    LoginRegHost,
    LoaderProvider,
    ArticleWebComponentHelper,
    IterablePlugin.IterableActivity,
    ArticlesTopAppBarInteractionHelper {

    @Inject
    lateinit var inlineVideoPlayerEvents: InlineVideoPlayerEvents

    @Inject
    lateinit var wapoConfigManager: WapoConfigManager

    @Inject
    lateinit var adService: AdService

    lateinit var sectionDisplayName: String

    private lateinit var binding: ActivityArticlesBinding

    private val intentHelper = IntentHelper()

    private lateinit var articleListViewModel: ArticleListViewModel

    private val readingHistoryViewModel: ReadingHistoryViewModel by viewModels()

    private lateinit var authorHelper: AuthorHelper

    private val destinationViewModel: Articles2DestinationViewModel by viewModels()

    private val audioFeatureArticleStateViewModel: AudioFeatureArticleStateViewModel by viewModels()

    private val pageViewTimeTrackerViewModel: PageViewTimeTrackerViewModel by viewModels()

    private val articlesPagerCollaborationViewModel: ArticlesPagerCollaborationViewModel by viewModels()

    private val giftCollaborationViewModel: GiftCollaborationViewModel by viewModels()

    private val articleWallHelperViewModel: ArticleWallHelperViewModel by viewModels()

    private val giftArticleRecipientViewModel: GiftArticleRecipientViewModel by viewModels()

    private val bottomCtaViewModel: BottomCtaViewModel by viewModels()

    private val articleTableOfContentsViewModel: ArticleTableOfContentsViewModel by viewModels()

    private val forYouActivityViewModel: ForYouActivityViewModel by viewModels()

    private val ellipsisHelperViewModel: EllipsisHelperViewModel by viewModels()

    private val playlistActivityViewModel: PlaylistActivityViewModel by viewModels()

    private val articles2ViewModel: Articles2ViewModel by viewModels()

    private val askThePostViewModel: AskThePostViewModel by viewModels()

    private val loginRegActivityViewModel: LoginRegActivityViewModel by viewModels()

    private val feedbackCollaborationViewModel: FeedbackCollaborationViewModel by viewModels()

    private val commentsViewModel: CommentsViewModel by viewModels()

    private val summaryCollaborationViewModel: ArticleSummaryCollaborationViewModel by viewModels()

    private val globalBannerViewModel: GlobalBannerViewModel by viewModels()

    private var isPushOriginated = false

    private val lowDataBannerViewModel: LowDataBannerViewModel by viewModels()

    private val userHistoryViewModel: UserHistoryViewModel by viewModels()

    private val videoActivityViewModel: VideoActivityViewModel by viewModels()
    private val adsContentViewModel: ContentViewModel by viewModels()
    private var isWebArticle: Boolean = false

    private var mapWallType: MapWallType? = null
    private var mapWallPrompt: Prompt? = null
    private var paywallPauseSessionActive = false
    private var wasAudioPlayingBeforePaywall = false

    private val paywallLifecycleCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentDetached(fm: FragmentManager, fragment: Fragment) {
            if (fragment is PaywallSheet2Fragment) {
                tryResumeAudioAfterPaywallDismiss(force = true)
            }
        }
    }

    private val config get() = ConfigManager.getInstance().config
    private val articleInlineMessageViewModel: ArticleInlineMessageViewModel by viewModels()

    private val iterableActivityViewModel: IterableActivityViewModel by viewModels()
    private lateinit var iterablePlugin: IterablePlugin

    private lateinit var talkToThePostBottomSheetFragmentFactory: TalkToThePostBottomSheetFragmentFactory
    private val isOpenedFromAudioReadArticle by lazy {
        intent?.getBooleanExtra(EllipsisMenuFragment.EXTRA_FROM_AUDIO_READ_ARTICLE, false) == true
    }

    private val recordAudioPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            val isFirstDenial = ActivityCompat.shouldShowRequestPermissionRationale(
                this@Articles2Activity,
                Manifest.permission.RECORD_AUDIO
            )
            if (isGranted) {
                talkToThePostBottomSheetFragmentFactory.tryToShowTalkFragment(
                    parentActivity = this@Articles2Activity,
                    parentFragmentManager = supportFragmentManager,
                    conversationId = askThePostViewModel.uiState.value.conversationId,
                    surfaceName = askThePostViewModel.askSamEntryPoint?.name?.lowercase(),
                    isFirstDenial = isFirstDenial
                )
            } else {
                talkToThePostBottomSheetFragmentFactory.showPermissionAlertDialog(
                    this,
                    isFirstDenial
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme()
        super.onCreate(savedInstanceState)
        iterablePlugin =
            IterablePlugin(this, iterableActivityViewModel, getPaywallSheetHelper()).also {
                lifecycle.addObserver(it)
            }
        supportFragmentManager.registerFragmentLifecycleCallbacks(paywallLifecycleCallbacks, false)
        binding = ActivityArticlesBinding.inflate(layoutInflater)
        binding.bottomSheetCta.initBehavior()
        binding.bottomSheetCta.setOnClickListener {
            bottomCtaViewModel.bottomCtaClicked()
        }
        bottomCtaViewModel.dispatchCtaType(BottomCtaType.NONE)
        setContentView(binding.root)

        val progressBarABVariant =
            PrefUtils.getABParametersMap(applicationContext)[ABTests.PROGRESS_BAR]
        if (progressBarABVariant == "variant") {
            binding.toolbar.visibility = View.GONE
            binding.composeToolbar.apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AndroidClassicTheme {
                        val screenState by articlesPagerCollaborationViewModel
                            .screenState
                            .observeAsState()

                        val currentPageContentState = screenState?.let {
                            it.activePages[it.currentPageId]
                        }

                        val latestCurrentPageContentState by articlesPagerCollaborationViewModel
                            .currentPageContentState
                            .observeAsState()

                        val audioDuration =
                            (latestCurrentPageContentState as? ArticleContentState.Success)
                                ?.article
                                ?.audio
                                ?.duration

                        val commentCount by commentsViewModel
                            .commentCount
                            .observeAsState()

                        val isCommentsAvailable by articlesPagerCollaborationViewModel
                            .currentPageCommentsAvailability
                            .observeAsState(false)

                        val currentPageSummaryAvailability by articlesPagerCollaborationViewModel
                            .currentPageSummaryAvailability
                            .observeAsState()

                        val isSummaryAvailable =
                            SummariesFeatureFlag.shouldEnableSummariesIcon(
                                currentPageSummaryAvailability?.first == true,
                                currentPageSummaryAvailability?.second == true,
                            )

                        val currentUrl = (currentPageContentState as? ArticleContentState.Success)
                            ?.article
                            ?.contenturl

                        LaunchedEffect(currentUrl) {
                            if (currentUrl != null) {
                                commentsViewModel.fetchCommentsData(currentUrl, isPushOriginated)
                            }
                        }

                        val scrollProgress by articlesPagerCollaborationViewModel.scrollProgress.collectAsStateWithLifecycle()

                        val savedArticleMeta by articleListViewModel.liveArticleByUrl.observeAsState()

                        val audioMediaConfig =
                            remember(currentPageContentState) { constructAudioMediaConfig() }
                        val playlistItem =
                            remember(audioMediaConfig) { audioMediaConfig?.toPlaylistAudio() }
                        val playlist by playlistActivityViewModel.playlist.observeAsState()

                        val isInPlaylist = remember(playlist, playlistItem) {
                            val id = playlistItem?.id ?: return@remember false
                            playlist.orEmpty().any { it.id == id }
                        }

                        ArticlesTopAppBar(
                            currentUrl = currentUrl,
                            audioDuration = audioDuration,
                            commentCount = commentCount,
                            isCommentsAvailable = isCommentsAvailable,
                            isSummaryAvailable = isSummaryAvailable,
                            scrollProgress = scrollProgress,
                            isSaved = savedArticleMeta != null,
                            isInPlaylist = isInPlaylist,
                            articlesTopAppBarInteractionHelper = this@Articles2Activity
                        )
                    }
                }
            }
        } else {
            binding.composeToolbar.visibility = View.GONE
            setSupportActionBar(binding.toolbar)
        }

        observePageChangeCollaboration()
        observePaywallEvents()
        observeBottomCta()
        observeAuthorFollowTriggerEvents()
        observeAskThePostTriggerEvent()
        observeContentLinkTypeChangeEvents()
        observeLinkClicks()
        observeLufOutcomePostClicks()
        observeContentState()
        observeImageTap()
        observeBookmarkTapEvents()
        observeGiftTapEvents()
        observeReadingHistorySaveEvents()
        observeRteTrackPageViewEvents()
        observeFirebaseAnalyticsTrackingEvents()
        observeArticleMetricsEvents()
        observePaywallVerificationEventsForAudio()
        observeArticleForPaywall()
        observeBottomCtaToPaywallAction()
        observeCurrentPageAudioAvailabilityEvent()
        observeCurrentPageSummaryAvailability()
        observeSummaryTooltipEvent()
        observeAudioClickEvent()
        observeGiftCollabEvent()
        observeEllipsisClick()
        observeAddToPlayListTapEvents()
        observeAddedToPlaylistEvent()
        observeAudioPlayerEllipsisClick()
        observeRemoveFromPlayListEvents()
        observeAudioCurrentPlayingEllipsisClick()
        observePlaylistClickTrackEvent()
        observeSummaryClickEvent()
        observeFeedbackLinkClickEvent()
        observeFeedbackSubmittedEvent()
        observeVideoClickEvent()
        observeAdsContentState()
        observeCommentCount()
        observeUserEvent()
        observeLowDataBannerEvent {
            lowDataBannerViewModel.updateLowDataBanner(false)
            AppPreferences.setIsLowDataModeEnabled(false)
            Measurement.trackLowDataModeOff(getCurrentTrackingInfo()?.pageName)
        }
        observeMapWallArticleFetch()
        observeInlineOfferDataUpdateEvent()
        observeBannerEvents()
        observeInlineOfferImpressionEvent()
        val articlesParcel = ArticlesParcel(intent)
        if (articlesParcel.getArticleMetas().isEmpty()) {
            // Closing activity when list is empty. Usually it happens when caller is not sending or
            // when ArticlesParcel lost articlesMetaMap value.
            finish()
            return
        }
        // In order to allow for 4 icons (Gift icon added) in Toolbar, design doesn't want to show
        // Actionbar Title in Articles2Activity. In all other Activities we show The Washington Post
        supportActionBar?.title = EMPTY_STRING
        val articlesList = articlesParcel.getArticleMetas()
        val pushTopic = articlesParcel.getPushTopic()
        sectionDisplayName = articlesParcel.getSectionDisplayName()
        val shouldPlayAudioArticle = intent.getBooleanExtra(SHOULD_PLAY_AUDIO_ARTICLE, false)
        val audioArticlePositionToPlay = intent.getIntExtra(ARTICLES_LIST_INDEX_OF_CLICKED, 0)
        setupCacheBypassFlag(articlesList, articlesParcel)
        destinationViewModel.startUp(
            articlesList,
            articlesParcel.getFrontArticleIndex(),
            pushTopic,
            shouldPlayAudioArticle,
            audioArticlePositionToPlay
        )
        // This is purely used for analytics purpose as we send the gift article info in analytics event irrespective of the status of the token validation
        articlesPagerCollaborationViewModel.isGiftArticle = articlesParcel.isGiftArticle()
        articlesPagerCollaborationViewModel.isNewsprint = articlesParcel.isNewsprint()
        articleListViewModel =
            FlagshipApplication.getInstance().savedArticleManager.getViewModel(
                this,
            )
        observeReadingListLiveArticleUpdates()
        authorHelper = AuthorHelper(this)

        // TableOfContents
        observeTableOfContentsViewEvents()

        setupOpeningAnimation()
        initializeTrackingDataWithTracker(articlesParcel)

        val pushId = articlesParcel.getPushId()
        val canTrackPushEvent = articlesParcel.isPushOriginated() && !pushId.isNullOrBlank()

        if (canTrackPushEvent) {

            val url = PrefUtils.getPushUrl(this, pushId)
            val type = PrefUtils.getPushType(this)
            val testGroup = PrefUtils.getTestGroup(this)
            val loginId = PaywallPrefHelper.getInstance(this).getLoginId(this)

            userHistoryViewModel.postPushEvent(
                url,
                pushId,
                type,
                testGroup,
                true,
                loginId
            )
        }
        observeFyFeedDataFetched()
        forYouActivityViewModel.fetchData(
            ForYouFeedRepositoryImpl.SURFACE_RECIRC,
            isPushOriginated,
            null,
            articleListViewModel.getCurrentPageUrl(),
        )
        observeScreenState()
        binding.newUpdates.setOnClickListener {
            binding.newUpdates.visibility = RecyclerView.GONE
            articlesPagerCollaborationViewModel.dispatchLufRefresh()
        }
        talkToThePostBottomSheetFragmentFactory = TalkToThePostBottomSheetFragmentFactory()
        observeOpenVideoEvent()
        observeBannerMessage()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (shouldNotSuppressPageView()) {
                    articlesPagerCollaborationViewModel.dispatchFirebaseAnalyticsTrackingEvent(
                        FirebaseAnalyticsTrackingEvent.BackPressTracking
                    )
                } else {
                    Measurement.setNavigationBehaviorInDefaultMap(Measurement.PATH_TO_VIEW_BACK_TO_FRONT)
                }
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })
    }

    private fun observeFyFeedDataFetched() {
        forYouActivityViewModel.feedDataReloaded.observe(this) {
            userHistoryViewModel.updateFYSessionId()
        }
    }

    private fun observeOpenVideoEvent() {
        videoActivityViewModel.openVideoEvent.observe(this) {
            val postTvVideos = listOf(it)
            val intent =
                VerticalVideosParcel
                    .Builder()
                    .setPostTvVideos(postTvVideos)
                    .setOffset(0)
                    .setPosition(0)
                    .setSourceScreen(sectionDisplayName)
                    .setVerticalVideosConfig(ConfigManager.getInstance().config.verticalVideosConfig)
                    .buildIntent(this)
            startActivity(intent)
        }
    }

    private fun observeBannerMessage() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                iterableActivityViewModel
                    .getBannerFlowForPlacement(IamMessageType.ARTICLE)
                    .collect { message ->
                        articles2ViewModel.updateArticleBanner(message)
                        if (message == null) {
                            articleInlineMessageViewModel.dispatchMessageUpdateEvent()
                        } else {
                            val offerData =
                                iterableActivityViewModel.getArticleInlineMessageFromMessages(this@Articles2Activity)
                            articleInlineMessageViewModel.setArticleInlineMessage(offerData)
                        }
                    }
            }
        }
    }

    private fun observeCommentCount() {
        commentsViewModel.commentCount.observe(this) { count ->
            val view = findViewById<TextView>(R.id.comments_count)
            view?.let {
                it.text = count
            }
        }
    }

    fun fetchElectionChildren(siteMapUrl: String) {
        wapoConfigManager.loadElectionConfig(
            binding.root.context,
            Constants.ConfigType.ELECTION_SUB_NAV_CONFIG,
            siteMapUrl,
        )
    }

    private fun observeScreenState() {
        articlesPagerCollaborationViewModel.screenState.observe(this) { screenState ->
            checkLiveUpdates(screenState)
        }
    }

    private fun checkLiveUpdates(screenState: ArticlesPagerCollaborationViewModel.ScreenState) {
        val articleContentState = screenState.activePages[screenState.currentPageId]
        if (articleContentState is ArticleContentState.Success && articleContentState.updatesCount > 0) {
            binding.newUpdates.text =
                if (articleContentState.updatesCount > 1) {
                    getString(
                        R.string.new_updates,
                        articleContentState.updatesCount.toString(),
                    )
                } else {
                    getString(R.string.new_update)
                }
            binding.newUpdates.visibility = View.VISIBLE
        } else {
            binding.newUpdates.visibility = View.GONE
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }

    override fun shouldCheckBackendHealth(): Boolean {
        if (ArticlesParcel(intent).isFailoverOriginated()) return false
        return super.shouldCheckBackendHealth()
    }

    /**
     * Initializes [FirebaseTrackingHelperData] so that it can be used later (when article is loaded)
     * in [FirebaseAnalyticsTracker].
     */
    private fun initializeTrackingDataWithTracker(articlesParcel: ArticlesParcel) {
        val firebaseTrackingHelperData =
            FirebaseTrackingHelperData(
                omnitureToPathView = articlesParcel.getOmnitureToPathView(),
                widgetData =
                    FirebaseTrackingHelperWidgetData(
                        isWidgetOriginated = articlesParcel.isWidgetOriginated(),
                        intent.getStringExtra(
                            WidgetData.EXTRAS_WIDGET_TYPE,
                        ),
                    ),
                isPushOriginated = articlesParcel.isPushOriginated(),
                isPrintOriginated = articlesParcel.isPrintOriginated(),
                isCarouselOriginated = articlesParcel.isCarouselOriginated(),
                isDeeplinkOriginated = articlesParcel.isDeepLinkOriginated(),
                isForYouSectionOriginated = articlesParcel.isForYouSectionOriginated(),
                isAirshipOriginated = articlesParcel.isAirshipOriginated(),
                isOneLinkOriginated = articlesParcel.isOneLinkOriginated(),
                isHabitTilesOriginated = articlesParcel.isHabitTilesOriginated(),
                isAskThePostOriginated = articlesParcel.isAskThePostOriginated(),
                isWpmmArticle = articlesParcel.isWpmmArticle(),
                isAlertOriginated = articlesParcel.isAlertPageOriginated(),
                sectionDisplayName = articlesParcel.getSectionDisplayName(),
                currentTabName = articlesParcel.getTabName(),
                firstSelectedIndex = articlesParcel.getFrontArticleIndex(),
                navigationBehavior = articlesParcel.getNavigationBehavior(),
                pushArticleTrackingHelperData =
                    PushArticleTrackingHelperData(
                        articlesParcel.getPushTitle(),
                        articlesParcel.getPushTopicPlatform(),
                        articlesParcel.getPushSentTimestamp(),
                        articlesParcel.getPushId(),
                        articlesParcel.getPushHeadline(),
                    ),
                isInlineLinkOriginated = articlesParcel.isInlineLinkOriginated(),
                isLufOutcomePostOriginated = articlesParcel.isLufOutcomePostOriginated(),
                positionInCarousel = articlesParcel.getPositionInBrights(),
                positionInMyPostCarousel = articlesParcel.getPositionInMyPostCarousel(),
                positionInForYouSection = articlesParcel.getPositionInForYouSection(),
                itId = articlesParcel.getItId(),
                sourceApp = articlesParcel.getSourceApp(),
                carouselTitle = articlesParcel.getCarouselTitle(),
                carouselCategoryId = articlesParcel.getCarouselCategoryId(),
                isOpenFromSearch = articlesParcel.isOpenedFromSearch(),
                isDefaultForYou = articlesParcel.isDefaultForYou(),
                isFromRelatedArticle = articlesParcel.getIsFromRelatedArticle(),
                isFromRecircModule = articlesParcel.isRecircModuleOriginated(),
            )
        articlesPagerCollaborationViewModel.firebaseAnalyticsTracker =
            FirebaseAnalyticsTracker(
                firebaseTrackingHelperData = firebaseTrackingHelperData
            )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                articlesPagerCollaborationViewModel.articleEvent.collect { event ->
                    when (event) {
                        is Article2Events.Article2TrackerEvent -> {
                            articleWallHelperViewModel.trackPageView(
                                omnitureData = event.omniture,
                                positionData = event.position,
                                currentAppSectionData = event.currentAppSection,
                                currentAppTabData = event.currentAppTab,
                                pushTrackingHelperData = event.pushTrackingHelperData,
                                measurementMap = event.measurementMap
                            )
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    private fun observeGiftCollabEvent() {
        giftCollaborationViewModel.giftCollabEvent.observe(this) {
            when (it) {
                GiftCollabEvent.DismissDelayedPaywall -> articleWallHelperViewModel.dispatchStopDelayedPaywall()
                GiftCollabEvent.Paywall ->
                    articleWallHelperViewModel.dispatchShowPaywallNow(
                        PaywallConstants.WallType.GIFT_SENDER_NO_SUB,
                    )

                GiftCollabEvent.ShowDelayPaywall -> articleWallHelperViewModel.dispatchShowPaywallDelayed()
                GiftCollabEvent.SignIn ->
                    articleWallHelperViewModel.logOutUser {
                        PaywallService.getConnector().showSignInScreen(
                            supportFragmentManager,
                            AuthIntentBuilder().build(),
                            null,
                            PaywallConstants.WallType.BOTTOM_CTA_GIFT_PAYWALL,
                            true,
                            null
                        )
                    }
            }
        }
    }

    private fun observeImageTap() {
        articlesPagerCollaborationViewModel.imageTapEvent.observe(
            this,
            Observer {
                val isNightMode =
                    (applicationContext as? NightModeController)?.isNightModeEnabled() ?: false
                val imagerUrl: String =
                    if (isNightMode && !it.darkModeImageUrl.isNullOrEmpty()) it.darkModeImageUrl else it.imageURL
                        ?: ""
                val intent = Intent(this, NativePhotoActivity::class.java)
                intent.putExtra(NativePhotoActivity.photoUrl, imagerUrl)
                intent.putExtra(NativePhotoActivity.photoCaption, it.fullCaption)
                startActivity(intent)
            },
        )
    }

    private fun observeLinkClicks() {
        articlesPagerCollaborationViewModel.linkClicked.observe(
            this,
            Observer {
                onLinkClicked(it)
            },
        )
    }

    private fun observeLufOutcomePostClicks() {
        articlesPagerCollaborationViewModel.lufOutcomePostClicked.observe(
            this,
            Observer {
                onLufOutcomePostClicked(it)
            },
        )
    }

    private fun observeContentState() {
        articlesPagerCollaborationViewModel.currentPageContentState.observe(this) {
            when (it) {
                is ArticleContentState.Success -> {
                    // Gift Recipient handling used to be done here. Now it's managed by
                    // ArticleWallHelperViewModel
                    giftCollaborationViewModel.trackingInfo = it.article.omniture
                    askThePostViewModel.analyticsData = Pair(
                        askThePostViewModel.analyticsData.first,
                        it.article.omniture?.toTrackingInfo()
                    )
                }

                else -> showOrHideActionIconTooltip(false)
            }
        }
    }

    private fun observePageChangeCollaboration() {
        articlesPagerCollaborationViewModel.currentPage.observe(
            this,
            Observer {
                Logger.d(
                    "PageChanged",
                    "URL - ${it.articleMeta.id}, position - ${it.position}, type - ${it.articleMeta.articleLinkType.name} ",
                )
                dismissMapWall(MapDismissOrigin.SYSTEM)
                FlagshipApplication.getInstance().videoManager2.releaseAllVideos()
                checkForUserBehaviorTrackingRequirement(it)
                startTimerForSavingToReadingHistory(getUrlWithoutParameters(it.articleMeta.id))
                val url = getUrlWithoutParameters(it.articleMeta.id)
                val jTid =
                    if (pageViewTimeTrackerViewModel.articleTimeStamp.articleUrl == url) {
                        pageViewTimeTrackerViewModel.articleTimeStamp.timeStamp
                    } else {
                        null
                    }
                giftCollaborationViewModel.giftArticleUrl = it.articleMeta.id
                dispatchFirebaseAnalyticsTrackingEvent(
                    getUrlWithoutParameters(it.articleMeta.id),
                    jTid,
                )
                audioFeatureArticleStateViewModel.selectCurrentArticleUrl(
                    getUrlWithoutParameters(it.articleMeta.id),
                )
                articleWallHelperViewModel.updateScrollDepth(0)
                mapWallType = null
                mapWallPrompt = null
            },
        )
    }

    /**
     * Check whether or not to dispatch the tracking user behavior even.
     * [it] is [ArticlePage] info for the article that is currently being viewed by user.
     */
    private fun checkForUserBehaviorTrackingRequirement(it: ArticlePage) {
        val key = getUrlWithoutParameters(it.articleMeta.id)
        val userBehaviorTrackingModel =
            articlesPagerCollaborationViewModel.trackingMap[key]
        userBehaviorTrackingModel?.let { trackingModel ->
            if (!trackingModel.hasBeenTracked) {
                trackingModel.sourceSection?.let { sourceSection ->
                    articlesPagerCollaborationViewModel.trackingMap[key] =
                        userBehaviorTrackingModel.copy(
                            articleId = userBehaviorTrackingModel.articleId,
                            sourceSection = userBehaviorTrackingModel.sourceSection,
                            hasBeenTracked = true,
                        )
                    articlesPagerCollaborationViewModel.dispatchTrackUserBehaviorEvent(
                        sourceSection,
                    )
                }
            }
        }
    }

    /**
     * Starts the timer of 5 sec as soon as the page change event is dispatched.
     */
    private fun startTimerForSavingToReadingHistory(url: String) {
        articlesPagerCollaborationViewModel.startTimer(url)
    }

    /**
     * Start observing all firebase analytics tracking events.
     */
    private fun observeFirebaseAnalyticsTrackingEvents() {
        articlesPagerCollaborationViewModel.firebaseAnalyticsTrackingEvent.observe(this) {
            articlesPagerCollaborationViewModel.startFirebaseAnalyticsTracking(it, this)
        }
    }

    /**
     * Dispatches tracking event for firebase analytics.
     * [url] url/id of the article in current page.
     */
    private fun dispatchFirebaseAnalyticsTrackingEvent(
        url: String,
        jTid: Long?,
    ) {
        articlesPagerCollaborationViewModel.firebaseAnalyticsTrackingMap[url]?.let {
            articlesPagerCollaborationViewModel.dispatchFirebaseAnalyticsTrackingEvent(
                FirebaseAnalyticsTrackingEvent.ArticleTracking(it, jTid),
            )
        }
    }

    private fun observePaywallEvents() {
        articleWallHelperViewModel.paywallEvent.observe(this) {
            when (it) {
                is WallUiEvent.ShowPaywall -> {
                    onPaywallWillShow()
                    showWallDialog(
                        PaywallService.getInstance().isWpUserLoggedIn,
                        PaywallConstants.getWallReason(
                            it.type,
                        ),
                        it.type,
                    )
                }

                is WallUiEvent.ShowPaywallByName -> {
                    onPaywallWillShow()
                    showWallDialog(
                        PaywallService.getInstance().isWpUserLoggedIn,
                        PaywallConstants.getWallReason(
                            PaywallConstants.WallType.METERED_PAYWALL,
                        ),
                        PaywallConstants.WallType.METERED_PAYWALL,
                        it.wallName,
                    )
                }

                is WallUiEvent.ShowGiftWall -> handleGiftWall(it.giftState)
                is WallUiEvent.ShowRegwall -> {
                    onPaywallWillShow()
                    showWallDialog(
                        PaywallService.getInstance().isWpUserLoggedIn,
                        PaywallConstants.getWallReason(
                            PaywallConstants.WallType.REGWALL,
                        ),
                        PaywallConstants.WallType.REGWALL,
                        it.wallName,
                    )
                }

                is WallUiEvent.ShowSoftwall -> {
                    onPaywallWillShow()
                    showWallDialog(
                        PaywallService.getInstance().isWpUserLoggedIn,
                        PaywallConstants.getWallReason(PaywallConstants.WallType.SOFTWALL),
                        PaywallConstants.WallType.SOFTWALL,
                        it.wallName,
                    )
                }

                is WallUiEvent.ShowMapWall -> {
                    if (articleWallHelperViewModel.canShowMapWall(it.mapWallPrompt)) {
                        mapWallType = it.mapWallType
                        mapWallPrompt = it.mapWallPrompt
                        observeMapWallScrollDepth()
                        articleWallHelperViewModel.fetchNewMapArticle(
                            forYouActivityViewModel,
                            isPushOriginated,
                            articleListViewModel.getCurrentPageUrl(),
                            false,
                        )
                    }
                }

                is WallUiEvent.StartDelayedWall -> {
                    FlagshipApplication.getInstance().paywallOmniture.setArcId(
                        it.trackingInfo?.arcId,
                    )
                    val pageName = it.trackingInfo?.pageName ?: it.articleStub.title
                    val arcId = it.trackingInfo?.arcId ?: it.trackingInfo?.contentId
                    Measurement.setPaywallArticle(pageName, arcId)
                    val articlesParcel = ArticlesParcel(intent)
                    if (it.passReferrer) {
                        it.articleStub.referrer = articlesParcel.getReferrer()
                        it.articleStub.tetroUtm = articlesParcel.getTetroUtm()
                    }
                    articleWallHelperViewModel.getDelayedTetroMetering(
                        it.articleStub,
                        getPaywallType(),
                        articlesParcel.getGiftToken(),
                    )
                }

                WallUiEvent.StopDelayedWall -> articleWallHelperViewModel.stopDelayedWall()
                WallUiEvent.DismissWall -> {
                    dismissWall()
                    tryResumeAudioAfterPaywallDismiss()
                }
            }
        }
    }

    private fun observeBottomCta() {
        bottomCtaViewModel.getSubText().observe(this) {
            binding.bottomSheetCta.setGiftText(it)
        }
        bottomCtaViewModel.ctaType.observe(this) {
            binding.bottomSheetCta.setCtaType(it)
        }
        bottomCtaViewModel.getSubStateLiveData().observe(this) {
            binding.bottomSheetCta.visibility =
                when (it) {
                    SubState.ActiveSub, SubState.PausedSub -> {
                        View.GONE
                    }

                    else -> {
                        View.VISIBLE
                    }
                }
        }
    }

    private fun observeAskThePostTriggerEvent() {
        articlesPagerCollaborationViewModel.askThePostClicked.observe(
            this,
            {
                if (it != null) {
                    onATPTextClicked(it)
                }
            },
        )
    }

    private fun observeAuthorFollowTriggerEvents() {
        articlesPagerCollaborationViewModel.authorClicked.observe(
            this,
            Observer {
                if (it != null) {
                    onAuthorClicked(it)
                }
            },
        )
    }

    private fun observeTableOfContentsViewEvents() {
        articleTableOfContentsViewModel.showTableOfContentsEvent.observe(this) Observer@{
            val fragmentTag = ArticleTableOfContentsFragment.TAG
            val fragment = supportFragmentManager.findFragmentByTag(fragmentTag)
            if (it) {
                if (fragment == null || !fragment.isVisible) {
                    ArticleTableOfContentsFragment().show(supportFragmentManager, fragmentTag)
                }
            } else {
                (fragment as? DialogFragment)?.dismiss()
            }
        }
    }

    private fun observeContentLinkTypeChangeEvents() {
        articlesPagerCollaborationViewModel.contentLinkType.observe(
            this,
            Observer {
                articlesPagerCollaborationViewModel.currentPage.value?.let {
                    it.articleMeta.id?.let { id ->
                        articleListViewModel.setCurrentPageUrl(getUrlWithoutParameters(id))
                    }
                }
            },
        )
    }

    private fun observeReadingListLiveArticleUpdates() {
        articleListViewModel.liveArticleByUrl.observe(
            this,
            Observer {
                articlesPagerCollaborationViewModel.contentLinkType.value?.let { linkType ->
                    articlesPagerCollaborationViewModel.setToolbarIconsState(it != null, linkType)
                    invalidateOptionsMenu()
                    Logger.d("PageChanged ReadingList", "${it != null}, contentLinkType=$linkType")
                }
            },
        )
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val url =
            articlesPagerCollaborationViewModel.currentPage.value
                ?.articleMeta
                ?.id
        when (articlesPagerCollaborationViewModel.toolbarIconsState) {
            ArticleToolbarIconsState.NonNative -> {
                menuInflater.inflate(R.menu.article_non_native, menu)

                isWebArticle = true
                if (url?.toUri()?.path?.contains("/my-post") == true) { // hide Gift option for /my-post/
                    menu
                        ?.children
                        ?.firstOrNull {
                            it.title ==
                                    resources.getString(
                                        R.string.article_action_gift,
                                    )
                        }?.isVisible = false
                }
            }

            ArticleToolbarIconsState.Native -> {
                isWebArticle = false
                menuInflater.inflate(
                    R.menu.article_native,
                    menu,
                )
                url?.let {
                    commentsViewModel.fetchCommentsData(it, isPushOriginated)
                }
            }
        }
        // Enable Audio action based on audio availability
        if (articlesPagerCollaborationViewModel.currentPageAudioAvailability.value != true) {
            disableMenuItem(menu, R.id.action_audio_play)
        }
        // Add/Remove Summary action based on summary availability
        val summariesEnabled =
            SummariesFeatureFlag.shouldEnableSummariesIcon(
                articlesPagerCollaborationViewModel.currentPageSummaryAvailability.value?.first == true,
                articlesPagerCollaborationViewModel.currentPageSummaryAvailability.value?.second == true,
            )
        menu
            ?.findItem(R.id.action_summary)
            ?.setVisible(summariesEnabled)

        // Add/Remove Comments action based on feeds
        menu
            ?.findItem(R.id.action_comment)
            ?.setVisible(
                articlesPagerCollaborationViewModel.currentPageCommentsAvailability.value == true,
            )?.actionView?.setOnClickListener {
                onCommentActionClicked()
            }

        return true
    }

    private fun disableMenuItem(
        menu: Menu?,
        itemId: Int,
    ) {
        val item = menu?.findItem(itemId)
        item?.isEnabled = false
        item?.icon?.tint(ContextCompat.getColor(applicationContext, R.color.menu_item_disabled))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // This url is used as a key to find an appropriate live-data from the live map to take the action.
        val url =
            articlesPagerCollaborationViewModel.currentPage.value
                ?.articleMeta
                ?.id ?: ""
        when (item.itemId) {
            android.R.id.home -> {
                // Respond to the action bar's Up/Home button
                onBackPressedDispatcher.onBackPressed()
                return true
            }

            R.id.action_share -> {
                articlesPagerCollaborationViewModel.initToolbarAction(
                    url,
                    ActionsOnIndividualArticles.ActionShare,
                )
            }

            R.id.action_bookmark, R.id.action_bookmark_checked -> {
                Measurement.setNavigationBehavior(NavigationBehavior.SAVE_CLICK)
                if (!paywallBookMark()) {
                    articlesPagerCollaborationViewModel.initToolbarAction(
                        url,
                        ActionsOnIndividualArticles.ActionBookmarkClick,
                    )
                }
            }

            R.id.action_gift -> {
                articlesPagerCollaborationViewModel.initToolbarAction(
                    url,
                    ActionsOnIndividualArticles.ActionGift,
                )
                Measurement.setNavigationBehavior(NavigationBehavior.GIFT_SEND_DIALOG)
            }

            R.id.action_ellipsis ->
                articlesPagerCollaborationViewModel.initToolbarAction(
                    url,
                    ActionsOnIndividualArticles.ActionEllipsisClick,
                )

            R.id.action_summary -> {
                if (!paywallSummary()) {
                    articlesPagerCollaborationViewModel.initToolbarAction(
                        url,
                        ActionsOnIndividualArticles.ActionSummaryClick,
                    )
                }
            }

            R.id.action_comment -> {
                onCommentActionClicked()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onCommentActionClicked() {
        val fbTrackingInfo = articlesPagerCollaborationViewModel.getCurrentTrackingInfo()
        fbTrackingInfo?.let {
            articlesPagerCollaborationViewModel.dispatchFirebaseAnalyticsTrackingEvent(
                FirebaseAnalyticsTrackingEvent.ViewCommentsTracking(it),
            )
        }
        val arcId = fbTrackingInfo?.omnitureX?.arcId
            ?: articlesPagerCollaborationViewModel.currentPage.value?.articleMeta?.id ?: ""
        val storyUrl = articleListViewModel.getCurrentPageUrl()
        val articleTitle =
            articlesPagerCollaborationViewModel.firebaseAnalyticsTrackingMap[arcId]?.title
                ?: articlesPagerCollaborationViewModel.firebaseAnalyticsTrackingMap[storyUrl]?.title
        val trackingInfo = fbTrackingInfo?.let {
            it.omnitureX?.toTrackingInfo()?.apply {
                contentURL = it.contentUrl
            }
        }
        CommentBottomSheetFragment.showComments(
            supportFragmentManager,
            arcId,
            storyUrl,
            articleTitle,
            "top-article",
            trackingInfo
        )
    }

    /**
     * When Ellipsis is clicked opening the Ellipsis Menu fragment in the Articles2
     */

    private fun observeEllipsisClick() {
        ellipsisHelperViewModel.ellipsisClickEvent.observe(
            this,
            Observer { article ->
                // hide button if url is in audio off list
                val isAudioDisabled = audioManager.isAudioDisabled(article.url)
                EllipsisMenuFragment
                    .create(
                        article.menuType,
                        "",
                        articlesPagerCollaborationViewModel.currentPageAudioAvailability.value == true && !isAudioDisabled,
                        article.url,
                    ).show(supportFragmentManager, EllipsisMenuFragment.tag)
            },
        )
    }

    /**
     * When Add to playlist is clicked from the Articles Ellipsis menu updating the Database
     */
    private fun observeAddToPlayListTapEvents() {
        audioMediaActivityViewModel.addPlaylistArticleEvent.observe(this) {
            val audioMediaConfig = constructAudioMediaConfig()
            playlistActivityViewModel.addToPlaylist(audioMediaConfig?.toPlaylistAudio())
            Measurement.trackActionButtonAddToPlaylist(
                audioMediaConfig?.sectionName,
                audioMediaConfig?.arcId,
            )
        }
    }

    /**
     * Observing events for the given playlist audio is added [PlaylistActivityViewModel.addToPlaylist] to the DB.
     * Right now the event is dispatched only for the successful cases. No events for failures.
     */
    private fun observeAddedToPlaylistEvent() {
        playlistActivityViewModel.addedToPlaylistEvent.observe(this) {
            addToPlaylistSnackbar()
        }
    }

    /**
     * Getting the media object of current article and them mapping to Playlist mapper in observeAddToPlayListTapEvents()
     */
    private fun constructAudioMediaConfig(): AudioMediaConfig? {
        val contentState = articlesPagerCollaborationViewModel.currentPageContentState.value
            ?: articlesPagerCollaborationViewModel.articleContentState.value
        return if (contentState is ArticleContentState.Success) {
            val article = contentState.article
            val audio = article.audio
            audio?.toAudioMediaConfig(
                article = article,
                audioTracker =
                    articlesPagerCollaborationViewModel.getAudioTracker(
                        false,
                        AudioPreferences.getAudioPlaybackSpeed(this),
                    ),
            )
        } else {
            null
        }
    }

    /**
     * When User clicks Add to playlist, Popping up custom Bottom sheet snack-bar for the add Playlist confirmation
     */

    private fun addToPlaylistSnackbar() {
        val inflater = LayoutInflater.from(this)
        val customAudioSnackbarBinding = CustomePlaylistSnackbarBinding.inflate(inflater)
        val snackBar =
            Snackbar
                .make(
                    binding.mainContent,
                    "",
                    Snackbar.LENGTH_LONG,
                ).setDuration(5000)
        val snackBarView = snackBar.view
        snackBarView.setPadding(0, 0, 0, 0)
        customAudioSnackbarBinding.playlistDescription.setOnClickListener {
            snackBar.dismiss()
            Measurement.trackActionButtonArticleAddToPlaylist()
            val intent = IntentHelper.getMainActivityIntent(this)
            intent.putExtra(
                EXTRAS_SECTION_URL,
                "https://www.washingtonpost.com/tablet/listen-to-the-post/",
            )
            intent.setAction(ACTION_OPEN_SECTION)
            startActivity(intent)
        }
        customAudioSnackbarBinding.snackbarClose.setOnClickListener {
            snackBar.dismiss()
        }
        (snackBarView as Snackbar.SnackbarLayout).addView(customAudioSnackbarBinding.root, 0)
        (snackBarView).getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT
        snackBar.show()
    }

    /**
     * When user is clicking the Ellipsis icon from the Current playing view from playlist Adapter
     */
    private fun observeAudioPlayerEllipsisClick() {
        audioMediaActivityViewModel.audioPlayerEllipsisClickEvent.observe(
            this,
            Observer { article ->
                ellipsisHelperViewModel.handleEllipsisClick(
                    article.toPlaylistAudio()!!.toEllipsisActionItem(
                        menuType = EllipsisMenu.AudioPlayerEllipsisButton,
                    ),
                )
            },
        )
    }

    /**
     * When user is clicking on the Remove from playlist from the ellipsis button of current playing view updating the Database to delete the audio record
     */
    private fun observeRemoveFromPlayListEvents() {
        audioMediaActivityViewModel.removePlayListEvent.observe(this) {
            playlistActivityViewModel.removePlaylistByIdAudio(it)
        }
    }

    /**
     * When user is clicking the Ellipsis icon from the Now playing view from playlist Adapter
     */
    private fun observeAudioCurrentPlayingEllipsisClick() {
        audioMediaActivityViewModel.audioPlayerCurrentPlayingEllipsisEvent.observe(
            this,
            Observer { article ->
                ellipsisHelperViewModel.handleEllipsisClick(
                    article.toPlaylistAudio()!!.toEllipsisActionItem(
                        menuType = EllipsisMenu.AudioPlayerEllipsisButton,
                    ),
                )
            },
        )
    }

    private fun observePlaylistClickTrackEvent() {
        playlistActivityViewModel.playlistClickTrackEvent.observe(this) {
            Measurement.trackPlayListButtonClick("")
        }
    }

    private fun proceedToSummaryScreen(
        summary: Summary?,
        article2: Article2,
    ) {
        val contentId = article2.arcId ?: return
        val contentUrl = article2.contenturl
        val lmt = article2.lmt ?: 0
        val articleMeta =
            com.wapo.flagship.features.aixp.models
                .ArticleMeta(contentId, contentUrl, lmt)
        val articleSummary =
            summary?.run {
                ArticleSummary(
                    url = url,
                    title = null,
                    disclaimer = disclaimer,
                    keyPointsHeading = keyPointsHeading,
                    overview = this.summary,
                    keyPoints =
                        keyPoints
                            ?.filter { it?.isEmpty()?.not() == true },
                    modelId = modelId,
                )
            }
        summaryCollaborationViewModel.apply {
            setArticleMeta(articleMeta)
            setForYouSummary(articleSummary)
            enableFeedback(
                SummariesFeatureFlag.realtimeFeedbackEnabled()
            )
        }
        SummaryFragment().show(supportFragmentManager, SUMMARY_TAG)
    }

    private fun proceedToSummaryFeedbackScreen() {
        FeedbackFragment(FeedbackFragment.FeedbackType.ARTICLE_SUMMARIES).show(
            supportFragmentManager,
            FEEDBACK_TAG
        )
    }

    private fun proceedToSummaryFeedbackStatusScreen() {
        FeedbackStatusFragment().show(supportFragmentManager, FEEDBACK_STATUS_TAG)
    }

    private fun observeSummaryClickEvent() {
        articlesPagerCollaborationViewModel.summaryClickEvent.observe(this) { article2 ->
            proceedToSummaryScreen(article2.summary, article2)
            updateSummaryTooltipEventState(true)
            dispatchArticleSummaryEvent(article2, summaryScreenSeen = true)
        }
    }

    private fun observeFeedbackLinkClickEvent() {
        feedbackCollaborationViewModel.feedbackLinkClickEvent.observe(this) {
            dispatchArticleSummaryEvent(
                articlesPagerCollaborationViewModel.summaryClickEvent.value,
                feedbackScreenSeen = true,
            )
            proceedToSummaryFeedbackScreen()
        }
    }

    private fun observeFeedbackSubmittedEvent() {
        feedbackCollaborationViewModel.feedbackSubmittedEvent.observe(this) {
            FeedbackStatusFragment(FeedbackFragment.FeedbackType.POST_ANSWERS).show(
                this.supportFragmentManager,
                FEEDBACK_STATUS_TAG,
            )
            Measurement.trackShowSubmitFeedback(
                articlesPagerCollaborationViewModel.getCurrentTrackingInfo()?.omnitureX?.toTrackingInfo(),
                true
            )
        }
    }

    private fun isLowDataBannerVisible(
        composeView: ComposeView,
        isVisible: Boolean,
        onTurnOffClick: () -> Unit,
    ) {
        if (!isWebArticle) {
            composeView.apply {
                setContent {
                    LowDataBannerView(
                        isVisible = isVisible,
                        onTurnOffClick = onTurnOffClick,
                    )
                }
            }
        }
    }

    private fun observeLowDataBannerEvent(onChangeClick: () -> Unit) {
        lowDataBannerViewModel.lowDataBannerState.observe(this) {
            isLowDataBannerVisible(binding.bannerView, it.isLowDataBannerEnable, onChangeClick)
            audioMediaActivityViewModel.setIsLowDataMode(it.isLowDataBannerEnable)
        }
    }

    private fun dispatchArticleSummaryEvent(
        article2: Article2?,
        summaryScreenSeen: Boolean = false,
        feedbackScreenSeen: Boolean = false,
        feedbackSubmit: Boolean = false,
    ) {
        article2 ?: return
        articlesPagerCollaborationViewModel.firebaseAnalyticsTrackingMap[
            getUrlWithoutParameters(
                article2.contenturl,
            ),
        ]?.let {
            articlesPagerCollaborationViewModel.dispatchFirebaseAnalyticsTrackingEvent(
                FirebaseAnalyticsTrackingEvent.ArticleSummaryTracking(
                    firebaseTrackingInfo = it,
                    summaryScreenSeen = summaryScreenSeen,
                    feedbackScreenSeen = feedbackScreenSeen,
                    feedbackSubmit = feedbackSubmit,
                    miscellanySuffix = MISCELLANY_SUFFIX_REALTIME,
                ),
            )
        }
    }

    private fun paywallAudio(): Boolean {
        val showPaywall =
            !PaywallService.getInstance().isPremiumUser &&
                    !PaywallService.getInstance().isFreeArticlesUser &&
                    !articleWallHelperViewModel.isValidGift
        (
                _categoryName == null ||
                        PaywallService.getInstance().isAtLimit(
                            _categoryName,
                            _article,
                        )
                )

        if (showPaywall) {
            val wallType = WallType.METERED_PAYWALL
            val wallReason = PaywallConstants.getWallReason(wallType)
            onPaywallWillShow()
            // Cancel the delayed article-wall job so it does not emit DismissWall
            // and close the inline audio paywall right after opening.
            articleWallHelperViewModel.dispatchStopDelayedPaywall()
            // Use the sheet helper directly to avoid triggering the generic wall path
            // that clears the active background audio session.
            getPaywallSheetHelper().showWall(
                wallType = wallType,
                wallReason = wallReason,
            )
        }

        return showPaywall
    }

    private fun paywallBookMark(): Boolean =
        when {
            !PaywallService.getInstance().isPremiumUser -> {
                articleWallHelperViewModel.dispatchShowPaywallNow(
                    PaywallConstants.WallType.SAVE_REGWALL,
                )
                true
            }

            !PaywallService.getInstance().isWpUserLoggedIn -> {
                reminderScreenFragment =
                    showReminderScreen(
                        ReminderScreenFragment.ReminderType.IAP_REGISTRATION_ASK_REMINDER,
                        true,
                    )
                true
            }

            else -> false
        }

    private fun paywallSummary(): Boolean {
        val showPaywall =
            !PaywallService.getInstance().isPremiumUser &&
                    !PaywallService.getInstance().isFreeArticlesUser &&
                    !articleWallHelperViewModel.isValidGift
        (
                _categoryName == null ||
                        PaywallService.getInstance().isAtLimit(
                            _categoryName,
                            _article,
                        )
                )

        if (showPaywall) {
            articleWallHelperViewModel.dispatchShowPaywallNow(
                PaywallConstants.WallType.METERED_PAYWALL,
            )
        }

        return showPaywall
    }

    fun setTheme() {
        val id: Int =
            if (FlagshipApplication.getInstance().nightModeManager.immediateNightModeStatus) {
                R.style.ArticlesActivityTheme_Night
            } else {
                R.style.ArticlesActivityTheme
            }
        super.setTheme(id)
    }

    /*
        All the callbacks that are coming from PostTvActivity are handled below
     */
    @Deprecated("Deprecated in Java")
    override fun onTrackingEvent(
        type: TrackingType,
        video: Video,
        value: Any?,
    ) {
        val valueMap = mutableMapOf(VideoTracker2.TRACKING_VALUE to value as? Int)
        inlineVideoPlayerEvents.trackStandardVideoEvent(
            type,
            video,
            valueMap,
            articlesPagerCollaborationViewModel.getCurrentTrackingInfo()
        )
    }

    /**
     * All the callbacks that are coming from PostTvPlayer2 are handled below
     */
    override fun onTrackingEvent(
        videoType: VideoTracker2.VideoType,
        type: TrackingType,
        video: Video,
        value: Any?,
    ) {
        val valueMap = value as MutableMap<*, *>
        inlineVideoPlayerEvents.trackStandardVideoEvent(
            type,
            video,
            valueMap,
            articlesPagerCollaborationViewModel.getCurrentTrackingInfo()
        )
    }

    override fun shareVideo(
        headline: String?,
        shareUrl: String?,
    ) {
        FlagshipApplication.getInstance().videoManager.pausePlay(false)
        FlagshipApplication.getInstance().videoManager.isBeingShared = true
        inlineVideoPlayerEvents.shareVideo(headline, shareUrl, this)
    }

    override fun startPIP(mVideo: Video?) {
        inlineVideoPlayerEvents.startPIP(mVideo, this)
    }

    override fun startPIP(
        video: Video?,
        playerName: String?,
    ) {
        val intent =
            FullScreenVideoParcel
                .Builder()
                .setVideo(video)
                .setPlayerName(playerName)
                .setMode(FullScreenVideoActivity.LaunchMode.PIP)
                .buildIntent(this)
        startActivity(intent)
    }

    override fun startFullScreen(video: Video, playerName: String?) {

    }

    override fun removeFragment(
        fragment: Fragment?,
        shouldSaveState: Boolean,
    ) {
        inlineVideoPlayerEvents.removeFragment(fragment, shouldSaveState, supportFragmentManager)
    }

    override fun openWeb(url: String?) {
        inlineVideoPlayerEvents.openWeb(url, this)
    }

    override fun logVideoError(eventLogBuilder: EventLog.Builder?) {
        // log non n/w errors
        val isNetworkError = !isConnectedOrConnecting(applicationContext)
        if (eventLogBuilder != null && !isNetworkError) {
            RemoteLog.e(applicationContext, eventLogBuilder.build())
        }
    }

    override fun addFragment(
        viewID: Int,
        fragment: Fragment,
        shouldSaveState: Boolean,
    ) {
        inlineVideoPlayerEvents.addFragment(
            viewID,
            fragment,
            shouldSaveState,
            supportFragmentManager,
        )
    }

    override fun isPIPEnabled(): Boolean = inlineVideoPlayerEvents.isPIPEnabled(this)

    override fun onPause() {
        super.onPause()
        pauseTracker()
        articlesPagerCollaborationViewModel.pausedVideo =
            FlagshipApplication.getInstance().videoManager.playingVideo
        if (!FlagshipApplication.getInstance().videoManager.isBeingShared) {
            FlagshipApplication.getInstance().videoManager.release()
        }

        dismissMapWall(MapDismissOrigin.SYSTEM)
    }

    override fun onResume() {
        super.onResume()
        bottomCtaViewModel.update()
        // Handle case where regwall is dismissed. Potentially may need to call tetro again.
        articleWallHelperViewModel.handleRegwallDismiss()
        Measurement.resumeCollection(this)
        articlesPagerCollaborationViewModel.pausedVideo?.let {
            FlagshipApplication.getInstance().videoManager.initMedia(it)
            articlesPagerCollaborationViewModel.pausedVideo = null
        }
        updateLowDataMode()
    }

    override fun getPersistentPlayerFrame(): FrameLayout = binding.persistentPlayerFrame

    override fun getActivityViewModelOwner(): ViewModelStoreOwner = this

    override fun getRootView(): CoordinatorLayout = binding.mainContent

    private fun onAuthorClicked(author: Author) {
        authorHelper.displayAuthorBottomSheet(
            AuthorItem(
                author.id,
                author.name,
                author.bio,
                author.expertise,
                author.image,
                null,
                0,
            ),
            lowDataBannerViewModel.lowDataBannerState.value?.isLowDataBannerEnable == true,
        )
        articlesPagerCollaborationViewModel.getCurrentTrackingInfo()?.let {
            articlesPagerCollaborationViewModel.dispatchFirebaseAnalyticsTrackingEvent(
                FirebaseAnalyticsTrackingEvent.AuthorFollowCardShownTracking(it),
            )
        }
    }

    private fun observeUserEvent() {
        articles2ViewModel.userEvent.observe(this) {
            when (it) {
                is UserEvent.PostAnswersInfoClick -> {
                    PostAnswersInfoBottomSheetFragment().show(
                        supportFragmentManager,
                        PostAnswersInfoBottomSheetFragment.TAG,
                    )
                }

                is UserEvent.PostAnswersFeedbackItemClick -> {
                    askThePostViewModel.initiatePostAnswersFeedback(
                        it.endpoint,
                        it.responseId,
                        it.reaction
                    )
                    Measurement.trackShowSubmitFeedback(
                        articlesPagerCollaborationViewModel.getCurrentTrackingInfo()?.omnitureX?.toTrackingInfo(),
                        false
                    )
                }

                is UserEvent.PostAnswersCarouselItemClick -> {
                    val intent = ArticlesParcel.builder().setCarouselOriginated(true)
                        .setArticleSingleUrl(it.url)
                    startActivity(intent.buildIntent(this))
                    Measurement.setPostAnswersCarouselInfo(
                        it.carouselPosition,
                        "",
                        getString(R.string.atp_article)
                    )
                }


                is UserEvent.GiveFeedbackClicked -> {
                    articles2ViewModel.eventTrigger(
                        UserEvent.PostAnswersFeedbackItemClick(
                            askThePostViewModel.feedbackData.first,
                            askThePostViewModel.feedbackData.second,
                            PostAnswersFeedbackReaction.YES
                        )
                    )
                }

                is UserEvent.DeepLinkItemClick -> {
                    DeepLinksProcessor.processAsync(it.link, scope = lifecycleScope)
                }

                else -> {}
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                askThePostViewModel.askThePostEvent.collect {
                    when (it) {
                        is AskThePostEvent.UserEvent -> {
                            when (it.userEvent) {
                                is UserEvent.TalkToThePostOpen -> {
                                    val isFirstDenial =
                                        ActivityCompat.shouldShowRequestPermissionRationale(
                                            this@Articles2Activity,
                                            Manifest.permission.RECORD_AUDIO
                                        )
                                    talkToThePostBottomSheetFragmentFactory.tryToShowTalkFragment(
                                        parentActivity = this@Articles2Activity,
                                        parentFragmentManager = supportFragmentManager,
                                        conversationId = askThePostViewModel.uiState.value.conversationId,
                                        surfaceName = askThePostViewModel.askSamEntryPoint?.name?.lowercase(),
                                        isFirstDenial = isFirstDenial,
                                        recordAudioPermissionRequest = recordAudioPermissionRequest
                                    )
                                }

                                else -> {}
                            }
                        }

                        is AskThePostEvent.InitiatePostAnswersFeedbackEvent -> {
                            FeedbackFragment(
                                FeedbackFragment.FeedbackType.ASK_THE_POST_ARTICLE,
                                it.feedBackEvent.first,
                                it.feedBackEvent.second,
                                it.feedBackEvent.third,
                            ).show(
                                supportFragmentManager,
                                FEEDBACK_TAG,
                            )
                        }

                        else -> {}
                    }

                }
            }
        }
    }

    private fun onATPTextClicked(questions: List<Question>) {
        articles2ViewModel.setQuestions(questions)
        Measurement.trackATPArticleClick(askThePostViewModel.analyticsData.second, true)
        AskThePostBottomSheetFragment().show(supportFragmentManager, ASK_THE_POST_BOTTOM_SHEET_TAG)
    }

    private fun onPaywallWillShow() {
        if (paywallPauseSessionActive) {
            return
        }

        val playbackState = audioMediaActivityViewModel.getNowPlayingAudioPlaybackState()
        val isPlayingBeforePaywall = playbackState is AudioPlaybackState.Playing
        paywallPauseSessionActive = true
        wasAudioPlayingBeforePaywall = isPlayingBeforePaywall

        if (isPlayingBeforePaywall) {
            audioMediaActivityViewModel.pauseMedia()
        }
    }

    private fun tryResumeAudioAfterPaywallDismiss(force: Boolean = false) {
        if (!paywallPauseSessionActive) {
            return
        }

        val isPaywallStillVisible = getPaywallSheetHelper().isPaywallSheetShowing()

        if (!force && (isPaywallStillVisible || isFinishing)) {
            return
        }

        if (wasAudioPlayingBeforePaywall) {
            audioMediaActivityViewModel.resumeMedia()
        }

        paywallPauseSessionActive = false
        wasAudioPlayingBeforePaywall = false
    }

    override fun showWallDialog(
        isLoggedIn: Boolean,
        reason: Int,
        wallType: PaywallConstants.WallType?,
        wallName: String?,
    ) {
        if (isOpenedFromAudioReadArticle && wallType == PaywallConstants.WallType.METERED_PAYWALL) {
            articleWallHelperViewModel.dispatchStopDelayedPaywall()
            getPaywallSheetHelper().showWall(
                wallName = wallName,
                wallType = wallType,
                wallReason = reason,
            )
            authorHelper.dismissAuthorBottomSheet()
            return
        }
        super.showWallDialog(isLoggedIn, reason, wallType, wallName)
        authorHelper.dismissAuthorBottomSheet()
    }

    private fun observeMapWallArticleFetch() {
        lifecycleScope.launch {
            forYouActivityViewModel.mapData.collect {
                it.first ?: return@collect

                CoroutineScope(Dispatchers.Main).launch {
                    val scrollDepth = articleWallHelperViewModel.scrollDepth.value ?: 0
                    val triggerDepth = mapWallPrompt?.trigger?.depth ?: 50
                    val recommendationsItem = it.first
                    val recommendNew = it.second
                    if (recommendNew || scrollDepth >= triggerDepth) {
                        showMapWall(recommendationsItem, recommendNew)
                    }
                }
            }
        }
    }

    private fun observeInlineOfferDataUpdateEvent() {
        articleInlineMessageViewModel.messageUpdateEvent.observe(this) {
            val offerData = iterableActivityViewModel.getArticleInlineMessageFromMessages(this)
            articleInlineMessageViewModel.setArticleInlineMessage(offerData)
        }
    }

    private fun observeInlineOfferImpressionEvent() {
        articleInlineMessageViewModel.messageImpressionEvent.observe(this) { event ->
            iterablePlugin.handleBannerLifecycleEvent(event)
        }
    }

    /**
     * This activity will listen to events fired from the Global CTA Banner in order to launch the correct Intent.
     */
    private fun observeBannerEvents() {
        globalBannerViewModel.bannerEvent.observe(this) { bannerEvent ->
            when (bannerEvent) {
                is BannerEvent.BannerClicked -> {
                    val banner = bannerEvent.iamMessageType?.let { type ->
                        val type = IamMessageType.entries.find { it.type == type }
                        type?.let {
                            iterableActivityViewModel.getBannerForPlacement(type)
                        }
                    } ?: bannerEvent.message
                    banner?.let {
                        iterablePlugin.executeBannerAction(
                            it,
                            IamMessageType.ARTICLE.type
                        )
                    }
                }

                is BannerEvent.BannerDismissed -> {
                    bannerEvent.message.let {
                        iterablePlugin.dismissBanner(it, null)
                    }
                }

                is BannerEvent.ImpressionEvent -> {
                    iterablePlugin.handleBannerLifecycleEvent(event = bannerEvent.event)
                }

                else -> {}
            }
        }
    }

    private fun observeMapWallScrollDepth() {
        articleWallHelperViewModel.scrollDepth.observe(
            this,
            object : Observer<Int> {
                override fun onChanged(value: Int) {
                    val recommendationsItem = forYouActivityViewModel.mapData.value.first
                    if (recommendationsItem != null && value >= (mapWallPrompt?.trigger?.depth
                            ?: 50)
                    ) {
                        articleWallHelperViewModel.scrollDepth.removeObserver(this)
                        showMapWall(recommendationsItem, false)
                    }
                }
            },
        )
        getCurrentTrackingInfo()?.let {
            Measurement.trackMapImpression(
                it.pageName,
                it.arcId,
                it.contentType,
                it.channel,
                it.subSection,
                it.authorId,
                Measurement.MAP_LISTENER_SCROLL,
            )
        }
    }

    fun showMapWall(
        recommendationsItem: RecommendationsItem?,
        recommendNew: Boolean,
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val isSaved =
                articleWallHelperViewModel.isMapRecommendationSaved(recommendationsItem?.getURL())
            withContext(Dispatchers.Main) {
                val mapWallUiData =
                    MapWallUiData(
                        mapWallPrompt?.promo?.labels?.first(),
                        mapWallPrompt?.promo?.logo,
                        recommendationsItem?.headline,
                        recommendationsItem?.byline(),
                        recommendationsItem?.getURL(),
                        recommendationsItem?.imageUrl,
                    )

                val saveOrRemoveMapRecommendation = {
                    articleWallHelperViewModel.saveOrRemoveMapRecommendation(
                        mapWallUiData,
                        isSaved,
                        recommendationsItem
                    )
                    /* Making an assumption that save DB transaction will be successful, rather than
                    waiting on result of DB transaction before progressing the UI along */
                    val message =
                        if (isSaved) {
                            "Removed from saved stories."
                        } else {
                            "Added to saved stories."
                        }
                    dismissMapWall(MapDismissOrigin.SYSTEM, message)
                }

                val recommendNewMapArticle = { recommendNewMapArticle() }

                val miscellany =
                    if (recommendNew) {
                        Measurement.MAP_RECIRC_DISPLAY_RECOMMEND
                    } else {
                        Measurement.MAP_RECIRC_DISPLAY
                    }
                binding.mapWallFrame.setContent {
                    MapWallView().MapWall(
                        mapWallUiData,
                        mapWallPrompt?.menu ?: false,
                        isSaved,
                        getCurrentTrackingInfo(),
                        { closeMapWall() },
                        { snoozeMapWall() },
                        saveOrRemoveMapRecommendation,
                        recommendNewMapArticle,
                    )
                }

                animateMapWall(true, null)

                getCurrentTrackingInfo()?.let {
                    Measurement.trackMapImpression(
                        it.pageName,
                        it.arcId,
                        it.contentType,
                        it.channel,
                        it.subSection,
                        it.authorId,
                        miscellany,
                    )
                }
            }
        }
    }

    private fun closeMapWall() {
        dismissMapWall(MapDismissOrigin.CLOSE_BUTTON)
    }

    private fun snoozeMapWall() {
        dismissMapWall(MapDismissOrigin.MENU_SNOOZE)
    }

    /**
     * Hides the MAP wall if it is currently visible
     * @param dismissOrigin How the MAP wall was dismissed. [MapDismissOrigin.SYSTEM] is any dismissal
     * not manually initiated by user
     * @param message Optional message that can be displayed after dismissing wall
     */
    private fun dismissMapWall(
        dismissOrigin: MapDismissOrigin,
        message: String? = null,
    ) {
        if (binding.mapWallFrame.visibility == View.VISIBLE) {
            var dismissalMessage = message

            if (dismissOrigin == MapDismissOrigin.CLOSE_BUTTON) {
                // Track close button dismissal
                articleWallHelperViewModel.trackMapWallDismissal(getCurrentTrackingInfo())
            }

            mapWallPrompt?.let {
                val hasMenu = it.menu ?: false
                val shouldProcessDismissal =
                    dismissOrigin == MapDismissOrigin.MENU_SNOOZE ||
                            (!hasMenu && dismissOrigin == MapDismissOrigin.CLOSE_BUTTON)
                if (shouldProcessDismissal) {
                    val snoozeDuration = articleWallHelperViewModel.processMapWallDismissalLogic(it)
                    val timePeriodString = timePeriodString(snoozeDuration)
                    if (timePeriodString.isNotEmpty()) {
                        dismissalMessage = "We won't show this feature for $timePeriodString."
                    }
                }
            }

            animateMapWall(false, dismissalMessage)
        }
    }

    private fun animateMapWall(
        show: Boolean,
        dismissalMessage: String?,
    ) {
        val transition: Transition = Slide(Gravity.BOTTOM)
        transition.addTarget(binding.mapWallFrame)
        transition.addListener(
            object : Transition.TransitionListener {
                override fun onTransitionEnd(transition: Transition) {
                    dismissalMessage?.let {
                        Toast.makeText(applicationContext, dismissalMessage, Toast.LENGTH_LONG)
                            .show()
                    }
                }

                override fun onTransitionStart(transition: Transition) {}

                override fun onTransitionCancel(transition: Transition) {}

                override fun onTransitionPause(transition: Transition) {}

                override fun onTransitionResume(transition: Transition) {}
            },
        )

        TransitionManager.beginDelayedTransition(binding.root, transition)
        binding.mapWallFrame.visibility =
            if (show) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    private fun recommendNewMapArticle() {
        dismissMapWall(MapDismissOrigin.SYSTEM)
        articleWallHelperViewModel.fetchNewMapArticle(
            forYouActivityViewModel,
            isPushOriginated,
            articleListViewModel.getCurrentPageUrl(),
            true,
        )
    }

    override fun updateSubscriptionState() {
        articleWallHelperViewModel.handlePaywallResume()
    }

    private fun onLinkClicked(url: String?) {
        Measurement.trackExternalLink(url)
        if (WPUrlAnalyser.getWPUrlAnalyser().isAcqOrCheckoutUrl(url)) {
            val hasSubscription = PaywallService.getInstance().isPremiumUser
            if (hasSubscription) {
                getActiveSubscriberDialog(this).show()
            } else {
                articleWallHelperViewModel.dispatchShowPaywallNow(
                    PaywallConstants.WallType.ARTICLE_LINKS_PAYWALL,
                )
            }
        } else {
            if (url?.endsWith(".pdf", ignoreCase = true) == true) {
                Utils.startWebChromeCustomTab(url, this)
            } else if (url?.startsWith("mailto", ignoreCase = true) == true) {
                Utils.handleSpecialLink(this, url)
            } else {
                WPUrlAnalyser
                    .getWPUrlAnalyser()
                    .analyseAndStartIntent(
                        this,
                        url,
                        INLINE_LINK_ORIGINATED,
                    )
            }
        }
    }

    private fun onLufOutcomePostClicked(url: String?) {
        if (WPUrlAnalyser.getWPUrlAnalyser().isAcqOrCheckoutUrl(url)) {
            val hasSubscription = PaywallService.getInstance().isPremiumUser
            if (hasSubscription) {
                getActiveSubscriberDialog(this).show()
            } else {
                articleWallHelperViewModel.dispatchShowPaywallNow(
                    PaywallConstants.WallType.ARTICLE_LINKS_PAYWALL,
                )
            }
        } else {
            WPUrlAnalyser.getWPUrlAnalyser().analyseAndStartIntent(
                this,
                url,
                LUF_OUTCOME_POST_ORIGINATED,
            )
        }
    }

    private fun setupOpeningAnimation() {
        val location =
            intent.getIntArrayExtra(EXTRA_REVEAL_ANIMATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
            location != null &&
            location.size >= 2 &&
            location[0] > 0 &&
            location[1] > 0
        ) {
            getRootView().visibility = View.INVISIBLE
            val viewTreeObserver = getRootView().viewTreeObserver
            if (viewTreeObserver.isAlive) {
                viewTreeObserver.addOnGlobalLayoutListener(
                    object : OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            revealActivity(location[0], location[1])
                            getRootView().viewTreeObserver.removeOnGlobalLayoutListener(this)
                        }
                    },
                )
            }
        } else {
            getRootView().visibility = View.VISIBLE
            overridePendingTransition(R.anim.slide_up, R.anim.slide_down)
        }
    }

    private fun updateLowDataMode() {
        lowDataBannerViewModel.updateLowDataBanner(
            AppPreferences.isLowDataModeEnabled() && config.lowDataModeConfig.enable,
        )
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun revealActivity(
        revealX: Int,
        revealY: Int,
    ) {
        val finalRadius =
            (
                    Math.max(
                        getRootView().width,
                        getRootView().height,
                    ) * 1.1
                    ).toFloat()
        val circularReveal =
            ViewAnimationUtils.createCircularReveal(
                getRootView(),
                revealX,
                revealY,
                0f,
                finalRadius,
            )
        circularReveal.duration = 400
        circularReveal.interpolator = AccelerateInterpolator()
        getRootView().visibility = View.VISIBLE
        circularReveal.start()
    }

    private fun shouldBypassCache(articlesParcel: ArticlesParcel): Boolean =
        articlesParcel.isPushOriginated() ||
                articlesParcel.isAlertPageOriginated() ||
                articlesParcel.isLiveBlogOriginated() ||
                articlesParcel.isDeepLinkOriginated() ||
                articlesParcel.isInlineLinkOriginated() ||
                config.articleContentUpdateRulesConfig.contentCacheAge <= 0

    /**
     * Currently, if shouldBypassCache flag is true - only one single article is passed to the activity,
     * but it makes more sense and flexibility to bind the bypass flag to each of [ArticleMeta]
     */
    private fun setupCacheBypassFlag(
        articles: List<ArticleMeta>,
        articlesParcel: ArticlesParcel,
    ) {
        if (shouldBypassCache(articlesParcel)) {
            articles.forEach { it.bypassCache = true }
        }
    }

    /**
     * Get the paywall type based on where the article is opened from.
     */
    private fun getPaywallType(): PaywallConstants.WallType {
        val isDeepLinkOriginated = intent.getBooleanExtra(DEEPLINK_ORIGINATED, false)
        val isAirshipOriginated = intent.getBooleanExtra(AIRSHIP_ORIGINATED, false)
        val isOneLinkOriginated = intent.getBooleanExtra(ONELINK_ORIGINATED, false)
        val isWidgetOriginated = intent.getBooleanExtra(WIDGET_ORIGINATED, false)
        val widgetType = intent.getStringExtra(WidgetData.EXTRAS_WIDGET_TYPE)
        return when {
            isDeepLinkOriginated -> PaywallConstants.WallType.ARTICLE_DEEP_LINK_PAYWALL
            isAirshipOriginated -> PaywallConstants.WallType.IAA_WALL
            isOneLinkOriginated -> PaywallConstants.WallType.ONELINK_WALL
            isWidgetOriginated &&
                    WidgetType.WIDGET.name.equals(
                        widgetType,
                        ignoreCase = true,
                    ) -> PaywallConstants.WallType.WIDGET_SMALL_PAYWALL

            isWidgetOriginated &&
                    WidgetType.TABLET_WIDGET.name.equals(
                        widgetType,
                        ignoreCase = true,
                    ) -> PaywallConstants.WallType.WIDGET_PAYWALL

            else -> PaywallConstants.WallType.METERED_PAYWALL
        }
    }

    /**
     * Handle tetro response from webview article.
     */
    fun handleWebviewTetroResponse(
        url: String,
        webTetroResponse: WebTetroResponse,
    ) {
        articleWallHelperViewModel.handleWebviewWall(url, webTetroResponse)
    }

    /**
     * Observes events posted for tap on bookmark button.
     */
    private fun observeBookmarkTapEvents() {
        articlesPagerCollaborationViewModel.bookmarkTapEvent.observe(
            this,
            Observer { article ->
                val savedArticleMeta = articleListViewModel.liveArticleByUrl.value
                val isBookmarked = savedArticleMeta != null
                if (!isBookmarked) {
                    saveArticle(article)
                } else {
                    removeSavedArticle(article, savedArticleMeta)
                }
            },
        )
    }

    /**
     * Observes events
     */
    private fun observeGiftTapEvents() {
        giftCollaborationViewModel.giftTrackingEvent.observe(this) { pair ->
            val contentUrl = getUrlWithoutParameters(pair.first)
            val trackingInfo =
                articlesPagerCollaborationViewModel.firebaseAnalyticsTrackingMap[contentUrl]

            // [trackingInfo] will be null for Web Type Articles
            articlesPagerCollaborationViewModel.dispatchFirebaseAnalyticsTrackingEvent(
                FirebaseAnalyticsTrackingEvent.GiftSenderTracking(
                    contentUrl,
                    trackingInfo,
                    pair.second.trackingName,
                ),
            )
        }
    }

    /**
     * Save the article in Saved Stories.
     */
    private fun saveArticle(article2: Article2) {
        val savedArticleModel =
            SavedArticleModel(
                article2.contenturl,
                System.currentTimeMillis()
            )
        val metadataModel =
            MetadataModel(
                article2.contenturl,
                System.currentTimeMillis()
            ).apply {
                imageURL = article2.socialImage
                headline = article2.title
                blurb = article2.blurb
                // get byline and date from items list
                article2.items?.forEach {
                    when (it) {
                        is ByLine -> {
                            byline = it.content
                        }

                        is Date -> {
                            publishedTime = it.content
                        }
                    }
                }
            }
        articleListViewModel.saveArticle(savedArticleModel, metadataModel)
        articlesPagerCollaborationViewModel.firebaseAnalyticsTrackingMap[
            getUrlWithoutParameters(
                article2.contenturl,
            ),
        ]?.let {
            articlesPagerCollaborationViewModel.dispatchFirebaseAnalyticsTrackingEvent(
                FirebaseAnalyticsTrackingEvent.BookmarkTracking(
                    it,
                    isSaving = true,
                ),
            )
        }
    }

    private fun removeSavedArticle(
        article2: Article2,
        savedArticleMeta: ArticleAndMetadata?,
    ) {
        savedArticleMeta ?: return
        articleListViewModel.removeArticles(
            listOf(savedArticleMeta),
        )
        articlesPagerCollaborationViewModel.firebaseAnalyticsTrackingMap[
            getUrlWithoutParameters(
                article2.contenturl,
            ),
        ]?.let {
            articlesPagerCollaborationViewModel.dispatchFirebaseAnalyticsTrackingEvent(
                FirebaseAnalyticsTrackingEvent.BookmarkTracking(
                    it,
                    isSaving = false,
                ),
            )
        }
    }

    /**
     * Show Tooltip for Action Bar icon. Not currently used.
     */
    private fun showOrHideActionIconTooltip(
        show: Boolean,
        iconId: Int? = null,
    ) {
        // Only one tooltip is shown for each instance of Articles2Activity.
        binding.toolbar.post {
            if (show && !isTooltipShownInCurrentInstanceOfArticleActivity(this) && iconId != null) {
                // This Stores anchorview for the tooltip
                val actionMenuItemView = findViewById<View>(iconId)

                // all the properties associated with the tooltip e.g. priority and prefKey.
                val tooltipData: TooltipData? =
                    when (iconId) {
                        R.id.action_summary -> {
                            TooltipData(
                                priorityData = TooltipPriority.SummaryTooltipPriority(),
                                sharedPrefKey = TooltipPriority.SummaryTooltipPriority().prefKey,
                                tooltipTextResourceId = R.string.article_summary_action_tooltip_text,
                                boldPortionResourceId = R.string.article_summary_action_tooltip_bold,
                                gravity = Gravity.BOTTOM,
                                verticalMargin = 24,
                            )
                        }
                        // No tooltips implemented in articles at this time.
                        else -> null
                    }

                tooltipData?.let {
                    // Store tooltip text and bold portion
                    val content =
                        getSpannableStringWithBoldText(
                            this,
                            it.tooltipTextResourceId,
                            it.boldPortionResourceId,
                        )

                    // Create tooltipProperties
                    val tooltipProperties =
                        TooltipProperties(
                            content,
                            actionMenuItemView,
                            null,
                            it,
                        )

                    // Show tooltip
                    if (TooltipPopupManager.get(this).showToolTip(tooltipProperties)) {
                        setTooltipShownInCurrentArticleActivityInstance(this)
                    }
                }
            } else if (!show) {
                TooltipPopupManager.get(this).dismissTooltip()
            }
        }
    }

    /**
     * Observes reading history events sent after reading an article for 5 seconds.
     */
    private fun observeReadingHistorySaveEvents() {
        articlesPagerCollaborationViewModel.readingHistorySaveEvent.observe(this, {
            it?.let { proceedToSaveReadingHistoryArticle(it) }
        })
    }

    /**
     * Observes reading history events sent after reading an article for 5 seconds.
     */
    private fun observeRteTrackPageViewEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                articleWallHelperViewModel.articleEvent.collect { event ->
                    when (event) {
                        is Article2Events.Article2PageViewEvent -> {
                            // track GA pageview
                            Measurement.trackWithTrackingInfo(
                                event.articlePageViewTrackingData.omniture,
                                event.articlePageViewTrackingData.position,
                                event.articlePageViewTrackingData.currentAppTab,
                                event.articlePageViewTrackingData.currentAppSection,
                                event.articlePageViewTrackingData.pushTrackingHelperData,
                                event.articlePageViewTrackingData.measurementMap
                            )

                            // track RTE pageview
                            val trackingDataOmniture = event.articlePageViewTrackingData.omniture
                            userHistoryViewModel.savePageViewEvent(
                                trackingDataOmniture?.arcId,
                                trackingDataOmniture?.contentType
                            )
                            userHistoryViewModel.postPageViewEvent()
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    /**
     * Saves the article to the reading history list.
     * [metadataModel] model required to save the article to the history list.
     */
    private fun proceedToSaveReadingHistoryArticle(metadataModel: MetadataModel) {
        val readingHistoryModel =
            ReadingHistoryModel(
                metadataModel.contentURL,
                metadataModel.canonicalURL,
                System.currentTimeMillis(),
                null,
                false
            )
        val meta = MetadataModel(
            contentURL = metadataModel.contentURL,
            syncLmt = System.currentTimeMillis()
        ).apply {
            imageURL = metadataModel.imageURL
            blurb = metadataModel.blurb
            headline = metadataModel.headline
            byline = metadataModel.byline
            publishedTime = metadataModel.publishedTime
        }
        readingHistoryViewModel.saveArticle(readingHistoryModel, meta)
    }

    private fun observeArticleMetricsEvents() {
        articlesPagerCollaborationViewModel.articleMetricsEvents.observe(this) { articleMetricsEvent ->
            when (articleMetricsEvent) {
                is ArticleMetricsEvent.StartLoading ->
                    EventTimerLog.startTimingEvent(
                        articleMetricsEvent.url,
                        EventTimerLog.ARTICLE_DOWNLOAD_TIME,
                    )

                is ArticleMetricsEvent.StopLoading ->
                    EventTimerLog.stopTimingEvent(
                        articleMetricsEvent.url,
                        EventTimerLog.ARTICLE_DOWNLOAD_TIME,
                    )

                is ArticleMetricsEvent.StartProcessing ->
                    EventTimerLog.startTimingEvent(
                        articleMetricsEvent.url,
                        EventTimerLog.STORY_RENDER_LOAD,
                    )

                is ArticleMetricsEvent.StopProcessing ->
                    EventTimerLog.stopTimingEvent(
                        articleMetricsEvent.url,
                        EventTimerLog.STORY_RENDER_LOAD,
                    )

                is ArticleMetricsEvent.StartDrawing ->
                    EventTimerLog.startTimingEvent(
                        articleMetricsEvent.url,
                        EventTimerLog.STORY_RENDER_DRAW,
                    )

                is ArticleMetricsEvent.StopDrawing -> {
                    // this is the last step in pipeline. The right time to sum up the results
                    EventTimerLog.stopTimingEvent(
                        articleMetricsEvent.url,
                        EventTimerLog.STORY_RENDER_DRAW,
                    )
                    logArticleMetrics(articleMetricsEvent.url, articleMetricsEvent.source)
                    EventTimerLog.dumpTimers(articleMetricsEvent.url)
                }
            }
        }
    }

    /**
     * Checks to see if the polly playback should be paywalled for an article.
     * Posts true if it should be, false otherwise. Uses the existing [paywallAudio] function to determine that.
     */
    private fun observePaywallVerificationEventsForAudio() {
        articleWallHelperViewModel.checkPaywallStatusForAudio.observe(this) {
            if (it) {
                articleWallHelperViewModel.showPaywallForAudio(paywallAudio())
            }
        }
    }

    /**
     * Check if current Article is loaded so that delayed paywall can be called.
     */
    private fun observeArticleForPaywall() {
        articleWallHelperViewModel.currentPaywallArticle.observe(this) {
//            // Internally this will stop the delayed paywall call of the previous article in the
//            // the ViewPager
            articleWallHelperViewModel.dispatchShowPaywallDelayed()
        }
    }

    /**
     * Checks to see if the article that was deeplinked into is a gift article
     * with a valid, not expired token
     */
    private fun handleGiftWall(giftState: GiftState) {
        val isFree = articleWallHelperViewModel.isCurrentNativeArticleFree()
        when {
            PaywallService.getInstance().isSubscriptionPaused -> {
                onPaywallWillShow()
                articleWallHelperViewModel.dispatchShowPaywallNow(
                    PaywallConstants.WallType.PAUSEWALL,
                )
            }

            giftState is GiftState.ValidNotExpired ->
                bottomCtaViewModel.dispatchCtaType(
                    BottomCtaType.GIFT,
                )

            isFree -> bottomCtaViewModel.dispatchCtaType(BottomCtaType.NONE)
            giftState is GiftState.NotGift -> {
                onPaywallWillShow()
                articleWallHelperViewModel.dispatchShowPaywallNow(
                    PaywallConstants.WallType.METERED_PAYWALL,
                )
            }

            giftState is GiftState.Expired -> {
                onPaywallWillShow()
                articleWallHelperViewModel.dispatchShowPaywallNow(
                    PaywallConstants.WallType.GIFT_EXPIRED_PAYWALL,
                )
            }

            else -> bottomCtaViewModel.dispatchCtaType(BottomCtaType.NONE)
        }
    }

    private fun logArticleMetrics(
        url: String,
        source: ArticleContentState.Source,
    ) {
        try {
            val loadTimeMS = EventTimerLog.getStopTime(url, EventTimerLog.STORY_RENDER_LOAD)
            val downloadTimeMS = EventTimerLog.getStopTime(url, EventTimerLog.ARTICLE_DOWNLOAD_TIME)
            val drawTimeMS = EventTimerLog.getStopTime(url, EventTimerLog.STORY_RENDER_DRAW)
            val totalTime = loadTimeMS + downloadTimeMS + drawTimeMS

            EventLog
                .Builder()
                .apply {
                    setMessage("Article Load Metrics")
                    setModule(LogModules.ARTICLES)
                    set("story_render_load", String.format("%.2f", loadTimeMS))
                    set("story_render_download", String.format("%.2f", downloadTimeMS))
                    set("story_render_draw", String.format("%.2f", drawTimeMS))
                    set("timer_total", String.format("%.2f", totalTime))
                    set("is_webp", Utils.isWebpSupported())
                    set("entry_point", getEntryPoint())
                    set("app_version", getAppVersionName(applicationContext))
                    set("source", source.name)
                    setContentUrl(url)
                }.run {
                    RemoteLog.m(this@Articles2Activity.applicationContext, build())
                }
        } catch (t: Throwable) {
            Logger.e("RemoteLog", "Failed to log articles metrics", t)
        }
    }

    fun getEntryPoint(): String {
        val articlesParcel = ArticlesParcel(intent)
        return when {
            articlesParcel.isPushOriginated() -> "push"
            articlesParcel.isPrintOriginated() -> "print"
            articlesParcel.isAlertPageOriginated() -> "alert"
            articlesParcel.isDeepLinkOriginated() -> "link"
            articlesParcel.isAirshipOriginated() -> "iaa"
            articlesParcel.isOneLinkOriginated() -> "onelink"
            articlesParcel.isWidgetOriginated() -> "widget"
            articlesParcel.isCarouselOriginated() -> "carousel"
            else -> "front"
        }
    }

    override fun onStop() {
        // Dialogs can be closed if any to avoid leaks.
        TooltipPopupManager.get(this).dismissTooltip()
        if (sectionDisplayName == "Recipes") {
            Measurement.trackSearchRecipePageView(
                Measurement.PAGE_RECIPE_FINDER_LANDING,
                Measurement.PATH_TO_VIEW_BACK_TO_FRONT,
            )
        }
        super.onStop()
    }

    override fun onDestroy() {
        // Reset tooltip state flag for future activity instances
        resetTooltipShownInCurrentArticleActivityInstance(this)
        supportFragmentManager.unregisterFragmentLifecycleCallbacks(paywallLifecycleCallbacks)
        if (isFinishing) {
            Measurement.clearDefaultMap()
            ArticlesParcel.removeArticlesMeta(intent)
        }
        if (::talkToThePostBottomSheetFragmentFactory.isInitialized) {
            talkToThePostBottomSheetFragmentFactory.onDestroy()
        }
        super.onDestroy()
    }

    /**
     * To prevent firing double pageviews.
     * Listen and Games should fire this pageview when they are Tabs but not when they are Section Fronts.
     */
    private fun shouldNotSuppressPageView(): Boolean =
        (
                !ArticlesParcel(intent).isOpenedFromSearch() &&
                        !ArticlesParcel(intent).isOpenedFromSectionFront() &&
//                    !ArticlesParcel(intent).isCarouselOriginated() &&
                        !ArticlesParcel(intent).isHabitTilesOriginated() &&
                        !ArticlesParcel(intent).isSubNavLUFOriginated() &&
                        !ArticlesParcel(intent).isInlineLinkOriginated() &&
                        !ArticlesParcel(intent).isRecircModuleOriginated()
                )

    /**
     * Depending on what type of bottom cta is shown, we show the paywall with related tracking info (using paywall type).
     */
    private fun observeBottomCtaToPaywallAction() {
        bottomCtaViewModel.bottomCtaToPaywallAction.observe(this) { action ->
            action?.let { showPaywallFromReminder(action.paywallType) }
                ?: Logger.e("Articles2Activity", "Unrecognized action on bottom CTA")
        }
    }

    private fun observeCurrentPageAudioAvailabilityEvent() {
        articlesPagerCollaborationViewModel.currentPageAudioAvailability.observe(this) {
            invalidateOptionsMenu()
        }
    }

    private fun observeCurrentPageSummaryAvailability() {
        articlesPagerCollaborationViewModel.currentPageSummaryAvailability.observe(this) {
            updateSummaryTooltipEventState(false)
            invalidateOptionsMenu()
        }
    }

    private fun updateSummaryTooltipEventState(isSummaryClickEvent: Boolean) {
        if (isSummaryClickEvent) {
            // marked the tooltip viewed once a user clicks on the button to prevent it from being
            // shown if user taps it before scrolling.
            TooltipPopupManager
                .get(this)
                .setTooltipShown(TooltipPriority.SummaryTooltipPriority().prefKey)
        }
        if (TooltipPopupManager.get(this).isTooltipShown(
                TooltipPriority.SummaryTooltipPriority().prefKey,
            )
        ) {
            // No need to trigger rv stop scroll event if tooltip is already shown.
            articlesPagerCollaborationViewModel.dispatchSummaryTooltipEvent(false)
        }
    }

    private fun observeSummaryTooltipEvent() {
        articlesPagerCollaborationViewModel.summaryTooltipEvent.observe(this) {
            showOrHideActionIconTooltip(it, R.id.action_summary)
        }
    }

    private fun observeAudioClickEvent() {
        articlesPagerCollaborationViewModel.audioClickEvent.observe(this) {
            audioMediaActivityViewModel.apply {
                setIsLowDataMode(
                    lowDataBannerViewModel.lowDataBannerState.value?.isLowDataBannerEnable == true
                )
            }.playMedia(it)
        }
    }

    /**
     * Observe video click event to fetch contextual targeting data from ads content api
     */
    private fun observeVideoClickEvent() {
        videoActivityViewModel.videoClickEvent.observe(this) { videoId ->
            adsContentViewModel.getContent(videoId)
        }
    }

    /**
     * Observe Ads contextual content api call response to send its data to pre roll ads
     */
    private fun observeAdsContentState() {
        adsContentViewModel.uiState.observe(this) { state ->
            if (state.id.isEmpty() || state.id != videoActivityViewModel.videoClickEvent.value) return@observe
            val uiState =
                when (state) {
                    is ContentUIState.Content ->
                        TargetingContentUIState.Content(
                            state.id,
                            state.items.map {
                                TargetingContent(
                                    id = it.id,
                                    adCall = it.adCall,
                                    permutive = it.permutive,
                                )
                            },
                        )

                    is ContentUIState.Loading -> TargetingContentUIState.Loading(state.id)
                    is ContentUIState.Error -> TargetingContentUIState.Error(state.id)
                    is ContentUIState.Cancelled -> TargetingContentUIState.Cancelled(state.id)
                    is ContentUIState.UITimeout -> TargetingContentUIState.UITimeout(state.id)
                }
            videoActivityViewModel.updateAdsContentUiState(state.id, uiState)
        }
    }

    override fun hasAudioPlayerSupportInThisScreen(): Boolean = true

    override fun provideLoginRegActivityViewModel(): LoginRegActivityViewModel =
        loginRegActivityViewModel

    fun getArticlesList(): List<ArticleMeta> = ArticlesParcel(intent).getArticleMetas()

    fun getCurrentArticleUrl(): String? = articleListViewModel.getCurrentPageUrl()

    private fun getCurrentTrackingInfo(): OmnitureX? {
        val currentUrl = articleListViewModel.getCurrentPageUrl()
        return articlesPagerCollaborationViewModel.firebaseAnalyticsTrackingMap[currentUrl]?.omnitureX
    }

    fun getTrackingInfoForUrl(url: String?): OmnitureX? =
        articlesPagerCollaborationViewModel.firebaseAnalyticsTrackingMap[url]?.omnitureX

    companion object {
        const val NUMBER_OF_RETRY_ATTEMPTS = 2
        private const val EMPTY_STRING = ""
        const val FEEDBACK_TAG = "feedback_fragment"
        const val FEEDBACK_STATUS_TAG = "feedback_status_fragment"
        const val SUMMARY_TAG = "summary_fragment"
        const val ASK_THE_POST_BOTTOM_SHEET_TAG = "ask_the_post_bottom_fragment"

    }

    override fun getLoader(): ILoader = FlagshipApplication.getInstance().imageService

    override fun openWebEmbed(url: String) {
        Utils.startWeb(url, this)
    }

    override fun onTopAppBarEventFired(event: ArticlesTopAppBarInteractionEvent) {
        when (event) {
            is ArticlesTopAppBarInteractionEvent.BackClickEvent -> {
                onBackPressedDispatcher.onBackPressed()
            }

            is ArticlesTopAppBarInteractionEvent.CommentsClickEvent -> {
                onCommentActionClicked()
            }

            is ArticlesTopAppBarInteractionEvent.GiftClickEvent -> {
                articlesPagerCollaborationViewModel.initToolbarAction(
                    event.url,
                    ActionsOnIndividualArticles.ActionGift,
                )
                Measurement.setNavigationBehavior(NavigationBehavior.GIFT_SEND_DIALOG)
            }

            is ArticlesTopAppBarInteractionEvent.ListenClickEvent -> {
                val audioMediaConfig = constructAudioMediaConfig()
                if (audioMediaConfig != null) {
                    articlesPagerCollaborationViewModel.dispatchAudioClickEvent(audioMediaConfig)
                }
            }

            is ArticlesTopAppBarInteractionEvent.PlaylistClickEvent -> {
                val audioMediaConfig = constructAudioMediaConfig()
                val playlistItem = audioMediaConfig?.toPlaylistAudio()

                if (event.currentlyInPlaylist) {
                    playlistActivityViewModel.removePlaylistByIdAudio(playlistItem?.id)
                } else {
                    playlistActivityViewModel.addToPlaylist(playlistItem)
                    Measurement.trackActionButtonAddToPlaylist(
                        audioMediaConfig?.sectionName,
                        audioMediaConfig?.arcId,
                    )
                }
            }

            is ArticlesTopAppBarInteractionEvent.SaveClickEvent -> {
                Measurement.setNavigationBehavior(NavigationBehavior.SAVE_CLICK)
                if (!paywallBookMark()) {
                    articlesPagerCollaborationViewModel.initToolbarAction(
                        event.url,
                        ActionsOnIndividualArticles.ActionBookmarkClick,
                    )
                }
            }

            is ArticlesTopAppBarInteractionEvent.ShareClickEvent -> {
                articlesPagerCollaborationViewModel.initToolbarAction(
                    event.url,
                    ActionsOnIndividualArticles.ActionShare,
                )
            }

            is ArticlesTopAppBarInteractionEvent.SummaryClickEvent -> {
                if (!paywallSummary()) {
                    articlesPagerCollaborationViewModel.initToolbarAction(
                        event.url,
                        ActionsOnIndividualArticles.ActionSummaryClick,
                    )
                }
            }
        }
    }
}
