package com.wapo.flagship.wapomain

import SharedScrollViewModel
import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ClipData
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.SparseArray
import android.webkit.URLUtil
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.android.material.snackbar.Snackbar
import com.wapo.adsinf.models.AdsModel
import com.wapo.adsinf.policy.AdService
import com.wapo.android.commons.iterable.toBundle
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.Logger
import com.wapo.android.commons.util.URLParser
import com.wapo.android.commons.util.Utils.coalesce
import com.wapo.android.commons.util.agerestriction.AgeRestrictionValidator
import com.wapo.android.commons.util.agerestriction.fakeagerestrictionshelper.AgeRestrictionsFakeSetUp
import com.wapo.android.commons.util.toDateLong
import com.wapo.android.remotelog.logger.EventTimerLog
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.AppContext
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.FusionActivity
import com.wapo.flagship.IntentHelper
import com.wapo.flagship.PaywallVerifyCallsSuppressHelper
import com.wapo.flagship.Utils
import com.wapo.flagship.agerestriction.AgeRestrictionActivity
import com.wapo.flagship.agerestriction.AgeRestrictionsScreenMode
import com.wapo.flagship.common.DialogFactory
import com.wapo.flagship.common.DialogFactory.getOsUpdateDialog
import com.wapo.flagship.common.DialogFactory.getReviewDialog
import com.wapo.flagship.common.DialogFactory.getUpdateDialog
import com.wapo.flagship.common.getUrlAndAnchorRefPair
import com.wapo.flagship.content.notifications.NotificationArticleType
import com.wapo.flagship.external.foryouwidget.workers.ForYouWidgetUpdateWorker
import com.wapo.flagship.features.aixp.SummariesFeatureFlag
import com.wapo.flagship.features.aixp.models.ArticleSummary
import com.wapo.flagship.features.aixp.ui.FeedbackFragment
import com.wapo.flagship.features.aixp.ui.FeedbackStatusFragment
import com.wapo.flagship.features.aixp.ui.SummaryFragment
import com.wapo.flagship.features.aixp.viewmodels.ArticleSummaryCollaborationViewModel.Companion.MISCELLANY_SUFFIX_REALTIME
import com.wapo.flagship.features.alerts.AlertsActivity
import com.wapo.flagship.features.articles.ArticleLinkType
import com.wapo.flagship.features.articles2.activities.Articles2Activity.Companion.FEEDBACK_TAG
import com.wapo.flagship.features.articles2.activities.ArticlesParcel
import com.wapo.flagship.features.articles2.activities.ArticlesParcel.Companion.builder
import com.wapo.flagship.features.articles2.activities.OPINION_PUSH_ORIGINATED
import com.wapo.flagship.features.articles2.activities.PUSH_ORIGINATED
import com.wapo.flagship.features.articles2.activities.WIDGET_ORIGINATED
import com.wapo.flagship.features.articles2.paywall.WallUiEvent
import com.wapo.flagship.features.ask.fragments.TalkToThePostBottomSheetFragment
import com.wapo.flagship.features.ask.fragments.TalkToThePostBottomSheetFragmentFactory
import com.wapo.flagship.features.ask.models.AskSamEntryPoint
import com.wapo.flagship.features.ask.models.AskThePostEvent
import com.wapo.flagship.features.ask.models.ShareType
import com.wapo.flagship.features.ask.ui.SourceBottomSheet
import com.wapo.flagship.features.ask.ui.SourceBottomSheetCaller
import com.wapo.flagship.features.ask.ui.toDateLong
import com.wapo.flagship.features.ask.viewmodels.AskThePostViewModel
import com.wapo.flagship.features.ask.viewmodels.ShareUrlState
import com.wapo.flagship.features.audio.AudioActivity
import com.wapo.flagship.features.audio.AudioTrackerEvent
import com.wapo.flagship.features.audio.PlayerType
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.audio.fragments.AudioPagerFragment
import com.wapo.flagship.features.audio.models.AudioPlaybackState
import com.wapo.flagship.features.audio.playlist.AudioSubtype
import com.wapo.flagship.features.audio.utils.AudioPreferences
import com.wapo.flagship.features.audio.viewmodels.PlaylistActivityViewModel
import com.wapo.flagship.features.backendhealth.viewmodels.BackendHealthCheckerViewModel
import com.wapo.flagship.features.casettlement.CASettlementDialog
import com.wapo.flagship.features.conversations.ui.CommentBottomSheetFragment
import com.wapo.flagship.features.conversations.ui.CommentBottomSheetFragment.Companion.COMMENT_SOURCE_DEEPLINK
import com.wapo.flagship.features.deeplinks.AirshipAnalytics
import com.wapo.flagship.features.deeplinks.AirshipInAppMessageListener
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor
import com.wapo.flagship.features.find.FindActivity
import com.wapo.flagship.features.find.SECTION_LIST_KEY
import com.wapo.flagship.features.gifting.events.GiftCollabEvent
import com.wapo.flagship.features.grid.GridActivity
import com.wapo.flagship.features.grid.MarginUpdatable
import com.wapo.flagship.features.grid.model.GlobalBannerMessage
import com.wapo.flagship.features.grid.model.Grid
import com.wapo.flagship.features.grid.viewmodel.sectionsHabitTiles.SectionsHabitTilesViewModel
import com.wapo.flagship.features.habittiles.HabitTilesPlugin
import com.wapo.flagship.features.inlineoffer.model.SectionInlineOfferViewModel
import com.wapo.flagship.features.main.OpenFragment
import com.wapo.flagship.features.mypost.MyPostActivity
import com.wapo.flagship.features.notification.AlertManager
import com.wapo.flagship.features.notification.AlertManagerProvider
import com.wapo.flagship.features.notification.AlertsActivityInterface
import com.wapo.flagship.features.notification.AlertsSettings
import com.wapo.flagship.features.notification.AlertsSettings.EntryPoint.Companion.TYPE
import com.wapo.flagship.features.notification.NotificationPermissionRequestPlugin
import com.wapo.flagship.features.onboarding2.fragment.OnboardingFragment
import com.wapo.flagship.features.onboarding2.models.OnboardingEvent
import com.wapo.flagship.features.onboarding2.models.UserClickEvent
import com.wapo.flagship.features.onboarding2.viewmodel.onboarding.OnboardingViewModel
import com.wapo.flagship.features.onetrust.OneTrustHelper
import com.wapo.flagship.features.pagebuilder.AdViewFactoryProvider
import com.wapo.flagship.features.personalizedpodcasts.events.UserEvent
import com.wapo.flagship.features.personalizedpodcasts.model.PersoPodTrackingInfo
import com.wapo.flagship.features.personalizedpodcasts.model.PersonalizedPodcast
import com.wapo.flagship.features.personalizedpodcasts.repo.PodcastResult
import com.wapo.flagship.features.personalizedpodcasts.viewmodel.PersonalizedPodcastViewModel
import com.wapo.flagship.features.personalizedpodcasts.viewmodel.PodcastGenerationState
import com.wapo.flagship.features.podcast.AudioProviderImpl
import com.wapo.flagship.features.posttv.VideoTracker2
import com.wapo.flagship.features.posttv.listeners.PostTvActivity
import com.wapo.flagship.features.posttv.listeners.PostTvApplication
import com.wapo.flagship.features.posttv.model.TrackingType
import com.wapo.flagship.features.posttv.model.Video
import com.wapo.flagship.features.search2.events.ConversationItem
import com.wapo.flagship.features.search2.fragments.PostAnswersInfoBottomSheetFragment
import com.wapo.flagship.features.search2.ui.CarouselUIItem
import com.wapo.flagship.features.sections.ConnectivityActivity
import com.wapo.flagship.features.sections.SectionActivity
import com.wapo.flagship.features.sections.SectionFrontsFragment
import com.wapo.flagship.features.sections.SectionsPagerView
import com.wapo.flagship.features.sections.model.Section
import com.wapo.flagship.features.sections.tracking.SectionTrackEvent
import com.wapo.flagship.features.sections.tracking.SectionsTracker
import com.wapo.flagship.features.sections.tracking.SectionsTrackerProvider
import com.wapo.flagship.features.sections.utils.JTidTracker
import com.wapo.flagship.features.sections.viewmodels.SectionNavigation
import com.wapo.flagship.features.sections.viewmodels.SectionTrackingViewModel
import com.wapo.flagship.features.sections.viewmodels.SectionWallHelperViewModel
import com.wapo.flagship.features.sections.viewmodels.sectionsribbon.SectionsRibbonEvents
import com.wapo.flagship.features.sections.viewmodels.sectionsribbon.SectionsRibbonViewModel
import com.wapo.flagship.features.settings.AppPreferences
import com.wapo.flagship.features.settings.AppPreferences.isCASettlementDialogShown
import com.wapo.flagship.features.settings.SettingsActivity
import com.wapo.flagship.features.settings.SettingsAlertsViewModel
import com.wapo.flagship.features.shared.PaywallDialogEvents
import com.wapo.flagship.features.shared.fragments.TopBarFragment
import com.wapo.flagship.features.signin.LoginRegFragment
import com.wapo.flagship.features.subscribebanner.state.BannerEvent
import com.wapo.flagship.features.subscribebanner.state.BannerLifecycleEvent
import com.wapo.flagship.features.support.FirebaseConfigListener
import com.wapo.flagship.features.unification.UnificationHelper
import com.wapo.flagship.features.video.FullScreenVideoActivity
import com.wapo.flagship.features.video.FullScreenVideoParcel
import com.wapo.flagship.features.video.VerticalVideosParcel
import com.wapo.flagship.features.video.VideoActivity
import com.wapo.flagship.features.video.models.VideoAdResponse
import com.wapo.flagship.json.MenuSection
import com.wapo.flagship.json.TrackingInfo
import com.wapo.flagship.model.ArticleMeta
import com.wapo.flagship.navigation.ui.BottomTab
import com.wapo.flagship.navigation.ui.BottomTabFragment
import com.wapo.flagship.navigation.ui.TopBarActionItem
import com.wapo.flagship.navigation.viewmodel.navbar.NavBarEvent
import com.wapo.flagship.navigation.viewmodel.navbar.NavBarViewModel
import com.wapo.flagship.navigation.viewmodel.sectionnav.SectionNavViewModel
import com.wapo.flagship.push.PushListener
import com.wapo.flagship.push.PushMetadata
import com.wapo.flagship.push.PushPreferencesHelper
import com.wapo.flagship.sdk.iterable.IterablePlugin
import com.wapo.flagship.sdk.iterable.models.IamMessageType
import com.wapo.flagship.snackbars.model.SnackBarType
import com.wapo.flagship.snackbars.viewmodel.SnackBarViewModel
import com.wapo.flagship.util.ChartbeatManager
import com.wapo.flagship.util.ChartbeatManager.userInteracted
import com.wapo.flagship.util.ConnectivityMonitor
import com.wapo.flagship.util.JUcidTracker
import com.wapo.flagship.util.PrefUtils
import com.wapo.flagship.util.ReachabilityUtil
import com.wapo.flagship.util.ScreenCaptureCompat
import com.wapo.flagship.util.Share
import com.wapo.flagship.util.Share.Builder
import com.wapo.flagship.util.UIUtil
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.util.tracking.states.AcquisitionEntranceTypeBuilder
import com.wapo.flagship.util.tracking.states.NavigationBehavior
import com.wapo.flagship.wapomain.MainConstants.ACTION_ASK
import com.wapo.flagship.wapomain.MainConstants.ACTION_FIND
import com.wapo.flagship.wapomain.MainConstants.ACTION_GAMES
import com.wapo.flagship.wapomain.MainConstants.ACTION_LISTEN
import com.wapo.flagship.wapomain.MainConstants.ACTION_MY_POST
import com.wapo.flagship.wapomain.MainConstants.ACTION_OPEN_AUDIO_PLAYER
import com.wapo.flagship.wapomain.MainConstants.ACTION_OPEN_COMMENTS_DEEPLINK
import com.wapo.flagship.wapomain.MainConstants.ACTION_OPEN_LOGIN_REDIRECT
import com.wapo.flagship.wapomain.MainConstants.ACTION_OPEN_MAGIC_LINK
import com.wapo.flagship.wapomain.MainConstants.ACTION_OPEN_SECTION
import com.wapo.flagship.wapomain.MainConstants.ACTION_OPEN_SECTION_DEEPLINK
import com.wapo.flagship.wapomain.MainConstants.ACTION_OPEN_SECTION_FIND
import com.wapo.flagship.wapomain.MainConstants.ACTION_OPEN_SECTION_RIBBON
import com.wapo.flagship.wapomain.MainConstants.ACTION_OPEN_SIGN_IN
import com.wapo.flagship.wapomain.MainConstants.ACTION_PRINT_EDITION
import com.wapo.flagship.wapomain.MainConstants.ACTION_PROMOCODE_OFFER
import com.wapo.flagship.wapomain.MainConstants.ACTION_SHOW_PAYWALL
import com.wapo.flagship.wapomain.MainConstants.ACTION_SHOW_PAYWALL_REASON
import com.wapo.flagship.wapomain.MainConstants.ACTION_TOP_STORIES
import com.wapo.flagship.wapomain.MainConstants.ACTION_WATCH
import com.wapo.flagship.wapomain.MainConstants.ARG_LAUNCH_INTENT_STATUS
import com.wapo.flagship.wapomain.MainConstants.ATP_SHARE_ID
import com.wapo.flagship.wapomain.MainConstants.AUTH_SCHEME
import com.wapo.flagship.wapomain.MainConstants.EXTRAS_AUDIO_ID
import com.wapo.flagship.wapomain.MainConstants.EXTRAS_AUDIO_SUBTYPE
import com.wapo.flagship.wapomain.MainConstants.EXTRAS_COMMENT_ID
import com.wapo.flagship.wapomain.MainConstants.EXTRAS_COMMENT_STORY_URL
import com.wapo.flagship.wapomain.MainConstants.EXTRAS_SECTION_BUNDLE_NAME
import com.wapo.flagship.wapomain.MainConstants.EXTRAS_SECTION_ID
import com.wapo.flagship.wapomain.MainConstants.EXTRAS_SECTION_OPEN_WITHOUT_STACK
import com.wapo.flagship.wapomain.MainConstants.EXTRAS_SECTION_TITLE
import com.wapo.flagship.wapomain.MainConstants.EXTRAS_SECTION_URL
import com.wapo.flagship.wapomain.MainConstants.EXTRA_CAMPAIGN_ENTRANCE_TYPE
import com.wapo.flagship.wapomain.MainConstants.EXTRA_PAYWALL_TYPE
import com.wapo.flagship.wapomain.MainConstants.EXTRA_WALL_NAME
import com.wapo.flagship.wapomain.MainConstants.RATE_APP_MESSAGE_INTERVAL
import com.wapo.flagship.wapomain.MainConstants.SAVED_STATE_CONTAINER_KEY
import com.wapo.flagship.wapomain.MainConstants.SAVED_STATE_CURRENT_TAB_KEY
import com.wapo.flagship.wapomain.MainConstants.SAVED_STATE_CURRENT_TAB_ROUTE_KEY
import com.wapo.flagship.wapomain.MainConstants.SAVED_STATE_NAV_KEY
import com.wapo.flagship.wapomain.MainConstants.SECTION_ID_KEY
import com.wapo.flagship.wapomain.MainConstants.SHORTCUT_ALERTS
import com.wapo.flagship.wapomain.MainConstants.SHORTCUT_MY_POST
import com.wapo.flagship.wapomain.MainConstants.SHORTCUT_POLITICS
import com.wapo.flagship.wapomain.ui.MainActivityBottomBarsState
import com.wapo.flagship.wapomain.ui.MainActivityBottomTabState
import com.wapo.flagship.wapomain.ui.MainActivityFallbackState
import com.wapo.flagship.wapomain.ui.MainActivityTopAppBarState
import com.wapo.flagship.wapomain.ui.MainActivityUI
import com.wapo.flagship.wapomain.ui.MainActivityUIEvent
import com.wapo.flagship.wapomain.ui.MainActivityUIState
import com.wapo.flagship.wapomain.ui.ToolBarUIState
import com.wapo.view.habittiles.ArticleSource
import com.wapo.view.habittiles.Tile
import com.wapo.view.tooltip.TooltipPopupManager
import com.washingtonpost.android.R
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.Config
import com.washingtonpost.android.follow.activity.FollowActivity
import com.washingtonpost.android.paywall.PaywallReactive
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.api.VerifyState
import com.washingtonpost.android.paywall.auth.AuthEntryPoint
import com.washingtonpost.android.paywall.auth.AuthHelper
import com.washingtonpost.android.paywall.auth.AuthIntentBuilder
import com.washingtonpost.android.paywall.bottomsheet.ui.PaywallSheet2Fragment
import com.washingtonpost.android.paywall.bottomsheet.viewmodel.PostIterableEventType
import com.washingtonpost.android.paywall.bottomsheet.viewmodel.PostIterableEventViewModel
import com.washingtonpost.android.paywall.features.promocodes.fragments.PromoCodeFetcherDialog
import com.washingtonpost.android.paywall.helper.WpPaywallHelper
import com.washingtonpost.android.paywall.models.DisplayTriggerType
import com.washingtonpost.android.paywall.reminder.ReminderScreenFragment
import com.washingtonpost.android.paywall.reminder.accounthold.AccountHoldFragment
import com.washingtonpost.android.paywall.util.EMPTY_STRING
import com.washingtonpost.android.paywall.util.PaywallConstants
import com.washingtonpost.android.paywall.util.PaywallConstants.WALL_NAME_AMAZON_MAIN
import com.washingtonpost.android.paywall.util.PaywallConstants.WallType
import com.washingtonpost.android.paywall.util.PaywallUtil
import com.washingtonpost.android.save.OneTrustProvider
import com.washingtonpost.android.save.SaveActivity
import com.washingtonpost.android.volley.VolleyError
import com.washingtonpost.android.volley.toolbox.ImageLoaderProvider
import com.washingtonpost.android.wapocontent.ILoader
import com.washingtonpost.android.wapocontent.LoaderProvider
import com.washingtonpost.customnav.viewmodel.CustomNavViewModel
import com.washingtonpost.foryou.ForYouActivity
import com.washingtonpost.foryou.data.RecommendationsItem
import com.washingtonpost.foryou.data.getURL
import com.washingtonpost.foryou.repo.ForYouFeedRepositoryImpl
import com.washingtonpost.foryou.ui.ForYouFragment
import com.washingtonpost.userhistory.viewmodel.UserHistoryViewModel
import com.wpds.theme.AndroidClassicTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import rx.Observable
import toWapoViewsTile
import java.lang.ref.WeakReference
import javax.inject.Inject

@AndroidEntryPoint
open class MainActivity :
    FusionActivity(),
    OpenFragment,
    SectionsTracker,
    SectionsTrackerProvider,
    ConnectivityActivity,
    AlertsActivityInterface,
    AlertManagerProvider,
    ImageLoaderProvider,
    SectionActivity,
    LoaderProvider,
    AdViewFactoryProvider,
    PostTvActivity,
    AudioActivity,
    SaveActivity,
    PaywallVerifyCallsSuppressHelper,
    GridActivity,
    FollowActivity,
    PaywallDialogEvents,
    OneTrustProvider,
    ForYouActivity,
    IterablePlugin.IterableActivity {

    @Inject
    lateinit var ageRestrictionValidator: AgeRestrictionValidator

    @Inject
    lateinit var adService: AdService

    companion object {
        const val TAG = "MainActivity"
        private const val AGE_RESTRICTION_INITIAL_RETRY_DELAY_MILLIS = 1_000L
    }

    private var containerId = R.id.bottom_tabs_main_container

    /**
     * Stores a pending mWeb URL when a signed-out user taps the ad-free CTA.
     * After sign-in completes, we fetch a nonce and redirect to this URL.
     */
    private var pendingAdFreeRedirectUrl: String? = null

    /**
     * Set to true after the sign-in fragment is dismissed (sign-in completed).
     * This ensures [processPendingAdFreeRedirect] only fires for the verify
     * that happens as a result of sign-in, not a periodic background check.
     */
    private var awaitingPostSignInVerify = false

    private var onboardingFragment: WeakReference<OnboardingFragment>? = null

    private val mainActivityViewModel: MainActivityViewModel by viewModels()

    val onboardingViewModel: OnboardingViewModel by viewModels()

    private val sectionNavViewModel: SectionNavViewModel by viewModels()

    internal val customNavViewModel: CustomNavViewModel by viewModels()
    private val sectionsHabitTilesViewModel: SectionsHabitTilesViewModel by viewModels()
    private val personalizedPodcastViewModel: PersonalizedPodcastViewModel by viewModels()

    private val userHistoryViewModel: UserHistoryViewModel by viewModels()

    private val backendHealthCheckerViewModel: BackendHealthCheckerViewModel by viewModels()

    private val sectionInlineOfferViewModel: SectionInlineOfferViewModel by viewModels()

    private val alertsViewModel: SettingsAlertsViewModel by viewModels()

    private val sharedScrollViewModel: SharedScrollViewModel by viewModels()

    private val askThePostViewModel: AskThePostViewModel by viewModels()

    private var reviewAppDialog: Dialog? = null
    private var updateAppDialog: Dialog? = null
    private var splashScreenViewProvider: SplashScreenViewProvider? = null

    //    private var mWearCompat: WearCompat? = null
    private val sectionWallHelperViewModel: SectionWallHelperViewModel by viewModels()

    private val postIterableActivityViewModel: PostIterableEventViewModel by viewModels()

    private val hideSubscribeBanner: Boolean
        get() {
            // If value is missing from config, default to showing the banner
            val subscribeBannerEnabled =
                ConfigManager.getInstance().config.paywallConf
                    .frontSubscriptionBanner
                    ?.enabled ?: true
            return !subscribeBannerEnabled || isLowDataModeEnable
        }

    /**
     * Handle Tab fragment stack saved state
     */
    private var savedStateSparseArray = SparseArray<Fragment.SavedState>()
    private var currentSelectItemId = 0

    /**
     * Handle Compose Top & Bottom Nav Bars' Logic & UI State
     */
    private val navBarViewModel: NavBarViewModel by viewModels()

    /**
     * Handle Compose Snackbar display logic
     */
    private val snackbarViewModel: SnackBarViewModel by viewModels()

    /**
     * VM to replace old interface calls (SectionTracker) and better handle tracking
     */
    private val sectionTrackingViewModel: SectionTrackingViewModel by viewModels()

    /**
     * To observe section ribbon changes
     */
    private val sectionsRibbonViewModel: SectionsRibbonViewModel by viewModels()

    /**
     * Compose navigation. Defined outside the Composable for state restoration
     */
    private lateinit var navController: NavHostController

    private var isSplashDismissed by mutableStateOf(false)

    private var isOnboardingVisible = mutableStateOf(false)

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted: Boolean ->
            if (isGranted) {
                FlagshipApplication.Companion.getInstance().alertsSettings.getAlertsTopicsList()
                    .forEach {
                        // fire push_topic_enroll for auto-subscribed topics
                        if (it.isEnabled) {
                            Measurement.trackAlertTopicEnroll(it.topic.topicKey, "prompt", true)
                        }
                    }
                openAlertsActivity(true)
            }
        }

    private lateinit var talkToThePostBottomSheetFragmentFactory: TalkToThePostBottomSheetFragmentFactory

    private var pendingTalkToThePostData: UserEvent.TryToOpenTalkToThePost? = null
    private var paywallPauseSessionActive = false
    private var wasAudioPlayingBeforePaywall = false

    private val paywallAudioLifecycleCallbacks =
        object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentAttached(
                fm: FragmentManager,
                fragment: Fragment,
                context: android.content.Context
            ) {
                if (fragment is PaywallSheet2Fragment) {
                    onPaywallWillShow()
                }
            }

            override fun onFragmentDetached(fm: FragmentManager, fragment: Fragment) {
                if (fragment is PaywallSheet2Fragment) {
                    tryResumeAudioAfterPaywallDismiss(force = true)
                }
            }
        }

    private val recordAudioPermissionRequest: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            val isFirstDenial = ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.RECORD_AUDIO
            )
            if (isGranted) {
                handleRecordAudioPermissionGranted(isFirstDenial)
            } else {
                talkToThePostBottomSheetFragmentFactory.showPermissionAlertDialog(
                    this,
                    isFirstDenial
                )
                pendingTalkToThePostData = null
            }
        }

    private fun handleRecordAudioPermissionGranted(isFirstDenial: Boolean) {
        askThePostViewModel.clearLiveConversationData()

        val data = pendingTalkToThePostData
        val timestamp = data?.timestamp ?: 0f
        val transcript = data?.transcript ?: ""
        val audioProvider = AudioProviderImpl()

        talkToThePostBottomSheetFragmentFactory.tryToShowTalkFragment(
            parentActivity = this,
            parentFragmentManager = supportFragmentManager,
            conversationId = askThePostViewModel.uiState.value.conversationId,
            surfaceName = askThePostViewModel.askSamEntryPoint?.name?.lowercase(),
            isFirstDenial = isFirstDenial,
            recordAudioPermissionRequest = recordAudioPermissionRequest,
            audioProvider = audioProvider,
            timestamp = timestamp,
            transcript = transcript,
        )

        pendingTalkToThePostData = null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        processIntentOnceRibbonIsReady(intent)
    }

    private var hasRestoredState: Boolean = false

    private var pendingRibbonIntent: Intent? = null

    private var counterAgeRestrictionCalls = 0
    private val ageRestrictionMaxCalls = 2
    private var ageRestrictionsFakeSetUp: AgeRestrictionsFakeSetUp? = null

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().let { splashScreen ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                splashScreen.setOnExitAnimationListener {
                    splashScreenViewProvider = it
                    splashViewModel.splashReadyToBeDismissed.value?.let { currentSplashState ->
                        if (currentSplashState.dismissSplash) {
                            splashScreenViewProvider?.remove()
                            isSplashDismissed = true
                        }
                    }
                }
            }
        }
        super.onCreate(savedInstanceState)
        supportFragmentManager.registerFragmentLifecycleCallbacks(
            paywallAudioLifecycleCallbacks,
            false
        )
        // When sign-in completes (fragment dismissed), mark that we need to check
        // verify status before opening the pending ad-free mweb URL.
        // Verify is triggered by dispatchVerifySub(NeedsVerification) in AuthHelper
        // and completes during PostLoginActivity/Onboarding, so by the time
        // the user returns to onResume we can check the result.
        supportFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
                    if (f is LoginRegFragment && pendingAdFreeRedirectUrl != null) {
                        awaitingPostSignInVerify = true
                        PaywallReactive.resetVerifyState()
                    }
                }
            },
            false
        )
        EventTimerLog.startTimingEvent(EventTimerLog.SYNC_EVENT, EventTimerLog.FRONT_LAUNCH_TIME)
        initFirebaseRemoteConfig()
        UnificationHelper.handleUnifiedAppTasks(this)

        if (savedInstanceState != null) {
            savedStateSparseArray = savedInstanceState.getSparseParcelableArray(
                SAVED_STATE_CONTAINER_KEY,
            )
                ?: savedStateSparseArray
            currentSelectItemId = savedInstanceState.getInt(SAVED_STATE_CURRENT_TAB_KEY)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ScreenCaptureCompat.register(this) {
                if (navBarViewModel.isCurrentTab(BottomTab.Ask)) {
                    askThePostViewModel.screenShotTaken()
                }
            }
        }

        var restoredNavState = savedInstanceState?.getParcelable<Bundle>(SAVED_STATE_NAV_KEY)

        EventTimerLog.startTimingEvent(EventTimerLog.SYNC_EVENT, EventTimerLog.FRONT_LAUNCH_TIME)
        setContent {
            AndroidClassicTheme {
                navController = rememberNavController()
                navController.restoreState(restoredNavState)
                restoredNavState = null

                val persoUiState by personalizedPodcastViewModel.uiState.collectAsState()
                LaunchedEffect(Unit) {
                    persoUiState.generationState.let {
                        if (it == PodcastGenerationState.Done) {
                            //refresh habit tiles when new podcast is generated
                            sectionsHabitTilesViewModel.fetchData(true)
                        }
                    }
                }

                val navBarViewUIState = navBarViewModel.uiState.collectAsStateWithLifecycle()
                val sectionNavUIState = sectionNavViewModel.uiState.collectAsStateWithLifecycle()
                val askThePostUIState = askThePostViewModel.uiState.collectAsStateWithLifecycle()
                val sectionRibbonUIState =
                    sectionsRibbonViewModel.uiState.collectAsStateWithLifecycle()

                // Visibility of persistent player based on if audio is playing or not
                val shouldShowPersistentPlayer by audioMediaActivityViewModel.shouldShowPp.observeAsState()

                // Handle various snackbars (Network Connection, Add to Playlist, etc)
                val snackBarUIState = snackbarViewModel.uiState.collectAsStateWithLifecycle().value
                val snackBar = snackBarUIState.snackBarType

                val failoverState by backendHealthCheckerViewModel.uiState.collectAsStateWithLifecycle()

                // Low Data mode notification config
                val lowDataModeNotificationConfig = snackBarUIState.lowDataModeNotificationConfig

                // State of low data mode
                val bannerState by lowDataBannerViewModel.lowDataBannerState.observeAsState()

                val isAdFree by PaywallReactive.isAdFree.collectAsStateWithLifecycle()

                val conversationHistory = askThePostUIState.value.conversationHistory

                val mainState = MainActivityUIState(
                    appBarState = sectionNavUIState.value.topBarState,
                    destination = navBarViewUIState.value.currentTab,
                    snackbar = snackBar,
                    lowDataModeNotificationConfig = lowDataModeNotificationConfig,
                    lowDataBannerState = bannerState,
                    isLowDataModeEnable = isLowDataModeEnable,
                    isNightModeEnable = nightModeManager.isNightModeEnabled(),
                    isAdFree = isAdFree,
                    appResumeCount = AppContext.getAppResumeCount(),
                    bottomTabState = MainActivityBottomTabState(
                        bottomTabContainerId = containerId,
                        isVisibleBottomNav = navBarViewUIState.value.isVisible,
                        bottomNavTabs = navBarViewUIState.value.tabs,
                        currentTab = navBarViewUIState.value.currentTab
                    ),
                    topAppBarState = MainActivityTopAppBarState(
                        toolBarState = ToolBarUIState(
                            isPrivateMode = askThePostUIState.value.isPrivateModeEnabled,
                            inResponseScreen = askThePostUIState.value.showResponse,
                            isUserLoggedIn = PaywallService.getInstance().loggedInUser != null,
                            showTooltip = isSplashDismissed && !isOnboardingVisible.value,
                            canShareChat = askThePostUIState.value.conversationId != null
                                    && !askThePostUIState.value.isPrivateModeEnabled,
                            showScreenShotToolTip = askThePostUIState.value.screenShotTaken,
                        ),
                        showBackButton = navBarViewUIState.value.showBackButton,
                        showAlertBadge = navBarViewUIState.value.showBadge,
                        sectionTitle = sectionNavUIState.value.sectionTitle,
                        conversationHistory = conversationHistory,
                    ),
                    bottomBarsState = MainActivityBottomBarsState(
                        shouldShowPersistentPlayer = shouldShowPersistentPlayer,
                        bottomSheetPrompt = sectionNavUIState.value.bottomSheetPrompt
                    ),
                    fallbackState = MainActivityFallbackState(
                        failoverState = failoverState
                    )
                )
                MainActivityUI(
                    state = mainState,
                    navController = navController,
                    onUIEvent = { event ->
                        handleMainUIEvent(event)
                    }
                )

                // Launch just one call to the first navigation
                LaunchedEffect(Unit) {
                    switchBottomTab(navBarViewModel.uiState.value.currentTab, true)
                }
            }
        }

        sectionsRibbonViewModel.initVisitedNewsprintSection(
            PrefUtils.getNewsprintHasVisitedSection(applicationContext),
        )

        splashViewModel.splashReadyToBeDismissed.observe(this) {
            if (!it.loadingAgeRestriction) {
                if (it.dismissSplash) {
                    splashScreenViewProvider?.remove()
                    isSplashDismissed = true
                }
                if (it.navigateToBlockAgeRestriction) {
                    val intent = AgeRestrictionActivity.getAgeRestrictionActivityIntent(
                        context = this,
                        ageRestrictionsScreenMode = AgeRestrictionsScreenMode.BlockAgeRestriction
                    )
                    startActivity(intent)
                    finish()
                }
                if (it.navigateToParentPermissionsBlockRestriction) {
                    val intent = AgeRestrictionActivity.getAgeRestrictionActivityIntent(
                        context = this,
                        ageRestrictionsScreenMode = AgeRestrictionsScreenMode.ParentPermissionsBlockRestriction
                    )
                    startActivity(intent)
                    finish()
                }
                if (it.navigateToUnknownState) {
                    val intent = AgeRestrictionActivity.getAgeRestrictionActivityIntent(
                        context = this,
                        ageRestrictionsScreenMode = AgeRestrictionsScreenMode.UnknownBlockAgeRestriction
                    )
                    startActivity(intent)
                    finish()
                }
                if (it.navigateToVisitPlayStore) {
                    val intent = AgeRestrictionActivity.getAgeRestrictionActivityIntent(
                        context = this,
                        ageRestrictionsScreenMode = AgeRestrictionsScreenMode.VisitPlayStoreBlockRestriction
                    )
                    startActivity(intent)
                    finish()
                }
                if (it.dismissSplash && it.showModalAgeRestrictionUpgrade && !AppContext.updateAgeRestrictionDialogShown()) {
                    val dialog = DialogFactory.getUpdateRestriction(
                        context = this,
                        canDismiss = true
                    ) {}
                    dialog.show()
                }
                if (it.dismissSplash && it.showModalAgeRestrictionError) {
                    val block = it.blockUser
                    val dialog = DialogFactory.getErrorAgeRestriction(
                        context = this,
                        canDismiss = true,
                        title = it.showModalAgeRestrictionErrorTitle,
                        message = it.showModalAgeRestrictionErrorMessage
                    ) {
                        if (block) {
                            finish()
                        }
                    }
                    dialog.show()
                }
                if (it.retryCall) {
                    if (counterAgeRestrictionCalls < ageRestrictionMaxCalls) {
                        counterAgeRestrictionCalls += 1
                        lifecycleScope.launch {
                            delay(ageRestrictionRetryDelayMillis(counterAgeRestrictionCalls))
                            if (!isFinishing && !isDestroyed) {
                                splashViewModel.isAgeEligible(
                                    activity = this@MainActivity,
                                    ageRestrictionValidator = ageRestrictionValidator,
                                    ageRestrictionsFakeSetUp = ageRestrictionsFakeSetUp,
                                )
                            }
                        }
                    } else {
                        val intent = AgeRestrictionActivity.getAgeRestrictionActivityIntent(
                            context = this,
                            ageRestrictionsScreenMode = AgeRestrictionsScreenMode.UnknownBlockAgeRestriction
                        )
                        startActivity(intent)
                        finish()
                    }
                } else {
                    counterAgeRestrictionCalls = 0
                }
                PrefUtils.setAgeRestrictionPref(this, AgeRestrictionsFakeSetUp())
            }
        }

        ageRestrictionsFakeSetUp = if (AppContextUtils.isDebuggableBuild()) {
            PrefUtils.getAgeRestrictionPref(this)
        } else null

        Logger.d(TAG, "call isAgeEligible")
        splashViewModel.isAgeEligible(
            activity = this,
            ageRestrictionValidator = ageRestrictionValidator,
            ageRestrictionsFakeSetUp = ageRestrictionsFakeSetUp
        )

        observeMainEvents()
        observeNavBarEvents()
        observeVerifyComplete()

        observeOnboardingEvents()
        observeIapState()
        observeIapStatusForOnboarding()
        observeBannerEvents()
        observeBannerMessage()
        observeLowDataBannerState()
        observePaywallEvents()
        observeGiftCollabEvent()
        observeAddedToPlaylistEvent()
        observeSectionsRibbonEvents()
        observeSectionTracking()
        observePodcastMetadata()

        // FusionActivity observers
        observeEllipsisClick()
        observeActionShare()
        observeActionComments()
        observeSaveOrRemoveArticle()
        observeSaveOrRemoveRecipe()
        observeGiftTapEvents()
        observeAddToPlayListTapEvents()
        observeAudioPlayerEllipsisClick()
        observeRemoveFromPlayListEvents()
        observePlaylistClickTrackEvent()
        observeVideoClickEvent()
        observeAdsContentState()
        observeFeedbackLinkClickEvent()
        observeAskThePostEvents()
        observeFeedbackSubmittedEvent()
        observePostIterableSignInEvent()
        observeBottomSheetPromptState()

        // Update MainActivity launch counts
        var countRuns = AppContext.getCountRuns()
        countRuns++
        AppContext.setCountRuns(countRuns)
        if (intent.data != null) {
            if (intent.getBooleanExtra(PUSH_ORIGINATED, false)) {
                ChartbeatManager.setPushReferrer("Push Alert")
                ChartbeatManager.setAppReferrer("Push Alerts")
            } else {
                ChartbeatManager.setAppReferrer("Deep Link")
            }
        } else {
            ChartbeatManager.setAppReferrer("Direct")
        }

        habitTilesPlugin =
            HabitTilesPlugin(
                contentManagerObs = getContentManagerObs(),
                onHabitTilesFetchData = { shouldBypassCache ->
                    sectionsHabitTilesViewModel.fetchData(shouldBypassCache)
                },
                onUserHistoryAddClickedToHabitTile = { link ->
                    userHistoryViewModel.addClickedToHabitTileViewedItem(link)
                }
            )
        lifecycle.addObserver(habitTilesPlugin)

        iterablePlugin = IterablePlugin(this, iterableActivityViewModel).also {
            lifecycle.addObserver(it)
        }

        hasRestoredState = savedInstanceState != null

        AppPreferences.setIsLowDataModeEnabled(false)

        talkToThePostBottomSheetFragmentFactory =
            TalkToThePostBottomSheetFragmentFactory() { onAskSamEvent(it) }
    }

    private fun ageRestrictionRetryDelayMillis(attempt: Int): Long =
        AGE_RESTRICTION_INITIAL_RETRY_DELAY_MILLIS * (1L shl (attempt - 1))

    private fun handleMainUIEvent(event: MainActivityUIEvent) {
        when (event) {
            is MainActivityUIEvent.DetermineTopBarState -> {
                sectionNavViewModel.determineTopBarState(
                    y = event.y,
                    hasSectionTitle = event.hasSectionTitle,
                )
            }

            is MainActivityUIEvent.SwitchBottomTab -> {
                switchBottomTab(event.clickedTab)
            }

            is MainActivityUIEvent.FallbackArticleClicked -> {
                val intent = builder()
                    .setArticleSingleUrl(event.contentUrl)
                    .failoverOriginated(true)
                    .buildIntent(this@MainActivity)
                startActivity(intent)
            }

            is MainActivityUIEvent.FallbackCallSite -> {
                DeepLinksProcessor.redirectToBrowser(
                    this@MainActivity,
                    event.url
                )
            }

            MainActivityUIEvent.FallbackRetry -> {
                checkBackendHealth()
            }

            is MainActivityUIEvent.HandleTopBarAction -> {
                navBarViewModel.handleTopBarAction(event.topBarActionItem)
            }

            MainActivityUIEvent.LowDataModeModalSeen -> {
                Measurement.trackLowDataModeModalSeen(pageNameFromTracking)
            }

            MainActivityUIEvent.LowDataModeAllow -> {
                snackbarViewModel.allowLowDataModeNotification()
                Measurement.trackLowDataModeModalAllow(pageNameFromTracking)
            }

            MainActivityUIEvent.LowDataModeDismiss -> {
                snackbarViewModel.disableLowDataModeNotification()
                Measurement.trackLowDataModeModalNotAllow(pageNameFromTracking)
            }

            MainActivityUIEvent.LowDataModeSnooze -> {
                snackbarViewModel.snoozeLowDataModeNotification()
                Measurement.trackLowDataModeModalSnooze(pageNameFromTracking)
            }

            MainActivityUIEvent.OnBackPressed -> {
                onBackPressedImpl()
            }

            is MainActivityUIEvent.OnNotificationIconClick -> {
                onboardingViewModel.pushPromptResponse(event.response)
            }

            MainActivityUIEvent.OpenListenToThePost -> {
                sharedScrollViewModel.triggerScrollToPlaylist()
                navBarViewModel.navigateToTab(BottomTab.Listen)
            }

            MainActivityUIEvent.ShowSnackbar -> {
                snackbarViewModel.showSnackBar(null)
            }

            MainActivityUIEvent.SnackBarDismiss -> {
                snackbarViewModel.hideSnackBar(pageNameFromTracking)
            }

            MainActivityUIEvent.SnackBarOpenSettings -> {
                Utils.openNetworkSettings(
                    this@MainActivity,
                )
            }

            MainActivityUIEvent.SnackBarTurnOnLowDataMode -> {
                lowDataBannerViewModel.updateLowDataBanner(true)
                snackbarViewModel.hideSnackBar(pageNameFromTracking)
                Measurement.trackLowDataModeNotificationOn(
                    pageNameFromTracking,
                )
            }

            MainActivityUIEvent.SnackBarResultDismissed -> {
                snackbarViewModel.hideSnackBar(null)
            }

            is MainActivityUIEvent.BottomPromptActionClicked -> {
                globalBannerViewModel.setBannerEvent(BannerEvent.BannerClicked(event.message))
            }

            is MainActivityUIEvent.BottomPromptDismissed -> {
                iterablePlugin.dismissBanner(
                    event.message,
                    sectionsRibbonViewModel.getCurrentSection()?.bundleName
                )
                sectionNavViewModel.showBottomSheetPrompt(null)
            }
        }
    }

    private fun observeBottomSheetPromptState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sectionNavViewModel.uiState
                    .map { it.bottomSheetPrompt }
                    .distinctUntilChanged()
                    .collect {
                        val bottomMargin = if (it != null) {
                            val isSubActive = PaywallService.getInstance().isPremiumUser
                            val campaignEntranceType = AcquisitionEntranceTypeBuilder.build(
                                IamMessageType.FRONT_HOME_SCROLL.type,
                                it.messageTracking?.campaignName,
                                it.attributionInfo.campaignId,
                                false
                            )
                            Measurement.trackSignInPromptShown(
                                isSubActive,
                                campaignEntranceType,
                                sectionsRibbonViewModel.getCurrentSection()?.bundleName
                            )
                            iterablePlugin.handleBannerLifecycleEvent(
                                BannerLifecycleEvent.StartImpression(
                                    it.attributionInfo
                                )
                            )
                            // Use a dimension resource for the prompt's height
                            resources.getDimensionPixelSize(R.dimen.min_sign_in_prompt_height)

                        } else {
                            val homePageMessage =
                                iterableActivityViewModel.getBannerForPlacement(IamMessageType.FRONT_HOME_SCROLL)
                            homePageMessage?.attributionInfo?.let { attributionInfo ->
                                iterablePlugin.handleBannerLifecycleEvent(
                                    BannerLifecycleEvent.EndImpression(
                                        attributionInfo
                                    )
                                )
                            }
                            0
                        }
                        updateFragmentMargin(bottomMargin)
                    }
            }
        }
    }

    private fun observeMainEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainActivityViewModel.mainEvent.collect { event ->
                    when (event) {
                        is MainActivityEvent.OnGetArticles -> {
                            continueGetArticlesList(
                                articlesUrls = event.articlesUrls,
                                bundle = event.bundle,
                                widgetType = event.widgetType,
                                articleContentUrl = event.articleContentUrl,
                                sectionDisplayName = event.sectionDisplayName
                            )
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    private fun observeNavBarEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                navBarViewModel.navBarEvent.collect { event ->
                    when (event) {
                        is NavBarEvent.TopBarAction -> {
                            when (event.item) {
                                TopBarActionItem.Alerts -> {
                                    if (!PushPreferencesHelper.areNotificationsEnabled(this@MainActivity)) {
                                        PushPreferencesHelper.showNotificationsBlockedDialog(
                                            supportFragmentManager
                                        )
                                    } else {
                                        openAlertsActivity(
                                            showAlertsSettings = !PushPreferencesHelper.isAnyAlertSubscribed()
                                        )
                                    }
                                }

                                TopBarActionItem.Settings -> openSettingsActivity()
                                TopBarActionItem.Back -> {
                                    onBackPressedImpl()
                                    askThePostViewModel.clearLiveConversationData()
                                }

                                TopBarActionItem.MyPost -> openMyPostActivity()
                                TopBarActionItem.EllipsisHow -> openHowThePostWorksSheet()
                                TopBarActionItem.EllipsisWhy -> openLearnMore()
                                TopBarActionItem.PrivateMode -> privateModeToggled()
                                TopBarActionItem.History -> openHistory()
                                TopBarActionItem.NewChat -> {
                                    if (askThePostViewModel.uiState.value.showRegisterInHistoryView) {
                                        shouldStartNewChat()
                                    } else {
                                        startNewChat()
                                    }
                                }

                                TopBarActionItem.Search -> openFindActivity()
                                TopBarActionItem.Share -> {
                                    if (askThePostViewModel.uiState.value.showRegisterInHistoryView) {
                                        askThePostViewModel.showRegisterBeforeShareModal(ShareType.CONVO)
                                    } else {
                                        shareConversation()
                                    }
                                }
                                TopBarActionItem.GiveFeedback -> {
                                    (askThePostViewModel.uiState.value.conversationHistory.lastOrNull() as? ConversationItem.Sources)?.turnId?.let { lastConversationId ->
                                        FeedbackFragment(
                                            feedbackType = FeedbackFragment.FeedbackType.ASK_THE_POST_ARTICLE,
                                            endpoint = EMPTY_STRING,
                                            responseId = lastConversationId,
                                        ).show(
                                            supportFragmentManager,
                                            FEEDBACK_TAG,
                                        )
                                    }
                                }
                            }
                        }

                        is NavBarEvent.NavEvent -> {
                            val fragmentManager = supportFragmentManager
                            val newFragment = BottomTabFragment.newInstance(event.tab)
                            fragmentManager.commit {
                                saveAndRetrieveFragment(
                                    fragmentManager,
                                    event.tab.ordinal,
                                    newFragment
                                )
                                replace(containerId, newFragment, event.tab.route)
                                sectionNavViewModel.reloadTitle()
                            }

                            supportFragmentManager.registerFragmentLifecycleCallbacks(
                                object : FragmentManager.FragmentLifecycleCallbacks() {
                                    override fun onFragmentResumed(
                                        fm: FragmentManager,
                                        fragment: Fragment
                                    ) {
                                        super.onFragmentResumed(fm, fragment)

                                        if (fragment is BottomTabFragment) {
                                            fm.unregisterFragmentLifecycleCallbacks(this) // one-time

                                            if (!hasRestoredState && intentHelper.shouldProcessIntent(
                                                    intent
                                                )
                                            ) {
                                                processIntentOnceRibbonIsReady(intent)
                                            }
                                        }
                                    }
                                }, false
                            )
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    private fun observeVerifyComplete() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                PaywallReactive.verifyComplete
                    .filterNotNull()
                    .collect {
                        // Verify just finished; connector.caSettlementValues may have
                        // been populated by WPPaywallApiService.showCASettlementDialogIfNeeded.
                        // showNextDialog() is idempotent (prefCASettlementShown pref +
                        // caSettlementDialog?.isAdded guard), so calling it on every emission
                        // while RESUMED is safe.
                        showNextDialog()
                    }
            }
        }
    }


    override fun showSources(sources: List<ArticleSource>) {
        val carouselItems = sources.mapIndexed { index, articleSource ->
            CarouselUIItem(
                id = index,
                url = "https://www.washingtonpost.com${articleSource.canonicalUrl}",
                publishDateMillis = articleSource.publishDate?.toDateLong() ?: 0L,
                content = articleSource.headline ?: "",
                imageUrl = articleSource.imageUrl ?: "",
                passages = listOf()
            )
        }

        askThePostViewModel.showSourceSheetWith(carouselItems)
        SourceBottomSheet(SourceBottomSheetCaller.PERSOPOD).show(
            supportFragmentManager,
            "SourceBottomSheetTag"
        )
    }

    override fun onLoadMore(fetchingRecs: Boolean) {
        if (fetchingRecs) {
            Measurement.trackForYouScroll()
        }
    }

    override fun openHabitTileLink(link: String?) {
        super.openHabitTileLink(link)
    }

    override fun onHabitTileClicked(destinationUrl: String) {
        openHabitTileLink(destinationUrl)
    }

    private fun observeAskThePostEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                askThePostViewModel.askThePostEvent.collect { event ->
                    when (event) {
                        is AskThePostEvent.InitiatePostAnswersFeedbackEvent -> {
                            val feedbackType =
                                if (navBarViewModel.isRoute(BottomTab.Ask.route)) {
                                    FeedbackFragment.FeedbackType.POST_ANSWERS
                                } else {
                                    FeedbackFragment.FeedbackType.ASK_THE_POST_ARTICLE
                                }
                            FeedbackFragment(
                                feedbackType,
                                event.feedBackEvent.first,
                                event.feedBackEvent.second,
                                event.feedBackEvent.third,
                            ).show(
                                supportFragmentManager,
                                "feedback_fragment",
                            )
                        }

                        is AskThePostEvent.ShareUrlStateEvent -> {
                            val askThePost = getString(R.string.ask_the_post)

                            when (event.shareUrlState) {
                                is ShareUrlState.Copy -> {
                                    val link = generateShareLink(event.shareUrlState.shareId)
                                    val clipboard = applicationContext.getSystemService(
                                        CLIPBOARD_SERVICE
                                    ) as android.content.ClipboardManager
                                    val clip: ClipData = ClipData.newPlainText(askThePost, link)
                                    clipboard.setPrimaryClip(clip)
                                }

                                is ShareUrlState.SuccessShare -> {
                                    val shareUrl = generateShareLink(event.shareUrlState.shareId)
                                    Builder()
                                        .shareUrl(shareUrl)
                                        .headline(askThePost)
                                        .fromPush(false)
                                        .isVerticalVideoShare(false)
                                        .build()
                                        .shareItem(this@MainActivity)
                                    askThePostViewModel.resetShareState()
                                }

                                else -> {
                                    Logger.e(
                                        TAG,
                                        "Unexpected ShareUrlState: ${event.shareUrlState}"
                                    )
                                }
                            }

                        }

                        is AskThePostEvent.UserEvent -> {
                            when (event.userEvent) {
                                is com.wapo.flagship.features.search2.events.UserEvent.PostAnswersCarouselItemClick -> {
                                    val intent = builder()
                                        .setArticleSingleUrl(event.userEvent.url)
                                        .buildIntent(this@MainActivity)
                                    startActivity(intent)
                                }

                                else -> {}
                            }
                        }

                        is AskThePostEvent.ScreenShotTaken -> {
                            if (event.screenShotTaken) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                    ScreenCaptureCompat.unregister(this@MainActivity)
                                }
                            }
                        }

                        else -> {
                            Logger.e(TAG, "Unexpected event state: $event")
                        }
                    }
                }
            }
        }
    }

    override fun shouldCheckBackendHealth(): Boolean {
        return true
    }

    private fun generateShareLink(shareId: String): String {
        val shareUrl =
            DeepLinksProcessor.ASK_THE_POST_SHARE_URL
        val link = shareUrl.toUri()
            .buildUpon()
            .appendQueryParameter("share_id", shareId)
            .toString()
        return link
    }

    private fun updateFragmentMargin(bottomMargin: Int) {
        // Find the current visible fragment
        val currentFragment = supportFragmentManager.findFragmentById(containerId)
        // If it's a BottomTabFragment, get its currently displayed child fragment
        val contentFragment = if (currentFragment is BottomTabFragment) {
            currentFragment.childFragmentManager.primaryNavigationFragment
                ?: currentFragment.childFragmentManager.fragments.firstOrNull()
        } else {
            currentFragment
        }
        // Check if the fragment can have its margin updated and call the method
        (contentFragment as? MarginUpdatable)?.updateBottomMargin(bottomMargin)
    }

    private fun observePostIterableSignInEvent() {
        postIterableActivityViewModel.postSignInOrSubscribeIterableEvent.observe(this) { event ->
            iterableActivityViewModel.syncInAppMessages()
            PaywallService.getConnector().onSubscriptionStatusChanged(true);
            when (event) {
                PostIterableEventType.SIGN_IN_OR_REGISTER -> {
                    if (PaywallService.getInstance().isWpUserLoggedIn) {
                        sectionNavViewModel.showBottomSheetPrompt(null)
                    }
                }

                else -> {}
            }
        }
    }

    private fun observeFeedbackSubmittedEvent() {
        feedbackCollaborationViewModel.feedbackSubmittedEvent.observe(this) {
            if (!navBarViewModel.isRoute(BottomTab.Ask.route)) {
                Measurement.trackArticleSummaryFeedbackSubmitEvent(
                    mainViewModel.activeRecommendationArticle.value?.pageName,
                    mainViewModel.activeRecommendationArticle.value?.arcId,
                    null,
                    MISCELLANY_SUFFIX_REALTIME
                )
            } else {
                Measurement.trackAskThePostFeedBackSubmitted()
            }
            val feedbackType =
                if (navBarViewModel.isRoute(BottomTab.Ask.route)) FeedbackFragment.FeedbackType.POST_ANSWERS else FeedbackFragment.FeedbackType.ASK_THE_POST_ARTICLE

            FeedbackStatusFragment(feedbackType).show(
                this.supportFragmentManager,
                "feedback_status_fragment",
            )
        }
    }

    override fun onSummaryIconClicked(recommendationsItem: RecommendationsItem) {
        recommendationsItem.summary ?: return
        mainViewModel.activeRecommendationArticle.postValue(recommendationsItem)
        // use the summary from recommendationsItem
        val summary =
            recommendationsItem.summary?.run {
                ArticleSummary(
                    url = url,
                    title = null,
                    disclaimer = disclaimer,
                    keyPointsHeading = keyPointsHeading,
                    overview = summary,
                    keyPoints =
                        keyPoints
                            ?.filter { it?.isNotEmpty() == true }
                            ?.map { it },
                    modelId = modelId,
                )
            }
        summaryCollaborationViewModel.apply {
            setArticleId(recommendationsItem.arcId)
            setForYouSummary(summary)
            enableFeedback(SummariesFeatureFlag.realtimeFeedbackEnabled())
        }

        // Track the summary seen event
        Measurement.trackArticleSummarySeenEvent(
            recommendationsItem.pageName,
            recommendationsItem.arcId,
            null,
            MISCELLANY_SUFFIX_REALTIME,
        )

        // Show the summary fragment
        SummaryFragment().show(supportFragmentManager, "summary_fragment")
    }

    override fun toggleNavBars(scrollY: Float, hasSectionTitle: Boolean) {
        sectionNavViewModel.determineTopBarState(
            y = scrollY,
            hasSectionTitle = hasSectionTitle,
        )
    }

    private fun observeFeedbackLinkClickEvent() {
        // track the feedback link click event
        feedbackCollaborationViewModel.feedbackLinkClickEvent.observe(this) { type ->
            Measurement.trackArticleSummaryFeedbackSeenEvent(
                mainViewModel.activeRecommendationArticle.value?.pageName,
                mainViewModel.activeRecommendationArticle.value?.arcId,
                null,
                MISCELLANY_SUFFIX_REALTIME,
            )

            val feedbackType = if (type == FeedbackFragment.FeedbackType.ARTICLE_SUMMARIES) {
                FeedbackFragment.FeedbackType.ARTICLE_SUMMARIES
            } else {
                if (navBarViewModel.isRoute(BottomTab.Ask.route)) {
                    FeedbackFragment.FeedbackType.POST_ANSWERS
                } else {
                    FeedbackFragment.FeedbackType.ASK_THE_POST_ARTICLE
                }
            }

            FeedbackFragment(feedbackType).show(
                supportFragmentManager,
                "feedback_fragment"
            )
        }
    }

    private fun userAccepted() {
        lifecycle.addObserver(
            NotificationPermissionRequestPlugin(this, notificationPermissionLauncher) {
                true
            },
        )
    }

    private fun observeOnboardingEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                onboardingViewModel.onboardingEvent.collect { event ->
                    when (event) {
                        is UserClickEvent -> {
                            when (event) {
                                is UserClickEvent.Subscribe -> {
                                    showPaywallDialog(
                                        PaywallConstants.ONBOARDING,
                                        WallType.ONBOARDING_PAYWALL,
                                    )
                                }

                                is UserClickEvent.SignIn -> {
                                    onboardingViewModel.logOutUser {
                                        PaywallService.getConnector().showSignInScreen(
                                            supportFragmentManager,
                                            AuthIntentBuilder().build(),
                                            null,
                                            WallType.ONBOARDING_PAYWALL,
                                            false,
                                            null
                                        )
                                    }
                                }

                                is UserClickEvent.Dismiss -> {
                                    isOnboardingVisible.value = false
                                    // TODO: Check if this logic is necessary
                                    mainActivityViewModel.fetchRecommendationsData(
                                        ForYouFeedRepositoryImpl.SURFACE_FEED,
                                        !intent.getBooleanExtra(PUSH_ORIGINATED, false),
                                    )
                                }

                                else -> {}
                            }
                        }

                        is OnboardingEvent.AcceptsPushNotifications -> {
                            if (event.accepted) {
                                userAccepted()
                            }
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    // This method should be removed on data layer changes
    private fun observeIapStatusForOnboarding() {
        onboardingViewModel.iapStatusKnown.observe(this) {
            if (it) {
                if (onboardingFragment?.get()?.isVisible != true &&
                    onboardingFragment?.get()?.isAdded != false &&
                    PaywallService.getInstance() != null &&
                    onboardingViewModel.shouldShow()
                ) {
                    onboardingFragment =
                        WeakReference(
                            OnboardingFragment().apply {
                                supportFragmentManager
                                    .beginTransaction()
                                    .add(android.R.id.content, this)
                                    .commit()
                            },
                        )
                    isOnboardingVisible.value = true
                }
            }
        }
    }

    /**
     * Observer for Iap State changes if user was in Grace Period or On Hold and has fixed their payment.
     */
    private fun observeIapState() {
        PaywallService.getInstance()?.apply {
            PaywallService.getConnector()?.iapSubStatus?.observe(this@MainActivity) {
                // This will help us determine that the user has fixed the payment as the IapSubState is Active
                // while the stored verify response still holds GracePeriod/OnHold flag
                if (it == PaywallConstants.IapSubStatus.ACTIVE && (isSubInGracePeriod || isSubOnHold)) {
                    // get their iterable message, otherwise use local banners
                    val globalBannerMessage =
                        iterableActivityViewModel.getGlobalBannerMessageFromMessages(this@MainActivity)
                    val selectedBanner: GlobalBannerMessage =
                        globalBannerMessage
                            ?: if (Utils.isProductFlavorAmazon()) {
                                val localBannerAmazon =
                                    PaywallService.getInstance().globalBannerConfig.banners.firstOrNull { it.id == WALL_NAME_AMAZON_MAIN }
                                GlobalBannerMessage(
                                    attributionInfo = null,
                                    productId = localBannerAmazon?.productId,
                                    offerId = localBannerAmazon?.offerId,
                                    offerTitle = localBannerAmazon?.title,
                                    offerSubtitle = localBannerAmazon?.subtitle,
                                    offerDetail = PaywallUtil.getBannerOfferText(
                                        localBannerAmazon?.productId,
                                        localBannerAmazon?.offerId
                                    ),
                                    offerUrl = null,
                                    ctaText = null,
                                    wallName = null,
                                    messageRequirements = null
                                )
                            } else {
                                val localBannerPlaystore =
                                    PaywallService.getInstance().globalBannerConfig.banners.firstOrNull { it.id == "main" }
                                GlobalBannerMessage(
                                    attributionInfo = null,
                                    productId = localBannerPlaystore?.productId,
                                    offerId = localBannerPlaystore?.offerId,
                                    offerTitle = localBannerPlaystore?.title,
                                    offerSubtitle = localBannerPlaystore?.subtitle,
                                    offerDetail = PaywallUtil.getBannerOfferText(
                                        localBannerPlaystore?.productId,
                                        localBannerPlaystore?.offerId
                                    ),
                                    offerUrl = null,
                                    ctaText = null,
                                    wallName = null,
                                    messageRequirements = null
                                )
                            }
                    globalBannerViewModel.setGlobalBannerState(
                        hasSub = true,
                        isInGracePeriod = isSubInGracePeriod,
                        isOnHold = false,
                        isTerminated = false,
                        isSignedIn = isWpUserLoggedIn,
                        globalBannerMessage = selectedBanner,
                        isPlayStorePaused = isAppStoreSubscriptionPaused,
                        isPlayStorePauseScheduled = isAppStoreSubscriptionPauseScheduled,
                        isSitePaused = isSiteSubscriptionPaused,
                        isSitePauseScheduled = isSiteSubscriptionPauseScheduled,
                        autoResumeTime = PaywallService.getConnector().autoResumeTime,
                        pauseTime = PaywallService.getConnector().pauseTime,
                        isMobileFreeTrial = isMobileFreeDaysUser,
                        mobileFreeTrialTime = getAccessExpiryDate(PaywallConstants.SubscriptionType.WASHPOST),
                        configHideBanner = hideSubscribeBanner,
                    )
                    // We need to verify the sub again with BE as our stored response of GracePeriod/OnHold is stale.
                    dispatchVerifySub(VerifyState.NeedsVerification)
                }
            }
        }
    }

    private fun observeBannerMessage() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    iterableActivityViewModel
                        .getBannerFlowForPlacement(IamMessageType.BANNER)
                        .collect { updateGlobalBannerState() }
                }
                launch {
                    iterableActivityViewModel
                        .getBannerFlowForPlacement(IamMessageType.ASK_THE_POST_BANNER)
                        .collect { message -> askThePostViewModel.setATPCTABanner(message) }
                }
                launch {
                    iterableActivityViewModel
                        .getBannerFlowForPlacement(IamMessageType.SECTION)
                        .collect { message ->
                            if (message == null) {
                                sectionInlineOfferViewModel.setSectionMessageData(null)
                            } else {
                                val offerData =
                                    iterableActivityViewModel.getSectionInlineMessageFromMessages(
                                        this@MainActivity
                                    )
                                sectionInlineOfferViewModel.setSectionMessageData(offerData)
                            }
                        }
                }
                launch {
                    iterableActivityViewModel
                        .getBannerFlowForPlacement(IamMessageType.FRONT_HOME_SCROLL)
                        .collect { updateGlobalBannerState() }
                }
            }
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
                            if (it.attributionInfo.messageId == "front_home_scroll") {
                                sectionsRibbonViewModel.getCurrentSection()?.bundleName
                            } else IamMessageType.BANNER.type
                        )
                    }
                }

                is BannerEvent.BannerDismissed -> {
                    bannerEvent.message.let {
                        iterablePlugin.dismissBanner(it, null)
                    }
                }

                is BannerEvent.GlobalBannerClicked -> {
                    Measurement.setNavigationBehavior(NavigationBehavior.GLOBAL_SUBSCRIBE_BUTTON)
                    if (bannerEvent.message.isLocalAmazonAdFreeBanner) {
                        handleAdFreeBannerCta(bannerEvent.message.offerUrl, bannerEvent.message)
                        return@observe
                    }

                    val message =
                        iterableActivityViewModel.getBannerForPlacement(IamMessageType.BANNER)
                    if (message != null) {
                        iterablePlugin.executeBannerAction(
                            message, IamMessageType.BANNER.type,
                            onShowStandardPaywall = { wallName, wallType, reason ->
                                if (!wallName.isNullOrEmpty()) {
                                    showPaywallDialogByName(reason, wallType, wallName)
                                } else {
                                    showPaywallDialog(reason, wallType)
                                }
                            })
                    } else {
                        val wallType = WallType.GLOBAL_SUBSCRIBE_BUTTON_PAYWALL
                        val reason = PaywallConstants.GLOBAL_SUBSCRIBE_BUTTON
                        if (!bannerEvent.message.wallName.isNullOrEmpty()) {
                            showPaywallDialogByName(
                                reason,
                                wallType,
                                bannerEvent.message.wallName!!
                            )
                        } else {
                            showPaywallDialog(reason, wallType)
                        }
                    }
                }

                is BannerEvent.ImpressionEvent -> {
                    iterablePlugin.handleBannerLifecycleEvent(event = bannerEvent.event)
                }

                is BannerEvent.SectionInLineClicked -> {
                    val messageType = IamMessageType.SECTION.type
                    val message = IamMessageType.fromType(messageType)
                        ?.let { iterableActivityViewModel.getBannerForPlacement(it) }
                    iterablePlugin.executeBannerAction(
                        message,
                        messageType,
                        onShowStandardPaywall = { wallName, wallType, reason ->
                            if (!wallName.isNullOrEmpty()) {
                                showPaywallDialogByName(reason, wallType, wallName)
                            } else {
                                showPaywallDialog(reason, wallType)
                            }
                        })
                }
            }
        }
    }

    private fun observePaywallEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                sectionWallHelperViewModel.paywallEvent.collect { event ->
                    when (event) {
                        is WallUiEvent.ShowRegwall -> {
                            showWallDialog(
                                PaywallService.getInstance().isWpUserLoggedIn,
                                PaywallConstants.getWallReason(WallType.REGWALL),
                                event.wallType,
                                event.wallName,
                            )
                        }
                        /** Add other [WallUiEvent] cases here as necessary */
                        else -> {
                            // Do nothing
                        }
                    }

                }
            }
        }
    }

    private fun observeGiftCollabEvent() {
        giftCollaborationViewModel.giftCollabEvent.observe(this) {
            when (it) {
                GiftCollabEvent.Paywall ->
                    showPaywallDialog(
                        0,
                        WallType.GIFT_SENDER_ACTION_BUTTONS,
                    )

                GiftCollabEvent.SignIn -> {
                    PaywallService.getConnector().showSignInScreen(
                        supportFragmentManager,
                        AuthIntentBuilder().build(),
                        null,
                        WallType.GIFT_SENDER_ACTION_BUTTONS,
                        true,
                        null
                    )
                }

                else -> {}
            }
        }
    }

    /**
     * Observing events for the given playlist audio is added [PlaylistActivityViewModel.addToPlaylist] to the DB.
     * Right now the event is dispatched only for the successful cases. No events for failures.
     */
    private fun observeAddedToPlaylistEvent() {
        playlistActivityViewModel.addedToPlaylistEvent.observe(this) {
            snackbarViewModel.showSnackBar(SnackBarType.AddedToPlaylist())
        }
    }

    private fun observePodcastMetadata() {
        personalizedPodcastViewModel.singlePersonalizedPodcast.observe(this) { state ->
            if (state == null) return@observe

            when (state) {
                is PodcastResult.Success -> {
                    handlePodcastMetadata(state.podcast)
                }

                is PodcastResult.PodcastExpired -> {
                    shareErrorToast(com.wapo.flagship.features.audio.R.string.podcast_expired_message)
                }

                else -> {
                    shareErrorToast(com.wapo.flagship.features.audio.R.string.podcast_error_message)
                }
            }
            personalizedPodcastViewModel.clearPodcastMetadata()
        }
    }

    private fun shareErrorToast(messageResource: Int) {
        val message = getString(messageResource)
        Toast.makeText(
            applicationContext,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun shareConversation() {
        askThePostViewModel.shareATP(false)
    }

    private fun openFindActivity() {
        Measurement.trackFind()
        startActivity(
            Intent(this, FindActivity::class.java).apply {
                putStringArrayListExtra(
                    SECTION_LIST_KEY,
                    sectionsRibbonViewModel.uiState.value.sections.map {
                        it.id
                    } as ArrayList<String>?
                )
            }
        )
    }

    private fun openMyPostActivity() {
        Measurement.trackMyPost()
        startActivity(
            Intent(this, MyPostActivity::class.java)
        )
    }

    private fun openAudioPlayer(intent: Intent) {
        val audioSubtype = intent.getStringExtra(EXTRAS_AUDIO_SUBTYPE)
        val audioId = intent.getStringExtra(EXTRAS_AUDIO_ID)

        if (audioSubtype.isNullOrEmpty() || audioId.isNullOrEmpty()) {
            Logger.w(TAG, "Audio deep link missing subtype or id")
            return
        }

        when (AudioSubtype.fromString(audioSubtype)) {
            AudioSubtype.PERSONALIZED_PODCAST -> {
                Logger.d(TAG, "Subtype is Personalized Podcast: $audioId")
                personalizedPodcastViewModel.getPodcastMetadata(audioId)
            }

            else -> {
                Logger.w(TAG, "Unknown audio subtype: $audioSubtype")
            }
        }
    }

    private fun handlePodcastMetadata(podcast: PersonalizedPodcast) {
        val currentInfo = personalizedPodcastViewModel.uiState.value.persoPodTrackingInfo?.first
        val trackingInfo =
            PersoPodTrackingInfo(
                date = podcast.createdAt,
                touchpoint = currentInfo?.touchpoint,
                podcastType = currentInfo?.podcastType,
                id = podcast.id,
                tags = null,
                title = podcast.title,
                transcriptUrl = podcast.transcript,
                sources = podcast.articlesUsed?.map { article ->
                    ArticleSource(
                        headline = article.headline,
                        canonicalUrl = article.canonicalUrl,
                        publishDate = article.displayDate,
                        text = article.text,
                        imageUrl = article.imageUrl
                    )
                }
            )

        personalizedPodcastViewModel.setPersoPodTrackingInfo(trackingInfo)
        val tracker = personalizedPodcastViewModel.createPodcastTracker(
            podcast.audioDuration?.toLong() ?: 0L,
            personalizedPodcastViewModel.uiState.value.persoPodTrackingInfo
        )
        // convert PersonalizedPodcasts to AudioMediaConfig
        val newMediaConfig = AudioMediaConfig(
            mediaId = podcast.id,
            playerType = PlayerType.PODCAST,
            title = podcast.title,
            streamUrl = podcast.audioFilePath,
            imageUrl = podcast.image,
            primaryLabel = podcast.kicker,
            audioType = podcast.itemType ?: "podcast",
            contentUrl = null,
            date = toDateLong(podcast.createdAt),
            duration = podcast.audioDuration?.toLong(),
            sectionName = podcast.kicker,
            isShared = true,
            audioTracking = tracker
        )

        lifecycleScope.launch {
            audioMediaActivityViewModel.playMedia(newMediaConfig)
            delay(100)
            val existingFragment =
                supportFragmentManager.findFragmentByTag(AudioPagerFragment.FRAGMENT_TAG)

            if (existingFragment == null) {
                val podcastPagerFragment = AudioPagerFragment.newInstance()
                podcastPagerFragment.show(
                    supportFragmentManager,
                    AudioPagerFragment.FRAGMENT_TAG
                )
            }
        }
    }

    private fun openHowThePostWorksSheet() {
        PostAnswersInfoBottomSheetFragment().show(
            supportFragmentManager,
            PostAnswersInfoBottomSheetFragment.TAG,
        )
    }

    private fun openLearnMore() {
        val url = ConfigManager.getInstance().config.search2Config.learnMoreUrl
        DeepLinksProcessor.processAsync(url, scope = lifecycleScope)
    }

    private fun privateModeToggled() {
        askThePostViewModel.togglePrivateMode()
    }

    private fun openHistory() {
        askThePostViewModel.showHistory(true)
        askThePostViewModel.getSessionHistory()
        SourceBottomSheet(SourceBottomSheetCaller.ASK).show(
            supportFragmentManager,
            "SourceBottomSheetTag"
        )
    }

    private fun startNewChat() {
        askThePostViewModel.hideOrShowResponse(false)
        askThePostViewModel.clearLiveConversationData()
        Measurement.trackNewChat()
    }

    private fun shouldStartNewChat() {
        Measurement.trackHistorySavePrompt()
        askThePostViewModel.showRegisterModal(true)
    }

    private fun observeSectionsRibbonEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sectionsRibbonViewModel.sectionsRibbonEvent.collect { event ->
                    when (event) {
                        is SectionsRibbonEvents.SelectedSectionIndexEvent -> {
                            sectionNavViewModel.updateRecentSections(event.id)
                        }

                        is SectionsRibbonEvents.VisitedNewsprintSectionEvent -> {
                            if (event.visited) {
                                PrefUtils.setNewsprintHasVisitedSection(
                                    applicationContext,
                                    event.visited
                                )
                            }
                        }

                        SectionsRibbonEvents.RibbonReadyEvent -> {
                            pendingRibbonIntent?.let {
                                val pendingIntent = it
                                pendingRibbonIntent = null
                                processIntent(pendingIntent)
                            }
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    private fun observeSectionTracking() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sectionTrackingViewModel.event.collect {
                    when (it) {
                        is SectionTrackEvent.PageView -> {
                            it.tracking?.let { tracking ->

                                Logger.d(
                                    "NAV_BEHAVE",
                                    sectionTrackingViewModel.getNavigationBehavior().toString()
                                )
                                // If navigation behavior is set, set measurement nav
                                sectionTrackingViewModel.getNavigationBehavior()
                                    ?.let { navBehavior ->
                                        Measurement.setNavigationBehaviorInDefaultMap(navBehavior)
                                        sectionTrackingViewModel.setNavigationBehavior(null)
                                    }

                                Measurement.trackAsTrackingInfo(
                                    tracking,
                                    navBarViewModel.getCurrentTabTrackingName(),
                                    it.displayName,
                                    JTidTracker.updatedJTid,
                                    JUcidTracker.jUcid,
                                )
                                val pagePath = coalesce(it.tracking?.pagePath, tracking.pageTitle)
                                val pageTitle = coalesce(it.tracking?.pageTitle, tracking.pageName)
                                ChartbeatManager.trackView(this@MainActivity, pagePath, pageTitle)
                                iterableActivityViewModel.restartSession(SectionFrontsFragment::class.java.simpleName)
                            }
                        }

                        is SectionTrackEvent.OnpageTap -> {
                            Measurement.trackOnpageTap(
                                sectionTrackingViewModel.getMiscellany(),
                                it.tracking?.pageName,
                                it.tracking?.contentID
                            )
                        }

                        is SectionTrackEvent.AudioInteraction -> {
                            Measurement.trackAudioInteraction(
                                it.avName,
                                it.touchpoint,
                                it.miscellany,
                                null
                            )
                        }

                        is SectionTrackEvent.AudioStart -> {
                            Measurement.trackAudioStart(
                                it.avName,
                                it.touchpoint,
                                it.miscellany,
                                null
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Observe low data banner state to refresh the UI
     */
    private fun observeLowDataBannerState() {
        lowDataBannerViewModel.lowDataBannerState.observe(this) {
            navBarViewModel.setLowDataModeEnable(
                it.isLowDataBannerEnable,
                AppPreferences.isLowDataModeEnabledFromSettings(),
            )
            sectionsRibbonViewModel.setIsLowDataModeEnable(it.isLowDataBannerEnable)
            updateGlobalBannerState(it.isLowDataBannerEnable)
            AppPreferences.setIsLowDataModeEnabled(it.isLowDataBannerEnable)
        }
    }

    /**
     * Passes state of Targeting consent category to the view model so it can be observed by For You.
     */
    override fun onOneTrustTargetingStateChanged(state: Boolean) {
        super.onOneTrustTargetingStateChanged(state)
        myPost2ViewModel.setTargetingEnabled(state)
        if (OneTrustHelper.isFunctionalityEnabled()) {
            alertsViewModel.updateDailyRead(state)
        }
    }

    /**
     * Passes state of Functionality consent category to the alerts view model so it can be observed for Alerts pref.
     */
    override fun onOneTrustFunctionalityStateChanged(state: Boolean) {
        super.onOneTrustFunctionalityStateChanged(state)
        if (OneTrustHelper.isTargetingEnabled()) {
            alertsViewModel.updateDailyRead(state)
        }
    }

    override fun updateSubscriptionState() {
        updateGlobalBannerState()
    }

    private fun processIntentOnceRibbonIsReady(intent: Intent?) {
        intent ?: return
        val deepLinkUrl = intent.dataString

        val isSectionDeepLink = if (deepLinkUrl != null) {
            val urlParser = URLParser(deepLinkUrl)
            DeepLinksProcessor.isSectionPath(urlParser) ||
                    DeepLinksProcessor.isSectionUrl(urlParser)
        } else {
            false
        }

        if (sectionsRibbonViewModel.isRibbonReady()) {
            processIntent(intent)
        } else if (!isSectionDeepLink) {
            processIntent(intent)
        } else {
            pendingRibbonIntent = intent
        }
    }

    private fun processIntent(intent: Intent?) {
        if (intent == null) return
        // ignore AppsFlyer deeplinks; let OneLinkListener handle them instead
        if (intent.data?.queryParameterNames?.contains("af_deeplink") == true) return
        if (config.versionConfig.isAppToBeForceUpdated(applicationContext)) {
            return
        }

        if (intentHelper.isStandardAppLaunch(intent)) {
            Measurement.trackAppLaunch()
        }
        // Check if intent is already processed or not. process only if it is not processed yet.
        // System delivers the same original launch intent in some cases (for ex: Activity recreation case)
        val bundle = intent.extras
        if (bundle != null &&
            bundle.getBoolean(ARG_LAUNCH_INTENT_STATUS, false) ||
            IntentHelper.Companion.isLaunchedFromRecent(intent)
        ) {
            Logger.d(TAG, "Intent is already processed. Skip processing!")
            return
        }

        setAlreadyDeeplinked(bundle, intent)

        when (intent.action) {
            Intent.ACTION_SEARCH -> {
                supportFragmentManager.executePendingTransactions()
                supportInvalidateOptionsMenu()
            }

            ACTION_TOP_STORIES -> navBarViewModel.navigateToTab(BottomTab.Home, false)

            ACTION_LISTEN -> navBarViewModel.navigateToTab(BottomTab.Listen, false)

            ACTION_GAMES -> navBarViewModel.navigateToTab(BottomTab.Games, false)

            ACTION_WATCH -> navBarViewModel.navigateToTab(BottomTab.Watch, false)

            ACTION_MY_POST, SHORTCUT_MY_POST -> {
                openMyPostActivity()
                return
            }

            ACTION_OPEN_AUDIO_PLAYER -> {
                openAudioPlayer(intent)
                intent.putExtra(ARG_LAUNCH_INTENT_STATUS, true)
                return
            }

            ACTION_FIND -> {
                openFindActivity()
                return
            }

            ACTION_ASK -> {
                val shareId = intent.getStringExtra(ATP_SHARE_ID)
                shareId?.let {
                    askThePostViewModel.processDeepLink(it)
                }
                navBarViewModel.navigateToTab(BottomTab.Ask, false)
                setAlreadyDeeplinked(bundle, intent)
                return
            }

            ACTION_PRINT_EDITION -> {
                if (navBarViewModel.isCurrentTab(BottomTab.Print)) {
                    navBarViewModel.navigateToTab(BottomTab.Print, false)
                } else {
                    sectionNavViewModel.openPrint()
                }
            }

            ACTION_OPEN_COMMENTS_DEEPLINK -> {
                val storyUrl = intent.getStringExtra(EXTRAS_COMMENT_STORY_URL)
                val commentId = intent.getStringExtra(EXTRAS_COMMENT_ID) ?: ""
                if (!storyUrl.isNullOrEmpty()) {
                    CommentBottomSheetFragment.showComments(
                        fragmentManager = supportFragmentManager,
                        storyID = "",
                        storyUrl = storyUrl,
                        storyTitle = null,
                        commentSource = COMMENT_SOURCE_DEEPLINK,
                        commentID = commentId,
                    )
                }
                setAlreadyDeeplinked(bundle, intent)
                return
            }

            SHORTCUT_ALERTS -> {
                openAlertsActivity()
                return
            }

            SHORTCUT_POLITICS -> {
                sectionNavViewModel.openPoliticsFromShortcut()
                return
            }

            ACTION_SHOW_PAYWALL -> {
                showWallDialog(
                    PaywallService.getInstance().isWpUserLoggedIn,
                    intent.getIntExtra(ACTION_SHOW_PAYWALL_REASON, PaywallConstants.METERED),
                    WallType.ONBOARDING_PAYWALL,
                )
            }

            ACTION_PROMOCODE_OFFER -> {
                showPromoFetcherDialog()
            }

            ACTION_OPEN_SECTION -> {
                if (sectionTrackingViewModel.getNavigationBehavior() == null) {
                    sectionTrackingViewModel.setNavigating(SectionNavigation.LABEL_TAP)
                }
                val path = intent.getStringExtra(EXTRAS_SECTION_URL)
                path?.let {
                    sectionNavViewModel.openSectionByUrl(it)
                }
                setAlreadyDeeplinked(bundle, intent)
                return
            }

            ACTION_OPEN_SECTION_FIND -> {
                val bundleName = intent.getStringExtra(EXTRAS_SECTION_BUNDLE_NAME)
                val title = intent.getStringExtra(EXTRAS_SECTION_TITLE)
                val sectionId = intent.getStringExtra(EXTRAS_SECTION_ID)
                val fragment =
                    SectionFrontsFragment().also {
                        it.updatePageWith(bundleName, title, sectionId)
                    }
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.container_fragment, fragment)
                    .commit()

                navBarViewModel.showBackButton(true)
                sectionNavViewModel.setSectionTitle(title)
            }

            ACTION_OPEN_SECTION_DEEPLINK -> {
                if (sectionTrackingViewModel.getNavigationBehavior() == null) {
                    sectionTrackingViewModel.setNavigating(SectionNavigation.DEEP_LINK)
                }
                val path = intent.getStringExtra(EXTRAS_SECTION_URL)
                path?.let {
                    sectionNavViewModel.openSectionByUrl(it)
                }
                setAlreadyDeeplinked(bundle, intent)
                return
            }

            ACTION_OPEN_SIGN_IN -> {
                val wallName = bundle?.getString(EXTRA_WALL_NAME)
                val index = bundle?.getInt(EXTRA_PAYWALL_TYPE) ?: WallType.IAA_WALL.ordinal
                val paywallType = WallType.entries.getOrNull(index) ?: WallType.IAA_WALL
                val isAcquisition = paywallType == WallType.IAA_WALL
                val campaignEntranceType = bundle?.getString(EXTRA_CAMPAIGN_ENTRANCE_TYPE)
                PaywallService.getConnector().showSignInScreen(
                    supportFragmentManager,
                    bundle,
                    wallName,
                    paywallType,
                    isAcquisition,
                    campaignEntranceType
                )
                return
            }

            ACTION_OPEN_LOGIN_REDIRECT -> {
                // process login intent after returning from third party auth
                intent.data?.let {
                    val entryPoint: AuthEntryPoint? =
                        AuthHelper.getInstance(applicationContext).restoreAndRemoveAuthEntryPoint()
                    LoginRegFragment()
                        .apply {
                            arguments = AuthIntentBuilder()
                                .addRedirectUrl(it.toString())
                                .apply { if (entryPoint != null) addEntryPoint(entryPoint) }
                                .build()
                        }.showNow(supportFragmentManager, LoginRegFragment.TAG)
                }
                return
            }

            ACTION_OPEN_MAGIC_LINK -> {
                // process magic link intent
                intent.data?.let {
                    AuthHelper.handleMagicLinkOrSocialRedirectAuth(this, intent.data)
                }
                return
            }

            ACTION_OPEN_SECTION_RIBBON -> {
                val sectionId = intent.getStringExtra(SECTION_ID_KEY) ?: return
                val openWithoutStack =
                    intent.getBooleanExtra(EXTRAS_SECTION_OPEN_WITHOUT_STACK, false)
                navBarViewModel.showBackButton(true)
                sectionNavViewModel.shouldFinishSectionOnBack(openWithoutStack)
                sectionNavViewModel.openSection(sectionId, null)
            }

            else -> {
                if (!IntentHelper.Companion.isLaunchedFromRecent(intent) &&
                    intentHelper.isWidgetOriginated(
                        intent,
                    )
                ) {
                    val articlesParcel = ArticlesParcel(intent)
                    val widgetId = articlesParcel.getWidgetId()
                    setAlreadyDeeplinked(bundle, intent)
                    mainActivityViewModel.getArticleUrlsById(
                        widgetId = widgetId,
                        bundle = bundle,
                        widgetType = articlesParcel.getWidgetType(),
                        articleContentUrl = articlesParcel.getArticleContentUrl(),
                        sectionDisplayName = articlesParcel.getSectionDisplayName()
                    )
                    return
                } else if (intentHelper.shouldRefreshForYouWidget(intent)) {
                    ForYouWidgetUpdateWorker.scheduleForceRefresh(this)
                    return
                }
            }
        }

        val scheme = if (intent.data == null) null else intent.scheme
        if (Intent.ACTION_VIEW == intent.action && AUTH_SCHEME == scheme) {
            AuthHelper.handleMagicLinkOrSocialRedirectAuth(this, intent.data)
        } else if (intent.data != null) {
            val isPushOriginated = intent.getBooleanExtra(PUSH_ORIGINATED, false)
            val pushMetadata = getPushMetadata(isPushOriginated, intent)
            val isWidgetOriginated = intent.getBooleanExtra(WIDGET_ORIGINATED, false)
            val isExternalOrigin =
                intent.`package`?.contains(applicationContext.packageName) != true
            val urlParser = URLParser(intent.dataString)
            urlParser.isPushOriginated = isPushOriginated
            urlParser.isExternalOrigin = isExternalOrigin
            urlParser.isWidgetOriginated = isWidgetOriginated

            val navBehavior =
                when {
                    isPushOriginated -> NavigationBehavior.PUSH.value
                    isWidgetOriginated -> NavigationBehavior.WIDGET.value
                    isExternalOrigin -> NavigationBehavior.DEEPLINK.value
                    else -> NavigationBehavior.LINK.value
                }
            sectionTrackingViewModel.setNavigationBehavior(navBehavior)

            val referrer =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    referrer
                } else {
                    Utils.getReferrerBelowSdk22(intent)
                }
            if (referrer != null) {
                urlParser.referrer = referrer.toString()
                if (isExternalOrigin && !isPushOriginated && !isWidgetOriginated) {
                    Measurement.trackDeepLinkOpen(urlParser.uri)
                }
            }

            DeepLinksProcessor.processAsync(
                urlParser = urlParser,
                pushMetadata = pushMetadata,
                activityContext = this,
                scope = lifecycleScope,
            )
        }
        setAlreadyDeeplinked(bundle, intent)
    }

    private fun continueGetArticlesList(
        articlesUrls: List<String>,
        bundle: Bundle?,
        widgetType: String?,
        articleContentUrl: String?,
        sectionDisplayName: String,
    ) {
        Measurement.trackLaunchFromWidget(widgetType)
        // DeepLinksProcessor can handle only one link right now. We should see if there
        // are any cases other than widgets need to open collection of article urls.
        // If so, handle with DeepLinksProcessor.
        if (!articleContentUrl.isNullOrEmpty()) {
            val articleMetas = articlesUrls.map {
                val pair = getUrlAndAnchorRefPair(it)
                ArticleMeta(pair.first, false).also { meta ->
                    meta.anchorId = pair.second
                }
            }
            val builder =
                builder().apply {
                    setArticleSingleUrl(articleContentUrl)
                    if (articlesUrls.isNotEmpty()) {
                        setArticleUrls(
                            articlesUrls,
                            articlesUrls.indexOf(articleContentUrl)
                        )
                        setArticleMetas(
                            articleMetas,
                            articlesUrls.indexOf(articleContentUrl)
                        )
                    }
                    setSectionDisplayName(sectionDisplayName)
                    widgetType(widgetType)
                    widgetOriginated(true)
                }
            val intent = builder.buildIntent(this)
            startActivity(intent)
            setAlreadyDeeplinked(bundle, intent)
            return
        }
    }

    private fun getPushMetadata(
        isPushOriginated: Boolean,
        intent: Intent,
    ): PushMetadata? =
        if (isPushOriginated) {
            val headline = intent.extras?.getString(PushListener.HEADLINE) ?: ""
            val kicker = intent.extras?.getString(PushListener.KICKER) ?: ""
            val trackingId = intent.extras?.getString(PushListener.TRACKING_NOTIFICATION_ID) ?: ""
            val analyticsId = intent.extras?.getString(PushListener.ANALYTICS_ID) ?: ""
            val pushTimestamp = intent.extras?.getString(PushListener.NOTIFICATION_TIMESTAMP) ?: ""
            PushMetadata(headline, kicker, trackingId, analyticsId, pushTimestamp)
        } else {
            null
        }

    /**
     * Set this ARG_LAUNCH_INTENT_STATUS to true so on resume of app, the deeplink isn't
     * triggered again
     */
    private fun setAlreadyDeeplinked(
        bundle: Bundle?,
        intent: Intent,
    ) {
        val bundle = bundle ?: Bundle()
        bundle.putBoolean(ARG_LAUNCH_INTENT_STATUS, true)
        intent.putExtras(bundle)
    }

    private fun showPromoFetcherDialog() {
        PromoCodeFetcherDialog()
            .apply { this.isCancelable = false }
            .show(supportFragmentManager, PromoCodeFetcherDialog.TAG)
    }

    override fun onResume() {
        super.onResume()
        UnificationHelper.determineNextScreen(this)

        onboardingViewModel.initIapSubStatus()
        if (PaywallService.getInstance() != null &&
            onboardingFragment?.get()?.isVisible == true &&
            onboardingViewModel.isAlreadyShown()
        ) {
            onboardingFragment?.get()?.dismiss()
        }
        navBarViewModel.subscribeAlertsBadge()

        checkConnectivity()
        updateGlobalBannerState()
        updateLowDataMode()
        trackBackToActivity() // Tracking page views when clicks back to Activity

        // After sign-in + PostLoginActivity/Onboarding, check if verify succeeded
        // and process the pending ad-free redirect.
        if (awaitingPostSignInVerify && pendingAdFreeRedirectUrl != null) {
            awaitingPostSignInVerify = false
            handlePendingAdFreeAfterSignIn()
        }
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

    /**
     * Check verify result and redirect to mweb, retrying once if needed.
     */
    private fun handlePendingAdFreeAfterSignIn() {
        val verifyResult = PaywallReactive.verifyComplete.value
        if (verifyResult == true) {
            // Verify already succeeded during sign-in flow
            processPendingAdFreeRedirect()
        } else {
            // Verify failed or didn't run — retry once
            lifecycleScope.launch {
                retryVerifyAndRedirect()
            }
        }
    }

    private suspend fun retryVerifyAndRedirect() {
        PaywallReactive.resetVerifyState()
        PaywallService.getInstance()?.verifyDeviceSubscription(true, false, false)

        // Wait for verify to complete (with timeout)
        val success = withTimeoutOrNull(10_000L) {
            PaywallReactive.verifyComplete.first { it != null }
        }

        if (success == true) {
            processPendingAdFreeRedirect()
        } else {
            pendingAdFreeRedirectUrl = null
            Snackbar.make(
                findViewById(android.R.id.content),
                "Something went wrong. Please try again later.",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun trackBackToActivity() {
        val currentTab = navBarViewModel.getCurrentTab()
        if (currentTab != BottomTab.Home && currentTab != BottomTab.Ask && currentTab != BottomTab.Search) {
            Measurement.trackBackFromActivity(currentTab.trackingName)
        }
    }

    /**
     * Legacy helper kept for reference; resume is handled in article/paywall flow.
     */
    private fun resumeAudioPlaybackIfNeeded() {
        try {
            val nowPlayingItem = audioMediaActivityViewModel.nowPlayingAudioItem.value
            val playbackState = audioMediaActivityViewModel.audioPlaybackState.value

            val shouldResumeFromPaused = playbackState == AudioPlaybackState.Paused
            val shouldResumeFromBuffering = playbackState == AudioPlaybackState.Buffering

            if (nowPlayingItem != null && (shouldResumeFromPaused || shouldResumeFromBuffering)) {

                // Resume the audio playback
                audioMediaActivityViewModel.resumeMedia()
            }

        } catch (e: Exception) {
            // Silently catch exceptions to avoid crashes
            Logger.e("MainActivity", "Error resuming audio playback: ${e.message}")
        }
    }

    /**
     * Update the state of the Global Banner for sections it is visible on.
     */
    private fun updateGlobalBannerState(hideBanner: Boolean = hideSubscribeBanner) {
        PaywallService.getInstance()?.apply {
            // If Iap Sub is Active, payment is fixed when sub is On Hold. This will give the most recent
            // Iap State whereas values coming from PaywallService (isOnHold) might be stale since verify
            // is called every 24 hrs.
            val isPaymentFixed =
                PaywallService.getConnector().currentIapStatus == PaywallConstants.IapSubStatus.ACTIVE
            // select global banner from iterable message, otherwise use local banners.
            val globalBannerMessage =
                iterableActivityViewModel.getGlobalBannerMessageFromMessages(this@MainActivity)

            val isAmazonStore =
                PaywallConstants.AMAZON_STORE == PaywallService.getConnector().getStoreType()

            val selectedBanner: GlobalBannerMessage =
                if (isAmazonStore && isPremiumUser && adService.currentAdsMode != AdsModel.Disabled) {
                    val wpUser = WpPaywallHelper.getLoggedInUser()
                    // Determine annual vs monthly:
                    // 1. Use rateDuration from the base subscription's SubItem (logged-in users)
                    // 2. Fall back to the cached subscription SKU name (anonymous users)
                    val baseSub = wpUser?.subscriptions?.firstOrNull {
                        !PaywallReactive.isAdFreeProduct(it)
                    }
                    val rateDuration = baseSub?.rateDuration?.toIntOrNull()
                    val isAnnual = if (rateDuration != null) {
                        rateDuration > 360
                    } else {
                        val sku =
                            PaywallService.getBillingHelper()?.cachedSubscription()?.storeProductId
                                ?: PaywallService.getBillingHelper()
                                    ?.getClassicOrRainbowSubscription()?.storeProductId
                        sku?.contains("annual", ignoreCase = true) == true
                    }
                    val adFreeUpgradeId =
                        if (isAnnual) "amazon_ad_free_yearly" else "amazon_ad_free_monthly"
                    val localAdFreeBanner =
                        PaywallService.getInstance().globalBannerConfig.banners.firstOrNull { it.id == adFreeUpgradeId }
                    GlobalBannerMessage(
                        attributionInfo = null,
                        offerTitle = localAdFreeBanner?.title,
                        offerSubtitle = localAdFreeBanner?.subtitle,
                        productId = localAdFreeBanner?.productId,
                        offerId = localAdFreeBanner?.offerId,
                        wallName = null,
                        ctaText = localAdFreeBanner?.action?.let {
                            android.text.SpannableStringBuilder(
                                it
                            )
                        },
                        offerUrl = localAdFreeBanner?.url,
                        offerDetail = "",
                        productName = localAdFreeBanner?.productName,
                        action = localAdFreeBanner?.action,
                        isAdFreeProduct = true,
                        messageRequirements = null
                    )
                } else {
                    // If no Iterable global-banner message is available, fall back to local banners from config based on the app store
                    globalBannerMessage
                        ?: if (Utils.isProductFlavorAmazon()) {
                            val localBannerAmazon =
                                PaywallService.getInstance().globalBannerConfig.banners.firstOrNull { it.id == WALL_NAME_AMAZON_MAIN }
                            GlobalBannerMessage(
                                attributionInfo = null,
                                productId = localBannerAmazon?.productId,
                                offerId = localBannerAmazon?.offerId,
                                offerTitle = localBannerAmazon?.title,
                                offerSubtitle = localBannerAmazon?.subtitle,
                                offerDetail = PaywallUtil.getBannerOfferText(
                                    localBannerAmazon?.productId,
                                    localBannerAmazon?.offerId
                                ),
                                offerUrl = localBannerAmazon?.url,
                                ctaText = null,
                                wallName = null,
                                productName = localBannerAmazon?.productName,
                                messageRequirements = null
                            )
                        } else {
                            val localBannerPlaystore =
                                PaywallService.getInstance().globalBannerConfig.banners.firstOrNull { it.id == "main" }
                            GlobalBannerMessage(
                                attributionInfo = null,
                                productId = localBannerPlaystore?.productId,
                                offerId = localBannerPlaystore?.offerId,
                                offerTitle = localBannerPlaystore?.title,
                                offerSubtitle = localBannerPlaystore?.subtitle,
                                offerDetail = PaywallUtil.getBannerOfferText(
                                    localBannerPlaystore?.productId,
                                    localBannerPlaystore?.offerId
                                ),
                                offerUrl = localBannerPlaystore?.url,
                                ctaText = null,
                                wallName = null,
                                productName = localBannerPlaystore?.productName,
                                messageRequirements = null
                            )
                        }
                }
            globalBannerViewModel.setGlobalBannerState(
                hasSub = isPremiumUser,
                isInGracePeriod = isSubInGracePeriod,
                isOnHold = isSubOnHold && !isPaymentFixed,
                isTerminated = isSubscriptionTerminated,
                isSignedIn = isWpUserLoggedIn,
                globalBannerMessage = selectedBanner,
                isPlayStorePaused = isAppStoreSubscriptionPaused,
                isPlayStorePauseScheduled = isAppStoreSubscriptionPauseScheduled,
                isSitePaused = isSiteSubscriptionPaused,
                isSitePauseScheduled = isSiteSubscriptionPauseScheduled,
                autoResumeTime = PaywallService.getConnector().autoResumeTime,
                pauseTime = PaywallService.getConnector().pauseTime,
                isMobileFreeTrial = isMobileFreeDaysUser,
                mobileFreeTrialTime = getAccessExpiryDate(PaywallConstants.SubscriptionType.WASHPOST),
                configHideBanner = hideBanner,
            )
        }
    }

    /**
     * Handles the ad-free banner CTA tap.
     * - If the user is signed in, fetches a nonce via [DeepLinksProcessor.processAsync] and opens the mWeb URL.
     * - If the user is not signed in, stores the URL and shows the sign-in screen.
     *   After sign-in, [processPendingAdFreeRedirect] picks up the stored URL.
     */
    private fun handleAdFreeBannerCta(mwebUrl: String?, offer: GlobalBannerMessage? = null) {
        val url = mwebUrl ?: return
        if (PaywallService.getInstance().isWpUserLoggedIn) {
            EventLog.Builder().apply {
                setMessage("Offer fallback to mweb")
                setModule(LogModules.PAYWALL)
                setContentUrl(mwebUrl)
            }.run {
                PaywallService.getConnector().logD(this)
            }
            // User is signed in — processAsync will fetch nonce and open in browser
            DeepLinksProcessor.processAsync(
                link = url,
                bundle = offer?.attributionInfo.toBundle(),
                scope = lifecycleScope,
            )
        } else {
            // User is not signed in — store URL and show sign-in screen
            pendingAdFreeRedirectUrl = url
            PaywallService.getConnector().showSignInScreen(
                supportFragmentManager,
                AuthIntentBuilder().build(),
                null,
                WallType.GLOBAL_SUBSCRIBE_BUTTON_PAYWALL,
                false,
                null
            )
        }
    }

    /**
     * Called after verify succeeds and the subscription is linked to the signed-in account.
     * If the user is now signed in and we have a pending ad-free redirect URL,
     * fetch a nonce and open the mWeb URL.
     */
    private fun processPendingAdFreeRedirect() {
        val url = pendingAdFreeRedirectUrl ?: return
        pendingAdFreeRedirectUrl = null
        if (PaywallService.getInstance().isWpUserLoggedIn) {
            EventLog.Builder().apply {
                setMessage("Offer fallback to mweb")
                setModule(LogModules.PAYWALL)
                setContentUrl(url)
            }.run {
                PaywallService.getConnector().logD(this)
            }
            DeepLinksProcessor.processAsync(
                link = url,
                scope = lifecycleScope,
            )
        }
    }


    private fun updateLowDataMode() {
        lowDataBannerViewModel.updateLowDataBanner(isLowDataModeEnable)
    }

    override fun checkConnectivity() {
        snackbarViewModel.setNoNetwork(!ReachabilityUtil.isConnected(this))

        if (config.lowDataModeConfig.enable) {
            ConnectivityMonitor
                .getInstance(applicationContext)
                .registerToNetworkChanges(
                    object : ConnectivityMonitor.NetworkListener {
                        override fun onPoorNetwork() {
                            runOnUiThread {
                                snackbarViewModel.isLowDataModeNotificationEnable { isLowDataModeNotificationEnabled ->
                                    if (isLowDataModeNotificationEnabled &&
                                        snackbarViewModel.uiState.value.snackBarType !is SnackBarType.LowDataConnection
                                    ) {
                                        Measurement.trackLowDataModeNotification(
                                            pageNameFromTracking,
                                        )
                                        snackbarViewModel.showSnackBar(
                                            SnackBarType.LowDataConnection(),
                                        )
                                    }
                                }
                            }
                        }

                        override fun onNetworkOk() {
                            if (snackbarViewModel.uiState.value.snackBarType is SnackBarType.LowDataConnection) {
                                runOnUiThread {
                                    snackbarViewModel.hideSnackBar(pageNameFromTracking)
                                }
                            }
                        }

                        override fun isCheckNetworkEnable(enable: () -> Unit) {
                            snackbarViewModel.isLowDataModeNotificationEnable { isNotificationEnabled ->
                                if (isNotificationEnabled &&
                                    lowDataBannerViewModel.lowDataBannerState.value?.isLowDataBannerEnable == false &&
                                    snackbarViewModel.uiState.value.snackBarType !is SnackBarType.LowDataConnection
                                ) {
                                    enable()
                                }
                            }
                        }
                    },
                )
        }
    }

    override fun handleAdFreeStatusChange() {
//        val fragment = supportFragmentManager.findFragmentById(
//            binding.mainView.id
//        ) as? SectionFrontsFragment ?: return
//        val pager = fragment.pager ?: return
//        pager.refreshFragments()
        // TODO : this needs to refresh fragment after Ads Flag status has changed
    }

    override fun playForYouAudio(
        recommendationsItem: RecommendationsItem,
        isActionAudio: Boolean,
        isAudioCarousel: Boolean,
        feed: String?,
        isFlexAudio: Boolean,
        isActionButton: Boolean,
        onLoadingChange: (Boolean) -> Unit,
    ) {
        if (audioPlaybackViewModel.hasAccessToAudioArticle()) {
            getCurrentSectionDisplayName()
            // fetch and play
            recommendationsItem.getURL().let { url ->
                mainViewModel
                    .fetchArticleAudioConfig(
                        url,
                        activeTabName,
                        appSection,
                        isActionAudio,
                        AudioPreferences.getAudioPlaybackSpeed(applicationContext),
                        isAudioCarousel,
                        feed,
                        isFlexAudio,
                        isActionButton,
                    ).observe(this) {
                        if (it != null) {
                            if (!audioMediaActivityViewModel.isMediaActive(it)) {
                                onLoadingChange(true)
                                audioMediaActivityViewModel.playMedia(it)
                                audioManager.audioTrackerEvent.observe(this) { event ->
                                    when (event) {
                                        AudioTrackerEvent.Start -> onLoadingChange(false)
                                        else -> {}
                                    }
                                }
                            } else {
                                audioMediaActivityViewModel.pauseOrPlay()
                            }
                        }
                    }
            }
        } else {
            // show regwall
            Measurement.setPaywallArticle(
                Measurement.getTrackingPageName(ForYouFragment.FOR_YOU_DISPLAY_NAME),
                recommendationsItem.arcId,
            )
            sectionWallHelperViewModel.dispatchShowRegwall(
                PaywallConstants.WALL_NAME_AUDIO_ACTION_BUTTON,
                WallType.AUDIO_ACTION_BUTTON_PAYWALL,
            )
        }
    }

    override fun onPause() {
        if (connectivityReceiver != null) {
            connectivityReceiver.unregister()
        }
        ConnectivityMonitor.getInstance(applicationContext).unRegisterToNetworkChanges()
        if (reviewAppDialog != null) {
            reviewAppDialog?.dismiss()
        }
        if (updateAppDialog != null) {
            updateAppDialog?.dismiss()
        }
        navBarViewModel.unsubscribeAlertsBadge()
//        mWearCompat?.onPause()
        super.onPause()
    }

    override fun onStop() {
        TooltipPopupManager.get(this).dismissTooltip()
        val lastVisitedTab = navBarViewModel.getCurrentTab().route
        if (lastVisitedTab != null) {
            PrefUtils.setLastVisitedBottomTab(this, lastVisitedTab)
        }
        super.onStop()
    }


    override fun onPostResume() {
        super.onPostResume()
        showNextDialog()
    }

    override fun onDestroy() {
        // Adding this so that so the user is asked again
        if (!AppContext.isDoNotAskAboutNightModeEnabled()) {
            AppContext.setShowNightModeSnackbar(true)
        }
        Measurement.fusionEventsSent.clear()
        Measurement.fusionMapEventsSent.clear()
        splashScreenViewProvider = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ScreenCaptureCompat.unregister(this)
        }
        supportFragmentManager.unregisterFragmentLifecycleCallbacks(paywallAudioLifecycleCallbacks)
        talkToThePostBottomSheetFragmentFactory.onDestroy()
        super.onDestroy()
        /*        if (mWearCompat != null) {
                    LocalBroadcastManager.getInstance(this).unregisterReceiver(wearReceiver)
                }*/
    }

    private fun openAlertsActivity(showAlertsSettings: Boolean = false) {
        startActivity(
            Intent(this, AlertsActivity::class.java)
                .putExtra(AlertsActivity.SHOW_ALERTS_SETTINGS, showAlertsSettings)
                .putExtra(TYPE, AlertsSettings.EntryPoint.ALERTS_TAB.trackingName),
        )
    }

    private fun openSettingsActivity() {
        val settingsIntent = Intent(this, SettingsActivity::class.java)
        startActivity(settingsIntent)
    }


    fun onBackPressedImpl() {
        supportFragmentManager.fragments.forEach { fragment ->
            if (fragment != null && fragment.isVisible) {
                with(fragment.childFragmentManager) {
                    if (backStackEntryCount > 1) {
                        if (backStackEntryCount == 2) {
                            Measurement.setNavigationBehavior(NavigationBehavior.BACK_TO_FRONT)
                            Measurement.trackBottomTabNavigation(navBarViewModel.getCurrentTabTrackingName())
                        }
                        popBackStack()
                        onFragmentsPopBackStack()
                        sectionNavViewModel.resetOpenSection()
                        if (backStackEntryCount == 2 && sectionNavViewModel.getFinishSectionOnBack()) {
                            finish()
                        }
                        return
                    }
                }
            }
        }

        if (navBarViewModel.isCurrentTab(BottomTab.Ask)) {
            askThePostViewModel.uiState.value.showResponse.let {
                askThePostViewModel.uiState.value.showPassage.let { showPassage ->
                    if (showPassage == true) {
                        askThePostViewModel.hideOrShowPassage(false)
                    } else if (it) {
                        askThePostViewModel.hideOrShowResponse(false)
                        askThePostViewModel.clearLiveConversationData()
                    } else {
                        navBarViewModel.navigateToTab(BottomTab.Home)
                    }
                    return
                }
            }
        }

        if (!navBarViewModel.isCurrentTab(BottomTab.Home)) {
            val popped = navController.popBackStack(
                route = BottomTab.Home.route,
                inclusive = false
            )
            if (!popped) {
                navController.navigate(BottomTab.Home.route) {
                    popUpTo(0) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
            navBarViewModel.navigateToTab(BottomTab.Home)
            return
        }
        finish()
    }

    private fun onFragmentsPopBackStack() {
        // Cases where switching Fragments within MainActivity.
        sectionsHabitTilesViewModel.fetchData()
    }

    fun isCurrentTabPrintEdition(): Boolean = navBarViewModel.isCurrentTab(BottomTab.Print)

    // -------------------------------------------------------------------
    // SectionsTracker Interface

    override fun trackLiveImageToggle(liveImageTrackingName: String?) {
        Measurement.trackLiveImageToggle(liveImageTrackingName)
    }

    private fun trackSectionStartLoading(sectionBundleName: String?) {
        EventTimerLog.startTimingEvent(sectionBundleName, EventTimerLog.FRONT_RENDER_LOAD)
        Logger.d(TAG, "Loading section : $sectionBundleName")
    }

    override fun trackSectionLoadComplete(
        sectionBundleName: String?,
        isFusion: Boolean,
    ) {
        val additionalFields: MutableMap<String, String> = HashMap()
        additionalFields["section_name"] = sectionBundleName ?: ""
        val outputType = if (isFusion) "fusion" else "classic"
        additionalFields["type"] = outputType
        val currentSection = sectionsRibbonViewModel.getCurrentSection()
        val currentPagerSection = getPager()?.currentFragment
        additionalFields[EventTimerLog.IS_CURRENTLY_VIEWED_SECTION_FIELD] =
            (!sectionBundleName.isNullOrEmpty() &&
                    (sectionBundleName == currentPagerSection?.sectionDisplayName ||
                            sectionBundleName == currentSection?.name ||
                            sectionBundleName == currentSection?.displayName)).toString()
        EventTimerLog.stopTimingEventAndLog(
            sectionBundleName,
            EventTimerLog.FRONT_RENDER_LOAD,
            this,
            true,
            additionalFields,
            "Section Load Metrics",
        )
    }

    override fun trackSlideShowSwipe(
        position: Int,
        navigationBehavior: String?,
    ) {
        Measurement.trackSlideShowSwipe(position, navigationBehavior)
    }

    override fun trackSlideShowOverlayClick(overlayLink: String?) {
        Measurement.trackSlideShowOverlayClick(overlayLink)
    }

    override fun onPagerShown(context: Context?) {
        EventTimerLog.stopTimingEventAndLog(
            EventTimerLog.SYNC_EVENT,
            EventTimerLog.FRONT_LAUNCH_TIME,
            this,
            false,
            EventTimerLog.FRONT_LAUNCH_TIME,
        )
    }

    override fun onSectionLoadStart(
        context: Context?,
        sectionBundleName: String?,
    ) {
        // Clear previous timer related to sectionBundleName if any
        EventTimerLog.dumpTimers(sectionBundleName)
        trackSectionStartLoading(sectionBundleName)
    }

    override fun onSectionLoadError(
        context: Context?,
        sectionBundleName: String?,
        hasCachedContent: Boolean,
        e: Throwable?,
        isFusion: Boolean,
    ) {
        val loadTimeMS =
            EventTimerLog.getStopTime(
                sectionBundleName,
                EventTimerLog.FRONT_RENDER_LOAD,
            )
        // Clear timer related to sectionBundleName
        EventTimerLog.dumpTimers(sectionBundleName)
        Logger.e(TAG, "Error in loading section : $sectionBundleName, error=$e")

        val hasNetwork =
            com.wapo.android.commons.util.Utils
                .isConnectedOrConnecting(applicationContext)
        if (e is Throwable && hasNetwork) {
            val sectionType = if (isFusion) "fusion" else "pagebuilder"
            val source = if (hasCachedContent) "cache" else "network"
            val errorType = e.javaClass.simpleName
            val responseCode: Int? = (e as? VolleyError)?.networkResponse?.statusCode

            EventLog
                .Builder()
                .apply {
                    setMessage("Section Load Error")
                    setModule(LogModules.SECTIONS)
                    set("section_type", sectionType)
                    set("error_type", errorType)
                    set("section_name", sectionBundleName)
                    set("section_source", source)
                    e.cause?.message?.let {
                        set("cause", it)
                    } ?: e.localizedMessage?.let { set("localized_message", it) }
                    set("response_time", loadTimeMS)
                    responseCode?.let { set("response_code", it) }
                }.run {
                    RemoteLog.e(applicationContext, build())
                }
        }
    }

    override fun onSectionLoadSuccess(
        context: Context?,
        sectionBundleName: String?,
        isFusion: Boolean,
    ) {
        trackSectionLoadComplete(sectionBundleName, isFusion)
    }

    // ---------------------------------------------------------------
    //  AlertsActivityInterface interface
    override val alertsSettings: AlertsSettings
        get() = FlagshipApplication.Companion.getInstance().alertsSettings

    override fun openAlertsSettings() {
        startActivity(
            Intent(this, AlertsActivity::class.java)
                .putExtra(
                    TYPE,
                    AlertsSettings.EntryPoint.ALERTS_TAB.trackingName,
                ).addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS),
        )
    }

    override fun openNotification(
        notificationUrl: String,
        notificationArticleType: String?,
        notificationTopic: String?,
    ) {
        val intent: Intent
        if (NotificationArticleType.VIDEO.name == notificationArticleType) {
            intent = Intent(this, VideoActivity::class.java)
            intent.putExtra(VideoActivity.VideoInfoUrlExtraParamName, notificationUrl)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra(TopBarFragment.BackActivityClassParam, MainActivity::class.java.name)
            startActivity(intent)
        } else {
            val activeTabName = activeTabName
            val builder =
                builder()
                    .setArticleSingleUrl(notificationUrl)
                    .alertOriginated(true)
                    .setTabName(activeTabName)
                    .setAppSection(activeTabName)
            if (TextUtils.equals(notificationTopic, PushListener.OPINION_TOPIC_KEY)) {
                builder.opinionPushOriginated(true)
            }
            intent = builder.buildIntent(this)
            intent.action = "ACTION_READ"
            startActivity(intent)
        }
    }

    // ---------------------------------------------------------------
    //  AlertManagerProvider interface
    override fun getAlertManager(): Observable<out AlertManager?> = getContentManagerObs()


    override fun logVideoError(eventLogBuilder: EventLog.Builder?) {
        // log non n/w errors
        val isNetworkError =
            !com.wapo.android.commons.util.Utils.isConnectedOrConnecting(
                applicationContext,
            )
        if (eventLogBuilder != null && !isNetworkError) {
            RemoteLog.e(applicationContext, eventLogBuilder.build())
        }
    }

    override fun getLoader(): ILoader = FlagshipApplication.Companion.getInstance().imageService

    override fun getSectionTracker(): SectionsTracker = this

    // TODO: This should be refactored
    override fun getPager(): SectionsPagerView? {
        val currentFragment = getCurrentFragment()
        if (currentFragment is SectionFrontsFragment) {
            return currentFragment.pager
        } else if (currentFragment is BottomTabFragment) {
            val childFragment =
                currentFragment.childFragmentManager.fragments.last() // get what's on top of the stack
            if (childFragment is SectionFrontsFragment) {
                return childFragment.pager
            }
        }
        return null
    }

    override fun getPersoPodcastViewModel(): PersonalizedPodcastViewModel? {
        return personalizedPodcastViewModel
    }

    override fun getPlayListViewModel(): PlaylistActivityViewModel? = playlistActivityViewModel

    override fun getAppSection(): String? {
        return "" // TODO: Set on ViewModel and get from it
    }

    // ---------------------------------------------------------------
    //  PostTvActivity interface
    override fun shareVideo(
        headline: String?,
        shareUrl: String?,
    ) {
        if (shareUrl == null) {
            Toast
                .makeText(
                    this.applicationContext,
                    "Something went wrong, try again later",
                    Toast.LENGTH_SHORT,
                ).show()
            return
        }
        Share
            .Builder()
            .shareUrl(shareUrl)
            .headline(headline)
            .fromPush(false)
            .arcId("")
            .isVideoShare(true)
            .build()
            .shareItem(this)
    }

    override fun startPIP(mVideo: Video) {
        if (UIUtil.isPIPSupported() && PrefUtils.getPIPEnabled(this)) {
            val i =
                VideoActivity.createIntent(
                    this,
                    VideoActivity::class.java,
                    FlagshipApplication.Companion.getInstance().videoManager.videoUrl,
                    null,
                    mVideo.shareUrl,
                    mVideo.headline,
                    mVideo.subtitleUrl,
                    null,
                    FlagshipApplication.Companion.getInstance().videoManager.videoUrl,
                    mVideo.headline,
                    mVideo.videoSection,
                    null,
                    mVideo.id,
                )
            i.putExtra(VideoActivity.IsPIPRequest, true)
            if (!(mVideo.subtitleUrl == null || mVideo.subtitleUrl == "")) {
                i.putExtra(VideoActivity.IsCaptionsAvailable, true)
            }
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            stopPersistentAudioPlayer()
            startActivity(i)
            overridePendingTransition(R.anim.slide_up, R.anim.slide_down)
        }
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

    override fun startFullScreen(video: Video, playerName: String) {
        if (video.aspectRatio < 1) {
            val postTvVideos = listOf(video)
            val intent =
                VerticalVideosParcel
                    .Builder()
                    .setPostTvVideos(postTvVideos)
                    .setOffset(0)
                    .setPosition(0)
                    .setSourceScreen(sectionNavViewModel.getSectionTitle())
                    .setTabName(activeTabName)
                    .setVerticalVideosConfig(ConfigManager.getInstance().config.verticalVideosConfig)
                    .buildIntent(this)
            startActivity(intent)
        } else {
            val intent =
                FullScreenVideoParcel
                    .Builder()
                    .setVideo(video)
                    .setPlayerName(playerName)
                    .setMode(FullScreenVideoActivity.LaunchMode.FULLSCREEN)
                    .buildIntent(this)
            startActivity(intent)
        }
    }

    override fun isPIPEnabled(): Boolean = PrefUtils.getPIPEnabled(this)

    @Deprecated("Deprecated in Java")
    override fun onTrackingEvent(
        type: TrackingType,
        video: Video,
        value: Any?,
    ) {
        if (value is MutableMap<*, *>) {
            trackVerticalVideoEvent(type, video, value)
        } else {
            val valueMap = mutableMapOf(VideoTracker2.TRACKING_VALUE to value as? Int)
            trackStandardVideoEvent(type, video, valueMap)
        }
    }

    override fun onTrackingEvent(
        videoType: VideoTracker2.VideoType,
        type: TrackingType,
        video: Video,
        value: Any?,
    ) {
        val valueMap = value as MutableMap<*, *>
        if (videoType == VideoTracker2.VideoType.VERTICAL_CAROUSEL ||
            videoType == VideoTracker2.VideoType.VERTICAL_FULLSCREEN
        ) {
            trackVerticalVideoEvent(type, video, valueMap)
        } else if (videoType == VideoTracker2.VideoType.WATCH_AUTOPLAY) {
            trackWatchVideoEvent(type, video, valueMap)
        } else {
            trackStandardVideoEvent(type, video, valueMap)
        }
    }

    private fun trackStandardVideoEvent(
        type: TrackingType,
        video: Video,
        valueMap: MutableMap<*, *>,
    ) {
        var eventLabel: String? = null
        var avExp: String? = null
        var avPlayerType: String? = null
        var avType: String? = null
        var aspectRatio: Float? = null
        if (video.isLooping) {
            eventLabel = VideoTracker2.LOOPING_FRONT
            avExp = VideoTracker2.LOOPING_FRONT
            avPlayerType = VideoTracker2.AV_PLAYER_TYPE_FRONT
        } else if (video.autoplay) {
            eventLabel = VideoTracker2.AUTOPLAY_FRONT
            avExp = VideoTracker2.AUTOPLAY_FRONT
            avPlayerType = VideoTracker2.AV_PLAYER_TYPE_FRONT
        } else if (video.promoIsLooping == true && !video.promoUrl.isNullOrEmpty()) {
            eventLabel = VideoTracker2.LOOPING_PROMO_FRONT
            avExp = VideoTracker2.LOOPING_PROMO_FRONT
            avPlayerType = VideoTracker2.AV_PLAYER_TYPE_FRONT
        }
        val videoStartId =
            if (valueMap.containsKey(VideoTracker2.VIDEO_START_ID)) {
                valueMap[VideoTracker2.VIDEO_START_ID] as String
            } else {
                ""
            }
        if (video.height != 0f && video.width != 0f) {
            aspectRatio = video.width / video.height
        }
        if (aspectRatio != null && aspectRatio < 1) {
            avType = VideoTracker2.AV_TYPE
        } else if (video.aspectRatio < 1) {
            avType = VideoTracker2.AV_TYPE
        }
        val avName =
            if (valueMap.containsKey(VideoTracker2.AV_NAME)) {
                valueMap[VideoTracker2.AV_NAME] as String
            } else {
                ""
            }
        when (type) {
            TrackingType.ON_PLAY_STARTED -> {
                if (video.playType == Video.PLAY_TYPE_NORMAL) {
                    Measurement.playVideo(
                        video.videoName ?: avName,
                        video.pageName,
                        video.videoSection,
                        video.videoSource,
                        video.videoCategory,
                        video.contentId,
                        eventLabel,
                        avExp,
                        avPlayerType,
                        video.arcId,
                        videoStartId,
                        avType
                    )
                } else {
                    Measurement.autoplayVideo(
                        video.videoName ?: avName,
                        video.pageName,
                        video.videoSection,
                        video.videoSource,
                        video.videoCategory,
                        video.contentId,
                        eventLabel,
                        avExp,
                        avPlayerType,
                        video.arcId,
                        videoStartId,
                        avType
                    )
                }
            }

            TrackingType.AD_PLAY_STARTED ->
                Measurement.trackVideoAdStart(
                    video.videoName,
                    video.pageName,
                    video.videoSection,
                    video.videoSource,
                    video.contentId,
                    video.videoCategory,
                    0,
                    "",
                    "",
                    ""
                )

            TrackingType.AD_PLAY_COMPLETED ->
                Measurement.trackVideoAdComplete(
                    video.videoName,
                    video.pageName,
                    video.videoSection,
                    video.videoSource,
                    video.videoCategory,
                    video.contentId,
                    0,
                    "",
                    "",
                    ""
                )

            TrackingType.VIDEO_PERCENTAGE_WATCHED -> {
                val percentageWatched =
                    if (valueMap.containsKey(VideoTracker2.TRACKING_VALUE)) {
                        valueMap[VideoTracker2.TRACKING_VALUE] as Int
                    } else {
                        0
                    }
                Measurement.trackCurrentVideoPercentage(
                    video.videoName ?: avName,
                    video.pageName,
                    video.videoSection,
                    video.videoSource,
                    video.videoCategory,
                    video.contentId,
                    percentageWatched,
                    eventLabel,
                    avExp,
                    avPlayerType,
                    video.arcId,
                    videoStartId,
                    0,
                    avType
                )
            }

            TrackingType.ON_PLAY_COMPLETED ->
                Measurement.stopVideo(
                    video.videoName ?: avName,
                    video.pageName,
                    video.videoSection,
                    video.videoSource,
                    video.videoCategory,
                    video.contentId,
                    eventLabel,
                    avExp,
                    avPlayerType,
                    video.arcId,
                    videoStartId,
                    avType
                )

            TrackingType.ON_MUTE,
            TrackingType.ON_CAPTION_TOGGLE,
            TrackingType.ON_PAUSE,
                ->
                Measurement.trackVideoInteraction(
                    video.videoName ?: avName,
                    video.pageName,
                    video.videoSection,
                    video.videoSource,
                    video.videoCategory,
                    video.contentId,
                    eventLabel,
                    avExp,
                    avPlayerType,
                    video.arcId,
                    valueMap[VideoTracker2.TRACKING_VALUE] as? String,
                )

            TrackingType.VIDEO_PROGRESS -> {
                val progressThreshold =
                    if (valueMap.containsKey(VideoTracker2.PROGRESS_THRESHOLD)) {
                        valueMap[VideoTracker2.PROGRESS_THRESHOLD] as Int
                    } else {
                        0
                    }
                val avExp =
                    if (video.autoplay) {
                        VideoTracker2.AUTOPLAY_FRONT
                    } else if (valueMap.containsKey(VideoTracker2.AV_EXP)) {
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
                val adResponse = video.source as? VideoAdResponse
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
                    avPlayerType,
                    engagedTime,
                    videoStartId,
                    false,
                    activeTabName,
                    avType
                )
            }

            else -> {
                // no op
            }
        }
    }

    private fun trackWatchVideoEvent(
        type: TrackingType,
        video: Video,
        valueMap: MutableMap<*, *>,
    ) {
        var eventLabel: String? = null
        val avExp = VideoTracker2.AUTOPLAY_FEED
        var avPlayerType: String? = null
        val adResponse = video.source as? VideoAdResponse
        val videoStartId =
            if (valueMap.containsKey(VideoTracker2.VIDEO_START_ID)) {
                valueMap[VideoTracker2.VIDEO_START_ID] as String
            } else {
                ""
            }
        val avType = VideoTracker2.AV_TYPE
        when (type) {
            TrackingType.ON_PLAY_STARTED, TrackingType.ON_RESUME -> {
                val progressThreshold =
                    if (valueMap.containsKey(VideoTracker2.PROGRESS_THRESHOLD)) {
                        valueMap[VideoTracker2.PROGRESS_THRESHOLD] as Int
                    } else {
                        0
                    }
                eventLabel = VideoTracker2.AUTOPLAY_FEED
                val avName =
                    if (valueMap.containsKey(VideoTracker2.AV_NAME)) {
                        valueMap[VideoTracker2.AV_NAME] as String
                    } else {
                        ""
                    }
                Measurement.autoplayWatchVideo(
                    video.videoName ?: avName,
                    video.pageName,
                    video.videoSection,
                    video.videoSource,
                    video.videoCategory,
                    video.contentId,
                    progressThreshold,
                    eventLabel,
                    avExp,
                    video.avPlayerType,
                    video.arcId,
                    videoStartId,
                    avType
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
                val avName =
                    if (valueMap.containsKey(VideoTracker2.AV_NAME)) {
                        valueMap[VideoTracker2.AV_NAME] as String
                    } else {
                        ""
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
                    null, // No ads in Carousel
                    null, // No ads in Carousel
                    video.arcId,
                    avExp,
                    video.avPlayerType,
                    activeTabName,
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
                Measurement.stopVerticalVideo(
                    video.videoName ?: avName,
                    video.pageName,
                    video.videoSection,
                    video.videoSource,
                    video.videoCategory,
                    video.contentId,
                    progressThreshold,
                    null, // No ads in Carousel
                    null, // No ads in Carousel
                    video.arcId,
                    avExp,
                    videoStartId,
                    video.avPlayerType,
                    activeTabName
                )
            }

            TrackingType.VIDEO_PROGRESS -> {
                val progressThreshold =
                    if (valueMap.containsKey(VideoTracker2.PROGRESS_THRESHOLD)) {
                        valueMap[VideoTracker2.PROGRESS_THRESHOLD] as Int
                    } else {
                        0
                    }
                val progressAvExp =
                    if (valueMap.containsKey(VideoTracker2.AV_EXP)) {
                        valueMap[VideoTracker2.AV_EXP] as String
                    } else {
                        avExp
                    }
                val engagedTime =
                    if (valueMap.containsKey(VideoTracker2.ENGAGED_TIME)) {
                        valueMap[VideoTracker2.ENGAGED_TIME] as String
                    } else {
                        ""
                    }
                val avName =
                    if (valueMap.containsKey(VideoTracker2.AV_NAME)) {
                        valueMap[VideoTracker2.AV_NAME] as String
                    } else {
                        ""
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
                    progressAvExp,
                    null,
                    engagedTime,
                    videoStartId,
                    true,
                    activeTabName,
                    avType
                )
            }

            else -> {
                // no op
            }
        }
    }

    /**
     * Carousel video tracking events
     */
    private fun trackVerticalVideoEvent(
        type: TrackingType,
        video: Video,
        valueMap: MutableMap<*, *>,
    ) {
        when (type) {
            TrackingType.ON_PLAY_STARTED -> {
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
                val videoStartId =
                    if (valueMap.containsKey(VideoTracker2.VIDEO_START_ID)) {
                        valueMap[VideoTracker2.VIDEO_START_ID] as String
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
                    isCarousel, // is video in carousel
                    miscellany, // carousel swipe direction
                    null, // No ads in Carousel
                    null, // No ads in Carousel,
                    video.arcId,
                    avExp,
                    videoStartId,
                    video.avPlayerType,
                    activeTabName
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
                val videoStartId =
                    if (valueMap.containsKey(VideoTracker2.VIDEO_START_ID)) {
                        valueMap[VideoTracker2.VIDEO_START_ID] as String
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
                Measurement.trackCurrentVerticalVideoPercentage(
                    video.videoName ?: avName,
                    video.pageName,
                    video.videoSection,
                    video.videoSource,
                    video.videoCategory,
                    video.contentId,
                    progressThreshold,
                    percentageWatched,
                    null, // No ads in Carousel
                    null, // No ads in Carousel
                    video.arcId,
                    avExp,
                    video.avPlayerType,
                    activeTabName,
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
                val videoStartId =
                    if (valueMap.containsKey(VideoTracker2.VIDEO_START_ID)) {
                        valueMap[VideoTracker2.VIDEO_START_ID] as String
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
                Measurement.stopVerticalVideo(
                    video.videoName ?: avName,
                    video.pageName,
                    video.videoSection,
                    video.videoSource,
                    video.videoCategory,
                    video.contentId,
                    progressThreshold,
                    null, // No ads in Carousel
                    null, // No ads in Carousel
                    video.arcId,
                    avExp,
                    videoStartId,
                    video.avPlayerType,
                    activeTabName
                )
            }

            else -> {
                // no op
            }
        }
    }

    private val config: Config
        get() = ConfigManager.getInstance().config

    private val isUpdateRequired: Boolean
        get() = config.versionConfig.isAppToBeUpdated(applicationContext)

    private val isOsUpgradeRequired: Boolean
        get() = config.versionConfig.isOsUpgradeRequired()

    private fun showNextDialog() {
        if (AirshipInAppMessageListener.messageDisplayed || iterableActivityViewModel.isInAppMessageDisplayed()) {
            Logger.d(
                TAG,
                "InAppMessage, showAppDialog, skip AppDialogs when iaa message is displayed",
            )
            return
        }
        iterableActivityViewModel.pauseIamMessages()
        val countRuns = AppContext.getCountRuns()
        val caSettlementValues = PaywallService.getConnector().caSettlementValues
        if (isOsUpgradeRequired) {
            if (updateAppDialog == null) {
                updateAppDialog =
                    getOsUpdateDialog(
                        this,
                        config.versionConfig.minSdkMessage,
                    )
            }
            if (updateAppDialog?.isShowing == false) {
                updateAppDialog?.show()
            }
        } else if (isUpdateRequired) {
            if (updateAppDialog == null) {
                val appForceUpdateRequired =
                    config.versionConfig.isAppToBeForceUpdated(applicationContext)
                AirshipAnalytics.startTracking(AirshipAnalytics.Screen.UPDATE_APP_SCREEN.name)
                updateAppDialog =
                    getUpdateDialog(
                        this,
                        !appForceUpdateRequired,
                        DialogInterface.OnDismissListener {
                            AirshipAnalytics.stopTracking(
                                AirshipAnalytics.Screen.UPDATE_APP_SCREEN.name,
                            )
                        },
                    )
            }
            if (updateAppDialog?.isShowing == false) {
                updateAppDialog?.show()
            }
        } else if (!isCASettlementDialogShown() &&
            caSettlementValues != null &&
            caSettlementValues.unFormattedExpirationDate.isNotEmpty()
        ) {
            if (caSettlementDialog?.isAdded != true) {
                caSettlementDialog = CASettlementDialog(caSettlementValues)
                caSettlementDialog.show(supportFragmentManager, CASettlementDialog.TAG)
            }
        } else if (AppContext.needShowRateMessage() &&
            System.currentTimeMillis() - AppContext.getLastRateMessageDisplayedTime() > ConfigManager.getInstance().config.rateAppPromptFrequency &&
            countRuns % RATE_APP_MESSAGE_INTERVAL == 0 &&
            (onboardingFragment == null || onboardingFragment?.get()?.isAdded == false) &&
            reminderScreenFragment == null
        ) {
            if (reviewAppDialog == null) {
                AirshipAnalytics.startTracking(AirshipAnalytics.Screen.REVIEW_APP_SCREEN.name)
                reviewAppDialog =
                    getReviewDialog(
                        this,
                        DialogInterface.OnDismissListener {
                            AirshipAnalytics.stopTracking(
                                AirshipAnalytics.Screen.REVIEW_APP_SCREEN.name,
                            )
                        },
                    )
                if (reviewAppDialog?.isShowing == false) {
                    reviewAppDialog?.show()
                    AppContext.setLastRateMessageDisplayedTime(System.currentTimeMillis())
                }
            }
        } else if (iterableActivityViewModel.hasIamMessages()) {
            iterableActivityViewModel.resumeIamMessages()
        } else if (reminderScreenFragment == null && (onboardingFragment == null || onboardingFragment?.get()?.isAdded == false)
            && (shouldShowAccountHold() || shouldShowAcquisitionReminder() || shouldShowSignInReminder())
        ) {
            Logger.d(TAG, "Account Status :" + PaywallService.getInstance().isSubOnHold)
            when {
                shouldShowAccountHold() -> {
                    accountHoldFragment =
                        showAccountHoldScreen(
                            false,
                            AccountHoldFragment.AccountHoldType.SECTION_DISPLAY,
                        )
                }

                shouldShowAcquisitionReminder() -> {
                    // Show Iap Registration Ask screen once purchase is completed.
                    acquisitionReminderFragment = showAcquisitionReminderScreen(false)
                }

                shouldShowSignInReminder() -> {
                    // Show Iap Registration Reminder screen in Phone/Tablet activities resume.
                    reminderScreenFragment =
                        showReminderScreen(
                            ReminderScreenFragment.ReminderType.IAP_REGISTRATION_ASK_REMINDER,
                            false,
                        )
                }
            }
        }
    }

    private fun shouldShowAccountHold(): Boolean =
        PaywallService.getInstance().isSubOnHold &&
                (accountHoldFragment == null || !accountHoldFragment.isAdded)

    private fun shouldShowAcquisitionReminder(): Boolean =
        !PaywallService.getInstance().isSubOnHold && (acquisitionReminderFragment == null || !acquisitionReminderFragment.isAdded)

    private fun shouldShowSignInReminder(): Boolean =
        !PaywallService.getInstance().isSubOnHold && acquisitionReminderFragment == null

    override fun getPersistentPlayerFrame(): FrameLayout? {
        return null
        // TODO("need to figure out persistent player")
    }

    override fun showPaywallDialog(
        paywallReason: Int,
        paywallType: WallType?,
    ) {
        showWallDialog(PaywallService.getInstance().isWpUserLoggedIn, paywallReason, paywallType)
    }

    override fun showPaywallDialogByName(
        paywallReason: Int,
        wallType: WallType,
        wallName: String,
    ) {
        Logger.d(
            "MainActivity",
            "showPaywallDialogByName: reason=$paywallReason, wallType=$wallType, wallName=$wallName"
        )
        showWallDialog(
            PaywallService.getInstance().isWpUserLoggedIn,
            paywallReason,
            wallType,
            wallName,
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSparseParcelableArray(SAVED_STATE_CONTAINER_KEY, savedStateSparseArray)
        outState.putInt(SAVED_STATE_CURRENT_TAB_KEY, currentSelectItemId)
        outState.putString(
            SAVED_STATE_CURRENT_TAB_ROUTE_KEY,
            navBarViewModel.getCurrentTab().route,
        )
        if (::navController.isInitialized) {
            outState.putParcelable(SAVED_STATE_NAV_KEY, navController.saveState())
        }
    }

    override fun openCustomSection(menuSection: MenuSection) {
        if (isWebSection(menuSection)) {
            Utils.startWeb(menuSection.bundleName, this)
        }
    }

    private fun isWebSection(menuSection: MenuSection): Boolean =
        MenuSection.WEB_TYPE == menuSection.type && URLUtil.isValidUrl(menuSection.bundleName)

    override fun isActivityLoadingPushAlert(): Boolean {
        val intent = intent
        return intent != null && intent.getBooleanExtra(PUSH_ORIGINATED, false)
    }

    override fun shouldSuppressPostPaywallInitVerifyCalls(): Boolean {
        // The app should not make paywall verify calls if it is opening an alert and the alert is a non-opinions article
        val intent = intent
        return (
                intent != null &&
                        intent.getBooleanExtra(PUSH_ORIGINATED, false) &&
                        !intent.getBooleanExtra(OPINION_PUSH_ORIGINATED, false)
                )
    }

    override fun onUserInteraction() {
        userInteracted()
    }

    override fun setTheme(resId: Int) {
        if (nightModeManager.immediateNightModeStatus) {
            super.setTheme(R.style.BaseWaPo_Phone_Night)
        } else {
            super.setTheme(R.style.BaseWaPo_Phone)
        }
    }

    /**
     * Returns true if consent for the targeting preference is provided by the user
     * false otherwise.
     */
    override fun isTargetingConsentProvided(): Boolean = OneTrustHelper.isTargetingEnabled()

    /**
     * Opens the OneTrust Preference Center where the user can view and change their consent choices.
     */
    override fun showOneTrustPreferenceDialog() = OneTrustHelper.ot.showPreferenceCenterUI(this)

    override fun onDismissed() {}

    override fun trackSectionPercentage(
        percentage: Int,
        totalFeatures: Int,
        sectionDisplayName: String,
        bundleId: String,
        grid: Grid?,
    ) {
        showBottomSheetPrompt(percentage)
        Measurement.trackSectionScrollingPercentage(percentage, totalFeatures, sectionDisplayName)
    }

    private fun showBottomSheetPrompt(percentage: Int) {
        val percent = percentage.toFloat().div(100.0f)
        val homePageMessage =
            iterableActivityViewModel.getBannerForPlacement(IamMessageType.FRONT_HOME_SCROLL)
        homePageMessage?.let { message ->
            val threshold = message.displayTrigger?.depth
            if (!audioMediaActivityViewModel.shouldShowPersistentPlayer() && sectionNavViewModel.uiState.value.bottomSheetPrompt == null
                && threshold != null && percent >= threshold && homePageMessage.displayTrigger?.type == DisplayTriggerType.ScrollDepth
            ) {
                sectionNavViewModel.showBottomSheetPrompt(message)
            }
        }
    }

    override fun trackAudioCarouselNavigation(swipeDirection: String) {
        Measurement.trackAudioCarouselSwipeNavigation(swipeDirection)
    }

    override fun trackImmersionCarouselSeen(backToFront: Boolean) {
        Measurement.trackImmersionCarouselSeen(backToFront)
    }

    override fun trackImmersionCarouselNavigation(swipeDirection: String) {
        Measurement.trackImmersionCarouselSwipeNavigation(swipeDirection)
    }

    override fun trackCommentsCarouselNavigation(swipeDirection: String?, position: Int) {
        Measurement.trackCommentsCarouselSwipeNavigation(swipeDirection, position)
    }

    override fun trackAudioCarouselSeen(backToFront: Boolean) {
        Measurement.trackAudioCarouselSeen(backToFront)
    }

    override fun trackExternalCarouselSeen(backToFront: Boolean) {
        Measurement.trackExternalCarouselSeen(backToFront)
    }

    override fun trackSevenLiveCarouselSeen(backToFront: Boolean) {
        Measurement.trackSevenLiveCarouselSeen(backToFront)
    }

    override fun trackLowDataModeTurnedOff(pageName: String?) {
        Measurement.trackLowDataModeOff(pageName)
    }

    override fun onCommentClicked(recommendationsItem: RecommendationsItem) {
        val trackingInfo = TrackingInfo().apply {
            arcId = recommendationsItem.arcId
            contentURL = recommendationsItem.getURL()
            pageName = "front-For You-${recommendationsItem.pageName}"
            contentType = recommendationsItem.contentType
            title = recommendationsItem.headlines?.basic ?: recommendationsItem.headline
        }
        CommentBottomSheetFragment.showComments(
            supportFragmentManager,
            storyID = trackingInfo.arcId ?: "",
            storyUrl = recommendationsItem.getURL(),
            storyTitle = recommendationsItem.headlines?.basic ?: recommendationsItem.headline
            ?: "",
            commentSource = "front - For You",
            trackingInfo = trackingInfo
        )
    }

    override fun onItemClicked(
        position: Int,
        recommendationsItem: RecommendationsItem,
        articleMetas: List<RecommendationsItem>,
        defaultForYouLaunched: Boolean?
    ) {
        val isOpenedFromSectionFront = true
        val sectionDisplayName: String = getString(R.string.for_you_display_name)
        val appSection: String = sectionDisplayName

        builder()
            .setArticleSingleUrl(recommendationsItem.getURL())
            .setArticleMetas(articleMetas.map {
                ArticleMeta(it.getURL(), false, ArticleLinkType.NONE)
            }, position)
            .setArticleOpenedFromSectionFront(isOpenedFromSectionFront)
            .setSectionDisplayName(sectionDisplayName)
            .setAppSection(appSection)
            .setTabName(activeTabName)
            .forYouSectionOriginated(true)
            .setDefaultForYou(defaultForYouLaunched ?: false)
            .setPositionInForYouSection(position)
            .buildIntent(this)
            .also {
                startActivity(it)
            }
    }

    override fun getCustomizedSections(): List<Section> = customNavViewModel.getActiveSections()

    private fun initFirebaseRemoteConfig() {
        FirebaseConfigListener.initAndFetchFirebaseConfig(
            applicationContext,
            onPreFetch = { _, storedValues ->
                // Fetch cached A/B values here
//                val exampleVariant = storedValues[ExampleFeatureFlag.EXAMPLE_AB_PARAMETER_NAME]
//                ExampleFeatureFlag.setTestGroup(exampleVariant)
            },
            onPostFetch = {
                // Re-initialize sub items in case the user's pricing variant has changed
                PaywallService.getBillingHelper().saveSubscriptionProducts(null)

                // Sync Iterable profile after Firebase config is fetched to ensure AB test values are sent
                FlagshipApplication.Companion.getInstance().iterableSdk.syncUserProfile()
            }
        )
    }

    override fun getHabitTiles(): List<Tile> =
        sectionsHabitTilesViewModel.getHabitTiles().map {
            it.toWapoViewsTile()
        }

    private fun switchBottomTab(clickedTab: BottomTab, firstCall: Boolean = false) {
        navBarViewModel.navigateToTab(clickedTab, firstCall = firstCall)
        // Release video resources
        (this.applicationContext as? PostTvApplication)?.videoManager2?.releaseAllVideos()
        // Check low data mode
        navBarViewModel.setLowDataModeEnable(
            isLowDataModeEnable,
            AppPreferences.isLowDataModeEnabledFromSettings(),
        )
        if (clickedTab.route == BottomTab.Ask.route) {
            if (askThePostViewModel.uiState.value.showResponse && (askThePostViewModel.uiState.value.shareUrlState is ShareUrlState.ShareProcessed ||
                        askThePostViewModel.uiState.value.shareUrlState is ShareUrlState.Processing)
            ) {
                askThePostViewModel.hideOrShowResponse(true)
            } else if (askThePostViewModel.uiState.value.showResponse) {
                askThePostViewModel.hideOrShowResponse(false)
                askThePostViewModel.clearLiveConversationData()
            }
            if (askThePostViewModel.uiState.value.showPassage == true) {
                askThePostViewModel.hideOrShowPassage(false)
            }
        } else {
            askThePostViewModel.setPrivateMode(false)
        }
    }

    private fun saveAndRetrieveFragment(
        supportFragmentManager: FragmentManager,
        tabId: Int,
        fragment: Fragment,
    ) {
        val currentFragment = getCurrentFragment()

        if (currentFragment != null) {
            savedStateSparseArray.put(
                currentSelectItemId,
                supportFragmentManager.saveFragmentInstanceState(currentFragment),
            )
        }
        currentSelectItemId = tabId
        fragment.setInitialSavedState(savedStateSparseArray[currentSelectItemId])
    }

    private fun getCurrentFragment(): Fragment? =
        supportFragmentManager.findFragmentById(
            currentSelectItemId,
        )

    override fun onAskSamEvent(userEvent: UserEvent) {
        val audioProvider = AudioProviderImpl()
        val persoUiState = personalizedPodcastViewModel.uiState.value
        when (userEvent) {
            is UserEvent.TryToOpenTalkToThePost -> {
                askThePostViewModel.clearLiveConversationData()
                pendingTalkToThePostData = userEvent

                val isFirstDenial = ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.RECORD_AUDIO
                )
                talkToThePostBottomSheetFragmentFactory.tryToShowTalkFragment(
                    parentActivity = this,
                    parentFragmentManager = supportFragmentManager,
                    conversationId = askThePostViewModel.uiState.value.conversationId,
                    surfaceName = askThePostViewModel.askSamEntryPoint?.name?.lowercase(),
                    isFirstDenial = isFirstDenial,
                    recordAudioPermissionRequest = recordAudioPermissionRequest,
                    audioProvider = audioProvider,
                    timestamp = userEvent.timestamp,
                    transcript = userEvent.transcript,
                )
            }

            is UserEvent.TalkToThePostOpening -> {
                askThePostViewModel.askSamEntryPoint = AskSamEntryPoint.PERSONALIZED_PODCAST
                persoUiState.persoPodTrackingInfo?.first.let {
                    audioProvider.trackPersoEvents(
                        "perso-${it?.podcastType}:${it?.date}",
                        getString(R.string.media_player),
                        getString(R.string.perso_podcast_ask_the_post),
                        avTags = it?.tags,
                        id = it?.id
                    )
                }
            }

            is UserEvent.TalkToThePostClose -> {
                if (askThePostViewModel.askSamEntryPoint == AskSamEntryPoint.PERSONALIZED_PODCAST) {
                    val fragment = supportFragmentManager
                        .findFragmentByTag(TalkToThePostBottomSheetFragment.TAG)
                    personalizedPodcastViewModel.restorePreviousPlayback()
                    (fragment as? DialogFragment)?.dismiss()
                }
            }

            else -> {}

        }
    }
}
