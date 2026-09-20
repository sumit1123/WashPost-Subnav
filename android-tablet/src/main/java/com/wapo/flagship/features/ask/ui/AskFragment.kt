package com.wapo.flagship.features.ask.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.wapo.android.commons.engagement.PageEngagementLifecycleObserver
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.articles2.activities.ArticlesParcel
import com.wapo.flagship.features.articles2.viewmodels.Articles2ViewModel
import com.wapo.flagship.features.ask.fragments.TalkToThePostBottomSheetFragmentFactory
import com.wapo.flagship.features.ask.models.AskSamEntryPoint
import com.wapo.flagship.features.ask.models.AskThePostEvent
import com.wapo.flagship.features.ask.models.MainAskThePostUIState
import com.wapo.flagship.features.ask.models.PostIterableBannerEvent
import com.wapo.flagship.features.ask.viewmodels.AskQuestionsViewModel
import com.wapo.flagship.features.ask.viewmodels.AskThePostViewModel
import com.wapo.flagship.features.ask.viewmodels.ShareUrlState
import com.wapo.flagship.features.audio.viewmodels.AudioMediaActivityViewModel
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor
import com.wapo.flagship.features.search2.events.UserEvent
import com.wapo.flagship.features.search2.fragments.PostAnswersInfoBottomSheetFragment
import com.wapo.flagship.features.search2.viewmodel.PostAnswersFeedbackReaction
import com.wapo.flagship.features.splash.SplashViewModel
import com.wapo.flagship.navigation.ui.BottomTab
import com.wapo.flagship.sdk.iterable.IterableAnalyticsHandler.Companion.ACTION_EVENT_KEY
import com.wapo.flagship.sdk.iterable.IterableAnalyticsHandler.Companion.ATP_CTA_ACTION_QUESTION
import com.wapo.flagship.sdk.iterable.IterableAnalyticsHandler.Companion.ATP_CTA_EVENT_KEY
import com.wapo.flagship.sdk.iterable.viewmodels.IterableActivityViewModel
import com.wapo.android.domain.repository.LoadRenderMetrics
import com.wapo.android.domain.repository.LoadRenderMetricsEvent
import com.wapo.flagship.features.subscribebanner.state.BannerEvent
import com.wapo.flagship.features.subscribebanner.state.BannerLifecycleEvent
import com.wapo.flagship.features.subscribebanner.viewmodel.GlobalBannerViewModel
import com.wapo.flagship.sdk.iterable.IterableAnalyticsHandler.Companion.APP_CONVERSION_ACTION_EVENT_KEY
import com.wapo.flagship.sdk.iterable.models.IamMessageType
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.util.tracking.Measurement.ASK_THE_POST_HISTORY_SAVE_DISMISSED
import com.wapo.flagship.wapomain.MainActivity
import com.washingtonpost.android.R
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.auth.AuthIntentBuilder
import com.washingtonpost.android.paywall.bottomsheet.viewmodel.PostIterableEventType
import com.washingtonpost.android.paywall.bottomsheet.viewmodel.PostIterableEventViewModel
import com.washingtonpost.android.paywall.util.PaywallConstants
import com.wpds.theme.AndroidClassicTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AskFragment : Fragment() {

    @Inject
    lateinit var loadRenderMetrics: LoadRenderMetrics

    private val askQuestionsViewModel: AskQuestionsViewModel by activityViewModels()
    private val askThePostViewModel: AskThePostViewModel by activityViewModels()
    private val audioMediaActivityViewModel: AudioMediaActivityViewModel by activityViewModels()
    private val articles2ViewModel: Articles2ViewModel by activityViewModels()
    private val splashViewModel: SplashViewModel by activityViewModels()

    private val iterableActivityViewModel: IterableActivityViewModel by activityViewModels()

    private val postIterableActivityViewModel: PostIterableEventViewModel by activityViewModels()

    private val globalBannerViewModel: GlobalBannerViewModel by activityViewModels()


    private lateinit var talkToThePostBottomSheetFragmentFactory: TalkToThePostBottomSheetFragmentFactory

    private val recordAudioPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            activity?.let { currentActivity ->
                val isFirstDenial = ActivityCompat.shouldShowRequestPermissionRationale(
                    currentActivity,
                    Manifest.permission.RECORD_AUDIO
                )
                if (isGranted) {
                    talkToThePostBottomSheetFragmentFactory.tryToShowTalkFragment(
                        parentActivity = currentActivity,
                        parentFragmentManager = parentFragmentManager,
                        conversationId = askThePostViewModel.uiState.value.conversationId,
                        surfaceName = askThePostViewModel.askSamEntryPoint?.name?.lowercase(),
                        isFirstDenial = isFirstDenial
                    )
                } else {
                    talkToThePostBottomSheetFragmentFactory.showPermissionAlertDialog(
                        currentActivity,
                        isFirstDenial
                    )
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadRenderMetrics.startLoadRenderMetrics(LoadRenderMetricsEvent.AskRenderEvent)
        val currentState = askThePostViewModel.uiState.value
        if (savedInstanceState == null && currentState.shareUrlState is ShareUrlState.Empty) {
            askThePostViewModel.clearLiveConversationData()
        }

        talkToThePostBottomSheetFragmentFactory = TalkToThePostBottomSheetFragmentFactory()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val askThePostMessage =
            iterableActivityViewModel.getBannerForPlacement(IamMessageType.ASK_THE_POST_BANNER)
        askThePostViewModel.setATPCTABanner(askThePostMessage)
        val isSignedOut = !PaywallService.getInstance().isWpUserLoggedIn
        askThePostViewModel.showRegisterView(isSignedOut)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val askThePostQuestionsUIState =
                    askQuestionsViewModel.uiState.collectAsStateWithLifecycle()
                val askThePostUIState = askThePostViewModel.uiState.collectAsStateWithLifecycle()
                val audioPlaybackState =
                    audioMediaActivityViewModel.showPlayerEvent.observeAsState().value?.audioPlaybackState
                val showAnonAlert = askThePostUIState.value.isPrivateModeRequested
                val isAnonModeOn = askThePostUIState.value.isPrivateModeEnabled
                val fragmentManager = parentFragmentManager
                AndroidClassicTheme {
                    Alert(
                        if (isAnonModeOn) stringResource(R.string.turn_off_anonymous_chat) else stringResource(
                            R.string.turn_on_anonymous_chat
                        ),
                        description = if (isAnonModeOn) stringResource(R.string.anonymous_mode_off_description) else stringResource(
                            R.string.anonymous_mode_description
                        ),
                        confirmText = stringResource(R.string.confirm),
                        {
                            if (askThePostViewModel.uiState.value.conversationHistory.isNotEmpty()) {
                                askThePostViewModel.clearLiveConversationData()
                                askThePostViewModel.hideOrShowResponse(false)
                            }
                            askThePostViewModel.setPrivateMode(!isAnonModeOn)
                            askThePostViewModel.setIsPrivateModeRequested(false)
                        },
                        { askThePostViewModel.setIsPrivateModeRequested(false) },
                        showAnonAlert
                    )
                    val mainAskThePostUIState = MainAskThePostUIState(
                        askThePostUIState = askThePostUIState.value,
                        askThePostQuestionsUiState = askThePostQuestionsUIState.value,
                        audioPlaybackState = audioPlaybackState
                    )
                    AskScreen(
                        mainAskThePostUIState,
                        askThePostUIEvent = {
                            handleAskThePostUIEvents(it)
                        },
                        fragmentManager
                    )
                }
            }
            Measurement.trackBottomTabNavigation(BottomTab.Ask.trackingName)
            observePageEngagement()
            observeUserEvents()
            observeAskThePostEvent()
            observeShareProcessingEvent()
            askThePostViewModel.uiState.value.incomingShareId?.let {
                askThePostViewModel.processDeepLink(it)
            }
        }
        observePostSignInIterableEvent()
        return view
    }

    override fun onDestroy() {
        talkToThePostBottomSheetFragmentFactory.onDestroy()
        super.onDestroy()
    }

    private fun observePostSignInIterableEvent() {
        postIterableActivityViewModel.postSignInOrSubscribeIterableEvent.observe(viewLifecycleOwner) {
            if (it != null) {
                sendIterableEvent(it)
                when (askThePostViewModel.uiState.value.postIterableBannerEvent) {
                    PostIterableBannerEvent.ContinueShareConvo -> {
                        askThePostViewModel.showRegisterView(false)
                        askThePostViewModel.setATPCTABanner(null)
                    }

                    PostIterableBannerEvent.SaveConversation -> {
                        askThePostViewModel.hideOrShowResponse(false)
                        askThePostViewModel.setATPCTABanner(null)
                    }

                    PostIterableBannerEvent.Dismiss -> {
                        askThePostViewModel.showRegisterView(false)
                        askThePostViewModel.setATPCTABanner(null)
                    }

                    PostIterableBannerEvent.Subscribing -> {
                        if (PaywallService.getInstance().isSubActive) askThePostViewModel.setATPCTABanner(
                            null
                        )
                    }

                    is PostIterableBannerEvent.ShareConversation -> {
                        val event =
                            askThePostViewModel.uiState.value.postIterableBannerEvent as PostIterableBannerEvent.ShareConversation
                        askThePostViewModel.shareATP(event.isTurn)
                        askThePostViewModel.setATPCTABanner(null)
                    }

                    else -> {
                        Logger.d(
                            TAG,
                            "No handling state - ${askThePostViewModel.uiState.value.postIterableBannerEvent}"
                        )
                    }
                }
                postIterableActivityViewModel.resetPostSignInOrSubscribeEvent()
            }
        }
    }

    private fun sendIterableEvent(type: PostIterableEventType) {
        when (type) {
            PostIterableEventType.SIGN_IN_OR_REGISTER -> {
                if (PaywallService.getInstance().isWpUserLoggedIn) {
                    askThePostPostSignInOrSubscribeEvent(isSignedInOrRegistered = true)
                }
            }

            PostIterableEventType.SUBSCRIBE -> {
                if (PaywallService.getInstance().isSubActive) {
                    askThePostPostSignInOrSubscribeEvent(isSubscribed = true)
                }
            }
        }
    }

    private fun handleAskThePostUIEvents(event: AskThePostEvent) {
        when (event) {
            AskThePostEvent.ClearLiveConversationData -> {
                askThePostViewModel.clearLiveConversationData()
            }

            is AskThePostEvent.ShareUrlStateEvent -> {
                val askThePost = getString(R.string.ask_the_post)
                when (event.shareUrlState) {
                    is ShareUrlState.Copy -> {
                        val link = generateShareLink(event.shareUrlState.shareId)
                        val clipboard =
                            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip: ClipData = ClipData.newPlainText(askThePost, link)
                        clipboard.setPrimaryClip(clip)
                    }

                    ShareUrlState.Error -> {
                        shareErrorToast(R.string.share_message_error)
                    }

                    is ShareUrlState.Processing -> {
                        event.shareUrlState.shareId.let {
                            askThePostViewModel.handleDeepLink(it)
                        }
                    }

                    ShareUrlState.ShareProcessed -> {}

                    is ShareUrlState.SuccessShare -> {
                        //handled in main activity, no need to do anything here
                    }

                    else -> {
                        Logger.e(
                            MainActivity.TAG,
                            "Error handling state - Share URL state: ${event.shareUrlState}"
                        )

                    }
                }
            }

            is AskThePostEvent.UserEvent -> {
                askThePostViewModel.eventTrigger(event.userEvent)
            }

            AskThePostEvent.PassagesClicked -> askThePostViewModel.seePassagesClicked()
            AskThePostEvent.TrackArticleClicked -> {
                Measurement.trackATPArticleClick(askThePostViewModel.analyticsData.second, false)

            }

            is AskThePostEvent.StartLiveConversation -> {
                askThePostViewModel.analyticsData =
                    Pair(event.question, askThePostViewModel.analyticsData.second)
                val question = event.question
                event.newText?.let {
                    askThePostViewModel.startLiveConversation(
                        it.trim(),
                        true,
                        type = "article"
                    )
                } ?: event.question?.text?.let {
                    askThePostViewModel.askSamEntryPoint =
                        AskSamEntryPoint.ARTICLE_EMBED
                    askThePostViewModel.hideOrShowResponse(true)
                    askThePostViewModel.startLiveConversation(
                        it.trim(),
                        true,
                        type = question.type ?: "article"
                    )
                }
            }

            is AskThePostEvent.StartSamLiveConversation -> {
                askThePostViewModel.askSamEntryPoint =
                    AskSamEntryPoint.ARTICLE_EMBED
                askThePostViewModel.analyticsData =
                    Pair(null, askThePostViewModel.analyticsData.second)
                askThePostViewModel.startLiveConversation(
                    event.text,
                    type = "article"
                )
            }

            is AskThePostEvent.IsPrivateModeRequested -> {
                askThePostViewModel.setIsPrivateModeRequested(true)
            }

            AskThePostEvent.OnDoneLoadingFullConversation -> {
                askThePostViewModel.onDoneLoadingFullConversation()
            }

            is AskThePostEvent.SetPassageData -> {
                askThePostViewModel.setPassageData(event.passageData)
            }

            is AskThePostEvent.InitiatePostAnswersFeedback -> {
                askThePostViewModel.initiatePostAnswersFeedback(
                    event.endpoint,
                    event.responseId,
                    event.reaction
                )
            }

            is AskThePostEvent.ShowSourceSheetWith -> {
                askThePostViewModel.showSourceSheetWith(event.carouselItems)
            }

            is AskThePostEvent.Share -> {
                askThePostViewModel.shareATP(event.isTurn)
            }

            is AskThePostEvent.SetCitationData -> {
                askThePostViewModel.setCitationData(
                    event.selectedPassageInfo
                )
                askThePostViewModel.seeCitationsClicked()
            }

            is AskThePostEvent.QuestionClicked -> {
                askQuestionsViewModel.updateEnteredText(event.question)
                lifecycleScope.launch {
                    askThePostViewModel.hideOrShowResponse(true)
                    askThePostViewModel.startLiveConversation(
                        event.question.trim(),
                        true,
                        event.tabName
                    )
                    askThePostQuestionAskedEvent()
                }
            }

            is AskThePostEvent.TextSubmitted -> {
                lifecycleScope.launch {
                    askQuestionsViewModel.updateEnteredText(event.text.trim())
                    askThePostViewModel.hideOrShowResponse(true)
                    askThePostViewModel.startLiveConversation(event.text.trim())
                    askThePostQuestionAskedEvent()
                }
            }

            AskThePostEvent.ResetShare -> {
                askThePostViewModel.resetShareState()
            }

            is AskThePostEvent.HideOrShowPassage -> {
                askThePostViewModel.hideOrShowPassage(event.show)
            }

            is AskThePostEvent.PostAnswersCarouselItemClick -> {
                askThePostViewModel.readMoreArticleClicked()
                askThePostViewModel.hideOrShowPassage(true)
                Measurement.trackViewPassage(event.itemClicked)
                articles2ViewModel.eventTrigger(
                    event.userEvent
                )
            }

            is AskThePostEvent.HideOrShowHistory -> {
                askThePostViewModel.showHistory(event.show)
            }

            is AskThePostEvent.OpenConversation -> {
                askThePostViewModel.openConversationWithId(
                    event.conversationId,
                )
                if (event.setPrivateMode != null) {
                    askThePostViewModel.setPrivateMode(event.setPrivateMode)
                }
                askThePostViewModel.hideOrShowResponse(event.hideOrShowResponse)
                askThePostViewModel.showHistory(event.showHistory)
            }

            AskThePostEvent.DismissSplash -> {
                splashViewModel.dismissSplashScreen()
            }

            is AskThePostEvent.ATPCtaClicked -> {
                askThePostViewModel.setPostIterableBannerEvent(event.postIterableBannerEvent)
                globalBannerViewModel.setBannerEvent(BannerEvent.BannerClicked(event.message))
            }

            is AskThePostEvent.ATPCtaDismissed -> {
                globalBannerViewModel.setBannerEvent(BannerEvent.BannerDismissed(event.banner))
                askThePostViewModel.showBanner(false)
            }

            is AskThePostEvent.CreateAccountOrSignIn -> {
                askThePostViewModel.showRegisterModal(false)
                askThePostViewModel.showRegisterBeforeShareModal(null, event.iterableEvent)
                askThePostViewModel.showHistory(false)
                showSignInScreen(event.navBehavior)
                askThePostViewModel.setPostIterableBannerEvent(event.iterableEvent)

            }

            is AskThePostEvent.DismissRegisterNewChatModal -> {
                Measurement.trackATPCloseProfileInteraction(ASK_THE_POST_HISTORY_SAVE_DISMISSED)
                askThePostViewModel.showRegisterModal(false)
                if (event.startNewChat) {
                    askThePostViewModel.clearLiveConversationData()
                    askThePostViewModel.hideOrShowResponse(false)
                }
            }

            AskThePostEvent.StartNewChat -> {
                askThePostViewModel.showRegisterModal(false)
                askThePostViewModel.hideOrShowResponse(false)
                askThePostViewModel.clearLiveConversationData()
                Measurement.trackNewChat()
            }

            is AskThePostEvent.ShowShareRegisterModal -> {
                if (event.shareType == null) {
                    Measurement.trackShareChatModalDismissed()
                }
                askThePostViewModel.showRegisterBeforeShareModal(event.shareType)
            }

            is AskThePostEvent.TalkToThePostOpen -> {
                askThePostViewModel.eventTrigger(UserEvent.TalkToThePostOpen())
            }

            is AskThePostEvent.AskThePostBannerSeen -> {
                event.message.attributionInfo.let {
                    globalBannerViewModel.setBannerEvent(
                        BannerEvent.ImpressionEvent(
                            BannerLifecycleEvent.StartImpression(it)
                        )
                    )
                }
            }

            else -> {
                Logger.d(TAG, "No handling state - $event")
                //no-op for other events that are handled in the MainActivity
            }
        }
    }

    private fun showSignInScreen(navBehavior: String) {
        val authIntent = AuthIntentBuilder().addIsSignUp(false).build()
        PaywallService.getConnector().showSignInScreen(
            parentFragmentManager,
            authIntent,
            navBehavior,
            PaywallConstants.WallType.ATP_SOFTWALL,
            false,
            null
        )
    }

    private fun observeShareProcessingEvent() {
        lifecycleScope.launch {
            askThePostViewModel.askThePostEvent.collect {
                when (it) {
                    is AskThePostEvent.ShareUrlStateEvent -> {
                        if (it.shareUrlState is ShareUrlState.Processing) {
                            askThePostViewModel.handleDeepLink(it.shareUrlState.shareId)
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    private fun observeAskThePostEvent() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                askThePostViewModel.askThePostEvent.collect { event ->
                    val askThePost = getString(R.string.ask_the_post)
                    when (event) {
                        is AskThePostEvent.InitiatePostAnswersFeedbackEvent -> {
                            //handled in MainActivity
                        }

                        is AskThePostEvent.ShareUrlStateEvent -> {
                            when (event.shareUrlState) {
                                is ShareUrlState.Copy -> {
                                    val link = generateShareLink(event.shareUrlState.shareId)
                                    val clipboard =
                                        requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip: ClipData = ClipData.newPlainText(askThePost, link)
                                    clipboard.setPrimaryClip(clip)
                                }

                                ShareUrlState.Empty -> {}
                                ShareUrlState.Error -> shareErrorToast(R.string.share_message_error)
                                ShareUrlState.Loading -> {}
                                is ShareUrlState.Processing -> {
                                    //handled in observeShareProcessingEvent, no need to do anything here
                                }

                                ShareUrlState.ShareExpired -> {}
                                ShareUrlState.ShareProcessed -> {}

                                is ShareUrlState.SuccessShare -> {
                                    //handled in MainActivity, no need to do anything here
                                }
                            }
                        }

                        is AskThePostEvent.UserEvent -> {
                            when (event.userEvent) {
                                is UserEvent.TalkToThePostOpen -> {
                                    activity?.let { currentActivity ->
                                        val isFirstDenial =
                                            ActivityCompat.shouldShowRequestPermissionRationale(
                                                currentActivity,
                                                Manifest.permission.RECORD_AUDIO
                                            )
                                        talkToThePostBottomSheetFragmentFactory.tryToShowTalkFragment(
                                            parentActivity = currentActivity,
                                            parentFragmentManager = parentFragmentManager,
                                            conversationId = askThePostViewModel.uiState.value.conversationId,
                                            surfaceName = askThePostViewModel.askSamEntryPoint?.name?.lowercase(),
                                            recordAudioPermissionRequest = recordAudioPermissionRequest,
                                            isFirstDenial = isFirstDenial
                                        )
                                    }
                                }

                                else -> {}
                            }
                        }

                        is AskThePostEvent.ATPCtaDismissed -> {
                            globalBannerViewModel.setBannerEvent(BannerEvent.BannerDismissed(event.banner))
                            askThePostViewModel.showBanner(false)
                        }

                        AskThePostEvent.StartNewChat -> {
                            //handled in the composable, no need to do anything here
                        }

                        is AskThePostEvent.ShowShareRegisterModal -> {
                            if (event.shareType == null) {
                                Measurement.trackShareChatModalDismissed()
                            }
                            askThePostViewModel.showRegisterBeforeShareModal(event.shareType)
                        }

                        is AskThePostEvent.AskThePostReady -> {
                            loadRenderMetrics.stopLoadRenderMetrics(LoadRenderMetricsEvent.AskRenderEvent)
                        }

                        else -> {
                            Logger.d(TAG, "No handling state - $event")
                            //no-op for other events that are handled in the MainActivity
                        }
                    }
                }
            }
        }
    }

    private fun askThePostQuestionAskedEvent() {
        val eventKey = ATP_CTA_EVENT_KEY
        val actionKey = ACTION_EVENT_KEY
        val map = mapOf(actionKey to ATP_CTA_ACTION_QUESTION)
        iterableActivityViewModel.sendIterableCustomEvent(eventKey, map)
    }

    private fun askThePostPostSignInOrSubscribeEvent(
        isSubscribed: Boolean = false,
        isSignedInOrRegistered: Boolean = false
    ) {
        val type = when {
            isSubscribed -> {
                "subscribed"
            }

            isSignedInOrRegistered -> {
                "registered"
            }

            else -> {
                return
            }
        }
        val eventKey = APP_CONVERSION_ACTION_EVENT_KEY
        val actionKey = ACTION_EVENT_KEY
        val map = mapOf(actionKey to type)
        iterableActivityViewModel.sendIterableCustomEvent(eventKey, map)
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

    private fun observePageEngagement() {
        lifecycle.addObserver(
            PageEngagementLifecycleObserver(
                pageName = Measurement.getTrackingPageName(BottomTab.Ask.trackingName),
                tabName = BottomTab.Ask.title,
                contentType = Measurement.CONTENT_TYPE_FRONT
            )
        )
    }

    private fun observeUserEvents() {
        articles2ViewModel.userEvent.observe(viewLifecycleOwner) {
            when (it) {
                is UserEvent.PostAnswersInfoClick -> {
                    PostAnswersInfoBottomSheetFragment().show(
                        parentFragmentManager,
                        PostAnswersInfoBottomSheetFragment.Companion.TAG,
                    )
                }

                is UserEvent.PostAnswersFeedbackItemClick -> {
                    Measurement.trackAskThePostFeedBackOpened()
                    askThePostViewModel.initiatePostAnswersFeedback(
                        it.endpoint,
                        it.responseId,
                        it.reaction
                    )
                }

                is UserEvent.PostAnswersCarouselItemClick -> {
                    val intent = ArticlesParcel.builder()
                        .askThePostOriginated(true)
                        .setArticleSingleUrl(it.url)
                        .setNavigationBehavior(Measurement.ASK_THE_POST_THREAD)
                    startActivity(intent.buildIntent(context))
                }

                is UserEvent.DeepLinkItemClick -> {
                    DeepLinksProcessor.processAsync(it.link, scope = lifecycleScope)
                }

                is UserEvent.GiveFeedbackClicked -> {
                    articles2ViewModel.eventTrigger(
                        UserEvent.PostAnswersFeedbackItemClick(
                            askThePostViewModel.feedbackData.first,
                            askThePostViewModel.feedbackData.second,
                            PostAnswersFeedbackReaction.NO
                        )
                    )
                }

                is UserEvent.SeePassagesClicked -> {
                    askThePostViewModel.hideOrShowPassage(true)
                }

                is UserEvent.SeeCitationsClicked -> {
                    askThePostViewModel.seeCitationsClicked()
                }

                else -> {}
            }
        }
    }

    fun shareErrorToast(messageResource: Int) {
        val message = getString(messageResource)
        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    companion object {
        private val TAG = AskFragment::class.java.simpleName
        private const val ASK_WALL_NAME = "ask_the_post"
        const val SEVEN_DAYS_MILLIS = 604800000L
        const val ONE_MINUTE_MILLIS = 60000L
        const val FOURTEEN_DAYS_MILLIS = 1209600000L
        const val FIVE_MINUTES_MILLIS = 300000L
    }
}
