package com.wapo.flagship.features.search2.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.FragmentManager
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.wapo.flagship.features.ask.models.AskThePostEvent
import com.wapo.flagship.features.ask.models.AskThePostUIState
import com.wapo.flagship.features.ask.ui.SourceBottomSheet
import com.wapo.flagship.features.ask.ui.SourceBottomSheetCaller
import com.wapo.flagship.features.search2.events.UserEvent
import com.wapo.flagship.features.search2.model.Citation
import com.wapo.flagship.features.search2.model.ParsedCitation
import com.wapo.flagship.features.search2.model.ParsedTextResult
import com.wapo.flagship.features.search2.state.PostAnswersUIState
import com.wapo.flagship.features.search2.viewmodel.PostAnswersFeedbackReaction
import com.wapo.flagship.features.search2.viewmodel.SearchViewModel.Companion.CITATION_PREFIX
import com.wapo.view.R
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.FranklinItcStandardFontFamily
import com.wpds.theme.wpdsColors
import com.wpds.utils.DevicePreviews
import com.wpds.utils.IconUtils
import com.wpds.utils.MarkdownText
import com.wpds.utils.shimmerEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class SelectedPassageInfo(
    val title: String?,
    val date: String?,
    val passages: List<String>?,
    val url: String?,
    val position: Int?,
)

@Composable
fun PostAnswersContainer(
    askThePostUIEvent: (AskThePostEvent) -> Unit,
    askThePostUIState: AskThePostUIState,
    postAnswersUIState: PostAnswersUIState,
    onUserEvent: (UserEvent) -> Unit,
    modifier: Modifier,
    fragmentManager: FragmentManager? = null,
    query: String? = null
) {
    Column(
        modifier = modifier,
    ) {
        when (postAnswersUIState) {
            is PostAnswersUIState.Error ->
                Error(
                    titleTextResId = R.string.ai_overview_error_title,
                    descriptionTextResId = R.string.ai_overview_error_description,
                    onUserEvent = onUserEvent,
                )

            is PostAnswersUIState.Loading ->
                Loading(
                    titleTextResId = null,
                    descriptionTextResId = R.string.ai_overview_loading_description,
                    query = query
                )

            is PostAnswersUIState.Success ->
                Success(
                    postAnswerUIItems = postAnswersUIState.data,
                    onUserEvent = onUserEvent,
                    askThePostUIEvent = askThePostUIEvent,
                    fragmentManager = fragmentManager
                )
        }
    }
}

@Composable
fun Error(
    modifier: Modifier = Modifier,
    titleTextResId: Int,
    descriptionTextResId: Int,
    onUserEvent: (UserEvent) -> Unit,
) {
    Column(modifier = modifier) {
        Title(titleTextResId = titleTextResId)
        Spacer(modifier = Modifier.height(8.dp))
        Description(descriptionTextResId = descriptionTextResId, onUserEvent = onUserEvent)
        ErrorContent()
    }
}

@Composable
fun ErrorContent(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .padding(top = 16.dp)
                .fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.ai_overview_error_content_title),
            style =
                TextStyle(
                    color = wpdsColors.gray0,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FranklinItcStandardFontFamily,
                ),
        )
    }
}

@Composable
fun Loading(
    modifier: Modifier = Modifier,
    titleTextResId: Int? = null,
    descriptionTextResId: Int,
    query: String? = null,
) {
    Column(
        modifier = modifier,
    ) {
        titleTextResId?.let {
            Title(titleTextResId = titleTextResId)
            Spacer(modifier = Modifier.height(4.dp))
        }
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
            Description(
                descriptionTextResId = descriptionTextResId,
                query = query,
                modifier = Modifier.alpha(isVisible)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextPlaceHolder()
    }
}

@Composable
fun TextPlaceHolder() {
    repeat(6) {
        Box(
            Modifier
                .height(12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .shimmerEffect(),
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun Description(
    modifier: Modifier = Modifier,
    descriptionTextResId: Int,
    query: String? = null,
    onUserEvent: ((UserEvent) -> Unit)? = null,
) {
    ClickableText(
        modifier = modifier,
        text = getDescriptionText(
            query ?: stringResource(descriptionTextResId),
            onUserEvent != null
        ),
        style =
            TextStyle(
                color = wpdsColors.gray80,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily(Font(R.font.franklinitcstd_light)),
            ),
        onClick = {
            onUserEvent?.invoke(
                UserEvent.PostAnswersInfoClick,
            )
        },
    )
}

@Composable
fun Title(
    modifier: Modifier = Modifier,
    titleTextResId: Int,
) {
    Row(modifier = modifier) {
        val textStyle =
            TextStyle(
                color = wpdsColors.onSecondary,
                fontSize = 14.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily(Font(R.font.franklinitcstd_bold)),
            )
        Text(
            text = stringResource(titleTextResId),
            style = textStyle,
        )
    }
}

@Composable
fun Success(
    askThePostUIEvent: (AskThePostEvent) -> Unit,
    modifier: Modifier = Modifier,
    postAnswerUIItems: List<PostAnswerUIItem>,
    onUserEvent: (UserEvent) -> Unit,
    fragmentManager: FragmentManager? = null
) {
    var isTextExpanded by rememberSaveable { mutableStateOf(false) }
    var lastCarousel by remember { mutableStateOf<PostAnswerUIItem.Carousel?>(null) }
    var feedbackItem by remember { mutableStateOf<PostAnswerUIItem.Feedback?>(null) }
    Column(modifier = modifier.padding(8.dp)) {
        postAnswerUIItems.forEachIndexed { index, it ->
            when (it) {
                is PostAnswerUIItem.Carousel -> lastCarousel = it
                is PostAnswerUIItem.Feedback -> feedbackItem = it
                is PostAnswerUIItem.TextItem ->
                    TextItem(
                        textItem = it,
                        onTextExpanded = { isTextExpanded = true },
                        onUserEvent = onUserEvent,
                        askThePostUIEvent = askThePostUIEvent,
                        fragmentManager = fragmentManager
                    )
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
        if (feedbackItem != null) {
            val responseText = postAnswerUIItems
                .filterIsInstance<PostAnswerUIItem.TextItem>()
                .firstOrNull { it.subtype == TextSubtype.BODY }
                ?.content

            BottomFooterIcons(
                text = responseText ?: "",
                postAnswerCarousel = lastCarousel,
                item = feedbackItem!!,
                askThePostUIEvent = askThePostUIEvent,
                fragmentManager = fragmentManager,
                onUserEvent = onUserEvent
            )
        }
    }
}

@Composable
fun Carousel(
    modifier: Modifier = Modifier,
    carousel: PostAnswerUIItem.Carousel,
    onUserEvent: (UserEvent) -> Unit,
    showPassage: () -> Unit,
    passageData: (SelectedPassageInfo) -> Unit,
    isSearch: Boolean = true,
) {
    carousel.subtype?.let {
        when (carousel.subtype) {
            CarouselSubtype.PASSAGES -> {
                LazyRow(modifier = modifier.fillMaxWidth()) {
                    itemsIndexed(carousel.items) { index, carouselItem ->
                        CarouselItem(
                            modifier = Modifier.padding(end = 8.dp),
                            carouselItem = carouselItem,
                            position = index,
                            onUserEvent = onUserEvent,
                            showPassage,
                            passageData,
                            isSearch,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VerticalCarousel(
    modifier: Modifier = Modifier,
    carousel: PostAnswerUIItem.Carousel,
    onUserEvent: (UserEvent) -> Unit,
    showPassage: () -> Unit,
    passageData: (SelectedPassageInfo) -> Unit,
    isSearch: Boolean = false,
) {
    carousel.subtype?.let {
        when (carousel.subtype) {
            CarouselSubtype.PASSAGES -> {
                LazyColumn(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(carousel.items) { index, carouselItem ->
                        VerticalCarouselItem(
                            modifier = Modifier,
                            carouselItem = carouselItem,
                            position = index,
                            onUserEvent = onUserEvent,
                            showPassage,
                            passageData,
                            isSearch,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun CarouselItem(
    modifier: Modifier,
    carouselItem: CarouselUIItem,
    position: Int,
    onUserEvent: (UserEvent) -> Unit,
    showPassage: () -> Unit,
    passageData: (SelectedPassageInfo) -> Unit,
    isSearch: Boolean,
) {
    Column(
        modifier =
            modifier
                .height(120.dp)
                .width(270.dp)
                .fillMaxHeight()
                .border(1.dp, wpdsColors.gray300, RoundedCornerShape(4.dp))
                .clickable {
                    if (!isSearch && carouselItem.passages?.isNotEmpty() == true) {
                        showPassage()
                        passageData(
                            SelectedPassageInfo(
                                carouselItem.content,
                                SimpleDateFormat(
                                    "MMMM dd, yyyy",
                                    Locale.US,
                                ).format(carouselItem.publishDateMillis),
                                carouselItem.passages,
                                carouselItem.url,
                                position,
                            ),
                        )
                    } else {
                        onUserEvent(
                            UserEvent.PostAnswersCarouselItemClick(
                                carouselItem.url,
                                carouselItem.passages,
                                position,
                            ),
                        )
                    }
                }
                .padding(8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = carouselItem.content,
                style =
                    TextStyle(
                        color = wpdsColors.gray40,
                        fontSize = 14.sp,
                        lineHeight = 17.5.sp,
                        fontFamily = FontFamily((Font(R.font.franklinitcstd_light))),
                    ),
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(10.dp))
            GlideImage(
                model = carouselItem.imageUrl,
                contentDescription = null,
                modifier =
                    Modifier
                        .height(64.dp)
                        .width(64.dp)
                        .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            CarouselItemFooter(carouselItem)
            Spacer(modifier = Modifier.weight(1f))
            if (!isSearch && carouselItem.passages?.isNotEmpty() == true) {
                Text(
                    text = "See passage",
                    style =
                        TextStyle(
                            color = wpdsColors.gray80,
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            fontFamily = FontFamily((Font(R.font.franklinitcstd_light))),
                        ),
                )
                Icon(
                    painter = painterResource(R.drawable.ic_keyboard_arrow_right_black_24dp),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = wpdsColors.gray80
                )
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun VerticalCarouselItem(
    modifier: Modifier,
    carouselItem: CarouselUIItem,
    position: Int,
    onUserEvent: (UserEvent) -> Unit,
    showPassage: () -> Unit,
    passageData: (SelectedPassageInfo) -> Unit,
    isSearch: Boolean,
) {
    AndroidClassicTheme {
        Row(
            modifier =
                modifier
                    .fillMaxWidth()
                    .border(1.dp, wpdsColors.gray300, RoundedCornerShape(4.dp))
                    .clickable {

                        if (!isSearch && carouselItem.passages?.isNotEmpty() == true) {
                            passageData(
                                SelectedPassageInfo(
                                    carouselItem.content,
                                    SimpleDateFormat(
                                        "MMMM dd, yyyy",
                                        Locale.US,
                                    ).format(carouselItem.publishDateMillis),
                                    carouselItem.passages,
                                    carouselItem.url,
                                    position,
                                ),
                            )
                            showPassage()
                        } else {
                            onUserEvent(
                                UserEvent.PostAnswersCarouselItemClick(
                                    carouselItem.url,
                                    carouselItem.passages,
                                    position,
                                ),
                            )
                        }
                    }
                    .padding(12.dp)
                    .height(IntrinsicSize.Min), // Ensure the Row is as tall as its tallest child
            verticalAlignment = Alignment.Top,
        ) {
            // Left Column for text content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Text(
                    text = "${position + 1}. ${carouselItem.content}",
                    style =
                        TextStyle(
                            color = wpdsColors.gray40,
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            fontFamily = FontFamily((Font(R.font.franklinitcstd_light))),
                            fontWeight = FontWeight.Normal
                        ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                CarouselItemFooter(carouselItem)
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Right Column for image and "See passage"
            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                // This inner Column groups the image and text together
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    GlideImage(
                        model = carouselItem.imageUrl,
                        contentDescription = null,
                        modifier =
                            Modifier
                                .size(width = 96.dp, height = 64.dp),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            if (!isSearch && carouselItem.passages?.isNotEmpty() == true) {
                                showPassage()
                                passageData(
                                    SelectedPassageInfo(
                                        carouselItem.content,
                                        SimpleDateFormat(
                                            "MMMM dd, yyyy",
                                            Locale.US,
                                        ).format(carouselItem.publishDateMillis),
                                        carouselItem.passages,
                                        carouselItem.url,
                                        position,
                                    ),
                                )
                            }
                        }
                    ) {
                        if (carouselItem.passages?.isNotEmpty() == true) {
                            Text(
                                text = "See passage",
                                style =
                                    TextStyle(
                                        color = wpdsColors.gray80,
                                        fontSize = 12.sp,
                                        lineHeight = 15.sp,
                                        fontFamily = FontFamily((Font(R.font.franklinitcstd_light))),
                                    ),
                            )
                            Icon(
                                painter = painterResource(R.drawable.ic_keyboard_arrow_right_black_24dp),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = wpdsColors.gray80
                            )
                        } else {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CarouselItemFooter(carouselItem: CarouselUIItem) {
    val datePattern = "MMMM dd, yyyy"
    Text(
        text =
            SimpleDateFormat(
                datePattern,
                Locale.US,
            ).format(carouselItem.publishDateMillis),
        style =
            TextStyle(
                color = wpdsColors.gray80,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontFamily = FontFamily((Font(R.font.franklinitcstd_light))),
            ),
    )
}

@Composable
fun TextItem(
    askThePostUIEvent: (AskThePostEvent) -> Unit,
    modifier: Modifier = Modifier,
    textItem: PostAnswerUIItem.TextItem,
    onTextExpanded: () -> Unit,
    onUserEvent: (UserEvent) -> Unit,
    fullResponse: Boolean = false,
    fragmentManager: FragmentManager? = null
) {
    Row(
        modifier =
            modifier.clickable(
                onClick = {
                    /* TODO uncomment in https://arcpublishing.atlassian.net/browse/AWA-10262
                    textItem.bottomSheetInfo?.let */
                    if (textItem.subtype == TextSubtype.DESCRIPTION) {
                        onUserEvent(
                            UserEvent.PostAnswersInfoClick,
                        )
                    }
                },
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ContentText(
            askThePostUIEvent,
            textItem,
            onTextExpanded,
            onUserEvent,
            fullResponse,
            fragmentManager
        )
        textItem.icon?.let { iconName ->
            IconUtils(LocalContext.current).getDrawableId(iconName)?.let { id ->
                Icon(
                    painter = painterResource(id),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .padding(start = 4.dp)
                            .align(Alignment.CenterVertically),
                    tint = getTextTypeColor(textItem.subtype),
                )
            }
        }
    }
}

@Composable
fun BottomFooterIcons(
    text: String,
    item: PostAnswerUIItem.Feedback,
    askThePostUIEvent: (AskThePostEvent) -> Unit,
    onUserEvent: (UserEvent) -> Unit,
    postAnswerCarousel: PostAnswerUIItem.Carousel?,
    fragmentManager: FragmentManager? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SourcesItem(
            askThePostUIEvent,
            postAnswerCarousel = postAnswerCarousel,
            fragmentManager = fragmentManager
        )
        CopyItem(text)
        FeedbackItem(
            item = item,
            onUserEvent = onUserEvent
        )
    }
}

@Composable
fun SourcesItem(
    askThePostUIEvent: (AskThePostEvent) -> Unit,
    postAnswerCarousel: PostAnswerUIItem.Carousel?,
    fragmentManager: FragmentManager? = null
) {
    OutlinedButton(
        onClick = {
            val carouselItems = postAnswerCarousel?.items
            if (!carouselItems.isNullOrEmpty() && fragmentManager != null) {
                askThePostUIEvent.invoke(AskThePostEvent.ShowSourceSheetWith(carouselItems))
                SourceBottomSheet(SourceBottomSheetCaller.SEARCH).show(
                    fragmentManager,
                    "SourceBottomSheetTag"
                )
            }
        },
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = wpdsColors.onSurface,
        ),
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 0.dp)
            .defaultMinSize(minHeight = 18.dp),
        border = BorderStroke(1.dp, wpdsColors.gray300),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_sources),
            contentDescription = "Sources Icon",
            modifier = Modifier.size(18.dp),
            tint = wpdsColors.gray0
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Sources", fontSize = 15.sp, fontFamily = FontFamily(
                Font(R.font.franklinitcstd_light),
            ),
            color = wpdsColors.gray0
        )
    }
}

@Composable
fun CopyItem(
    text: String
) {
    val context = LocalContext.current
    var isCopied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    IconButton(
        onClick = {
            val clipboardManager =
                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

            val plainText = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
            val clipData = ClipData.newPlainText("response text", plainText)
            clipboardManager.setPrimaryClip(clipData)
            scope.launch {
                isCopied = true
                delay(3000L)
                isCopied = false
            }
        },
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape),
    ) {
        Icon(
            painter = painterResource(id = if (isCopied) R.drawable.icon_save_checkmark else R.drawable.ic_copy),
            contentDescription = "Copy Text",
            modifier = Modifier.size(18.dp),
            tint = wpdsColors.gray80
        )
    }
}

@Composable
fun FeedbackItem(
    item: PostAnswerUIItem.Feedback,
    onUserEvent: (UserEvent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FeedbackButton(
                item = item,
                reaction = PostAnswersFeedbackReaction.YES,
                onUserEvent = onUserEvent,
            )
            Spacer(modifier = Modifier.width(15.dp))
            FeedbackButton(
                item = item,
                reaction = PostAnswersFeedbackReaction.NO,
                onUserEvent = onUserEvent,
            )
        }
    }
}

@Composable
fun FeedbackAnswerItem(
    item: PostAnswerUIItem.Feedback,
    onUserEvent: (UserEvent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.ai_overview_feedback_answer),
                textAlign = TextAlign.Center,
                style =
                    TextStyle(
                        color = wpdsColors.accessible,
                        fontSize = 14.sp,
                        lineHeight = 17.5.sp,
                        fontFamily = FontFamily((Font(R.font.franklinitcstd_light))),
                    ),
            )
            FeedbackButton(
                item = item,
                reaction = PostAnswersFeedbackReaction.YES,
                onUserEvent = onUserEvent,
            )
            FeedbackButton(
                item = item,
                reaction = PostAnswersFeedbackReaction.NO,
                onUserEvent = onUserEvent,
            )
        }
    }
}

@Composable
fun FeedbackButton(
    modifier: Modifier = Modifier,
    item: PostAnswerUIItem.Feedback,
    reaction: PostAnswersFeedbackReaction,
    onUserEvent: (UserEvent) -> Unit,
) {
    Row(
        modifier =
            modifier
                .clickable {
                    onUserEvent(
                        UserEvent.PostAnswersFeedbackItemClick(
                            item.endPointUrl,
                            item.responseId,
                            reaction,
                        ),
                    )
                },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = reaction.iconId),
            contentDescription = "",
            modifier = modifier.size(18.dp),
            tint = wpdsColors.primary,
        )
    }
}

@Composable
fun Passage(
    passageInfo: SelectedPassageInfo?,
    showBackButton: Boolean = true,
    showPassage: () -> Unit,
    onUserEvent: () -> Unit,
    backPressed: () -> Unit,
    closePressed: () -> Unit = {},
) {
    val title = passageInfo?.title
    val date = passageInfo?.date
    val passages = passageInfo?.passages
    val roundedCornerShape = if (!showBackButton) RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 0.dp
    ) else RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    Surface(
        modifier =
            Modifier
                .fillMaxSize(),
        shape = roundedCornerShape,
        color = wpdsColors.surface,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(if (isSystemInDarkTheme()) wpdsColors.gray400Static else wpdsColors.gray700Static),
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .padding(top = 64.dp), // Add top padding to account for the close button
            ) {
                item {
                    if (title != null) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.fillMaxWidth(),
                            color = if (isSystemInDarkTheme()) Color.White else Color.Black,
                            fontFamily =
                                FontFamily(
                                    Font(R.font.postoniwide_bold),
                                ),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                item {
                    if (date != null) {
                        Text(
                            text = date,
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            color = colorResource(com.wpds.wpds.R.color.gray80),
                            modifier = Modifier.fillMaxWidth(),
                            fontFamily =
                                FontFamily(
                                    Font(R.font.franklinitcstd_light),
                                ),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                //  Passages (dynamically added items)
                if (!passages.isNullOrEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                    items(passages.size) { index ->
                        val passageText = passages[index]
                        if (passages.size > 1) {
                            Text(
                                "Passage ${index + 1}",
                                color = if (isSystemInDarkTheme()) Color.White else Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                lineHeight = 20.sp,
                                fontFamily =
                                    FontFamily(
                                        Font(R.font.franklinitcstd_bold),
                                    ),
                            )
                        }
                        Text(
                            text = passageText,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            modifier = Modifier.padding(bottom = 8.dp),
                            color = if (isSystemInDarkTheme()) Color.White else Color.Black,
                            fontFamily =
                                FontFamily(
                                    Font(R.font.franklinitcstd_light),
                                ),
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { onUserEvent.invoke() },
                        shape = RoundedCornerShape(percent = 50),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = if (isSystemInDarkTheme()) wpdsColors.gray400Static else Color.White,
                                contentColor = if (isSystemInDarkTheme()) Color.White else Color.Black,
                            ),
                        border = BorderStroke(1.dp, Color.LightGray),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    ) {
                        Text(
                            "Read full article",
                            fontSize = 16.sp,
                            lineHeight = 16.sp,
                            fontFamily =
                                FontFamily(
                                    Font(R.font.franklinitcstd_bold),
                                ),
                        )
                    }
                }
            }

            // Close button (returns to previous composable
            if (showBackButton) {
                IconButton(
                    onClick = backPressed,
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 10.dp, top = 20.dp),
                ) {
                    Icon(
                        modifier = Modifier.graphicsLayer(rotationZ = 180f),
                        painter = painterResource(id = R.drawable.ic_keyboard_arrow_right_black_24dp),
                        contentDescription = "Close",
                        tint = if (isSystemInDarkTheme()) Color.White else Color.Black,
                    )
                }
            } else {
                IconButton(
                    onClick = closePressed,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 16.dp, top = 20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = wpdsColors.gray80
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryItem(
    title: String,
    date: String,
    loadConversation: () -> Unit,
    deleteConversation: () -> Unit,
    currentConversation: Boolean = false
) {
    Box(modifier = Modifier.background(if (currentConversation) wpdsColors.gray600 else wpdsColors.surface)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { loadConversation() }) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        color = wpdsColors.gray0
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = date,
                        fontSize = 14.sp,
                        color = wpdsColors.gray100
                    )
                }

                EllipsisMenuWithDelete(deleteConversation)
            }
        }
        Divider(
            color = wpdsColors.gray400,
            thickness = 1.dp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Preview
@Composable
fun PassagePreview() {
    Passage(
        SelectedPassageInfo(
            title = "This is the title",
            date = "This is the date",
            passages =
                listOf(
                    "\"...In Europe, several aviation officials, who spoke on the condition of anonymity to discuss the process by which jets are grounded, said they traditionally look to the FAA for guidance on U.S.-built planes. And for days, they stuck to that, opting not to take action in the face of mounting pressure to do so. As recently as Tuesday morning, regulators in some nations in Europe issued statements standing by the U.S. decision to keep the 737 Max flying...\"",
                    "\"...In Brazil, the controversy surrounding the 737 Max even before Sunday's crash was not so much a rejection of U.S. leadership as an attempt to function in its absence. When the Max models were first introduced, for instance, Brazil ignored an FAA decision not to require additional pilot training for the aircraft's software, instead determining that such training was in fact needed...\"",
                ),
            "",
            0,
        ),
        true,
        {
        },
        {},
        {}
    )
}

@Preview
@Composable
fun TextItemPreview() {
    TextItem(
        textItem =
            PostAnswerUIItem.TextItem(
                subtype = TextSubtype.TITLE,
                content =
                    "Project 2025 is a strategic plan developed by a coalition of conservative groups to overhaul the U.S. federal government if a Republican is elected president in 2024. The plan includes reducing regulations, particularly those related to the environment, cutting back the powers of agencies like the EPA, and promoting energy independence through increased fossil fuel production. \n" +
                            "\n" +
                            "It also aims to reform immigration policies, strengthen national security, and reduce taxes and government spending. Supporters believe Project 2025 will curtail federal overreach and enhance individual freedoms.",
                mimeType = MimeType.PLAIN,
            ),
        onTextExpanded = {},
        onUserEvent = {},
        askThePostUIEvent = { }
    )
}

@Composable
fun CitationInlineIcon(
    number: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(RoundedCornerShape(2.dp))
            .border(
                width = 1.5.dp,
                color = wpdsColors.gray300,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            fontSize = 14.sp,
            lineHeight = 12.sp,
            letterSpacing = 0.sp,
            fontFamily = FontFamily(Font(R.font.franklinitcstd_light)),
            color = wpdsColors.onSurface
        )
    }
}


@Composable
fun HtmlWithCitations(
    askThePostUIEvent: (AskThePostEvent) -> Unit,
    textItem: PostAnswerUIItem.TextItem,
    fragmentManager: FragmentManager? = null,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null
) {
    val parsed = textItem.parsed
    if (parsed == null) {
        Text(text = textItem.content)
        return
    }

    val inlineMap = buildCitationInlineMap(
        askThePostUIEvent = askThePostUIEvent,
        parsed.citations,
        textItem.citations,
        fragmentManager,
        SourceBottomSheetCaller.SEARCH
    ) { citation, cit ->
        citation.id?.removePrefix(CITATION_PREFIX)?.toIntOrNull() == cit.number
    }

    Text(
        modifier = Modifier.animateContentSize(),
        text = parsed.annotated,
        inlineContent = inlineMap,
        style = provideTextItemStyle(textItem.subtype),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = onTextLayout ?: {}
    )
}

@Composable
fun ContentText(
    askThePostUIEvent: (AskThePostEvent) -> Unit,
    textItem: PostAnswerUIItem.TextItem,
    onTextExpanded: () -> Unit,
    onUserEvent: (UserEvent) -> Unit,
    fullResponse: Boolean = false,
    fragmentManager: FragmentManager?,
) {
    textItem.mimeType?.let {
        when (textItem.mimeType) {
            MimeType.HTML -> {
                if (textItem.subtype == TextSubtype.BODY) {
                    var shouldShowExpandButton by rememberSaveable { mutableStateOf(true) }
                    var maxLines by rememberSaveable { mutableIntStateOf(3) }
                    maxLines = if (!fullResponse) maxLines else Int.MAX_VALUE
                    shouldShowExpandButton = if (!fullResponse) shouldShowExpandButton else false
                    Column {
                        HtmlWithCitations(
                            textItem = textItem,
                            askThePostUIEvent = askThePostUIEvent,
                            fragmentManager = fragmentManager,
                            maxLines = if (!fullResponse) maxLines else Int.MAX_VALUE,
                            onTextLayout = {
                                shouldShowExpandButton = it.hasVisualOverflow
                            }
                        )
                        AnimatedVisibility(shouldShowExpandButton) {
                            ShowMoreButton {
                                maxLines = Int.MAX_VALUE
                                shouldShowExpandButton = false
                                onTextExpanded()
                                onUserEvent(UserEvent.PostAnswerShowMoreClick())
                            }
                        }
                    }
                } else {
                    HtmlText(textItem.content)
                }
            }

            MimeType.PLAIN -> {
                if (textItem.subtype == TextSubtype.BODY) {
                    var shouldShowExpandButton by rememberSaveable { mutableStateOf(true) }
                    var maxLines by rememberSaveable { mutableIntStateOf(3) }
                    maxLines = if (!fullResponse) maxLines else Int.MAX_VALUE
                    shouldShowExpandButton = if (!fullResponse) shouldShowExpandButton else false

                    val citationRegex = remember { Regex("\\[(\\d+)\\](?:,\\s*)?") }
                    val parsed = remember(textItem.content) {
                        val b = AnnotatedString.Builder()
                        val citations = mutableListOf<ParsedCitation>()
                        var last = 0
                        citationRegex.findAll(textItem.content).forEach { m ->
                            val start = m.range.first
                            if (last < start) b.append(textItem.content.substring(last, start))
                            val num = m.groupValues[1].toIntOrNull() ?: return@forEach
                            val id = num.toString()
                            b.appendInlineContent(id, "[$num]")
                            citations += ParsedCitation(num, id)
                            last = m.range.last + 1
                        }
                        if (last < textItem.content.length) b.append(textItem.content.substring(last))
                        ParsedTextResult(b.toAnnotatedString(), citations)
                    }

                    val inlineMap = buildCitationInlineMap(
                        askThePostUIEvent,
                        parsed.citations,
                        textItem.citations,
                        fragmentManager,
                        SourceBottomSheetCaller.ASK
                    ) { citation, cit ->
                        citation.source?.citationId == cit.tag
                    }

                    Column {
                        MarkdownText(
                            modifier = Modifier.animateContentSize(),
                            source = parsed.annotated,
                            inlineContent = inlineMap,
                            maxLines = maxLines,
                            style = provideTextItemStyle(textItem.subtype),
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = {
                                shouldShowExpandButton = it.hasVisualOverflow
                            }
                        )
                        AnimatedVisibility(shouldShowExpandButton) {
                            ShowMoreButton {
                                maxLines = Int.MAX_VALUE
                                shouldShowExpandButton = false
                                onTextExpanded()
                                onUserEvent(UserEvent.PostAnswerShowMoreClick())
                            }
                        }
                    }
                } else if (textItem.subtype == TextSubtype.TITLE) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.animateContentSize()
                    ) {
                        Text(
                            modifier = Modifier.animateContentSize(),
                            text =
                                getDescriptionText(
                                    textItem.content,
                                    textItem.subtype == TextSubtype.DESCRIPTION,
                                ),
                            // TODO in https://arcpublishing.atlassian.net/browse/AWA-10261 check for bottomSheetInfo rather than checking the subtype,
                            style = provideTextItemStyle(textItem.subtype),
                        )

                        Spacer(Modifier.width(6.dp))

                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ask_sources_icon),
                            contentDescription = "ATP Search Icon",
                            modifier = Modifier.size(50.dp),
                            tint = Color.Unspecified
                        )
                    }
                } else {
                    Text(
                        modifier = Modifier.animateContentSize(),
                        text =
                            getDescriptionText(
                                textItem.content,
                                textItem.subtype == TextSubtype.DESCRIPTION,
                            ),
                        // TODO in https://arcpublishing.atlassian.net/browse/AWA-10261 check for bottomSheetInfo rather than checking the subtype,
                        style = provideTextItemStyle(textItem.subtype),
                    )
                }
            }
        }
    }
}

private fun buildCitationInlineMap(
    askThePostUIEvent: (AskThePostEvent) -> Unit,
    parsedCitations: List<ParsedCitation>,
    citationSources: List<Citation>?,
    fragmentManager: FragmentManager?,
    sourceBottomSheetCaller: SourceBottomSheetCaller,
    citationMatcher: (Citation, ParsedCitation) -> Boolean
): Map<String, InlineTextContent> {
    return parsedCitations.associate { cit ->
        val citation = citationSources?.firstOrNull { citationSource ->
            citationMatcher(citationSource, cit)
        }
        cit.tag to InlineTextContent(
            Placeholder(
                width = 20.sp,
                height = 20.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center
            )
        ) {
            CitationInlineIcon(
                number = cit.number,
                onClick = {
                    val source = citation?.source
                    if (source != null) {
                        askThePostUIEvent.invoke(
                            AskThePostEvent.SetCitationData(
                                SelectedPassageInfo(
                                    title = "${cit.number}. ${source.content}",
                                    date = source.publishDate?.let {
                                        SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                                            .apply { timeZone = TimeZone.getTimeZone("UTC") }
                                            .format(it)
                                    } ?: "",
                                    passages = listOf(source.passages ?: ""),
                                    url = source.url,
                                    position = cit.number
                                ))
                        )
                        fragmentManager?.let {
                            SourceBottomSheet(sourceBottomSheetCaller)
                                .show(it, "SourceBottomSheetTag")
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ShowMoreButton(
    modifier: Modifier = Modifier,
    onExpandClicked: () -> Unit,
) {
    Row(
        modifier =
            modifier
                .padding(top = 8.dp)
                .background(Color.Transparent)
                .border(1.dp, Color.LightGray, RoundedCornerShape(16.dp))
                .clickable {
                    onExpandClicked()
                }
                .padding(horizontal = 17.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            style =
                TextStyle(
                    color = wpdsColors.onSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily(Font(R.font.franklinitcstd_bold)),
                ),
            text = stringResource(R.string.ai_overview_show_more_expand_button),
        )
        Icon(
            painter = painterResource(com.wpds.wpds.R.drawable.chevron_arrow_down),
            contentDescription = null,
            tint = wpdsColors.onSecondary,
        )
    }
}

@Composable
fun EllipsisMenuWithDelete(deleteConversation: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(com.wpds.wpds.R.drawable.dots_horizontal),
                contentDescription = "Menu",
                tint = wpdsColors.gray80,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Delete chat") },
                onClick = {
                    deleteConversation()
                    expanded = false
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(com.washingtonpost.android.R.drawable.ic_trash_can),
                        contentDescription = "Delete"
                    )
                }
            )
        }
    }
}

@Composable
private fun getDescriptionText(
    baseText: String,
    isClickable: Boolean,
): AnnotatedString =
    if (isClickable) {
        buildAnnotatedString {
            append(baseText)
            append(" ")
            withStyle(
                style = SpanStyle(textDecoration = TextDecoration.Underline),
            ) {
                append(stringResource(R.string.ai_overview_description_learn_more))
            }
        }
    } else {
        AnnotatedString(baseText)
    }

@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context -> TextView(context) },
        update = { it.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT) },
    )
}

@Composable
fun provideTextItemStyle(textSubtype: TextSubtype?): TextStyle =
    when (textSubtype) {
        TextSubtype.TITLE ->
            TextStyle(
                color = getTextTypeColor(textSubtype),
                fontSize = 16.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily(Font(R.font.franklinitcstd_bold)),
            )

        TextSubtype.SUBTITLE ->
            TextStyle(
                color = getTextTypeColor(textSubtype),
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily(Font(R.font.franklinitcstd_light)),
            )

        TextSubtype.DESCRIPTION ->
            TextStyle(
                color = getTextTypeColor(textSubtype),
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontFamily = FontFamily(Font(R.font.franklinitcstd_light)),
            )

        TextSubtype.BODY ->
            TextStyle(
                color = getTextTypeColor(textSubtype),
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontFamily = FontFamily(Font(R.font.franklinitcstd_light)),
            )

        null ->
            TextStyle(
                color = getTextTypeColor(null),
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontFamily = FontFamily(Font(R.font.franklinitcstd_light)),
            )
    }

@Composable
private fun getTextTypeColor(textSubtype: TextSubtype?): Color =
    when (textSubtype) {
        TextSubtype.TITLE -> wpdsColors.onSecondary
        TextSubtype.SUBTITLE -> wpdsColors.accessible
        TextSubtype.DESCRIPTION -> wpdsColors.accessible
        TextSubtype.BODY -> wpdsColors.gray0
        null -> wpdsColors.accessible
    }

@Preview(showSystemUi = true, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun PreviewCarouselItem() {
    CarouselItem(
        modifier = Modifier,
        carouselItem =
            CarouselUIItem(
                id = 2,
                "",
                755118720000,
                "Trump took a private flight with Project 2025 leader in 2022",
                "",
                listOf("Snippet of text"),
            ),
        position = 2,
        onUserEvent = {},
        showPassage = {},
        {},
        true,
    )
}

@Preview(showSystemUi = true, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun PreviewLoadingState() {
    PostAnswersContainer(
        askThePostUIState = AskThePostUIState(),
        askThePostUIEvent = {},
        postAnswersUIState = PostAnswersUIState.Loading,
        onUserEvent = {},
        modifier = Modifier,
        fragmentManager = null
    )
}

@Preview(showSystemUi = true, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun PreviewErrorState() {
    PostAnswersContainer(
        askThePostUIState = AskThePostUIState(),
        askThePostUIEvent = {},
        postAnswersUIState = PostAnswersUIState.Error,
        onUserEvent = {},
        modifier = Modifier,
        fragmentManager = null
    )
}

@Preview(showSystemUi = true, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun PreviewCarousel() {
    Carousel(
        modifier = Modifier.fillMaxSize(),
        carousel =
            PostAnswerUIItem.Carousel(
                CarouselSubtype.PASSAGES,
                listOf(
                    CarouselUIItem(
                        id = 2,
                        "",
                        755118720000,
                        "Trump took a private flight with Project 2025 leader in 2022",
                        "",
                        listOf("Snippet of text"),
                    ),
                    CarouselUIItem(
                        id = 2,
                        "",
                        755118720000,
                        "Trump took a private flight with Project 2025 leader in 2022",
                        "",
                        listOf("Snippet of text"),
                    ),
                    CarouselUIItem(
                        id = 2,
                        "",
                        755118720000,
                        "Trump took a private flight with Project 2025 leader in 2022",
                        "",
                        listOf("Snippet of text"),
                    ),
                    CarouselUIItem(
                        id = 2,
                        "",
                        755118720000,
                        "Trump took a private flight with Project 2025 leader in 2022",
                        "",
                        listOf("Snippet of text"),
                    ),
                ),
            ),
        onUserEvent = {},
        showPassage = {},
        {},
    )
}

@Preview(showSystemUi = true, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun PreviewSuccessState() {
    val mockDataList =
        listOf(
            PostAnswerUIItem.TextItem(
                subtype = TextSubtype.TITLE,
                content = "AI OVERVIEW",
                mimeType = MimeType.PLAIN,
            ),
            PostAnswerUIItem.TextItem(
                subtype = TextSubtype.SUBTITLE,
                content = "Overview is AI-generated from published reporting. Please verify by consulting the provided articles.",
                mimeType = MimeType.PLAIN,
            ),
            PostAnswerUIItem.TextItem(
                subtype = TextSubtype.BODY,
                content =
                    "Project 2025 is a strategic plan developed by a coalition of conservative groups to overhaul the U.S. federal government if a Republican is elected president in 2024. The plan includes reducing regulations, particularly those related to the environment, cutting back the powers of agencies like the EPA, and promoting energy independence through increased fossil fuel production. \n" +
                            "\n" +
                            "It also aims to reform immigration policies, strengthen national security, and reduce taxes and government spending. Supporters believe Project 2025 will curtail federal overreach and enhance individual freedoms.",
                mimeType = MimeType.PLAIN,
            ),
            PostAnswerUIItem.Carousel(
                subtype = CarouselSubtype.PASSAGES,
                items =
                    listOf(
                        CarouselUIItem(
                            id = 2,
                            "",
                            755118720000,
                            "Trump took a private flight with Project 2025 leader in 2022",
                            "",
                            listOf("Snippet of text"),
                        ),
                        CarouselUIItem(
                            id = 2,
                            "",
                            755118720000,
                            "Trump took a private flight with Project 2025 leader in 2022",
                            "",
                            listOf("Snippet of text"),
                        ),
                        CarouselUIItem(
                            id = 2,
                            "",
                            755118720000,
                            "Trump took a private flight with Project 2025 leader in 2022",
                            "",
                            listOf("Snippet of text"),
                        ),
                        CarouselUIItem(
                            id = 2,
                            "",
                            755118720000,
                            "Trump took a private flight with Project 2025 leader in 2022",
                            "",
                            listOf("Snippet of text"),
                        ),
                        CarouselUIItem(
                            id = 2,
                            "",
                            755118720000,
                            "Trump took a private flight with Project 2025 leader in 2022",
                            "",
                            listOf("Snippet of text"),
                        ),
                    ),
            ),
            PostAnswerUIItem.Feedback(endPointUrl = "", responseId = ""),
        )
    Success(
        askThePostUIEvent = {},
        postAnswerUIItems = mockDataList,
        onUserEvent = {}
    )
}

@DevicePreviews
@Composable
fun PreviewPostAnswersContainer() {
    val mockDataList =
        listOf(
            PostAnswerUIItem.TextItem(
                subtype = TextSubtype.TITLE,
                content = "AI OVERVIEW",
                mimeType = MimeType.PLAIN,
            ),
            PostAnswerUIItem.TextItem(
                subtype = TextSubtype.SUBTITLE,
                content = "Overview is AI-generated from published reporting. Please verify by consulting the provided articles.",
                mimeType = MimeType.PLAIN,
            ),
            PostAnswerUIItem.TextItem(
                subtype = TextSubtype.BODY,
                content =
                    "Project 2025 is a strategic plan developed by a coalition of conservative groups to overhaul the U.S. federal government if a Republican is elected president in 2024. The plan includes reducing regulations, particularly those related to the environment, cutting back the powers of agencies like the EPA, and promoting energy independence through increased fossil fuel production. \n" +
                            "\n" +
                            "It also aims to reform immigration policies, strengthen national security, and reduce taxes and government spending. Supporters believe Project 2025 will curtail federal overreach and enhance individual freedoms.",
                mimeType = MimeType.PLAIN,
            ),
            PostAnswerUIItem.Carousel(
                subtype = CarouselSubtype.PASSAGES,
                items =
                    listOf(
                        CarouselUIItem(
                            id = 2,
                            "",
                            755118720000,
                            "Trump took a private flight with Project 2025 leader in 2022",
                            "",
                            listOf("Snippet of text"),
                        ),
                        CarouselUIItem(
                            id = 2,
                            "",
                            755118720000,
                            "Trump took a private flight with Project 2025 leader in 2022",
                            "",
                            listOf("Snippet of text"),
                        ),
                        CarouselUIItem(
                            id = 2,
                            "",
                            755118720000,
                            "Trump took a private flight with Project 2025 leader in 2022",
                            "",
                            listOf("Snippet of text"),
                        ),
                        CarouselUIItem(
                            id = 2,
                            "",
                            755118720000,
                            "Trump took a private flight with Project 2025 leader in 2022",
                            "",
                            listOf("Snippet of text"),
                        ),
                        CarouselUIItem(
                            id = 2,
                            "",
                            755118720000,
                            "Trump took a private flight with Project 2025 leader in 2022",
                            "",
                            listOf("Snippet of text"),
                        ),
                    ),
            ),
            PostAnswerUIItem.Feedback(endPointUrl = "", responseId = ""),
        )
    PostAnswersContainer(
        askThePostUIState = AskThePostUIState(),
        askThePostUIEvent = {},
        postAnswersUIState =
            PostAnswersUIState.Success(
                mockDataList,
            ),
        onUserEvent = {},
        modifier = Modifier,
        fragmentManager = null,
    )
}

@Composable
fun FixedOrderContentView(
    askThePostUIState: AskThePostUIState,
    askThePostUIEvent: (AskThePostEvent) -> Unit,
    postAnswerUIItems: List<PostAnswerUIItem>,
    onUserEvent: (UserEvent) -> Unit,
    modifier: Modifier = Modifier,
    showPassage: () -> Unit,
    passageData: (SelectedPassageInfo) -> Unit,
    fragmentManager: FragmentManager? = null,
) {
    var isBodyTextExpanded by rememberSaveable { mutableStateOf(true) }

    // Finding the first occurrence of each specific item type from the input list.
    val bodyItem =
        postAnswerUIItems
            .filterIsInstance<PostAnswerUIItem.TextItem>()
            .find { it.subtype == TextSubtype.BODY }

    val descriptionItem =
        postAnswerUIItems
            .filterIsInstance<PostAnswerUIItem.TextItem>()
            .find { it.subtype == TextSubtype.DESCRIPTION }

    val feedbackItemData =
        postAnswerUIItems
            .filterIsInstance<PostAnswerUIItem.Feedback>()
            .firstOrNull()

    val carouselItemData =
        postAnswerUIItems
            .filterIsInstance<PostAnswerUIItem.Carousel>()
            .firstOrNull()

    Column(
        modifier =
            modifier
                .padding(8.dp)
                .fillMaxWidth()
                .wrapContentHeight(),
    ) {
        bodyItem?.let {
            // Checking to see if we need to create multiple paragraphs
            if (bodyItem.content.contains("\\n\\n")) {
                val paragraphs = bodyItem.content.split("\\n\\n")
                paragraphs.forEach {
                    TextItem(
                        textItem = PostAnswerUIItem.TextItem(
                            subtype = TextSubtype.BODY,
                            content = it,
                            mimeType = MimeType.PLAIN,
                            citations = bodyItem.citations,
                        ),
                        onTextExpanded = { isBodyTextExpanded = true },
                        onUserEvent = onUserEvent,
                        fullResponse = true,
                        fragmentManager = fragmentManager,
                        askThePostUIEvent = askThePostUIEvent
                    )
                    Spacer(Modifier.height(16.dp))
                }

            } else {
                TextItem(
                    textItem = it,
                    onTextExpanded = { isBodyTextExpanded = true },
                    onUserEvent = onUserEvent,
                    fullResponse = true,
                    fragmentManager = fragmentManager,
                    askThePostUIEvent = askThePostUIEvent
                )
            }
        }

        AnimatedVisibility(visible = isBodyTextExpanded) {
            Column {
                // Render Description TextItem if found
                descriptionItem?.let {
                    Spacer(Modifier.height(16.dp))
                    TextItem(
                        textItem = it,
                        onTextExpanded = { },
                        onUserEvent = onUserEvent,
                        askThePostUIEvent = askThePostUIEvent,
                    )
                }

                // Render FeedbackItem if found
                feedbackItemData?.let {
                    Spacer(Modifier.height(16.dp))
                    FeedbackAnswerItem(
                        item = it,
                        onUserEvent = onUserEvent,
                    )
                }

                // Render Carousel if found
                carouselItemData?.let {
                    Spacer(Modifier.height(32.dp))
                    androidx.compose.material.Text(
                        "Sources",
                        color = wpdsColors.gray0,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        fontFamily =
                            FontFamily(
                                Font(R.font.franklinitcstd_bold),
                            ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Carousel(
                        carousel = it,
                        onUserEvent = onUserEvent,
                        showPassage = showPassage,
                        passageData = passageData,
                        isSearch = false,
                    )
                }
            }
        }
    }
}

@Preview(
    name = "Reduced width",
    showSystemUi = true,
    device = "spec:width=150dp,height=392dp,dpi=440,orientation=portrait",
)
@Composable
fun PreviewFeedbackItem() {
    FeedbackItem(
        item = PostAnswerUIItem.Feedback(endPointUrl = "", responseId = ""),
        onUserEvent = {},
    )
}

@Preview(showSystemUi = true, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun PreviewFixedOrderContentView() {
    val jumbledItems =
        listOf(
            PostAnswerUIItem.Feedback(
                // Feedback item first in list
                endPointUrl = "feedback_url",
                responseId = "response1",
            ),
            PostAnswerUIItem.TextItem(
                // Title (will be ignored by FixedOrderContentView)
                subtype = TextSubtype.TITLE,
                content = "This Title Will Be Ignored by This Composable",
                mimeType = MimeType.PLAIN,
                icon = null,
                bottomSheetInfo = null,
            ),
            PostAnswerUIItem.Carousel(
                // Carousel item second
                subtype = CarouselSubtype.PASSAGES,
                items =
                    listOf(
                        CarouselUIItem(
                            1,
                            "img",
                            System.currentTimeMillis(),
                            "Carousel Item 1",
                            "url1",
                            listOf("Snippet of text")
                        ),
                        CarouselUIItem(
                            2,
                            "img",
                            System.currentTimeMillis(),
                            "Carousel Item 2",
                            "url2",
                            listOf("Snippet of text")
                        ),
                    ),
            ),
            PostAnswerUIItem.TextItem(
                // Body item third
                subtype = TextSubtype.BODY,
                content = "This is the main body text. It might be long enough to require expansion. Clicking 'Show More' (if present) should reveal the items below.",
                mimeType = MimeType.PLAIN,
                icon = null,
                bottomSheetInfo = null,
            ),
            PostAnswerUIItem.TextItem(
                // Description item last in list
                subtype = TextSubtype.DESCRIPTION,
                content = "This is the description text, appearing after body expansion.",
                mimeType = MimeType.PLAIN,
                icon = null, // Replace with a valid icon name if applicable for your TextItem
                bottomSheetInfo = null,
            ),
        )

    FixedOrderContentView(
        askThePostUIState = AskThePostUIState(),
        postAnswerUIItems = jumbledItems,
        onUserEvent = {},
        showPassage = {},
        passageData = {},
        askThePostUIEvent = {}
    )
}
