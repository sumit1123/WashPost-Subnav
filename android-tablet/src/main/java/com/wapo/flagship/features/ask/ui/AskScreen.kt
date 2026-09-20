package com.wapo.flagship.features.ask.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.wapo.flagship.features.ask.fragments.TextFieldComposable
import com.wapo.flagship.features.ask.fragments.calculateKeyboardOverlap
import com.wapo.flagship.features.ask.models.AskThePostEvent
import com.wapo.flagship.features.ask.models.MainAskThePostUIState
import com.wapo.flagship.features.ask.viewmodels.ShareUrlState
import com.wapo.flagship.features.audio.models.AudioPlaybackState
import com.washingtonpost.android.R
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.wpdsColors

@Composable
fun AskScreen(
    mainAskThePostUIState: MainAskThePostUIState,
    askThePostUIEvent: (AskThePostEvent) -> Unit,
    fragmentManager: androidx.fragment.app.FragmentManager
) {
    val questionsList = mainAskThePostUIState.askThePostQuestionsUiState.questions
    val askThePostUIState = mainAskThePostUIState.askThePostUIState
    val historyList = askThePostUIState.historyList
    val showShareExpired = askThePostUIState.shareUrlState
    val showResponse = askThePostUIState.showResponse
    val playbackState = mainAskThePostUIState.audioPlaybackState

    AnimatedVisibility(visible = questionsList != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(wpdsColors.appBarBg)
        ) {
            askThePostUIEvent.invoke(AskThePostEvent.DismissSplash)

            if (!showResponse) {
                ConstraintLayout(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(wpdsColors.appBarBg)
                ) {
                    val (contentArea, searchBar) = createRefs()
                    val bottomOffset = calculateKeyboardOverlap()

                    // --- DYNAMIC CONTENT AREA ---
                    Box(
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier
                            .width(580.dp)
                            .constrainAs(contentArea) {
                                top.linkTo(parent.top)
                                bottom.linkTo(searchBar.top)
                                start.linkTo(parent.start)
                                end.linkTo(parent.end)
                                height = Dimension.fillToConstraints
                            }
                    ) {
                        QuestionsContent(mainAskThePostUIState, askThePostUIEvent)
                    }

                    // --- SHARED SEARCH BAR & DISCLAIMER ---
                    val margin = when (playbackState) {
                        is AudioPlaybackState.Playing, AudioPlaybackState.Paused -> 55.dp
                        else -> 0.dp
                    }
                    val offsetMargin = if (bottomOffset > 56.dp) 0.dp else margin
                    val disclaimerGradient = Brush.verticalGradient(
                        colors = listOf(
                            wpdsColors.appBarBg,
                            wpdsColors.atpFade
                        )
                    )

                    Box(
                        modifier = Modifier
                            .wrapContentHeight()
                            .background(wpdsColors.appBarBg)
                            .padding(bottom = bottomOffset)
                            .constrainAs(searchBar) {
                                bottom.linkTo(parent.bottom, margin = offsetMargin)
                                start.linkTo(parent.start)
                                end.linkTo(parent.end)
                            }
                            .background(disclaimerGradient)
                    ) {
                        Column {
                            TextFieldComposable(
                                modifier = Modifier.width(580.dp),
                                askThePostUIState = askThePostUIState,
                                askThePostUIEvent = askThePostUIEvent,
                                onTextSubmitted = {
                                    askThePostUIEvent.invoke(AskThePostEvent.TextSubmitted(it.trim()))
                                },
                                onFocused = {}
                            )
                            Box(modifier = Modifier
                                .width(580.dp)) {
                                Text(
                                    text = stringResource(R.string.ask_the_post_disclaimer),
                                    fontSize = 12.sp,
                                    lineHeight = 15.sp,
                                    color = wpdsColors.gray100,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // --- SHARED RESPONSE VIEW ---
            AnimatedVisibility(
                visible = showResponse,
                enter = fadeIn() + slideInHorizontally(),
                exit = fadeOut() + slideOutHorizontally(),
            ) {
                if (historyList != null || (askThePostUIState.showResponse && askThePostUIState.conversationHistory.isNotEmpty())) {
                    AskResponse(mainAskThePostUIState, askThePostUIEvent, fragmentManager)
                }
            }

            // --- SHARED ALERT ---
            Alert(
                stringResource(R.string.shared_link_expired_title),
                stringResource(R.string.shared_link_expired_description),
                stringResource(com.wapo.flagship.features.audio.R.string.ok_button),
                onConfirm = { askThePostUIEvent.invoke(AskThePostEvent.ResetShare) },
                onDismiss = { askThePostUIEvent.invoke(AskThePostEvent.ResetShare) },
                showShareExpired == ShareUrlState.ShareExpired,
                showCancel = false
            )
        }
    }
}

@Composable
private fun QuestionsContent(
    state: MainAskThePostUIState,
    event: (AskThePostEvent) -> Unit
) {
    val questions = state.askThePostQuestionsUiState.questions
    val timeOfDay = state.askThePostQuestionsUiState.timeOfDay

    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val viewportHeight = maxHeight

        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // This container acts as the "Elastic" spacer.
            // It tries to be half the screen height, but because it's in a
            // scrollable Column, it will be pushed up naturally by the pills.
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // We set a height that targets the middle, but we use
                    // wrapContentHeight for the internal text so it can move.
                    .heightIn(min = 0.dp, max = viewportHeight / 2),
                contentAlignment = Alignment.BottomStart
            ) {
                Column(modifier = Modifier.padding(bottom = 92.dp)) {
                    Text(
                        text = "${timeOfDay?.title}",
                        color = wpdsColors.gray0,
                        fontSize = 18.sp,
                        fontFamily = FontFamily(Font(com.wpds.wpds.R.font.franklinitcstd_bold))
                    )
                    Text(
                        text = timeOfDay?.subTitle ?: "",
                        fontSize = 18.sp,
                        color = wpdsColors.gray80,
                        fontFamily = FontFamily(Font(com.washingtonpost.android.save.R.font.franklin_std_light))
                    )
                }
            }

            // Question Pills Area
            questions?.forEach { question ->
                question.text?.let {
                    QuestionPill(
                        text = question.text.toAnnotatedString(),
                        onClick = {
                            event.invoke(
                                AskThePostEvent.QuestionClicked(
                                    question.text.toAnnotatedString().toString(),
                                    ""
                                )
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            // Bottom spacing to ensure the last pill is fully visible
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun QuestionPill(
    text: AnnotatedString,
    onClick: () -> Unit
) {
    AndroidClassicTheme {
        val gradientBrush = Brush.linearGradient(
            colors = listOf(
                wpdsColors.aiIcon,
                wpdsColors.pillBorder
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                onClick = onClick,
                shape = RoundedCornerShape(30.dp),
                border = BorderStroke(width = 1.dp, brush = gradientBrush),
                color = wpdsColors.askPill
            ) {
                Row(
                    modifier = Modifier.padding(start = 15.dp, top = 11.dp, bottom = 11.dp, end = 23.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = com.wpds.wpds.R.drawable.ai_icon),
                        contentDescription = null,
                        tint = wpdsColors.aiIcon,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = text,
                        lineHeight = 20.sp,
                        fontSize = 16.sp,
                        color = wpdsColors.gray0,
                        fontFamily = FontFamily(Font(com.wpds.wpds.R.font.franklinitcstd_light))
                    )
                }
            }
        }
    }
}