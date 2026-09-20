package com.wapo.flagship.features.ask.ui

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Tab
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.DateUtil
import com.wapo.flagship.features.articles2.viewmodels.Articles2ViewModel
import com.wapo.flagship.features.ask.models.AskThePostEvent
import com.wapo.flagship.features.ask.models.PostIterableBannerEvent
import com.wapo.flagship.features.ask.viewmodels.AskThePostViewModel
import com.wapo.flagship.features.search2.events.UserEvent
import com.wapo.flagship.features.search2.ui.CarouselSubtype
import com.wapo.flagship.features.search2.ui.HistoryItem
import com.wapo.flagship.features.search2.ui.Passage
import com.wapo.flagship.features.search2.ui.PostAnswerUIItem
import com.wapo.flagship.features.search2.ui.VerticalCarousel
import com.wapo.flagship.features.search2.viewmodel.SearchViewModel
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.util.tracking.Measurement.ASK_THE_POST_HISTORY
import com.wapo.flagship.util.tracking.Measurement.ASK_THE_POST_HISTORY_SAVE_DISMISSED
import com.wapo.flagship.views.RegisterView
import com.washingtonpost.android.R
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.auth.AuthIntentBuilder
import com.washingtonpost.android.paywall.util.PaywallConstants
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.wpdsColors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SourceBottomSheet(
    private val caller: SourceBottomSheetCaller? = null
) : BottomSheetDialogFragment() {

    private val askThePostViewModel: AskThePostViewModel by activityViewModels()
    private val articles2ViewModel: Articles2ViewModel by activityViewModels()
    private val searchViewModel: SearchViewModel by activityViewModels()

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isSignedOut = !PaywallService.getInstance().isWpUserLoggedIn
        askThePostViewModel.showRegisterView(isSignedOut)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {

                var showDeleteAlert by remember { mutableStateOf(false) }
                var showTurnOffPrivateModeAlert by remember { mutableStateOf(false) }
                var conversationId by remember { mutableStateOf("") }
                var sendHistoryAnalytics by remember { mutableStateOf(true) }
                val scope = rememberCoroutineScope()
                var tabIndex by remember { mutableIntStateOf(0) }

                val askThePostUIState = askThePostViewModel.uiState.collectAsState().value
                val showPassages = askThePostUIState.showPassage
                val showCitations = askThePostUIState.showCitation
                val passageInfo = askThePostUIState.selectedPassageInfo
                val url = passageInfo?.url
                val position = passageInfo?.position
                val citationInfo = askThePostUIState.storedCitationInfo
                val showHistory = askThePostUIState.showHistory
                val historyList = askThePostUIState.historyList
                val isPrivateModeEnabled = askThePostUIState.isPrivateModeEnabled
                val items = askThePostUIState.sourceSheetItems
                val categories = askThePostUIState.historyCategories
                val pager = rememberPagerState(initialPage = 0) {
                    categories.size
                }

                val askThePostUIEvent: (AskThePostEvent) -> Unit = { event ->
                    handleUIEvent(event)
                }

                BackHandler {
                    if (showPassages == true) {
                        askThePostUIEvent.invoke(AskThePostEvent.PassagesClicked)
                    } else if (showCitations) {
                        askThePostUIEvent.invoke(AskThePostEvent.CitationsClicked)
                    } else {
                        askThePostUIEvent.invoke(AskThePostEvent.HideOrShowHistory(false))
                        dismiss()
                    }
                }

                LaunchedEffect(pager.currentPage) {
                    if (!askThePostUIState.showRegisterInHistoryView) {
                        tabIndex = pager.currentPage
                    }
                }

                AndroidClassicTheme {
                    Surface(
                        modifier = Modifier.fillMaxHeight(),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        color = wpdsColors.surface
                    ) {
                        if (showPassages == true) {
                            Passage(
                                passageInfo = passageInfo,
                                true,
                                { askThePostUIEvent.invoke(AskThePostEvent.UserEvent(UserEvent.SeePassagesClicked())) },
                                {
                                    askThePostUIEvent.invoke(
                                        AskThePostEvent.UserEvent(
                                            UserEvent.PostAnswersCarouselItemClick(
                                                url ?: "",
                                                passageInfo?.passages,
                                                position ?: 0
                                            ), caller
                                        )
                                    )

                                },
                                {
                                    askThePostUIEvent.invoke(AskThePostEvent.HideOrShowPassage(false))
                                }

                            )
                            Measurement.trackViewPassage(position.toString())
                        } else if (showCitations) {
                            Passage(
                                passageInfo = citationInfo,
                                false,
                                showPassage = {
                                    askThePostUIEvent.invoke(AskThePostEvent.UserEvent(UserEvent.SeeCitationsClicked()))
                                },
                                onUserEvent = {
                                    askThePostUIEvent.invoke(
                                        AskThePostEvent.UserEvent(
                                            UserEvent.PostAnswersCarouselItemClick(
                                                citationInfo?.url ?: "",
                                                citationInfo?.passages,
                                                citationInfo?.position ?: 0
                                            ), caller
                                        )
                                    )
                                },
                                backPressed = {
                                },
                                closePressed = {
                                    dismiss()
                                    askThePostUIEvent.invoke(AskThePostEvent.CitationsClicked)
                                }
                            )
                        } else {
                            Column {
                                if (showHistory) {
                                    if (sendHistoryAnalytics) Measurement.trackViewHistory(!askThePostUIState.isUserLoggedIn)
                                    sendHistoryAnalytics = false
                                    Alert(
                                        title = stringResource(R.string.delete_chat_title),
                                        description = stringResource(R.string.delete_chat_description),
                                        confirmText = stringResource(
                                            R.string.delete_btn
                                        ),
                                        onConfirm = {
                                            askThePostUIEvent.invoke(
                                                AskThePostEvent.DeleteConversation(
                                                    conversationId,
                                                    conversationId == askThePostUIState.conversationId
                                                )
                                            )
                                            showDeleteAlert = false
                                        },
                                        onDismiss = { showDeleteAlert = false },
                                        showDeleteAlert
                                    )
                                    Alert(
                                        title = stringResource(R.string.leaving_anonymous_chat_title),
                                        description = stringResource(
                                            R.string.leaving_anonymous_chat_description
                                        ),
                                        confirmText = stringResource(R.string.confirm),
                                        onConfirm = {
                                            scope.launch {
                                                askThePostUIEvent.invoke(
                                                    AskThePostEvent.OpenConversation(
                                                        conversationId = conversationId,
                                                        setPrivateMode = false,
                                                        hideOrShowResponse = true,
                                                        showHistory = false
                                                    )
                                                )
                                            }
                                        },
                                        { showTurnOffPrivateModeAlert = false },
                                        showTurnOffPrivateModeAlert
                                    )
                                    Row(modifier = Modifier.fillMaxWidth()) {

                                        TabRow(
                                            modifier = Modifier.width(300.dp)
                                                .align(Alignment.CenterVertically),
                                            containerColor = wpdsColors.surface,
                                            selectedTabIndex = tabIndex,
                                            divider = {},
                                            indicator = { tabPositions ->
                                                TabRowDefaults.Indicator(
                                                    color = wpdsColors.gray0,
                                                    height = 4.dp,
                                                    modifier = Modifier.tabIndicatorOffset(
                                                        currentTabPosition = tabPositions[tabIndex]
                                                    )
                                                )
                                            }
                                        ) {
                                            categories.forEachIndexed { index, tab ->
                                                val tabSelected = index == tabIndex
                                                val font =
                                                    if (tabSelected) com.wpds.wpds.R.font.franklinitcstd_bold else com.wpds.wpds.R.font.franklinitcstd_light

                                                Tab(
                                                    modifier = Modifier.background(wpdsColors.surface),
                                                    selected = tabSelected,
                                                    text = {
                                                        androidx.compose.material.Text(
                                                            color = wpdsColors.gray0,
                                                            text = tab,
                                                            fontSize = 16.sp,
                                                            letterSpacing = 0.sp,
                                                            maxLines = 1,
                                                            fontFamily = FontFamily(Font(font)),
                                                        )
                                                    }, onClick = {
                                                        if (!askThePostUIState.showRegisterInHistoryView) {
                                                            scope.launch {
                                                                pager.animateScrollToPage(index)
                                                            }
                                                            tabIndex = index
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.weight(1f))

                                        IconButton(onClick = {
                                            askThePostUIEvent.invoke(
                                                AskThePostEvent.HideOrShowHistory(
                                                    show = false,
                                                    skipTracking = true
                                                )
                                            )
                                            sendHistoryAnalytics = true
                                            dismiss()
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Close",
                                                tint = wpdsColors.gray80
                                            )
                                        }
                                    }
                                    HorizontalPager(
                                        verticalAlignment = Alignment.Top,
                                        modifier = Modifier
                                            .background(wpdsColors.surface)
                                            .wrapContentHeight()
                                            .fillMaxWidth(),
                                        userScrollEnabled = !askThePostUIState.showRegisterInHistoryView,
                                        state = pager
                                    ) { index ->
                                        // Our page content
                                        when (categories[index]) {
                                            stringResource(R.string.history) -> {
                                                if (historyList != null && !askThePostUIState.showRegisterInHistoryView) {
                                                    LazyColumn {
                                                        items(historyList) { historyItem ->
                                                            HistoryItem(
                                                                historyItem.historyResponse.conversationTitle
                                                                    ?: "",
                                                                DateUtil().formatRelativeDate(
                                                                    historyItem.historyResponse.lastUpdatedDate
                                                                        ?: ""
                                                                ),
                                                                {
                                                                    scope.launch {
                                                                        conversationId =
                                                                            historyItem.historyResponse.conversationId
                                                                                ?: ""
                                                                        if (historyItem.historyResponse.conversationId == askThePostUIState.conversationId) {
                                                                            dismiss()
                                                                            return@launch
                                                                        }
                                                                        if (isPrivateModeEnabled) {
                                                                            showTurnOffPrivateModeAlert =
                                                                                true
                                                                        } else {
                                                                            askThePostUIEvent.invoke(
                                                                                AskThePostEvent.OpenConversation(
                                                                                    conversationId = historyItem.historyResponse.conversationId
                                                                                        ?: "",
                                                                                    setPrivateMode = null,
                                                                                    hideOrShowResponse = true,
                                                                                    showHistory = false
                                                                                )
                                                                            )
                                                                        }
                                                                    }
                                                                },
                                                                {
                                                                    showDeleteAlert = true
                                                                    conversationId =
                                                                        historyItem.historyResponse.conversationId
                                                                            ?: ""
                                                                },
                                                                historyItem.historyResponse.conversationId == askThePostUIState.conversationId
                                                            )
                                                        }
                                                    }
                                                } else if (askThePostUIState.showRegisterInHistoryView) {
                                                    Column(
                                                        modifier = Modifier
                                                            .background(wpdsColors.surface)
                                                            .fillMaxHeight(),
                                                        verticalArrangement = Arrangement.Bottom
                                                    ) {
                                                        RegisterView(
                                                            header = stringResource(R.string.ask_the_post_register_headline),
                                                            subtitle = stringResource(R.string.ask_the_post_register_subtitle),
                                                            ctaText = stringResource(R.string.ask_the_post_register_cta),
                                                            ctaSubTextPrefix = stringResource(R.string.ask_the_post_register_cta_subtitle_prefix),
                                                            ctaSubTextSuffix = stringResource(R.string.ask_the_post_register_cta_subtitle_suffix),
                                                            showCloseButton = false,
                                                            primaryButtonClicked = {
                                                                askThePostUIEvent.invoke(
                                                                    AskThePostEvent.CreateAccountOrSignIn(
                                                                        ASK_THE_POST_HISTORY,
                                                                        PostIterableBannerEvent.ContinueShareConvo
                                                                    )
                                                                )
                                                            },
                                                            linkClicked = {
                                                                askThePostUIEvent.invoke(
                                                                    AskThePostEvent.CreateAccountOrSignIn(
                                                                        ASK_THE_POST_HISTORY,
                                                                        PostIterableBannerEvent.ContinueShareConvo
                                                                    )
                                                                )
                                                            }

                                                        )
                                                    }
                                                }
                                            }

                                            else -> {
                                                SharedLinksView(
                                                    askThePostUIState,
                                                    askThePostUIEvent
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    IconButton(onClick = {
                                        askThePostUIEvent.invoke(
                                            AskThePostEvent.HideOrShowHistory(
                                                false
                                            )
                                        )
                                        sendHistoryAnalytics = true
                                        dismiss()
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = wpdsColors.gray80
                                        )
                                    }
                                    VerticalCarousel(
                                        modifier = Modifier.fillMaxHeight(),
                                        PostAnswerUIItem.Carousel(
                                            subtype = CarouselSubtype.PASSAGES,
                                            items = items
                                        ),
                                        onUserEvent = { event ->
                                            askThePostUIEvent.invoke(
                                                AskThePostEvent.UserEvent(
                                                    event,
                                                    caller
                                                )
                                            )
                                        },
                                        showPassage = {
                                            askThePostUIEvent.invoke(
                                                AskThePostEvent.HideOrShowPassage(
                                                    true
                                                )
                                            )
                                        },
                                        {
                                            askThePostUIEvent.invoke(
                                                AskThePostEvent.SetPassageData(
                                                    it
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleUIEvent(event: AskThePostEvent) {
        when (event) {
            AskThePostEvent.PassagesClicked -> {
                askThePostViewModel.seePassagesClicked()
                Measurement.trackBackFromPassage()
            }

            AskThePostEvent.CitationsClicked -> {
                askThePostViewModel.seeCitationsClicked()
            }

            is AskThePostEvent.HideOrShowHistory -> {
                if (!event.skipTracking) {
                    Measurement.trackCloseSources()
                }
                askThePostViewModel.showHistory(event.show)
            }

            is AskThePostEvent.OpenConversation -> {
                askThePostViewModel.openConversation(
                    setPrivateMode = event.setPrivateMode,
                    conversationId = event.conversationId,
                    hideOrShowResponse = event.hideOrShowResponse,
                    showHistory = event.showHistory
                )
                dismiss()
            }

            is AskThePostEvent.DeleteConversation -> {
                lifecycleScope.launch {
                    askThePostViewModel.deleteConversation(
                        event.conversationId
                    )
                    if (event.clearConversation == true) {
                        askThePostViewModel.clearLiveConversationData()
                        askThePostViewModel.hideOrShowResponse(false)
                        dismiss()
                    }
                }

            }

            is AskThePostEvent.UserEvent -> {
                when (event.userEvent) {
                    is UserEvent.PostAnswersCarouselItemClick -> {
                        when (event.caller) {
                            SourceBottomSheetCaller.ASK -> {
                                articles2ViewModel.eventTrigger(event.userEvent)
                            }

                            SourceBottomSheetCaller.SEARCH -> {
                                searchViewModel.itemClicked(
                                    event.userEvent
                                )
                            }

                            SourceBottomSheetCaller.PERSOPOD -> {
                                dismiss()
                                askThePostViewModel.eventTrigger(event.userEvent)
                            }

                            else -> {
                                // PERSOPOD: MainActivity observes askThePostViewModel.askThePostEvent.
                                askThePostViewModel.eventTrigger(event.userEvent)

                            }
                        }
                    }

                    is UserEvent.SeeCitationsClicked -> {
                        dismiss()
                        askThePostViewModel.seeCitationsClicked()
                    }

                    is UserEvent.SeePassagesClicked -> {
                        articles2ViewModel.eventTrigger(event.userEvent)
                    }

                    else -> {
                        Logger.d("SourceBottomSheet", "Unhandled user event: ${event.userEvent}")
                    }
                }

            }

            is AskThePostEvent.CopyShareUrl -> {
                askThePostViewModel.copyShareLink(event.shareUrl)
            }

            is AskThePostEvent.HideOrShowPassage -> {
                if (!event.show) {
                    Measurement.trackBackFromPassage()
                }
                askThePostViewModel.hideOrShowPassage(event.show)
            }

            AskThePostEvent.OnDoneLoadingFullConversation -> {
                askThePostViewModel.onDoneLoadingFullConversation()
            }

            is AskThePostEvent.SetPassageData -> {
                askThePostViewModel.setPassageData(event.passageData)
            }

            is AskThePostEvent.DeleteShareUrlOrAll -> {
                askThePostViewModel.deleteShareLinks(event.shareUrl)
            }
            is AskThePostEvent.CreateAccountOrSignIn -> {
                dismiss()
                askThePostViewModel.setPostIterableBannerEvent(event.iterableEvent)
                askThePostViewModel.showRegisterModal(false)
                val authIntent = AuthIntentBuilder().addIsSignUp(false).build()
                PaywallService.getConnector().showSignInScreen(
                    parentFragmentManager,
                    authIntent,
                    event.navBehavior,
                    PaywallConstants.WallType.ATP_SOFTWALL,
                    false,
                    null
                )
            }

            is AskThePostEvent.DismissRegisterNewChatModal -> {
                Measurement.trackATPCloseProfileInteraction(ASK_THE_POST_HISTORY_SAVE_DISMISSED)
                askThePostViewModel.showRegisterModal(false)
            }

            AskThePostEvent.StartNewChat -> {
                //No need to track this event as it's tracked in the AskThePostScreen when the new conversation is created
            }

            is AskThePostEvent.ShowShareRegisterModal -> {
                askThePostViewModel.showRegisterBeforeShareModal(event.shareType)
            }

            else -> {
                Logger.d("SourceBottomSheet", "Unhandled user event: $event")
            }

        }
    }


    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog

        dialog?.apply {
            setCancelable(true)
            setCanceledOnTouchOutside(false)
        }

        val bottomSheet =
            dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as? FrameLayout
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.isHideable = false

            behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState != BottomSheetBehavior.STATE_EXPANDED) {
                        behavior.state = BottomSheetBehavior.STATE_EXPANDED
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {}
            })
        }
    }
}

enum class SourceBottomSheetCaller {
    ASK,
    SEARCH,
    PERSOPOD
}