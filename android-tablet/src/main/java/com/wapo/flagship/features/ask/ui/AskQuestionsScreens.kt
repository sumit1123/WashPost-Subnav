// Copyright (c) 2024 The Washington Post. All rights reserved.
package com.wapo.flagship.features.ask.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wapo.flagship.features.ask.events.AskQuestionsClickEvent
import com.wapo.flagship.features.ask.models.AskThePostQuestionsUiState
import com.wapo.flagship.features.ask.viewmodels.AskQuestionsViewModel
import com.wapo.view.R
import com.wpds.theme.FranklinItcStandardFontFamily
import com.wpds.theme.wpdsColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskQuestionsContainer(
    askThePostQuestionsUiState: AskThePostQuestionsUiState,
    displayDisclaimer: Boolean,
    onClick: (AskQuestionsClickEvent) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var isBottomSheetVisible = rememberSaveable { mutableStateOf(false) }
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )

    val questions = askThePostQuestionsUiState.questions
    if (questions.isNullOrEmpty()) {
        Box(
            modifier =
                Modifier
                    .height(72.dp)
                    .fillMaxWidth(),
        ) {
            CircularProgressIndicator(
                modifier =
                    Modifier
                        .size(18.dp)
                        .wrapContentSize()
                        .align(Alignment.Center),
                color = wpdsColors.primary,
                strokeWidth = 2.dp,
            )
        }
        return
    }

    AskQuestions(askThePostQuestionsUiState, onClick)
    if (displayDisclaimer) {
        AskDisclaimer { _ ->
            scope.launch {
                isBottomSheetVisible.value = true
                sheetState.expand()
            }
        }
        AskLearnMoreSheet(isBottomSheetVisible, sheetState, onClick, onDismiss = {
            scope.launch {
                isBottomSheetVisible.value = false
                sheetState.hide()
            }
        })
    }
}

@Composable
fun AskQuestions(
    askThePostQuestionsUiState: AskThePostQuestionsUiState,
    onClick: (AskQuestionsClickEvent) -> Unit,
) {
    val questions = askThePostQuestionsUiState.questions
    val items =
        questions?.mapNotNull {
            if (it.uuid != null && it.text != null) {
                Pair(
                    it.uuid,
                    it.text,
                )
            } else {
                null
            }
        } ?: emptyList()
    if (items.isEmpty()) return

    val list1State = rememberLazyListState()
    val list2State = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val scrollState =
        rememberScrollableState { delta ->
            scope.launch {
                list1State.scrollBy(-delta)
                list2State.scrollBy(-delta)
            }
            delta
        }

    Column(
        modifier =
            Modifier
                .scrollable(
                    scrollState,
                    Orientation.Horizontal,
                    flingBehavior = ScrollableDefaults.flingBehavior(),
                ) // Workaround for Gradient
                .graphicsLayer { alpha = 0.99F }
                .drawWithContent {
                    val isInitialState =
                        list1State.firstVisibleItemIndex == 0 && list1State.firstVisibleItemScrollOffset == 0
                    val colors =
                        listOf(
                            if (isInitialState) Color.White else Color.Transparent,
                            Color.White, Color.White, Color.White, Color.White, Color.White,
                            Color.White, Color.White, Color.Transparent
                        )
                    drawContent()
                    drawRect(
                        brush = Brush.horizontalGradient(colors),
                        blendMode = BlendMode.DstIn,
                    )
                },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val rowSize = (items.size - items.size % 2) / 2

        LazyRow(
            state = list1State,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(start = 0.dp),
            userScrollEnabled = false,
        ) {
            items(rowSize.coerceAtMost(items.size), itemContent = {
                val index = it % rowSize
                QuestionItem(items[index].first, items[index].second, onClick)
            })
        }
        if (items.size > 1) {
            LazyRow(
                state = list2State,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(start = 0.dp),
                userScrollEnabled = false,
            ) {
                items(rowSize.coerceAtMost(items.size), itemContent = {
                    val index = it % rowSize
                    QuestionItem(
                        items[rowSize + index].first,
                        items[rowSize + index].second,
                        onClick,
                    )
                })
            }
        }
    }
}

@Composable
fun QuestionItem(
    id: String,
    text: String,
    onClick: ((AskQuestionsClickEvent) -> Unit)? = null,
) {
    Box(
        modifier =
            Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            wpdsColors.questionItemStartColor.copy(alpha = 0.7f),
                            wpdsColors.questionItemEndColor.copy(alpha = 0.7f)
                        ),
                    ),
                    shape = RoundedCornerShape(9999.dp)
                )
                .clickable {
                    onClick?.invoke(AskQuestionsClickEvent.QuestionClickEvent(id, text))
                },
    ) {
        Text(
            text = text,
            modifier =
                Modifier
                    .padding(vertical = 8.dp, horizontal = 12.dp),
            color = wpdsColors.primary,
            style =
                TextStyle(
                    fontFamily = FranklinItcStandardFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                ),
        )
    }
}

@Composable
fun AskDisclaimer(
    onClick: ((AskQuestionsClickEvent) -> Unit)? = null,
) {
    val helpIconContentId = "help_icon_inlineContent"
    val aiIconSize = 16.sp
    val helpIconSize = 12.sp
    val annotatedString =
        buildAnnotatedString {
            append("\u202F")
            withStyle(
                style =
                    SpanStyle(
                        fontFamily = FranklinItcStandardFontFamily,
                        fontSize = 12.sp,
                        color = wpdsColors.gray80,
                        letterSpacing = 0.sp,
                    ),
            ) {
                append(AskQuestionsViewModel.DISCLAIMER)
            }
            append("\u202F")
            appendInlineContent(helpIconContentId, "[help_icon]")
        }
    val inlineContent =
        mapOf(
            Pair(
                helpIconContentId,
                InlineTextContent(
                    Placeholder(
                        width = helpIconSize,
                        height = helpIconSize,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextBottom,
                    ),
                ) {
                    Box {
                        Icon(
                            modifier =
                                Modifier
                                    .padding(horizontal = with(LocalDensity.current) { 0.sp.toDp() })
                                    .size(with(LocalDensity.current) { helpIconSize.toDp() }),
                            painter = painterResource(com.wpds.wpds.R.drawable.help),
                            tint = wpdsColors.gray80,
                            contentDescription = null,
                        )
                    }
                },
            ),
        )
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onClick?.invoke(AskQuestionsClickEvent.LearnMoreClickEvent)
                },
    ) {
        Text(
            text = annotatedString,
            inlineContent = inlineContent,
            modifier =
                Modifier
                    .padding(top = 12.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskLearnMoreSheet(
    isBottomSheetVisible: MutableState<Boolean>,
    sheetState: SheetState,
    onClick: ((AskQuestionsClickEvent) -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    if (!isBottomSheetVisible.value) return
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = wpdsColors.gray600,
        contentColor = wpdsColors.primary,
    ) {
        AskLearnMoreSheetContent(onClick = onClick)
    }
}

@Composable
fun AskLearnMoreSheetContent(
    isModalBottomSheet: Boolean = true,
    onClick: ((AskQuestionsClickEvent) -> Unit)? = null,
) {
    Text(
        text = AskQuestionsViewModel.LEARN_MORE_TITLE,
        modifier =
            Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
        color = wpdsColors.primary,
        style =
            TextStyle(
                fontFamily = FranklinItcStandardFontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            ),
    )
    ClickableText(
        text = getLearnMoreSheetDescription(),
        modifier =
            Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = if (isModalBottomSheet) 64.dp else 0.dp),
        style =
            TextStyle(
                color = wpdsColors.primary,
                fontFamily = FranklinItcStandardFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 17.5.sp,
            ),
        onClick = { offset ->
            getLearnMoreSheetDescription().getStringAnnotations(offset, offset).firstOrNull()?.let {
                onClick?.invoke(AskQuestionsClickEvent.DeepLinkClickEvent(it.item))
            }
        },
    )
}

fun getLearnMoreSheetDescription(): AnnotatedString =
    buildAnnotatedString {
        val linkStyle =
            SpanStyle(
                fontFamily = FranklinItcStandardFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                textDecoration = TextDecoration.Underline,
            )
        append(
            "In response to your question, this tool searches articles published by our newsroom since 2016 and ranks the results based on relevancy. We then use a large language model to write a response to answer your question. This tool may not always understand your question or be able to provide an answer. In some cases we will provide a pre-written answer. If you have questions about this tool, please see our ",
        )
        val faqAnnotatedString =
            buildAnnotatedString {
                val faqText = "FAQ"
                val faqLength = faqText.length
                pushStyle(linkStyle)
                append(faqText)
                addStringAnnotation(
                    tag = faqText,
                    annotation = "https://www.washingtonpost.com/technology/2024/11/07/faq-ask-the-post-ai/",
                    start = 0,
                    end = faqLength,
                )
            }
        append(faqAnnotatedString)
        append(" and ")
        val policyAnnotatedString =
            buildAnnotatedString {
                val policyText = "AI policy"
                val policyTextLength = policyText.length
                pushStyle(linkStyle)
                append(policyText)
                addStringAnnotation(
                    tag = policyText,
                    annotation = "https://www.washingtonpost.com/policies-and-standards/#ai",
                    start = 0,
                    end = policyTextLength,
                )
            }
        append(policyAnnotatedString)
        append(" or ")
        val supportAnnotatedString =
            buildAnnotatedString {
                val supportText = "contact customer care"
                val supportTextLength = supportText.length
                pushStyle(linkStyle)
                append(supportText)
                addStringAnnotation(
                    tag = supportText,
                    annotation = "https://helpcenter.washingtonpost.com/hc/en-us/",
                    start = 0,
                    end = supportTextLength,
                )
            }
        append(supportAnnotatedString)
        append(".")
    }
