package com.wapo.flagship.features.ask.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.constraintlayout.compose.ConstraintLayout
import com.wapo.flagship.features.ask.fragments.ScrollableComponent
import com.wapo.flagship.features.ask.fragments.TextFieldComposable
import com.wapo.flagship.features.ask.fragments.calculateKeyboardOverlap
import com.wapo.flagship.features.ask.models.AskThePostEvent
import com.wapo.flagship.features.ask.models.MainAskThePostUIState
import com.wapo.flagship.features.ask.models.PostIterableBannerEvent
import com.wapo.flagship.features.audio.models.AudioPlaybackState
import com.wapo.flagship.features.search2.events.UserEvent
import com.wapo.flagship.features.search2.ui.Passage
import com.wapo.flagship.features.search2.ui.SelectedPassageInfo
import com.wapo.flagship.util.tracking.Measurement.ASK_THE_POST_HISTORY_SAVE
import com.wapo.flagship.util.tracking.Measurement.ASK_THE_POST_HISTORY_SHARE
import com.wapo.flagship.views.RegisterView
import com.washingtonpost.android.R
import com.wpds.theme.wpdsColors

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun AskResponse(
    mainAskThePostUIState: MainAskThePostUIState,
    askThePostUiEvent: (AskThePostEvent) -> Unit,
    fragmentManager: androidx.fragment.app.FragmentManager
) {
    val askThePostUIState = mainAskThePostUIState.askThePostUIState
    val showPassage = askThePostUIState.showPassage
    var passageInfo by remember { mutableStateOf<SelectedPassageInfo?>(null) }
    val scrollState = rememberScrollState()
    val playbackState = mainAskThePostUIState.audioPlaybackState
    val disclaimerGradient = Brush.verticalGradient(
        colors = listOf(
            wpdsColors.appBarBg,
            wpdsColors.atpFade
        )
    )

    if (showPassage != true) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box {
                ConstraintLayout(
                    modifier = Modifier
                        .width(580.dp)
                        .padding(top = 5.dp)
                        .align(Alignment.Center)
                        .background(wpdsColors.appBarBg)
                ) {
                    val (content, inputContainer) = createRefs()
                    val bottomMargin =
                        when (playbackState is AudioPlaybackState.Playing || playbackState == AudioPlaybackState.Paused) {
                            true -> 124.dp
                            false -> 126.dp
                        }

                    val bottomOffset = calculateKeyboardOverlap()

                    Column(
                        modifier = Modifier
                            .padding(bottom = bottomMargin)
                            .constrainAs(content) {
                                top.linkTo(parent.top)
                                start.linkTo(parent.start)
                                end.linkTo(parent.end)
                            }

                    ) {

                        if (askThePostUIState.conversationHistory.size > 1 && askThePostUIState.showBanner) {
                            val msg = askThePostUIState.askThePostMessage
                            LaunchedEffect(msg?.attributionInfo?.messageId) {
                                msg?.let {
                                    askThePostUiEvent.invoke(
                                        AskThePostEvent.AskThePostBannerSeen(
                                            it
                                        )
                                    )
                                }
                            }
                            AskThePostIterableBanner(askThePostUIState, askThePostUiEvent)
                        }

                        BoxWithConstraints(modifier = Modifier.weight(1f)) {
                            ScrollableComponent(
                                mainAskThePostUIState,
                                askThePostUiEvent,
                                {},
                                true,
                                bottomOffset,
                                scrollableAreaHeight = this.maxHeight,
                                scrollState,
                                fragmentManager,
                                false
                            )
                        }
                    }
                    val margin = when (playbackState) {
                        is AudioPlaybackState.Playing, AudioPlaybackState.Paused, is AudioPlaybackState.None -> 62.dp
                        else -> 0.dp
                    }

                    val offsetMargin = if (bottomOffset > 56.dp) 0.dp else margin
                    Box(
                        modifier = Modifier
                            .width(580.dp)
                            .background(wpdsColors.appBarBg)
                            .constrainAs(inputContainer) {
                                start.linkTo(parent.start)
                                end.linkTo(parent.end)
                                bottom.linkTo(parent.bottom, margin = offsetMargin)
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .width(580.dp)
                                .background(disclaimerGradient)
                        ) {
                            TextFieldComposable(
                                modifier = Modifier
                                    .width(580.dp),
                                askThePostUIState = askThePostUIState,
                                askThePostUIEvent = askThePostUiEvent,
                                onTextSubmitted = {
                                    askThePostUiEvent.invoke(AskThePostEvent.TextSubmitted(it.trim()))
                                },
                                onFocused = {}
                            )
                            Box(
                                modifier = Modifier
                                    .padding(bottom = bottomOffset)
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(R.string.ask_the_post_disclaimer),
                                    fontSize = 12.sp,
                                    lineHeight = 15.sp,
                                    color = wpdsColors.gray100,
                                    fontFamily = FontFamily(Font(com.wapo.view.R.font.franklinitcstd_light)),
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        .padding(bottom = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (askThePostUIState.showRegisterSaveChatModal) {
                Dialog(
                    onDismissRequest = {
                        askThePostUiEvent.invoke(AskThePostEvent.DismissRegisterNewChatModal(false))
                    },
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false,
                        decorFitsSystemWindows = false
                    ),
                    content = {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            Box(
                                modifier = Modifier.background(wpdsColors.surface)
                            ) {
                                RegisterView(
                                    header = stringResource(R.string.ask_the_post_save_chat_headline),
                                    subtitle = stringResource(R.string.ask_the_post_save_chat_description),
                                    ctaText = stringResource(R.string.ask_the_post_register_new_chat_cta),
                                    ctaSubTextPrefix = stringResource(R.string.ask_the_post_register_cta_subtitle_prefix),
                                    ctaSubTextSuffix = stringResource(R.string.ask_the_post_register_cta_subtitle_suffix),
                                    secondaryCtaText = stringResource(R.string.ask_the_post_register_cta),
                                    showCloseButton = true,
                                    primaryButtonClicked = {
                                        askThePostUiEvent.invoke(
                                            AskThePostEvent.DismissRegisterNewChatModal(
                                                true
                                            )
                                        )
                                    },
                                    dismissModal = {
                                        askThePostUiEvent.invoke(
                                            AskThePostEvent.DismissRegisterNewChatModal(
                                                false
                                            )
                                        )
                                    },
                                    secondaryButtonClicked = {
                                        askThePostUiEvent.invoke(
                                            AskThePostEvent.CreateAccountOrSignIn(
                                                ASK_THE_POST_HISTORY_SAVE,
                                                PostIterableBannerEvent.SaveConversation
                                            )
                                        )
                                    },
                                    linkClicked = {
                                        askThePostUiEvent.invoke(
                                            AskThePostEvent.CreateAccountOrSignIn(
                                                ASK_THE_POST_HISTORY_SAVE,
                                                PostIterableBannerEvent.SaveConversation
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    })
            }

            if (askThePostUIState.showRegisterShareConvoModal != null) {
                val isShareConvo = askThePostUIState.showRegisterShareConvoModal
                Dialog(
                    onDismissRequest = {
                        askThePostUiEvent.invoke(AskThePostEvent.ShowShareRegisterModal(null))
                    }, properties = DialogProperties(
                        usePlatformDefaultWidth = false,
                        decorFitsSystemWindows = false
                    ),
                    content = {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            val descriptionResource =
                                if (isShareConvo == com.wapo.flagship.features.ask.models.ShareType.CONVO) {
                                    R.string.ask_the_post_share_modal_description
                                } else {
                                    R.string.ask_the_post_share_modal_response_description
                                }

                            val headlineResource =
                                if (isShareConvo == com.wapo.flagship.features.ask.models.ShareType.CONVO) {
                                    R.string.ask_the_post_share_modal_chat_headline
                                } else {
                                    R.string.ask_the_post_share_modal_response_headline
                                }
                            Box(modifier = Modifier.background(wpdsColors.surface)) {
                                RegisterView(
                                    header = stringResource(headlineResource),
                                    subtitle = stringResource(descriptionResource),
                                    ctaText = stringResource(R.string.ask_the_post_register_cta),
                                    ctaSubTextPrefix = stringResource(R.string.ask_the_post_register_cta_subtitle_prefix),
                                    ctaSubTextSuffix = stringResource(R.string.ask_the_post_register_cta_subtitle_suffix),
                                    showCloseButton = true,
                                    primaryButtonClicked = {
                                        askThePostUiEvent.invoke(
                                            AskThePostEvent.CreateAccountOrSignIn(
                                                ASK_THE_POST_HISTORY_SHARE,
                                                PostIterableBannerEvent.ShareConversation(
                                                    isShareConvo == com.wapo.flagship.features.ask.models.ShareType.TURN
                                                )
                                            )
                                        )
                                    },
                                    dismissModal = {
                                        askThePostUiEvent.invoke(
                                            AskThePostEvent.ShowShareRegisterModal(
                                                null
                                            )
                                        )
                                    },
                                    linkClicked = {
                                        askThePostUiEvent.invoke(
                                            AskThePostEvent.CreateAccountOrSignIn(
                                                ASK_THE_POST_HISTORY_SHARE,
                                                PostIterableBannerEvent.ShareConversation(
                                                    isShareConvo == com.wapo.flagship.features.ask.models.ShareType.TURN
                                                )
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    })
            }
        }
    } else {
        val bottomOffset = calculateKeyboardOverlap()
        val margin = when (playbackState) {
            is AudioPlaybackState.Playing, AudioPlaybackState.Paused,
            is AudioPlaybackState.None -> 55.dp

            else -> 0.dp
        }
        val offsetMargin = if (bottomOffset > 56.dp) 0.dp else margin

        val url = passageInfo?.url
        val position = passageInfo?.position
        if (url != null && position != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isSystemInDarkTheme()) wpdsColors.gray400Static else wpdsColors.gray700Static)
            ) {

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(600.dp)
                        .padding(bottom = offsetMargin)
                ) {
                    Passage(
                        passageInfo,
                        false,
                        { askThePostUiEvent.invoke(AskThePostEvent.UserEvent(UserEvent.SeePassagesClicked())) },
                        onUserEvent = {
                            askThePostUiEvent.invoke(
                                AskThePostEvent.PostAnswersCarouselItemClick(
                                    UserEvent.PostAnswersCarouselItemClick(
                                        url,
                                        passageInfo?.passages,
                                        position,
                                    ), passageInfo?.position.toString()
                                )
                            )
                        },
                        {
                            askThePostUiEvent.invoke(AskThePostEvent.HideOrShowPassage(false))
                        }
                    )
                }

                IconButton(
                    onClick = {
                        askThePostUiEvent.invoke(AskThePostEvent.HideOrShowPassage(false))
                    },
                    modifier = Modifier.padding(
                        top = 4.dp,
                        start = 0.dp,
                        end = 4.dp,
                        bottom = 4.dp
                    ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_back_arrow),
                        contentDescription = "Close sheet",
                        tint = wpdsColors.gray0,
                    )
                }
            }
        }
    }
}
