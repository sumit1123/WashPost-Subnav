package com.wapo.flagship.features.ask.ui

import androidx.compose.foundation.BorderStroke
import android.icu.text.BreakIterator
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.wapo.flagship.features.ask.viewmodels.TalkToThePostViewModel
import com.wapo.flagship.features.ask.viewmodels.TalkToThePostViewModel.TalkUiState
import com.wapo.flagship.features.search2.events.UserEvent
import com.washingtonpost.android.R
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.wpdsColors
import com.wpds.utils.fadingEdge
import com.wpds.utils.getTextShimmerEffectBrush
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import java.text.StringCharacterIterator

@Composable
fun TalkToThePostView(
    uiStateFlow: MutableStateFlow<TalkUiState>,
    captionsEnabledFlow: MutableStateFlow<Boolean>,
    previousResponseTextFlow: MutableStateFlow<String>,
    responseTextFlow: MutableStateFlow<String>,
    orbRadiusFlow: MutableStateFlow<TalkToThePostViewModel.OrbRadius>,
    captionsIndexFlow: MutableStateFlow<Int>,
    deviceVolumeFlow: MutableStateFlow<Int>,
    thinkingTextFlow: MutableStateFlow<String>,
    eventTrigger: (UserEvent) -> Unit,
) {
    val uiState by uiStateFlow.collectAsState()
    val captionsEnabled by captionsEnabledFlow.collectAsState()
    val previousResponseText by previousResponseTextFlow.collectAsState()
    val responseText by responseTextFlow.collectAsState()
    val orbRadius by orbRadiusFlow.collectAsState()
    val captionsIndex by captionsIndexFlow.collectAsState()
    val deviceVolume by deviceVolumeFlow.collectAsState()
    val thinkingText by thinkingTextFlow.collectAsState()

    Card(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(wpdsColors.wallPrimaryBg)) {
            ConstraintLayout(modifier = Modifier.fillMaxSize()) {
                val (orb, captions, stateText, buttons) = createRefs()
                Column(modifier = Modifier.constrainAs(orb) { top.linkTo(parent.top) }) {
                    Spacer(
                        modifier = Modifier.height(
                            if (uiState == TalkUiState.RESPONDING_CAPTIONS) {
                                56.dp
                            } else {
                                210.dp
                            }
                        )
                    )
                    Orb(uiState, orbRadius)
                }

                if (uiState == TalkUiState.RESPONDING_CAPTIONS) {
                    Column(
                        modifier = Modifier
                            .constrainAs(captions) {
                                top.linkTo(orb.bottom)
                                bottom.linkTo(buttons.top)
                                height = Dimension.fillToConstraints
                            },
                    ) {
                        Spacer(modifier = Modifier.height(34.dp))
                        Captions(previousResponseText, responseText, captionsIndex, eventTrigger)
                    }
                } else {
                    Box(modifier = Modifier.constrainAs(stateText) { bottom.linkTo(buttons.top) }) {
                        Row {
                            Spacer(modifier = Modifier.weight(1f))
                            StateText(uiState, deviceVolume, thinkingText)
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                Column(modifier = Modifier.constrainAs(buttons) { bottom.linkTo(parent.bottom) }) {
                    Buttons(uiState, captionsEnabled, eventTrigger)
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun Orb(uiState: TalkUiState, orbRadius: TalkToThePostViewModel.OrbRadius) {
    val color: Color
    when (uiState) {
        // There are three variations on the listening state
        TalkUiState.FIRST_LISTEN_IN_SESSION,
        TalkUiState.RESPONSE_PAUSED_LISTENING,
        TalkUiState.LISTENING -> {
            color = wpdsColors.atpPurpleStatic
        }
        TalkUiState.LISTENING_ON_HOLD,
        TalkUiState.THINKING -> {
            color = wpdsColors.gray100
        }
        TalkUiState.ACTIVE_IN_ANDROID_AUTO -> {
            color = wpdsColors.atpPurpleStatic
        }
        TalkUiState.ONBOARDING,
        TalkUiState.RESPONDING,
        TalkUiState.RESPONDING_CAPTIONS,
        TalkUiState.RECOGNITION_ERROR,
        TalkUiState.RECOGNITION_ERROR_MAX,
        TalkUiState.API_ERROR -> {
            color = wpdsColors.atpPinkStatic
        }
    }

    val minRadius: Int
    val medRadius: Int
    val maxRadius: Int
    if (uiState == TalkUiState.RESPONDING_CAPTIONS) {
        minRadius = 30
        medRadius = 40
        maxRadius = 50
    } else {
        minRadius = 120
        medRadius = 130
        maxRadius = 140
    }

    val targetRadius = when (orbRadius) {
        TalkToThePostViewModel.OrbRadius.MIN_RADIUS -> {
            minRadius
        }
        TalkToThePostViewModel.OrbRadius.MED_RADIUS -> {
            medRadius
        }
        TalkToThePostViewModel.OrbRadius.MAX_RADIUS -> {
            maxRadius
        }
    }


    val audioReactiveRadius by animateFloatAsState(
        targetValue = targetRadius.toFloat()
    )

    Row {
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .blur(radius = 20.dp, BlurredEdgeTreatment.Unbounded)
                .height((maxRadius * 2).dp)
                .width((maxRadius * 2).dp)
        ) {
            Canvas(
                modifier = Modifier
                    .height((maxRadius * 2).dp)
                    .width((maxRadius * 2).dp)
            ) {
                drawCircle(
                    color = color,
                    radius = when (uiState) {
                        TalkUiState.FIRST_LISTEN_IN_SESSION,
                        TalkUiState.LISTENING,
                        TalkUiState.RESPONSE_PAUSED_LISTENING,
                        TalkUiState.RESPONDING,
                        TalkUiState.RESPONDING_CAPTIONS,
                        TalkUiState.RECOGNITION_ERROR,
                        TalkUiState.RECOGNITION_ERROR_MAX,
                        TalkUiState.API_ERROR,
                        TalkUiState.ACTIVE_IN_ANDROID_AUTO -> {
                            audioReactiveRadius.dp.toPx()
                        }
                        else -> {
                            medRadius.toFloat().dp.toPx()
                        }
                    },
                    alpha = 1f
                )
            }

        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun StateText(uiState: TalkUiState, deviceVolume: Int, thinkingText: String) {
    if (uiState == TalkUiState.ACTIVE_IN_ANDROID_AUTO) {
        Text(
            text = stringResource(R.string.ask_sam_active_in_android_auto),
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            fontFamily = FontFamily(Font(com.wpds.wpds.R.font.franklinitcstd_light)),
            color = wpdsColors.gray100,
        )
    } else if (deviceVolume == 0) {
        val gradientBrush = Brush.linearGradient(
            colors = listOf(wpdsColors.atpPurpleStatic, wpdsColors.atpPinkStatic)
        )

        val text = buildAnnotatedString {
            withStyle(style = SpanStyle(brush = gradientBrush)) {
                append("Your volume is off")
            }
        }

        Text(
            text = text,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            fontFamily = FontFamily(Font(com.wpds.wpds.R.font.franklinitcstd_light))
        )

    } else if (uiState == TalkUiState.THINKING) {
        val textStyle = TextStyle(
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            fontFamily = FontFamily(Font(com.wpds.wpds.R.font.franklinitcstd_light))
        )

        val gradientBrush = getTextShimmerEffectBrush(
            thinkingText,
            textStyle,
            listOf(wpdsColors.gray80, wpdsColors.gray100, wpdsColors.gray200),
            1500
        )

        val text = buildAnnotatedString {
            withStyle(style = SpanStyle(brush = gradientBrush)) {
                append(thinkingText)
            }
        }

        Text(
            text = text,
            style = textStyle
        )
    } else {
        val text = when (uiState) {
            TalkUiState.ONBOARDING,
            TalkUiState.FIRST_LISTEN_IN_SESSION -> {
                "Go ahead, ask anything"
            }
            TalkUiState.LISTENING_ON_HOLD -> {
                "On hold. Tap pause again to resume"
            }
            TalkUiState.RESPONDING,
            TalkUiState.RECOGNITION_ERROR,
            TalkUiState.RECOGNITION_ERROR_MAX,
            TalkUiState.API_ERROR -> {
                "Responding..."
            }
            TalkUiState.RESPONDING_CAPTIONS -> {
                ""
            }
            TalkUiState.RESPONSE_PAUSED_LISTENING -> {
                "Response paused. Listening..."
            }
            TalkUiState.LISTENING -> {
                "Listening..."
            }
            TalkUiState.ACTIVE_IN_ANDROID_AUTO -> ""
            else -> {
                ""
            }
        }
        Text(
            text = text,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            fontFamily = FontFamily(Font(com.wpds.wpds.R.font.franklinitcstd_light)),
            color = wpdsColors.gray100
        )
    }
}

@Composable
fun Captions(previousText: String, newText: String, currentIndex: Int, eventTrigger: (UserEvent) -> Unit) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .padding(start = 30.dp, end = 30.dp)
            .verticalScroll(scrollState)
            .fadingEdge(scrollState)
    ) {
        Text(
            text = previousText.replace("\\n", "\n"),
            fontSize = 24.sp,
            lineHeight = 38.4.sp,
            textAlign = TextAlign.Start,
            fontFamily = FontFamily(Font(com.washingtonpost.android.recirculation.R.font.georgia_regular)),
            color = wpdsColors.gray0,
        )

        val sanitizedNewText = newText.replace("\\n", "\n")
        var substringText by remember { mutableStateOf("") }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { // Version N is required for BreakIterator.getWordInstance()
            val breakIterator = remember { BreakIterator.getWordInstance() }
            LaunchedEffect(sanitizedNewText) {
                breakIterator.text = StringCharacterIterator(sanitizedNewText)
                var nextIndex = breakIterator.following(currentIndex)
                while (nextIndex != BreakIterator.DONE) {
                    substringText = sanitizedNewText.subSequence(0, nextIndex).toString()
                    eventTrigger.invoke(UserEvent.TalkToThePostUpdateCaptionsIndex(nextIndex))
                    nextIndex = breakIterator.next()
                    delay(100L)
                    try {
                        scrollState.animateScrollTo(scrollState.maxValue)
                    } catch(e: CancellationException) {
                        // No-op, we want to ignore the exception and and allow the launched effect to continue
                    }
                }
                eventTrigger.invoke(UserEvent.TalkToThePostCaptionsDoneRendering())
            }
        } else {
            substringText = sanitizedNewText
            eventTrigger.invoke(UserEvent.TalkToThePostCaptionsDoneRendering())
        }

        Text(
            text = substringText,
            fontSize = 24.sp,
            lineHeight = 38.4.sp,
            textAlign = TextAlign.Start,
            fontFamily = FontFamily(Font(com.washingtonpost.android.recirculation.R.font.georgia_regular)),
            color = wpdsColors.gray0,
            modifier = Modifier.padding(bottom = 40.dp)
        )
    }
}

@Composable
fun Buttons(talkUiState: TalkUiState, captionsEnabled: Boolean, eventTrigger: (UserEvent) -> Unit) {
    if (talkUiState == TalkUiState.ACTIVE_IN_ANDROID_AUTO) {
        Row(
            modifier = Modifier.padding(top = 33.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.weight(1f))
            OutlinedIconButton(
                onClick = { eventTrigger.invoke(UserEvent.TalkToThePostClose()) },
                colors = IconButtonColors(
                    contentColor = Color.Transparent,
                    containerColor = wpdsColors.secondary,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = wpdsColors.secondary,
                ),
                border = BorderStroke(1.dp, wpdsColors.primary),
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    painter = painterResource(com.wpds.wpds.R.drawable.close),
                    contentDescription = "close button",
                    tint = wpdsColors.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }
        return
    }

    val buttonColor: Color
    val iconColor: Color
    val playPauseIcon: Int
    when (talkUiState) {
        TalkUiState.FIRST_LISTEN_IN_SESSION -> {
            buttonColor = wpdsColors.secondary
            iconColor = wpdsColors.primary
            playPauseIcon = com.wpds.wpds.R.drawable.pause
        }
        TalkUiState.LISTENING_ON_HOLD -> {
            buttonColor = wpdsColors.primary
            iconColor = wpdsColors.secondary
            playPauseIcon = com.wpds.wpds.R.drawable.pause
        }
        TalkUiState.THINKING -> {
            buttonColor = wpdsColors.secondary
            iconColor = wpdsColors.primary
            playPauseIcon = com.wpds.wpds.R.drawable.pause
        }
        TalkUiState.ONBOARDING,
        TalkUiState.RESPONDING,
        TalkUiState.RESPONDING_CAPTIONS,
        TalkUiState.RECOGNITION_ERROR,
        TalkUiState.RECOGNITION_ERROR_MAX,
        TalkUiState.API_ERROR -> {
            buttonColor = wpdsColors.secondary
            iconColor = wpdsColors.primary
            playPauseIcon = com.wpds.wpds.R.drawable.pause
        }
        TalkUiState.RESPONSE_PAUSED_LISTENING -> {
            buttonColor = wpdsColors.primary
            iconColor = wpdsColors.secondary
            playPauseIcon = com.wpds.wpds.R.drawable.play
        }
        TalkUiState.LISTENING -> {
            buttonColor = wpdsColors.secondary
            iconColor = wpdsColors.primary
            playPauseIcon = com.wpds.wpds.R.drawable.pause
        }
        TalkUiState.ACTIVE_IN_ANDROID_AUTO -> return
    }
    Row(
        modifier = Modifier.padding(top = 33.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Context Menu Button & Actual Menu
        Box {
            var menuOpen by remember { mutableStateOf(false) }

            OutlinedIconButton(
                onClick = {
                    menuOpen = !menuOpen
                },
                colors = IconButtonColors(
                    contentColor = Color.Transparent,
                    containerColor = wpdsColors.secondary,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = wpdsColors.secondary
                ),
                border = BorderStroke(1.dp, wpdsColors.primary),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(com.wapo.flagship.features.aixp.R.drawable.ic_ellipsis),
                    contentDescription = "more options button",
                    tint = wpdsColors.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            DropdownMenu(
                expanded = menuOpen,
                containerColor = wpdsColors.secondary,
                onDismissRequest = { menuOpen = false }
            ) {
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            painter = painterResource(com.wpds.wpds.R.drawable.soundwave),
                            contentDescription = "soundwave icon",
                            tint = wpdsColors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    text = {
                        Text(
                            text = "Voice selection",
                            fontFamily = FontFamily(Font(com.wpds.wpds.R.font.franklinitcstd_light)),
                            fontSize = 16.sp,
                            color = wpdsColors.primary
                        )
                    },
                    onClick = {
                        eventTrigger.invoke(UserEvent.TalkToThePostOpenVoiceSelectionSheet())
                    },
                )
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            painter = painterResource(com.wpds.wpds.R.drawable.cc),
                            contentDescription = "closed caption icon",
                            tint = wpdsColors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    text = {
                        Text(
                            text = "Captions",
                            fontFamily = FontFamily(Font(com.wpds.wpds.R.font.franklinitcstd_light)),
                            fontSize = 16.sp,
                            color = wpdsColors.primary
                        )
                    },
                    trailingIcon = {
                        Switch(
                            checked = captionsEnabled,
                            onCheckedChange = { eventTrigger.invoke(UserEvent.TalkToThePostToggleCaptions()) },
                            enabled = true,
                            colors = getSwitchColors(),
                            modifier = Modifier.scale(0.75f)
                        )
                    },
                    onClick = { eventTrigger.invoke(UserEvent.TalkToThePostToggleCaptions()) },
                )
            }
        }

        Spacer(modifier = Modifier.width(24.dp))

        // Play/Pause Button
        OutlinedIconButton(
            onClick = {
                eventTrigger.invoke(UserEvent.TalkToThePostPlayPauseTapped())
            },
            colors = IconButtonColors(
                contentColor = Color.Transparent,
                containerColor = buttonColor,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = buttonColor
            ),
            border = BorderStroke(1.dp, wpdsColors.primary),
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                painter = painterResource(playPauseIcon),
                contentDescription = "play/pause button",
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        // Close Button
        OutlinedIconButton(
            onClick = {
                eventTrigger.invoke(UserEvent.TalkToThePostClose())
            },
            colors = IconButtonColors(
                contentColor = Color.Transparent,
                containerColor = wpdsColors.secondary,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = wpdsColors.secondary
            ),
            border = BorderStroke(1.dp, wpdsColors.primary),
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                painter = painterResource(com.wpds.wpds.R.drawable.close),
                contentDescription = "close button",
                tint = wpdsColors.primary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun getSwitchColors(): SwitchColors {
    return SwitchColors(
        checkedThumbColor = wpdsColors.onPrimary,
        checkedTrackColor = wpdsColors.gray100,
        checkedBorderColor = wpdsColors.alpha400,
        checkedIconColor = Color.Unspecified,
        uncheckedThumbColor = wpdsColors.onPrimary,
        uncheckedTrackColor = wpdsColors.gray200,
        uncheckedBorderColor = wpdsColors.alpha400,
        uncheckedIconColor = Color.Unspecified,
        disabledCheckedThumbColor = Color.Unspecified,
        disabledCheckedTrackColor = Color.Unspecified,
        disabledCheckedBorderColor = Color.Unspecified,
        disabledCheckedIconColor = Color.Unspecified,
        disabledUncheckedThumbColor = Color.Unspecified,
        disabledUncheckedTrackColor = Color.Unspecified,
        disabledUncheckedBorderColor = Color.Unspecified,
        disabledUncheckedIconColor = Color.Unspecified
    )
}

@Composable
@Preview(device = "spec:width=411dp,height=891dp")
fun PreviewTalkToThePostBottomSheetView() {
    AndroidClassicTheme {
        Surface(
            color = Color.Transparent,
        ) {
            TalkToThePostView(
                uiStateFlow = MutableStateFlow(TalkUiState.THINKING),
                captionsEnabledFlow = MutableStateFlow(true),
                previousResponseTextFlow = MutableStateFlow(""),
                responseTextFlow = MutableStateFlow(
                    "Good question.\n\nThe president's authority to fire the director of the National " +
                            "Portrait Gallery is unclear. Top congressional Democrats have asserted " +
                            "that the president does not have legal authority for the firing. The " +
                            "Smithsonian Institution, which includes the National Portrait Gallery, " +
                            "is an independent institution and not a traditional government agency, " +
                            "and hiring and firing decisions have historically been handled by the " +
                            "Smithsonian's secretary.\nThe current secretary, Lonnie G. Bunch III, is " +
                            "expected to discuss the president's attempt to oust the director at a " +
                            "board meeting. It is unclear if the president has the power to fire the " +
                            "director, as the Smithsonian's programming is not under the purview of " +
                            "the executive branch [1], [2], [3]."
                ),
                orbRadiusFlow = MutableStateFlow(TalkToThePostViewModel.OrbRadius.MED_RADIUS),
                captionsIndexFlow = MutableStateFlow(260),
                deviceVolumeFlow = MutableStateFlow(1),
                thinkingTextFlow = MutableStateFlow("Finding the right information ..."),
                eventTrigger = {},
            )
        }
    }
}
