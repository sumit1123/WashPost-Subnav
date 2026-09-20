package com.wapo.flagship.features.articles3.views

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatTextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.secondsToDuration
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.utils.KickerStyleHelper
import com.wapo.flagship.features.articles3.models.ui.CarouselItemUiModel
import com.wapo.flagship.features.articles3.models.ui.CarouselUiModel
import com.wapo.flagship.features.articles3.models.ui.ForYouCarouselItemUiModel
import com.wapo.flagship.features.articles3.models.ui.RecircCarouselItemUiModel
import com.wapo.flagship.features.articles3.models.ui.InlineCarouselItemUiModel
import com.wapo.flagship.features.articles3.models.ui.SourceCommentUiModel
import com.wapo.flagship.features.comments.model.Author
import com.wapo.flagship.features.comments.model.PromoImage
import com.wapo.flagship.features.comments.model.Transcript
import com.wapo.flagship.features.comments.model.Video
import com.wapo.text.WpTextAppearanceSpan
import com.wapo.text.applyUnderline
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.wpdsColors
import com.wpds.theme.wpdsColorsLight
import com.wpds.wpds.R
import com.washingtonpost.foryou.repo.ForYouFeedRepositoryImpl
import com.washingtonpost.foryou.viewmodel.ForYouActivityViewModel
import com.washingtonpost.userhistory.ForYouViewedAction
import com.washingtonpost.userhistory.models.RecommendationsHelperItem
import com.washingtonpost.userhistory.viewmodel.UserHistoryViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.wapo.Utils
import com.washingtonpost.userhistory.models.AutoRecircHelperItem
import java.util.concurrent.TimeUnit

private val CARD_WIDTH = 280.dp
private val CARD_HORIZONTAL_SPACING = 12.dp
private val LEGACY_ARTICLE_CARD_WIDTH = 266.dp
private val LEGACY_ARTICLE_CARD_SPACING = 15.dp
private val LEGACY_ARTICLE_IMAGE_SIZE = 100.dp

@Composable
fun CarouselView(
    uiModel: CarouselUiModel,
    articlesInteractionHelper: ArticlesInteractionHelper,
    userHistoryViewModel: UserHistoryViewModel? = null,
    forYouActivityViewModel: ForYouActivityViewModel? = null,
    onCarouselVisibilityChanged: (Boolean) -> Unit = {},
) {
    val carouselHasVideos = uiModel.items.any { item ->
        (item as? SourceCommentUiModel)?.video != null
    }
    val cardMinHeight = remember { mutableIntStateOf(Int.MIN_VALUE) }
    val minCardHeightUpdateEvent: (Int) -> Unit = { newMinHeight ->
        if (cardMinHeight.intValue < newMinHeight) cardMinHeight.intValue = newMinHeight
    }

    // Build recommendations helper items for fy_viewed tracking (For You carousel only)
    val shouldTrackFyViewed = uiModel.uiStyle == CarouselUiStyle.FOR_YOU &&
            userHistoryViewModel != null &&
            forYouActivityViewModel != null

    val recommendationsHelperItems = remember(uiModel.items) {
        if (shouldTrackFyViewed) {
            uiModel.items.mapNotNull { item ->
                when (item) {
                    is ForYouCarouselItemUiModel -> RecommendationsHelperItem(
                        item.articleId,
                        item.recReason,
                        uiModel.requestId,
                        uiModel.recipeId,
                        uiModel.testId,
                        item.contentType
                    )
                    else -> null
                }
            }
        } else {
            emptyList()
        }
    }

    val shouldTrackAutoRecircViewed = (uiModel.uiStyle == CarouselUiStyle.AUTO_RECIRC ||
            uiModel.uiStyle == CarouselUiStyle.SEVEN_LIVE) &&
            userHistoryViewModel != null &&
            forYouActivityViewModel != null
    val autoRecircHelperItems = remember(uiModel.items) {
        if (shouldTrackAutoRecircViewed) {
            uiModel.items.mapIndexedNotNull { index, item ->
                when (item) {
                    is RecircCarouselItemUiModel -> AutoRecircHelperItem(
                        articleId = item.articleId.orEmpty(),
                        requestId = uiModel.requestId.orEmpty(),
                        currentUrl = item.url.orEmpty(),
                        collectionCategory = uiModel.category.orEmpty(),
                        positionInModule = index
                    )
                    else -> null
                }
            }
        } else {
            emptyList()
        }
    }

    // Track carousel visibility for fy_viewed timer start/stop
    // Note: Visibility is now detected by the parent Content composable using LazyListState
    // The callback will be invoked when carousel visibility changes

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // Top Divider
        HorizontalDivider(
            color = wpdsColors.divider,
            thickness = 1.dp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))

        // Label
        if (uiModel.label.isNotEmpty()) {
            CarouselLabel(uiModel)
            Spacer(Modifier.height(16.dp))
        }

        Box {
            // Invisible subcompose pass: measure each card to find the tallest height
            SubComposeMeasureCards(
                items = uiModel.items,
                uiStyle = uiModel.uiStyle,
                carouselHasVideos = carouselHasVideos,
                onHeightMeasured = minCardHeightUpdateEvent,
                articlesInteractionHelper = articlesInteractionHelper,
                userHistoryViewModel = userHistoryViewModel,
                shouldTrackFyViewed = shouldTrackFyViewed,
                shouldTrackAutoRecircViewed = shouldTrackAutoRecircViewed,
            )
            // Real carousel, all cards rendered at the max measured height
            CarouselRow(
                items = uiModel.items,
                cardMinHeight = cardMinHeight.intValue,
                uiStyle = uiModel.uiStyle,
                carouselHasVideos = carouselHasVideos,
                articlesInteractionHelper = articlesInteractionHelper,
                userHistoryViewModel = userHistoryViewModel,
                shouldTrackFyViewed = shouldTrackFyViewed,
                recommendationsHelperItems = recommendationsHelperItems,
                shouldTrackAutoRecircViewed = shouldTrackAutoRecircViewed,
                autoRecircHelperItems = autoRecircHelperItems,
            )
        }
    }
}

@Composable
private fun CarouselLabel(uiModel: CarouselUiModel) {
    if (uiModel.label.isEmpty()) return
    when (uiModel.uiStyle) {
        CarouselUiStyle.SEVEN_LIVE -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(com.washingtonpost.android.R.drawable.ic_label_briefs),
                    contentDescription = "The 7 logo",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(25.dp, 24.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                DefaultCarouselLabel(label = uiModel.label)
                Spacer(modifier = Modifier.width(5.dp))
                Image(
                    painter = painterResource(com.washingtonpost.android.sections.R.drawable.the_seven_live_chip),
                    contentDescription = null,
                )
            }
        }

        else -> DefaultCarouselLabel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            label = uiModel.label
        )
    }
}

@Composable
private fun DefaultCarouselLabel(
    modifier: Modifier = Modifier,
    label: String,
) {
    Text(
        text = label,
        color = wpdsColors.gray20,
        fontSize = 18.sp,
        letterSpacing = 0.0.sp,
        fontFamily = FontFamily(Font(R.font.franklinitcstd_bold)),
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/**
 * Renders each card via [SubcomposeLayout] at size 0x0 solely to measure its natural height.
 * Mirrors SubComposeCarousel + SubComposeCarouselCard from FtsCarouselRecirculationViewHolder.
 */
@Composable
private fun SubComposeMeasureCards(
    items: List<CarouselItemUiModel>,
    uiStyle: CarouselUiStyle,
    carouselHasVideos: Boolean,
    onHeightMeasured: (Int) -> Unit,
    articlesInteractionHelper: ArticlesInteractionHelper,
    userHistoryViewModel: UserHistoryViewModel? = null,
    shouldTrackFyViewed: Boolean = false,
    shouldTrackAutoRecircViewed: Boolean = false,
) {
    items.forEach { item ->
        SubcomposeLayout { constraints ->
            val placeable = subcompose(item) {
                CarouselCardContent(
                    item = item,
                    uiStyle = uiStyle,
                    carouselHasVideos = carouselHasVideos,
                    cardMinHeight = Dp.Unspecified, // unconstrained during measurement
                    articlesInteractionHelper = articlesInteractionHelper,
                    userHistoryViewModel = userHistoryViewModel,
                    shouldTrackFyViewed = shouldTrackFyViewed,
                    shouldTrackAutoRecircViewed = shouldTrackAutoRecircViewed,
                )
            }.firstOrNull()?.measure(constraints)
            onHeightMeasured(placeable?.height ?: 0)
            layout(0, 0) {}
        }
    }
}

@Composable
private fun CarouselRow(
    items: List<CarouselItemUiModel>,
    cardMinHeight: Int,
    uiStyle: CarouselUiStyle,
    carouselHasVideos: Boolean,
    articlesInteractionHelper: ArticlesInteractionHelper,
    userHistoryViewModel: UserHistoryViewModel? = null,
    shouldTrackFyViewed: Boolean = false,
    recommendationsHelperItems: List<RecommendationsHelperItem> = emptyList(),
    shouldTrackAutoRecircViewed: Boolean = false,
    autoRecircHelperItems: List<AutoRecircHelperItem> = emptyList(),
) {
    val listState = rememberLazyListState()
    val cardHeightInDp = (cardMinHeight / AppContextUtils.getDeviceDensity()).dp
    val isLegacyArticleStyle = isLegacyArticleCarousel(uiStyle)

    // Add scroll tracking for fy_viewed only for For You carousel
    if (shouldTrackFyViewed && userHistoryViewModel != null && recommendationsHelperItems.isNotEmpty()) {
        LaunchedEffect(listState) {
            snapshotFlow { listState.layoutInfo.visibleItemsInfo.map { it.index } }
                .distinctUntilChanged()
                .collect { visibleIndices ->
                    // Start timers for visible items
                    if (visibleIndices.isNotEmpty()) {
                        userHistoryViewModel.startOrStopForYouViewedTimers(
                            recommendationsItems = recommendationsHelperItems,
                            visibleItemIndices = visibleIndices,
                            surface = ForYouFeedRepositoryImpl.SURFACE_RECIRC
                        )
                    } else {
                        userHistoryViewModel.stopAllForYouViewedTimers()
                    }
                }
        }
    }
    if (shouldTrackAutoRecircViewed && userHistoryViewModel != null && autoRecircHelperItems.isNotEmpty()) {
        LaunchedEffect(listState) {
            snapshotFlow { listState.layoutInfo.visibleItemsInfo.map { it.index } }
                .distinctUntilChanged()
                .collect { visibleIndices ->
                    // Start timers for visible items
                    if (visibleIndices.isNotEmpty()) {
                        userHistoryViewModel.startOrStopAutoRecircViewedTimers(
                            recommendationsItems = autoRecircHelperItems,
                            visibleItemIndices = visibleIndices,
                        )
                    } else {
                        userHistoryViewModel.stopAllAutoRecircViewedTimers()
                    }
                }
        }
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(
            if (isLegacyArticleStyle) LEGACY_ARTICLE_CARD_SPACING else CARD_HORIZONTAL_SPACING
        ),
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    ) {
        itemsIndexed(items) { index, item ->
            val dividerColor = wpdsColors.divider
            CarouselCardContent(
                modifier = Modifier
                    .then(
                        if (index != items.lastIndex) {
                            Modifier
                                .drawBehind {
                                    drawLine(
                                        color = dividerColor,
                                        start = Offset(size.width, 0f),
                                        end = Offset(size.width, size.height),
                                        strokeWidth = 1.dp.toPx(),
                                    )
                                }
                                .padding(end = 16.dp)
                        } else {
                            Modifier
                        }
                    ),
                item = item,
                uiStyle = uiStyle,
                carouselHasVideos = carouselHasVideos,
                cardMinHeight = cardHeightInDp,
                articlesInteractionHelper = articlesInteractionHelper,
                userHistoryViewModel = userHistoryViewModel,
                shouldTrackFyViewed = shouldTrackFyViewed,
                shouldTrackAutoRecircViewed = shouldTrackAutoRecircViewed,
            )
        }
    }
}

@Composable
private fun CarouselCardContent(
    modifier: Modifier = Modifier,
    item: CarouselItemUiModel,
    uiStyle: CarouselUiStyle,
    carouselHasVideos: Boolean,
    cardMinHeight: Dp,
    articlesInteractionHelper: ArticlesInteractionHelper,
    userHistoryViewModel: UserHistoryViewModel? = null,
    shouldTrackFyViewed: Boolean = false,
    @Suppress("UNUSED_PARAMETER") shouldTrackAutoRecircViewed: Boolean = false,
) {
    when (item) {
        is SourceCommentUiModel -> FtsCarouselCard(
            modifier = modifier,
            uiModel = item,
            carouselHasVideos = carouselHasVideos,
            cardMinHeight = cardMinHeight,
            articlesInteractionHelper = articlesInteractionHelper
        )
        is ForYouCarouselItemUiModel -> ForYouCarouselItemCard(
            modifier = modifier,
            uiModel = item,
            cardMinHeight = cardMinHeight,
            articlesInteractionHelper = articlesInteractionHelper,
            userHistoryViewModel = userHistoryViewModel,
            shouldTrackFyViewed = shouldTrackFyViewed,
        )
        is RecircCarouselItemUiModel -> if (uiStyle == CarouselUiStyle.SEVEN_LIVE) {
            SevenLiveRecircCard(
                modifier = modifier,
                uiModel = item,
                cardMinHeight = cardMinHeight,
                articlesInteractionHelper = articlesInteractionHelper,
            )
        } else {
            RecircCarouselItemCard(
                modifier = modifier,
                uiModel = item,
                cardMinHeight = cardMinHeight,
                articlesInteractionHelper = articlesInteractionHelper,
            )
        }
        is InlineCarouselItemUiModel -> InlineCarouselItemCard(
            modifier = modifier,
            uiModel = item,
            cardMinHeight = cardMinHeight,
            articlesInteractionHelper = articlesInteractionHelper,
        )
    }
}

private fun isLegacyArticleCarousel(uiStyle: CarouselUiStyle): Boolean {
    return uiStyle == CarouselUiStyle.FOR_YOU ||
        uiStyle == CarouselUiStyle.MOST_READ ||
        uiStyle == CarouselUiStyle.AUTO_RECIRC ||
        uiStyle == CarouselUiStyle.SEVEN_LIVE
}

@Composable
private fun FtsCarouselCard(
    modifier: Modifier = Modifier,
    uiModel: SourceCommentUiModel,
    carouselHasVideos: Boolean,
    cardMinHeight: Dp,
    articlesInteractionHelper: ArticlesInteractionHelper
) {
    Box(
        modifier = modifier.clickable {
            uiModel.url?.let {
                articlesInteractionHelper.onEventFired(
                    ArticleInteractionEvent.CarouselItemClick(it)
                )
            }
        }
    ) {
        Card(
            modifier = Modifier
                .width(CARD_WIDTH)
                .then(
                    if (cardMinHeight != Dp.Unspecified) {
                        Modifier.height(cardMinHeight)
                    } else {
                        Modifier
                    }
                )
                .border(
                    width = 1.dp,
                    color = wpdsColors.outline,
                    shape = RoundedCornerShape(8.dp)
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = wpdsColors.gridCardBg)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Content
                Box(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        top = 16.dp,
                        end = 16.dp,
                        bottom = 12.dp
                    )
                ) {
                    if (uiModel.video != null) {
                        VideoItem(uiModel.video)
                    } else if (uiModel.body != null) {
                        CommentText(uiModel.body, carouselHasVideos)
                    }
                }
                // Author
                AuthorSection(uiModel.author)
            }
        }
    }
}

@Composable
private fun CommentText(comment: String, carouselHasVideos: Boolean) {
    val annotatedString = AnnotatedString.fromHtml(comment)
    Text(
        text = annotatedString,
        color = wpdsColors.gray20,
        fontSize = 16.sp,
        letterSpacing = 0.0.sp,
        fontFamily = FontFamily(Font(R.font.franklinitcstd_light)),
        lineHeight = 20.sp,
        minLines = 3,
        maxLines = if (carouselHasVideos) 5 else 3,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun VideoItem(video: Video) {
    Row {
        VideoPromoImage(video)
        if (video.transcript != null) {
            Spacer(modifier = Modifier.width(12.dp))
            TranscriptText(video.transcript)
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun VideoPromoImage(video: Video) {
    val sizeModifier = if (video.transcript != null) {
        Modifier
            .height(100.dp)
            .width(100.dp)
    } else {
        Modifier
            .height(100.dp)
            .fillMaxWidth()
    }

    Box(modifier = sizeModifier) {
        // Thumbnail
        Box(
            modifier = sizeModifier
                .background(
                    shape = RoundedCornerShape(8.dp),
                    color = wpdsColors.alpha400
                )
        ) {
            GlideImage(
                model = video.promoImage?.url,
                contentDescription = stringResource(com.wapo.flagship.features.articles.R.string.video_thumbnail),
                contentScale = ContentScale.Crop,
                modifier = sizeModifier.clip(RoundedCornerShape(8.dp))
            )
            // Gradient overlay
            Box(
                modifier = sizeModifier
                    .background(
                        shape = RoundedCornerShape(8.dp),
                        brush = Brush.verticalGradient(
                            listOf(
                                wpdsColorsLight.gray0.copy(alpha = 0.0f),
                                wpdsColorsLight.gray0.copy(alpha = 0.27f),
                                wpdsColorsLight.gray0.copy(alpha = 0.75f)
                            )
                        )
                    )
            )
        }

        // Play icon and timestamp
        Column {
            Spacer(modifier = Modifier.weight(1f))
            Row(modifier = Modifier.padding(12.dp)) {
                Icon(
                    painter = painterResource(id = R.drawable.play),
                    contentDescription = "play button",
                    tint = wpdsColorsLight.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                secondsToDuration(video.duration)?.let { duration ->
                    Text(
                        text = duration,
                        color = wpdsColorsLight.onPrimary,
                        fontSize = 14.sp,
                        letterSpacing = 0.0.sp,
                        fontFamily = FontFamily(Font(R.font.franklinitcstd_light)),
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TranscriptText(transcript: Transcript?) {
    val transcriptText = transcript?.text ?: return
    Text(
        text = transcriptText,
        color = wpdsColors.onSurface,
        fontSize = 16.sp,
        letterSpacing = 0.0.sp,
        fontFamily = FontFamily(Font(R.font.franklinitcstd_light)),
        lineHeight = 20.sp,
        maxLines = 5,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun AuthorSection(author: Author?) {
    author ?: return
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
        if (!author.name.isNullOrEmpty()) {
            Text(
                text = author.name,
                color = wpdsColors.onSurfaceSubtle,
                fontSize = 16.sp,
                letterSpacing = 0.0.sp,
                fontFamily = FontFamily(Font(R.font.franklinitcstd_light)),
                lineHeight = 20.sp,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (!author.expertise.isNullOrEmpty()) {
            Text(
                text = author.expertise,
                color = wpdsColors.onSurfaceSubtle,
                fontSize = 14.sp,
                letterSpacing = 0.0.sp,
                fontFamily = FontFamily(Font(R.font.franklinitcstd_light)),
                lineHeight = 17.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LegacyArticleCarouselLabel(
    labelText: String?,
    secondaryLabel: String?,
    isOpinions: Boolean,
) {
    if (labelText.isNullOrBlank()) return

    if (isOpinions) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                AppCompatTextView(context).apply {
                    includeFontPadding = false
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    text = buildLegacyOpinionsKickerSpannable(
                        context = context,
                        labelText = labelText,
                        secondaryLabel = secondaryLabel,
                    )
                }
            },
            update = { textView ->
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                textView.text = buildLegacyOpinionsKickerSpannable(
                    context = textView.context,
                    labelText = labelText,
                    secondaryLabel = secondaryLabel,
                )
            }
        )
        return
    }

    val labelContent = buildAnnotatedString { append(labelText) }

    Text(
        text = labelContent,
        color = wpdsColors.gray20,
        fontSize = 14.sp,
        fontFamily = FontFamily(Font(R.font.franklinitcstd_bold)),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun buildLegacyOpinionsKickerSpannable(
    context: Context,
    labelText: String,
    secondaryLabel: String?,
): SpannableStringBuilder {
    val spannable = SpannableStringBuilder(labelText)
    spannable.setSpan(
        WpTextAppearanceSpan(context, KickerStyleHelper.getTextKickerDefaultStyle(context)),
        0,
        labelText.length,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
    )

    // Keep legacy OPINIONS orange underline treatment used in classic views.
    spannable.applyUnderline(
        context,
        0,
        minOf(1, labelText.length),
        R.color.opinion_spark,
        context.resources.getInteger(com.wapo.view.R.integer.first_part_opinion_left_padding_underline).toFloat(),
        context.resources.getInteger(com.wapo.view.R.integer.first_part_opinion_right_padding_underline).toFloat(),
        -4f,
    )
    if (labelText.length > 2) {
        spannable.applyUnderline(
            context,
            2,
            labelText.length,
            R.color.opinion_spark,
            context.resources.getInteger(com.wapo.view.R.integer.second_part_opinion_left_padding_underline).toFloat(),
            context.resources.getInteger(com.wapo.view.R.integer.second_part_opinion_right_padding_underline).toFloat(),
            -4f,
        )
    }

    if (!secondaryLabel.isNullOrBlank()) {
        val start = spannable.length
        spannable.append(" ")
        spannable.append(secondaryLabel)
        spannable.setSpan(
            WpTextAppearanceSpan(context, KickerStyleHelper.getDisplayTransparencyStyle(context)),
            start,
            spannable.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
    }

    // Ensure carousel opinions text matches the 14sp legacy article-card label size.
    spannable.setSpan(
        AbsoluteSizeSpan(14, true),
        0,
        spannable.length,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
    )

    return spannable
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ForYouCarouselItemCard(
    modifier: Modifier = Modifier,
    uiModel: ForYouCarouselItemUiModel,
    cardMinHeight: Dp,
    articlesInteractionHelper: ArticlesInteractionHelper,
    userHistoryViewModel: UserHistoryViewModel? = null,
    shouldTrackFyViewed: Boolean = false,
) {
    Box(
        modifier = modifier.clickable {
            uiModel.url?.let { url ->
                if (shouldTrackFyViewed && userHistoryViewModel != null) {
                    val recommendationsHelperItem = RecommendationsHelperItem(
                        uiModel.articleId,
                        uiModel.recReason,
                        null,
                        null,
                        null,
                        uiModel.contentType
                    )
                    userHistoryViewModel.captureForYouViewAction(
                        action = ForYouViewedAction.CLICKED,
                        recommendationsItem = recommendationsHelperItem,
                        adapterPosition = 0,
                        surface = ForYouFeedRepositoryImpl.SURFACE_RECIRC
                    )
                }
                articlesInteractionHelper.onEventFired(ArticleInteractionEvent.CarouselItemClick(url))
            }
        }
    ) {
        Card(
            modifier = Modifier
                .width(LEGACY_ARTICLE_CARD_WIDTH)
                .then(if (cardMinHeight != Dp.Unspecified) Modifier.height(cardMinHeight) else Modifier),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = wpdsColors.surface)
        ) {
            Column {
                val label = if (uiModel.isOpinions) uiModel.label?.transparency?.text else uiModel.label?.basic?.text
                LegacyArticleCarouselLabel(
                    labelText = label,
                    secondaryLabel = uiModel.secondaryLabel,
                    isOpinions = uiModel.isOpinions,
                )
                Row(modifier = Modifier.padding(top = 12.dp)) {
                    uiModel.imageUrl?.let { imageUrl ->
                        GlideImage(
                            model = imageUrl,
                            contentDescription = uiModel.headline,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(LEGACY_ARTICLE_IMAGE_SIZE)
                        )
                    }
                    uiModel.headline?.let { headline ->
                        Text(
                            text = headline,
                            color = wpdsColors.gray20,
                            fontSize = 16.sp,
                            fontFamily = FontFamily(Font(R.font.franklinitcstd_light)),
                            lineHeight = 20.sp,
                            modifier = Modifier
                                .padding(start = if (uiModel.imageUrl.isNullOrEmpty()) 0.dp else 8.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun RecircCarouselItemCard(
    modifier: Modifier = Modifier,
    uiModel: RecircCarouselItemUiModel,
    cardMinHeight: Dp,
    articlesInteractionHelper: ArticlesInteractionHelper,
) {
    Box(
        modifier = modifier.clickable {
            uiModel.url?.let { url ->
                articlesInteractionHelper.onEventFired(ArticleInteractionEvent.CarouselItemClick(url))
            }
        }
    ) {
        Card(
            modifier = Modifier
                .width(LEGACY_ARTICLE_CARD_WIDTH)
                .then(if (cardMinHeight != Dp.Unspecified) Modifier.height(cardMinHeight) else Modifier),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = wpdsColors.surface)
        ) {
            Column {
                val label = if (uiModel.isOpinions) uiModel.label?.transparency?.text else uiModel.label?.basic?.text
                LegacyArticleCarouselLabel(
                    labelText = label,
                    secondaryLabel = uiModel.secondaryLabel,
                    isOpinions = uiModel.isOpinions,
                )
                Row(modifier = Modifier.padding(top = 12.dp)) {
                    uiModel.imageUrl?.let { imageUrl ->
                        GlideImage(
                            model = imageUrl,
                            contentDescription = uiModel.headline,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(LEGACY_ARTICLE_IMAGE_SIZE)
                        )
                    }
                    uiModel.headline?.let { headline ->
                        Text(
                            text = headline,
                            color = wpdsColors.gray20,
                            fontSize = 16.sp,
                            fontFamily = FontFamily(Font(R.font.franklinitcstd_light)),
                            lineHeight = 20.sp,
                            modifier = Modifier
                                .padding(start = if (uiModel.imageUrl.isNullOrEmpty()) 0.dp else 8.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun SevenLiveRecircCard(
    modifier: Modifier = Modifier,
    uiModel: RecircCarouselItemUiModel,
    cardMinHeight: Dp,
    articlesInteractionHelper: ArticlesInteractionHelper,
) {
    val (relativeTime, isRecent) = remember(uiModel.timestamp) {
        val date = Utils.toDateLong(uiModel.timestamp, Utils.getDefaultDateFormat())
        val diff = System.currentTimeMillis() - date

        if (date > 0L && diff >= 0L) {
            Pair(
                Utils.getAbbreviatedRelativeTime(date),
                diff <= TimeUnit.HOURS.toMillis(1)
            )
        } else {
            Pair(null, false)
        }
    }

    Box(
        modifier = modifier.clickable {
            uiModel.url?.let { url ->
                articlesInteractionHelper.onEventFired(ArticleInteractionEvent.CarouselItemClick(url))
            }
        }
    ) {
        Card(
            modifier = Modifier
                .width(LEGACY_ARTICLE_CARD_WIDTH)
                .then(if (cardMinHeight != Dp.Unspecified) Modifier.height(cardMinHeight) else Modifier),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = wpdsColors.surface)
        ) {
            Column {
                if (!relativeTime.isNullOrBlank()) {
                    Text(
                        text = relativeTime,
                        color = if (isRecent) wpdsColors.liveUpdateTextColor else wpdsColors.onSurfaceSubtle,
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.franklinitcstd_light)),
                        maxLines = 1,
                        modifier = Modifier
                            .padding(top = 6.dp, bottom = 12.dp)
                            .fillMaxWidth()
                    )
                }
                Row {
                    uiModel.imageUrl?.let { imageUrl ->
                        GlideImage(
                            model = imageUrl,
                            contentDescription = uiModel.headline,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(LEGACY_ARTICLE_IMAGE_SIZE)
                        )
                    }
                    uiModel.headline?.let { headline ->
                        Text(
                            text = headline,
                            color = wpdsColors.gray20,
                            fontSize = 16.sp,
                            fontFamily = FontFamily(Font(R.font.franklinitcstd_light)),
                            lineHeight = 20.sp,
                            modifier = Modifier
                                .padding(start = if (uiModel.imageUrl.isNullOrEmpty()) 0.dp else 8.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun InlineCarouselItemCard(
    modifier: Modifier = Modifier,
    uiModel: InlineCarouselItemUiModel,
    cardMinHeight: Dp,
    articlesInteractionHelper: ArticlesInteractionHelper,
) {
    Box(
        modifier = modifier.clickable {
            uiModel.url?.let { url ->
                articlesInteractionHelper.onEventFired(ArticleInteractionEvent.CarouselItemClick(url))
            }
        }
    ) {
        Card(
            modifier = Modifier
                .width(CARD_WIDTH)
                .then(if (cardMinHeight != Dp.Unspecified) Modifier.height(cardMinHeight) else Modifier)
                .border(
                    width = 1.dp,
                    color = wpdsColors.outline,
                    shape = RoundedCornerShape(8.dp)
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = wpdsColors.gridCardBg)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    uiModel.imageUrl?.let { imageUrl ->
                        GlideImage(
                            model = imageUrl,
                            contentDescription = uiModel.headline,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        )
                    }
                    Column(modifier = Modifier.padding(12.dp)) {
                        val kickerText = uiModel.kicker?.displayLabel
                        if (!kickerText.isNullOrEmpty()) {
                            Text(
                                text = kickerText.uppercase(),
                                color = wpdsColors.primary,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily(Font(R.font.franklinitcstd_bold)),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                        uiModel.headline?.let { headline ->
                            Text(
                                text = headline,
                                color = wpdsColors.gray20,
                                fontSize = 16.sp,
                                fontFamily = FontFamily(Font(R.font.franklinitcstd_bold)),
                                lineHeight = 20.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class CarouselUiStyle {
    FTS,
    FOR_YOU,
    MOST_READ,
    AUTO_RECIRC,
    SEVEN_LIVE,
    DEFAULT
}

@Preview(showBackground = true)
@Composable
private fun CarouselViewFtsPreview() {
    AndroidClassicTheme {
        CarouselView(
            uiModel = CarouselUiModel(
                label = "From the Source",
                items = listOf(
                    SourceCommentUiModel(
                        url = "https://example.com/1",
                        author = Author(
                            name = "Afhshin Beheshti",
                            expertise = "President of the COVID-19 International Research Team",
                            image = null
                        ),
                        body = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
                        video = null
                    ),
                    SourceCommentUiModel(
                        url = "https://example.com/2",
                        author = Author(
                            name = "Jane Smith",
                            expertise = "Senior Health Correspondent",
                            image = null
                        ),
                        body = "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.",
                        video = null
                    ),
                    SourceCommentUiModel(
                        url = "https://example.com/3",
                        author = Author(
                            name = "John Doe",
                            expertise = "Technology Editor",
                            image = null
                        ),
                        body = "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.",
                        video = null
                    )
                ),
                uiStyle = CarouselUiStyle.FTS
            ),
            articlesInteractionHelper = dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CarouselViewFtsWithVideoPreview() {
    AndroidClassicTheme {
        CarouselView(
            uiModel = CarouselUiModel(
                label = "From the Source w/ Videos",
                items = listOf(
                    SourceCommentUiModel(
                        url = "https://example.com/1",
                        author = Author(
                            name = "Afhshin Beheshti",
                            expertise = "President of the COVID-19 International Research Team",
                            image = null
                        ),
                        body = null,
                        video = Video(
                            duration = 84.15,
                            promoImage = PromoImage(
                                url = "https://d1i4t8bqe7zgj6.cloudfront.net/03-31-2022/t_a0b2f41cd476490dbabf137462159584_name_Screen_Shot_2022_03_31_at_12_12_44_PM.png",
                                aspectRatio = 1.5
                            ),
                            transcript = Transcript(
                                text = "Hi, I'm Afhshin Beheshti and I'm the President of the COVID-19 International Research Team..."
                            )
                        )
                    ),
                    SourceCommentUiModel(
                        url = "https://example.com/1",
                        author = Author(
                            name = "Afhshin Beheshti",
                            expertise = "President of the COVID-19 International Research Team",
                            image = null
                        ),
                        body = null,
                        video = Video(
                            duration = 84.15,
                            promoImage = PromoImage(
                                url = "https://d1i4t8bqe7zgj6.cloudfront.net/03-31-2022/t_a0b2f41cd476490dbabf137462159584_name_Screen_Shot_2022_03_31_at_12_12_44_PM.png",
                                aspectRatio = 1.5
                            ),
                            transcript = Transcript(
                                text = "Hi, I'm Afhshin Beheshti and I'm the President of the COVID-19 International Research Team..."
                            )
                        )
                    ),
                    SourceCommentUiModel(
                        url = "https://example.com/2",
                        author = Author(
                            name = "Jane Smith",
                            expertise = "Senior Health Correspondent",
                            image = null
                        ),
                        body = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam.",
                        video = null
                    )
                ),
                uiStyle = CarouselUiStyle.FTS
            ),
            articlesInteractionHelper = dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CarouselViewSingleItemPreview() {
    AndroidClassicTheme {
        CarouselView(
            uiModel = CarouselUiModel(
                label = "From the Source - Single Item",
                items = listOf(
                    SourceCommentUiModel(
                        url = "https://example.com/1",
                        author = Author(
                            name = "Afhshin Beheshti",
                            expertise = "President of the COVID-19 International Research Team",
                            image = null
                        ),
                        body = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
                        video = null
                    )
                ),
                uiStyle = CarouselUiStyle.FTS
            ),
            articlesInteractionHelper = dummyArticlesInteractionHelper
        )
    }
}



