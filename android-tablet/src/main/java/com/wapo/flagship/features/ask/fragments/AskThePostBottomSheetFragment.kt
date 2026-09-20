package com.wapo.flagship.features.ask.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wapo.android.commons.util.Logger
import com.wapo.android.commons.util.Utils
import com.wapo.flagship.features.articles2.models.Question
import com.wapo.flagship.features.articles2.viewmodels.Articles2ViewModel
import com.wapo.flagship.features.ask.models.AskSamEntryPoint
import com.wapo.flagship.features.ask.models.AskThePostEvent
import com.wapo.flagship.features.ask.models.AskThePostQuestionsUiState
import com.wapo.flagship.features.ask.models.AskThePostUIState
import com.wapo.flagship.features.ask.models.MainAskThePostUIState
import com.wapo.flagship.features.ask.models.PostIterableBannerEvent
import com.wapo.flagship.features.ask.models.ShareType
import com.wapo.flagship.features.ask.ui.Alert
import com.wapo.flagship.features.ask.ui.SourceBottomSheet
import com.wapo.flagship.features.ask.ui.SourceBottomSheetCaller
import com.wapo.flagship.features.ask.ui.dottedBorder
import com.wapo.flagship.features.ask.ui.toDateLong
import com.wapo.flagship.features.ask.viewmodels.AskThePostViewModel
import com.wapo.flagship.features.ask.viewmodels.ShareUrlState
import com.wapo.flagship.features.search2.events.ConversationItem
import com.wapo.flagship.features.search2.events.UserEvent
import com.wapo.flagship.features.search2.fragments.PostAnswersInfoBottomSheetFragment
import com.wapo.flagship.features.search2.model.Citation
import com.wapo.flagship.features.search2.model.Source
import com.wapo.flagship.features.search2.ui.CarouselUIItem
import com.wapo.flagship.features.search2.ui.FixedOrderContentView
import com.wapo.flagship.features.search2.ui.MimeType
import com.wapo.flagship.features.search2.ui.Passage
import com.wapo.flagship.features.search2.ui.PostAnswerUIItem
import com.wapo.flagship.features.search2.ui.TextItem
import com.wapo.flagship.features.search2.ui.TextSubtype
import com.wapo.flagship.features.search2.viewmodel.PostAnswersFeedbackReaction
import com.wapo.flagship.util.PrefUtils
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.wapomain.MainActivity.Companion.TAG
import com.washingtonpost.android.R
import com.washingtonpost.android.paywall.PaywallService
import com.wpds.components.CtaButton
import com.wpds.components.WapoText
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.wpdsColors
import com.wpds.theme.wpdsColorsDark
import com.wpds.utils.doublePulseEffect
import com.wpds.wptheme.WpTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val POST_ANSWERS_ARTICLES = "post-answers-articles"

@AndroidEntryPoint
class AskThePostBottomSheetFragment() : DialogFragment() {

    private val articles2ViewModel: Articles2ViewModel by activityViewModels()
    private val askThePostViewModel: AskThePostViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setContent {
                val questions by articles2ViewModel.questions.observeAsState(initial = emptyList())
                val fragmentManager = parentFragmentManager
                val askThePostUIState by askThePostViewModel.uiState.collectAsStateWithLifecycle()
                val mainAskThePostUIState = MainAskThePostUIState(
                    askThePostUIState = askThePostUIState,
                    askThePostQuestionsUiState = AskThePostQuestionsUiState(),
                    articleQuestions = questions
                )
                AskThePostQuestionsBottomSheet(
                    mainAskThePostUIState,
                    askThePostUIEvent = { askThePostUIEvent ->
                        handleAskThePostUIEvent(askThePostUIEvent)
                    },
                    onDismiss = { dismiss() },
                    fragmentManager = fragmentManager
                )
            }
        }

    private fun handleAskThePostUIEvent(event: AskThePostEvent) {
        when (event) {
            AskThePostEvent.ClearLiveConversationData -> {
                askThePostViewModel.clearLiveConversationData()
            }

            is AskThePostEvent.UserEvent -> {
                articles2ViewModel.eventTrigger(event.userEvent)
            }

            is AskThePostEvent.TalkToThePostOpen -> {
                askThePostViewModel.eventTrigger(UserEvent.TalkToThePostOpen())
            }

            AskThePostEvent.DismissBottomSheet -> {
                dismiss()
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

            is AskThePostEvent.OpenHowItWorks -> {
                PostAnswersInfoBottomSheetFragment().show(
                    parentFragmentManager,
                    PostAnswersInfoBottomSheetFragment.TAG,
                )
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
                if (askThePostViewModel.uiState.value.showRegisterInHistoryView) {
                    askThePostViewModel.showRegisterBeforeShareModal(if (event.isTurn) ShareType.TURN else ShareType.CONVO)
                } else {
                    askThePostViewModel.shareATP(event.isTurn)
                }
            }

            is AskThePostEvent.SetCitationData -> {
                askThePostViewModel.setCitationData(
                    event.selectedPassageInfo
                )
                askThePostViewModel.seeCitationsClicked()
            }

            is AskThePostEvent.ConfirmAnonEnable -> {
                if (askThePostViewModel.uiState.value.conversationHistory.isNotEmpty()) {
                    askThePostViewModel.clearLiveConversationData()
                    askThePostViewModel.hideOrShowResponse(false)
                }
                askThePostViewModel.setPrivateMode(!askThePostViewModel.uiState.value.isPrivateModeEnabled)
                askThePostViewModel.setIsPrivateModeRequested(false)
            }

            is AskThePostEvent.DismissAnonAlert -> {
                askThePostViewModel.setIsPrivateModeRequested(false)
            }

            else -> {
                Logger.e(TAG, "Not handling state - State: $event")
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AskThePostQuestionsBottomSheet(
    mainAskThePostUIState: MainAskThePostUIState,
    askThePostUIEvent: (AskThePostEvent) -> Unit,
    onDismiss: () -> Unit,
    fragmentManager: FragmentManager
) {
    val scope = rememberCoroutineScope()
    val bottomSheetState =
        rememberModalBottomSheetState(
            initialValue = ModalBottomSheetValue.Expanded,
            skipHalfExpanded = true,
            confirmValueChange = { false },
        )
    val askThePostUIState = mainAskThePostUIState.askThePostUIState
    val showPassage = askThePostUIState.showPassage
    val passageInfo = askThePostUIState.selectedPassageInfo
    val conversationHistory = askThePostUIState.conversationHistory
    var displayedTextInMain by remember { mutableStateOf<String?>(null) }
    val disclaimerGradient = Brush.verticalGradient(
        colors = listOf(
            wpdsColors.surface,
            wpdsColors.atpFade
        )
    )

    BackHandler {
        if (conversationHistory.isEmpty()) {
            askThePostUIEvent.invoke(AskThePostEvent.ClearLiveConversationData)
            askThePostUIEvent.invoke(AskThePostEvent.TrackArticleClicked)
            askThePostUIEvent.invoke(AskThePostEvent.DismissBottomSheet)
        } else if (showPassage == true) {
            askThePostUIEvent.invoke(AskThePostEvent.PassagesClicked)

        } else {
            askThePostUIEvent.invoke(AskThePostEvent.ClearLiveConversationData)

        }
    }

    LaunchedEffect(bottomSheetState.isVisible) {
        if (!bottomSheetState.isVisible) {
            onDismiss()
        }
    }
    MaterialTheme {
        AndroidClassicTheme {
            val isAnonModeOn = askThePostUIState.isPrivateModeEnabled
            val showAnonAlert = askThePostUIState.isPrivateModeRequested

            Alert(
                title = if (isAnonModeOn) stringResource(R.string.turn_off_anonymous_chat) else stringResource(
                    R.string.turn_on_anonymous_chat
                ),
                description = if (isAnonModeOn) stringResource(R.string.anonymous_mode_off_description) else stringResource(
                    R.string.anonymous_mode_description
                ),
                confirmText = stringResource(R.string.confirm),
                onConfirm = { askThePostUIEvent(AskThePostEvent.ConfirmAnonEnable) },
                onDismiss = { askThePostUIEvent(AskThePostEvent.DismissAnonAlert) },
                showAlert = showAnonAlert
            )
        }

        ModalBottomSheetLayout(
            sheetState = bottomSheetState,
            sheetContent = {
                Box {
                    var bottomBarHeight by remember { mutableStateOf(0.dp) }
                    val density = LocalDensity.current
                    QuestionAnswerComposable(
                        mainAskThePostUIState = mainAskThePostUIState,
                        askThePostUIEvent = askThePostUIEvent,
                        onDismissSheet = {
                            scope.launch {
                                askThePostUIEvent.invoke(AskThePostEvent.TrackArticleClicked)

                                if (conversationHistory.isNotEmpty()) {
                                    askThePostUIEvent.invoke(AskThePostEvent.ClearLiveConversationData)
                                } else {
                                    bottomSheetState.hide()
                                    askThePostUIEvent.invoke(AskThePostEvent.DismissBottomSheet)
                                }
                            }
                        },
                        onClearSubmittedText = {
                            askThePostUIEvent.invoke(AskThePostEvent.ClearLiveConversationData)
                        },
                        onContextQuestionClicked = { question ->
                            scope.launch {
                                askThePostUIEvent.invoke(
                                    AskThePostEvent.StartLiveConversation(
                                        question
                                    )
                                )
                                if (bottomSheetState.currentValue == ModalBottomSheetValue.HalfExpanded) {
                                    bottomSheetState.show()
                                }
                            }
                        },
                        modifier = Modifier.padding(bottom = bottomBarHeight),
                        fragmentManager = fragmentManager
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                    ) {
                        // Gradient sits behind both children, visible only in the gap
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(disclaimerGradient)
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextFieldComposable(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(wpdsColors.surface),
                                askThePostUIState = askThePostUIState,
                                askThePostUIEvent = askThePostUIEvent,
                                onTextSubmitted = { newText ->
                                    displayedTextInMain = newText
                                    askThePostUIEvent.invoke(
                                        AskThePostEvent.StartLiveConversation(
                                            newText = newText
                                        )
                                    )
                                    scope.launch {
                                        if (bottomSheetState.currentValue ==
                                            ModalBottomSheetValue.HalfExpanded
                                        ) {
                                            bottomSheetState.show()
                                        }
                                    }
                                },
                                onFocused = {

                                }
                            )
                            AndroidClassicTheme {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(wpdsColors.surface)
                                        .onGloballyPositioned { coordinates ->
                                            bottomBarHeight =
                                                with(density) { coordinates.size.height.toDp() }
                                        }
                                ) {
                                    Text(
                                        text = stringResource(R.string.ask_the_post_disclaimer),
                                        fontSize = 12.sp,
                                        lineHeight = 15.sp,
                                        color = wpdsColors.gray100,
                                        fontFamily = FontFamily(Font(com.wapo.view.R.font.franklinitcstd_light)),
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                    if (showPassage == true) {
                        val url = passageInfo?.url
                        val position = passageInfo?.position
                        if (passageInfo != null && url != null && position != null) {
                            Passage(
                                passageInfo,
                                true,
                                {
                                    askThePostUIEvent.invoke(AskThePostEvent.UserEvent(UserEvent.SeePassagesClicked()))
                                },
                                onUserEvent = {
                                    askThePostUIEvent.invoke(
                                        AskThePostEvent.UserEvent(
                                            UserEvent.PostAnswersCarouselItemClick(
                                                url,
                                                passageInfo.passages,
                                                position,
                                            )
                                        )
                                    )
                                },
                                {
                                    askThePostUIEvent.invoke(AskThePostEvent.HideOrShowPassage(false))
                                }
                            )
                        }
                    }
                }
            },
            sheetBackgroundColor = Color.Transparent,
            scrimColor = Color.Black.copy(alpha = 0.32f),
        ) {
        }
    }
}

@Composable
fun QuestionAnswerComposable(
    mainAskThePostUIState: MainAskThePostUIState,
    askThePostUIEvent: (AskThePostEvent) -> Unit,
    onDismissSheet: () -> Unit,
    onClearSubmittedText: () -> Unit,
    onContextQuestionClicked: (Question) -> Unit,
    modifier: Modifier,
    fragmentManager: FragmentManager
) {
    AndroidClassicTheme {
        val conversationHistory = mainAskThePostUIState.askThePostUIState.conversationHistory
        var expanded by remember { mutableStateOf(false) }
        val scrollState = rememberScrollState()
        Surface(
            modifier = if (conversationHistory.isNotEmpty()) modifier.fillMaxHeight() else modifier,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = wpdsColors.surface,
            elevation = 8.dp,
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(
                            top = 16.dp,
                            start = 16.dp,
                            end = 16.dp,
                        ),
            ) {
                // --- START: Persistent Header ---
                AskThePostHeader(
                    askThePostUIEvent,
                    expanded,
                    mainAskThePostUIState.askThePostUIState.isPrivateModeEnabled,
                    conversationHistory.lastOrNull() is ConversationItem.QuestionItem,
                    onDismissSheet,
                    onClearSubmittedText
                )

                Spacer(modifier = Modifier.height(16.dp))
                // --- END: Persistent Header ---

                // --- START: Scrollable
                //BoxWithConstraints allows us to measure height of scrollable area
                BoxWithConstraints(
                    modifier = if (conversationHistory.isEmpty()) Modifier else Modifier.weight(
                        1f
                    )
                ) {
                    ScrollableComponent(
                        mainAskThePostUIState = mainAskThePostUIState,
                        askThePostUIEvent = askThePostUIEvent,
                        onContextQuestionClicked,
                        true,
                        0.dp,
                        scrollableAreaHeight = this.maxHeight,
                        scrollState,
                        fragmentManager,
                        true
                    )
                    // --- END: Scrollable
                }
            }
            Spacer(modifier.height(50.dp))
        }
    }
}

@Composable
fun AskThePostHeader(
    askThePostUIEvent: (AskThePostEvent) -> Unit,
    expanded: Boolean,
    isPrivateModeEnabled: Boolean,
    hasConversationStarted: Boolean,
    onDismissSheet: () -> Unit,
    onClearSubmittedText: () -> Unit
) {
    var expanded by remember { mutableStateOf(expanded) }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "More context on this article",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 22.5.sp,
                color = wpdsColors.gray0,
                fontFamily =
                    FontFamily(
                        Font(com.wapo.view.R.font.franklinitcstd_bold),
                    ),
                letterSpacing = 0.em
            )
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Powered by Ask The Post AI",
                    style = MaterialTheme.typography.caption,
                    color = wpdsColors.gray80,
                    fontSize = 14.sp,
                    lineHeight = 17.5.sp,
                    fontFamily =
                        FontFamily(
                            Font(com.wapo.view.R.font.franklinitcstd_light),
                        ),
                    letterSpacing = 0.em
                )
                IconButton(
                    onClick = {
                        askThePostUIEvent.invoke(AskThePostEvent.UserEvent(UserEvent.PostAnswersInfoClick))
                    },
                    modifier =
                        Modifier
                            .size(16.dp)
                            .padding(top = 2.dp),
                ) {
                    Icon(
                        painter = painterResource(com.wapo.flagship.features.audio.R.drawable.info),
                        contentDescription = "Info",
                        tint = Color.Gray,
                        modifier =
                            Modifier
                                .size(16.dp)
                                .padding(0.dp),
                    )
                }
            }
        }
        AskThePostEllipsisMenuRow(
            askThePostUIEvent = askThePostUIEvent,
            hasConversationStarted,
            expanded,
            isPrivateModeEnabled = isPrivateModeEnabled,
            onDismissRequest = {
                expanded = false
            },
            onIconClicked = {
                expanded = true
            },
            onDismissSheet = onDismissSheet,
            onClearSubmittedText = onClearSubmittedText
        )
    }
}

@Composable
fun AskThePostEllipsisMenuRow(
    askThePostUIEvent: (AskThePostEvent) -> Unit,
    hasConversationStarted: Boolean,
    expanded: Boolean,
    isPrivateModeEnabled: Boolean,
    onIconClicked: () -> Unit,
    onDismissSheet: () -> Unit,
    onDismissRequest: () -> Unit,
    onClearSubmittedText: () -> Unit
) {
    Row(verticalAlignment = Alignment.Top) {
        // Close button for the entire sheet if there isn't submitted text else it clear the
        // submitted text and returns to the previous questions screen.
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onDismissRequest.invoke() },
            offset = DpOffset((-56).dp, (-12).dp),
            modifier = Modifier.background(
                wpdsColors.gray700
            )
        ) {
            if (hasConversationStarted) {
                DropdownMenuItem(onClick = {
                    askThePostUIEvent.invoke(AskThePostEvent.UserEvent(UserEvent.GiveFeedbackClicked()))
                    onDismissRequest.invoke()
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(com.wpds.wpds.R.drawable.comment),
                            contentDescription = stringResource(R.string.give_feedback),
                            tint = wpdsColors.gray20
                        )
                        Text(
                            text = stringResource(R.string.give_feedback),
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            color = wpdsColors.gray20,
                            maxLines = 1,
                            modifier = Modifier.padding(start = 8.dp),
                            fontFamily =
                                FontFamily(
                                    Font(com.wapo.view.R.font.franklinitcstd_light),
                                ),
                        )
                    }
                }
            }
            AskDropdownMenuItem(
                onClick = {
                    askThePostUIEvent.invoke(AskThePostEvent.OpenHowItWorks)
                    onDismissRequest.invoke()
                },
                iconResource = R.drawable.ic_info,
                textResource = R.string.how_it_works_ask_the_post
            )
            val faqAskThePostAiUrl = stringResource(R.string.faq_ask_the_post_ai_url)
            AskDropdownMenuItem(
                onClick = {
                    askThePostUIEvent.invoke(AskThePostEvent.UserEvent(UserEvent.DeepLinkItemClick(faqAskThePostAiUrl)))
                    onDismissRequest.invoke()
                },
                iconResource = com.wapo.flagship.features.audio.R.drawable.ic_external_link,
                textResource = R.string.why_we_built_this
            )
            AskDropdownMenuItem(
                onClick = {
                    askThePostUIEvent.invoke(AskThePostEvent.IsPrivateModeRequested(true))
                    onDismissRequest.invoke()
                },
                iconResource = if (isPrivateModeEnabled) R.drawable.ic_disable_private_mode else R.drawable.ic_enable_private_mode,
                textResource = if (isPrivateModeEnabled) R.string.turn_off_anonymous_chat else R.string.turn_on_anonymous_chat
            )
        }
        IconButton(
            onClick = onIconClicked,
            modifier = Modifier.padding(vertical = 4.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_ellipsis_playlist),
                contentDescription = "menu",
                tint = wpdsColors.gray0,
            )
        }
        IconButton(
            onClick =
                if (!hasConversationStarted) {
                    onDismissSheet

                } else {
                    onClearSubmittedText
                },
            modifier = Modifier.padding(top = 4.dp, start = 0.dp, end = 4.dp, bottom = 4.dp),
        ) {
            Icon(
                painter = painterResource(com.wpds.wpds.R.drawable.close),
                contentDescription = "Close sheet",
                tint = wpdsColors.gray0,
            )
        }
    }
}

@Composable
private fun AskDropdownMenuItem(
    onClick: () -> Unit,
    iconResource: Int,
    textResource: Int,
) {
    DropdownMenuItem(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(iconResource),
                contentDescription = stringResource(textResource),
                tint = wpdsColors.gray20
            )
            Text(
                text = stringResource(textResource),
                fontSize = 16.sp,
                lineHeight = 20.sp,
                color = wpdsColors.gray20,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp),
                fontFamily =
                    FontFamily(
                        Font(com.wapo.view.R.font.franklinitcstd_light),
                    ),
            )
        }
    }
}

@Composable
fun TextFieldComposable(
    modifier: Modifier,
    askThePostUIState: AskThePostUIState,
    askThePostUIEvent: (AskThePostEvent) -> Unit,
    onTextSubmitted: (String) -> Unit,
    onFocused: () -> Unit
) {
    val isPrivateModeEnabled = askThePostUIState.isPrivateModeEnabled

    // Defined styles based on mode
    val shape = if (isPrivateModeEnabled) RoundedCornerShape(14.dp) else CircleShape
    val containerHeight = if (isPrivateModeEnabled) 120.dp else 64.dp
    val border = if (isPrivateModeEnabled) {
        // Dotted border when anonymous
        Modifier.dottedBorder(
            color = wpdsColors.onSurface,
            strokeWidth = 2.dp,
            dashLength = 2.dp,
            gapLength = 4.dp,
            shape = shape
        )
    } else {
        // Standard solid border
        Modifier.border(1.dp, if (isSystemInDarkTheme()) wpdsColors.gray300Static else wpdsColors.gray300, shape)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 0.dp)
            .height(containerHeight)
            .then(border),
        shape = shape,
        elevation = 0.dp,
        color = if (isSystemInDarkTheme()) wpdsColors.gray300Static else wpdsColors.gray700Static
    ) {
        Column(
            modifier =
                Modifier
                    .padding(top = 0.dp)
                    .background(wpdsColors.appBarBg),
        ) {
            MyTextFieldWithButton(
                askThePostUIState,
                askThePostUIEvent,
                onTextSubmitted = onTextSubmitted,
                onFocused = onFocused
            )
            if (isPrivateModeEnabled) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                        .clickable {
                            askThePostUIEvent.invoke(
                                AskThePostEvent.IsPrivateModeRequested(
                                    true
                                )
                            )
                        }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_disable_private_mode),
                        contentDescription = "Private Chat Icon",
                        tint = wpdsColors.gray0
                    )
                    Text(
                        text = stringResource(R.string.anonymous_chat_on),
                        fontWeight = FontWeight.Bold,
                        color = wpdsColors.gray0,
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(com.wapo.view.R.font.franklinitcstd_bold)),
                    )
                }
            }
        }
    }
}

@Composable
fun ScrollableComponent(
    mainAskThePostUIState: MainAskThePostUIState,
    askThePostUIEvent: (AskThePostEvent) -> Unit,
    onContextQuestionClicked: (Question) -> Unit,
    handleScroll: Boolean,
    imePadding: Dp = 0.dp,
    scrollableAreaHeight: Dp,
    scrollState: ScrollState,
    fragmentManager: FragmentManager,
    isInArticles: Boolean
) {
    val askThePostUIState = mainAskThePostUIState.askThePostUIState
    val isStreamingLoading =
        askThePostUIState.isStreamingResponseLoading
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val continuingConversation =
        askThePostUIState.continuingConversation
    val conversationHistory = askThePostUIState.conversationHistory
    val questions = mainAskThePostUIState.articleQuestions

    // State to measure the content that appears *after* the last question.
    var responseContentHeight by remember { mutableStateOf(0.dp) }
    var lastQuestionY by remember { mutableStateOf(0f) }
    val questionCount = conversationHistory.count { it is ConversationItem.QuestionItem }

    // TODO(AWA-12516): Remove once the backend fixes the SSE issue with cached responses.
    val lastQuestionIndex = conversationHistory.indexOfLast { it is ConversationItem.QuestionItem }
    val latestResponseText = if (lastQuestionIndex != -1) {
        conversationHistory
            .subList(lastQuestionIndex + 1, conversationHistory.size)
            .filterIsInstance<ConversationItem.ResponseItem>()
            .lastOrNull()?.text ?: ""
    } else ""

    // Streaming text animation: reveal 4–5 characters at a time so the response
    // appears to build progressively rather than jumping in large chunks.
    var displayedStreamingText by remember { mutableStateOf("") }

    LaunchedEffect(questionCount) {
        displayedStreamingText = ""
    }

    // Always animate towards the latest text. We intentionally do NOT gate on
    // isStreamingLoading because every REPLY event sets it to false before
    // updating the text, creating a race where the animation would never run.
    LaunchedEffect(latestResponseText) {
        if (latestResponseText.isEmpty()) {
            displayedStreamingText = ""
            return@LaunchedEffect
        }
        // Skip animation when replaying a historical/continued conversation
        if (continuingConversation) {
            displayedStreamingText = latestResponseText
            return@LaunchedEffect
        }
        while (displayedStreamingText.length < latestResponseText.length) {
            val nextLength = minOf(displayedStreamingText.length + (4..5).random(), latestResponseText.length)
            displayedStreamingText = latestResponseText.substring(0, nextLength)
            delay(30L)
        }
    }

    // This trigger runs after a new question is added.
    LaunchedEffect(questionCount, continuingConversation) {
        if (questionCount == 0) return@LaunchedEffect
        if (continuingConversation) {
            scrollState.scrollTo(0)
        }
        // A small, pragmatic delay ensures Compose has finished its layout pass
        // and measured the question's position before scrolling.
        delay(500)

        scope.launch {
            scrollState.animateScrollTo(
                lastQuestionY.toInt(), animationSpec = tween(
                    durationMillis = 700,
                    easing = LinearEasing
                )
            )
            askThePostUIEvent.invoke(AskThePostEvent.OnDoneLoadingFullConversation)
        }
    }

    Column(
        modifier = if (handleScroll) Modifier.verticalScroll(scrollState)
            .padding(horizontal = 8.dp) else Modifier.padding(8.dp)
    ) {
        if (lastQuestionIndex != -1) {
            val mainConversation = conversationHistory.subList(0, lastQuestionIndex + 1)
            mainConversation.forEach { item ->
                when (item) {
                    is ConversationItem.QuestionItem -> {
                        Box(
                            // This modifier's only job is to measure the question's position.
                            modifier = Modifier.onGloballyPositioned { layoutCoordinates ->
                                lastQuestionY = layoutCoordinates.positionInParent().y
                            }
                        ) {
                            QuestionAsked(item.text)
                        }
                    }

                    is ConversationItem.ResponseItem -> {
                        val citations = buildCitations(conversationHistory, item.turnId)
                        FixedOrderContentView(
                            askThePostUIState,
                            askThePostUIEvent,
                            listOf(
                                PostAnswerUIItem.TextItem(
                                    TextSubtype.BODY,
                                    item.text,
                                    MimeType.PLAIN,
                                    null,
                                    null,
                                    null,
                                    citations = citations
                                )
                            ),
                            {
                                askThePostUIEvent.invoke(AskThePostEvent.UserEvent(it))
                            },
                            Modifier,
                            {
                                askThePostUIEvent.invoke(AskThePostEvent.PassagesClicked)
                            },
                            { askThePostUIEvent.invoke(AskThePostEvent.SetPassageData(it)) },
                            fragmentManager
                        )
                    }

                    is ConversationItem.Sources -> {
                        BottomFooterIcons(
                            askThePostUIState,
                            askThePostUIEvent,
                            conversationHistory,
                            item.turnId,
                            {
                                askThePostUIEvent.invoke(
                                    AskThePostEvent.InitiatePostAnswersFeedback(
                                        it.first, it.second,
                                        it.third
                                    )
                                )
                            },
                            fragmentManager,
                            isInArticles
                        )
                    }

                    is ConversationItem.ErrorItem -> {
                        TextItem(
                            askThePostUIEvent,
                            Modifier.padding(8.dp),
                            PostAnswerUIItem.TextItem(
                                TextSubtype.BODY,
                                content = item.message,
                                mimeType = MimeType.PLAIN,
                                null,
                                null
                            ),
                            {},
                            {
                                askThePostUIEvent.invoke(AskThePostEvent.UserEvent(it))
                            },
                            true
                        )
                    }

                    else -> {}
                }
            }

            // Render the latest response in a separate, measurable Column ---
            val latestResponseItems =
                conversationHistory.subList(lastQuestionIndex + 1, conversationHistory.size)
            Column(
                modifier = Modifier.onGloballyPositioned {
                    // Measure the height of all new content as it appears.
                    responseContentHeight = with(density) { it.size.height.toDp() }
                }
            ) {
                latestResponseItems.forEach { item ->
                    when (item) {
                        is ConversationItem.ResponseItem -> {
                            val citations = buildCitations(conversationHistory, item.turnId)
                            val textToDisplay = if (displayedStreamingText.length < item.text.length) displayedStreamingText else item.text
                            FixedOrderContentView(
                                askThePostUIState,
                                askThePostUIEvent,
                                listOf(
                                    PostAnswerUIItem.TextItem(
                                        TextSubtype.BODY,
                                        textToDisplay,
                                        MimeType.PLAIN,
                                        null,
                                        null,
                                        null,
                                        citations = citations
                                    )
                                ),
                                {
                                    askThePostUIEvent.invoke(AskThePostEvent.UserEvent(it))
                                },
                                Modifier,
                                {
                                    askThePostUIEvent.invoke(AskThePostEvent.PassagesClicked)
                                },
                                {
                                    askThePostUIEvent.invoke(AskThePostEvent.SetPassageData(it))
                                },
                                fragmentManager
                            )
                        }

                        is ConversationItem.Sources -> {
                            BottomFooterIcons(
                                askThePostUIState,
                                askThePostUIEvent,
                                conversationHistory,
                                item.turnId,
                                {
                                    askThePostUIEvent.invoke(
                                        AskThePostEvent.InitiatePostAnswersFeedback(
                                            it.first, it.second,
                                            it.third
                                        )
                                    )
                                },
                                fragmentManager,
                                isInArticles
                            )
                        }

                        is ConversationItem.ErrorItem -> {
                            TextItem(
                                askThePostUIEvent,
                                Modifier.padding(8.dp),
                                PostAnswerUIItem.TextItem(
                                    TextSubtype.BODY,
                                    content = item.message,
                                    mimeType = MimeType.PLAIN,
                                    null,
                                    null
                                ),
                                {},
                                {},
                                true
                            )
                        }

                        else -> {}
                    }
                }

                if (isStreamingLoading) {
                    val infiniteTransition = rememberInfiniteTransition()
                    val isVisible by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                        exit = fadeOut(animationSpec = tween(durationMillis = 300))
                    ) {
                        TextItem(
                            askThePostUIEvent,
                            Modifier.padding(8.dp).alpha(isVisible),
                            PostAnswerUIItem.TextItem(
                                TextSubtype.BODY,
                                content = stringResource(com.wapo.view.R.string.ai_overview_loading),
                                mimeType = MimeType.PLAIN,
                                null,
                                null
                            ),
                            {},
                            {},
                            true
                        )
                    }
                }
            }

            val minHeight = imePadding + 120.dp
            // The Dynamic Spacer ---
            // Its height is the remaining space in the viewport. It shrinks as the response grows.
            val spacerHeight =
                (scrollableAreaHeight - responseContentHeight).coerceAtLeast(minHeight)
            Spacer(modifier = Modifier.height(spacerHeight))

        } else {
            // Display ContextItem(s)
            if (questions.isNotEmpty()) {
                questions.forEachIndexed { index, question ->
                    ContextItem(
                        question = question,
                        onClick = {
                            onContextQuestionClicked(question)
                        },
                    )
                    if (index < questions.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = wpdsColors.gray400)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }
}

private fun buildCitations(
    conversationHistory: List<ConversationItem>,
    turnId: String
): List<Citation> {
    return conversationHistory
        .filterIsInstance<ConversationItem.Sources>()
        .lastOrNull { it.turnId == turnId }
        ?.articleSources
        ?.map { it ->
            val baseUrl = "https://www.washingtonpost.com"
            val fullUrl = it.canonicalUrl?.let { canonical ->
                if (canonical.startsWith("/")) {
                    baseUrl + canonical
                } else {
                    "$baseUrl/$canonical"
                }
            } ?: baseUrl
            Citation(
                turnId,
                Source(
                    url = fullUrl,
                    publishDate = it.publishDate.toDateLong(),
                    content = it.headline,
                    passages = it.text,
                    citationId = it.citationId
                )
            )
        } ?: emptyList()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyTextFieldWithButton(
    askThePostUIState: AskThePostUIState,
    askThePostUIEvent: (AskThePostEvent) -> Unit,
    onTextSubmitted: (String) -> Unit,
    onFocused: () -> Unit
) {
    var textState by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val isKeyBoardVisible = WindowInsets.isImeVisible
    var isFirstRun by remember { mutableStateOf(true) }
    val isConnectionClosed =
        askThePostUIState.connectionClosed
    val textColor = colorResource(com.washingtonpost.android.recirculation.R.color.gray0)
    val keyboardController = LocalSoftwareKeyboardController.current
    val isPrivateModeEnabled = askThePostUIState.isPrivateModeEnabled
    val conversationHistory =
        askThePostUIState.conversationHistory

    LaunchedEffect(key1 = isKeyBoardVisible) {
        if (isFirstRun && !isKeyBoardVisible) {
            isFirstRun = false
        } else {
            if (!isKeyBoardVisible) {
                focusManager.clearFocus()
            }
        }
    }

    MaterialTheme {
        val showShareMessage =
            askThePostUIState.shareUrlState is ShareUrlState.ShareProcessed && askThePostUIState.showRegisterInHistoryView
        AnimatedVisibility(
            visible = showShareMessage,
            enter = fadeIn() + slideInHorizontally(),
            exit = fadeOut() + slideOutHorizontally()
        ) {
            Column {
                WpTheme.Text.WapoText(
                    text = stringResource(R.string.ask_the_post_share_message),
                    textSize = 16
                )

                WpTheme.Button.CtaButton(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                    textModifier = Modifier,
                    text = stringResource(R.string.ask_the_post_register_cta),
                    textSize = 16,
                    useElevation = false,
                    onClick = {
                        askThePostUIEvent.invoke(
                            AskThePostEvent.CreateAccountOrSignIn(
                                Measurement.ASK_THE_POST_DEEPLINK,
                                PostIterableBannerEvent.ContinueShareConvo
                            )
                        )
                    }
                )

            }
        }
        AnimatedVisibility(
            visible = !showShareMessage,
            enter = fadeIn() + slideInHorizontally(),
            exit = fadeOut() + slideOutHorizontally()
        ) {
            TextField(
                value = textState,
                textStyle = TextStyle(
                    fontFamily = FontFamily(Font(com.wapo.view.R.font.franklinitcstd_light)),
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Start,
                    color = wpdsColors.gray0
                ),
                onValueChange = { textState = it },
                placeholder = {
                    Text(
                        if (conversationHistory.isEmpty()) stringResource(
                            com.wapo.view.R.string.atp_text_placeholder
                        ) else stringResource(com.wapo.view.R.string.atp_text_followup_placeholder),
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontFamily = FontFamily(Font(com.wapo.view.R.font.franklinitcstd_light)),
                        color = wpdsColors.gray100
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(wpdsColors.appBarBg)
                    .padding(5.dp)
                    .onFocusChanged {
                        if (it.isFocused) {
                            onFocused.invoke()
                            isFocused = true
                        } else {
                            isFocused = false
                        }
                    },
                colors =
                    TextFieldDefaults.textFieldColors(
                        textColor = textColor,
                        placeholderColor = colorResource(com.wpds.wpds.R.color.gray80),
                        cursorColor = textColor,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        backgroundColor = Color.Transparent,
                    ),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                keyboardActions =
                    KeyboardActions(
                        onSend = {
                            if (textState.isNotEmpty() && textState.isNotBlank() && isConnectionClosed) {
                                onTextSubmitted(textState)
                                textState = ""
                                keyboardController?.hide()
                            }
                        },
                        onDone = {
                            if (textState.isNotEmpty() && textState.isNotBlank() && isConnectionClosed) {
                                onTextSubmitted(textState)
                                textState = ""
                                keyboardController?.hide()
                            }
                        }
                    ),
                trailingIcon = {
                    val buttonState = if (Utils.isAmazonBuild() || isPrivateModeEnabled) {
                        if (textState.isEmpty()) {
                            TextFieldButtonState.SUBMIT_TEXT_DISABLED
                        } else {
                            TextFieldButtonState.SUBMIT_TEXT_ENABLED
                        }
                    } else {
                        if (textState.isEmpty() && !isFocused && isConnectionClosed) {
                            TextFieldButtonState.TALK
                        } else if (textState.isEmpty() && isFocused) {
                            TextFieldButtonState.SUBMIT_TEXT_DISABLED
                        } else {
                            TextFieldButtonState.SUBMIT_TEXT_ENABLED
                        }
                    }

                    var canShowPulse by remember { mutableStateOf(false) }
                    val context = LocalContext.current
                    LaunchedEffect(key1 = true) {
                        delay(2000)
                        canShowPulse = !PrefUtils.getTalkToThePostPulseShown(context)
                        PrefUtils.setTalkToThePostPulseShown(context)
                    }
                    IconButton(
                        modifier = Modifier.then(
                            if (canShowPulse && buttonState == TextFieldButtonState.TALK) {
                                Modifier.doublePulseEffect()
                            } else {
                                Modifier
                            }
                        ),
                        onClick = {
                            if (buttonState == TextFieldButtonState.SUBMIT_TEXT_ENABLED && isConnectionClosed) {
                                onTextSubmitted(textState)
                                textState = ""
                                keyboardController?.hide()
                            } else if (buttonState == TextFieldButtonState.TALK) {
                                askThePostUIEvent.invoke(AskThePostEvent.TalkToThePostOpen)
                            }
                        }
                    ) {
                        VectorWithGreyCircleBackground(isConnectionClosed, buttonState)
                        val buttonActive =
                            buttonState == TextFieldButtonState.TALK || buttonState == TextFieldButtonState.SUBMIT_TEXT_ENABLED
                        val iconTint = if (isSystemInDarkTheme()) {
                            if (buttonActive) Color.Black else wpdsColors.gray200
                        } else {
                            if (buttonActive) Color.White else wpdsColors.gray200
                        }
                        Icon(
                            painter = painterResource(
                                when (buttonState) {
                                    TextFieldButtonState.SUBMIT_TEXT_DISABLED, TextFieldButtonState.SUBMIT_TEXT_ENABLED -> {
                                        com.wapo.view.R.drawable.arrow_up
                                    }

                                    TextFieldButtonState.TALK -> {
                                        com.wpds.wpds.R.drawable.soundwave
                                    }
                                }
                            ),
                            contentDescription = "Send",
                            modifier = Modifier
                                .size(16.dp),
                            tint = iconTint,
                        )
                    }
                },
            )
        }

    }
    Spacer(modifier = Modifier.height(16.dp))
}


@Composable
fun VectorWithGreyCircleBackground(isConnectionClosed: Boolean, buttonState: TextFieldButtonState) {
    MaterialTheme {
        val baseCircleColor =
            if (isSystemInDarkTheme()) colorResource(com.wpds.wpds.R.color.gray700) else Color.LightGray.copy(
                alpha = 0.7f
            )
        val finalCircleColor = if (
            buttonState == TextFieldButtonState.SUBMIT_TEXT_ENABLED ||
            buttonState == TextFieldButtonState.TALK
        ) {
            if (isSystemInDarkTheme()) Color.White else wpdsColorsDark.gray700
        } else {
            baseCircleColor
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
        ) {
            if (!isConnectionClosed) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.LightGray,
                    strokeWidth = 2.dp
                )
            } else {
                Canvas(
                    modifier = Modifier.size(32.dp),
                    onDraw = {
                        val circleRadius = size.minDimension / 2
                        val center = Offset(size.width / 2, size.height / 2)
                        drawCircle(
                            color = finalCircleColor,
                            radius = circleRadius,
                            center = center,
                        )
                    },
                )
            }
        }
    }
}

@Composable
fun ContextItem(
    question: Question,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = painterResource(com.wpds.wpds.R.drawable.ai_icon),
            contentDescription = "Ai Icon",
            tint = colorResource(com.wapo.text.R.color.atp_underline),
            modifier = Modifier.padding(top = 0.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            if (question.label?.isNotBlank() == true) {
                Text(
                    text = question.label,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    lineHeight = 17.5.sp,
                    color = wpdsColors.gray0,
                    fontFamily =
                        FontFamily(
                            Font(com.wapo.view.R.font.franklinitcstd_bold),
                        ),
                    letterSpacing = 0.em
                )
            }
            question.text?.let {
                Text(
                    text = it,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    color = wpdsColors.gray60,
                    fontFamily =
                        FontFamily(
                            Font(com.wapo.view.R.font.franklinitcstd_light),
                        ),
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = false
                        )
                    ),
                    letterSpacing = 0.em
                )
            }
        }
    }
}

@Composable
fun QuestionAsked(currentText: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Spacer(modifier = Modifier.width(40.dp))

        Text(
            currentText,
            color = wpdsColors.gray0,
            modifier =
                Modifier
                    .padding(8.dp)
                    .background(
                        colorResource(com.wpds.wpds.R.color.alpha500),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
                    .widthIn(max = 300.dp),
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontFamily =
                FontFamily(
                    Font(com.wapo.view.R.font.franklinitcstd_light),
                ),
            letterSpacing = 0.em
        )
    }

    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
fun BottomFooterIcons(
    askThePostUIState: AskThePostUIState,
    askThePostUIEvent: (AskThePostEvent) -> Unit,
    conversationHistory: List<ConversationItem>,
    turnId: String,
    userEvent: (Triple<String, String, PostAnswersFeedbackReaction>) -> Unit,
    fragmentManager: FragmentManager?,
    isInArticles: Boolean = false
) {
    val context = LocalContext.current
    var isCopied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sourcesForArticle =
        conversationHistory.find { it is ConversationItem.Sources && it.turnId == turnId }
    val shareUrlState = askThePostUIState.shareUrlState
    val isPrivateModeEnabled = askThePostUIState.isPrivateModeEnabled
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = {
                    if (fragmentManager != null && sourcesForArticle is ConversationItem.Sources) {
                        val carouselItems =
                            sourcesForArticle.articleSources.mapIndexed { index, articleSource ->
                                CarouselUIItem(
                                    id = index,
                                    url = "https://www.washingtonpost.com${articleSource.canonicalUrl}",
                                    publishDateMillis = articleSource.publishDate?.toDateLong()
                                        ?: 0L,
                                    content = articleSource.headline ?: "",
                                    imageUrl = articleSource.imageUrl ?: "",
                                    passages = listOf(articleSource.text ?: "")
                                )
                            }
                        askThePostUIEvent.invoke(AskThePostEvent.ShowSourceSheetWith(carouselItems))
                        Measurement.trackOpenedSources(turnId)
                        SourceBottomSheet(SourceBottomSheetCaller.ASK).show(
                            fragmentManager,
                            "SourceBottomSheetTag"
                        )
                    }
                },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = wpdsColors.onSurface,
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp),
                border = BorderStroke(1.dp, wpdsColors.gray300),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_sources),
                    contentDescription = "Sources Icon",
                    modifier = Modifier.size(20.dp),
                    tint = wpdsColors.gray0
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Sources", fontSize = 16.sp, fontFamily = FontFamily(
                        Font(com.wapo.view.R.font.franklinitcstd_light),
                    ),
                    color = wpdsColors.gray0
                )
            }
            IconButton(
                onClick = {
                    val responseToCopy =
                        conversationHistory.find { it is ConversationItem.ResponseItem && it.turnId == turnId }

                    val textToCopy =
                        (responseToCopy as ConversationItem.ResponseItem).text

                    val clipboardManager =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

                    // Create a ClipData object with the response text
                    val clipData = ClipData.newPlainText("response text", textToCopy)


                    clipboardManager.setPrimaryClip(clipData)
                    scope.launch {
                        isCopied = true
                        delay(3000L)
                        isCopied = false
                    }
                    Measurement.trackCopyResponse(turnId)
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
            ) {
                Icon(
                    painter = painterResource(id = if (isCopied) R.drawable.icon_save_checkmark else R.drawable.ic_copy),
                    contentDescription = "Copy Text",
                    modifier = Modifier.size(20.dp),
                    tint = wpdsColors.gray80
                )
            }

            if (conversationHistory.last() is ConversationItem.Sources && (conversationHistory.last() as ConversationItem.Sources).turnId == turnId) {
                IconButton(onClick = {
                    userEvent(
                        Triple(
                            "",
                            turnId,
                            PostAnswersFeedbackReaction.YES
                        )
                    )
                }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_thumbs_up_unfilled_16),
                        contentDescription = "Thumbs Up",
                        modifier = Modifier.size(20.dp),
                        tint = wpdsColors.gray80
                    )
                }
                IconButton(onClick = {
                    userEvent(
                        Triple(
                            "",
                            turnId,
                            PostAnswersFeedbackReaction.NO
                        )
                    )
                }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_thumbs_down_unfilled_16),
                        contentDescription = "Thumbs Down",
                        modifier = Modifier.size(20.dp),
                        tint = wpdsColors.gray80
                    )
                }
            }
            if (!isPrivateModeEnabled && !isInArticles && askThePostUIState.shareUrlState !is ShareUrlState.ShareProcessed && askThePostUIState.isUserLoggedIn) {
                IconButton(onClick = {
                    if (!PaywallService.getInstance().isWpUserLoggedIn) {
                        askThePostUIEvent.invoke(
                            AskThePostEvent.ShowShareRegisterModal(
                                ShareType.TURN
                            )
                        )
                    } else {
                        askThePostUIEvent.invoke(AskThePostEvent.Share(true))
                    }
                }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_atp_forward),
                        contentDescription = "Share",
                        modifier = Modifier.size(20.dp),
                        tint = wpdsColors.gray80
                    )
                }
            }
        }

        if (shareUrlState is ShareUrlState.ShareProcessed && askThePostUIState.isUserLoggedIn) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(R.string.ask_the_post_share_message),
                fontSize = 12.sp,
                color = wpdsColors.gray100,
                fontFamily = FontFamily(Font(com.wapo.view.R.font.franklinitcstd_light))
            )
        }
    }
}

@Composable
fun calculateKeyboardOverlap(defaultNavPadding: Dp = 56.dp): Dp {
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val navBottom = WindowInsets.navigationBars.getBottom(density)

    return with(density) {
        val imeDp = imeBottom.toDp()
        val navDp = navBottom.toDp()
        if (imeBottom > 0) {
            (imeDp - navDp).coerceAtLeast(0.dp)
        } else {
            defaultNavPadding
        }
    }
}

enum class TextFieldButtonState {
    TALK,
    SUBMIT_TEXT_DISABLED,
    SUBMIT_TEXT_ENABLED
}
