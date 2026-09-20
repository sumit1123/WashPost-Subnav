// Copyright (c) 2025 The Washington Post. All rights reserved.
package com.wapo.flagship.features.articles2.viewholders

import android.content.res.Configuration
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.secondsToDuration
import com.wapo.flagship.external.toDp
import com.wapo.flagship.features.comments.model.Author
import com.wapo.flagship.features.comments.model.SourceComment
import com.wapo.flagship.features.articles.R
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.models.FtsCarousel
import com.wapo.flagship.features.comments.model.PromoImage
import com.wapo.flagship.features.comments.model.Transcript
import com.wapo.flagship.features.comments.model.Video
import com.wapo.flagship.features.subscribebanner.state.BannerLifecycleEvent
import com.wapo.flagship.util.tracking.Measurement
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.wpdsColors
import com.wpds.theme.wpdsColorsLight
import kotlinx.coroutines.launch

class FtsCarouselRecirculationViewHolder(
    private val composeView: ComposeView,
    private val articlesInteractionHelper: ArticlesInteractionHelper
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<FtsCarousel>(composeView) {

    private var cardWidth = 0.dp
    private var containerHorizontalPadding = 0.dp
    private var carouselHorizontalPadding = 0.dp
    private var carouselContentPadding = 0.dp
    private var arrowsOffset = 0.dp
    private var canDisplayArrows = false

    override fun bind(
        item: FtsCarousel,
        position: Int,
    ) {
        super.bind(item, position)

        composeView.setContent {
            val parentViewWidth = remember { mutableIntStateOf(composeView.measuredWidth) }
            AndroidClassicTheme {
                Surface(modifier = Modifier
                    .onGloballyPositioned {
                        parentViewWidth.intValue = composeView.measuredWidth
                    }
                    .fillMaxWidth(),
                    color = Color.Unspecified) {
                    // Obtain the window size class and receive updates when it changes
                    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
                    CarouselContainer(item, windowSizeClass, parentViewWidth)
                }
            }
        }
    }

    @Composable
    fun CarouselContainer(
        item: FtsCarousel,
        windowSizeClass: WindowSizeClass,
        parentViewWidth: MutableIntState
    ) {
        // Skip rendering container when there are no comments
        if (item.sourceComments.isEmpty()) return

        val coroutineScope = rememberCoroutineScope()
        val listState = rememberLazyListState()
        val leftArrowDisplayState = remember { mutableStateOf(false) }
        val rightArrowDisplayState = remember { mutableStateOf(false) }
        val isCompactWidth = isWindowWidthSizeClassCompact(windowSizeClass)
        val cardMinHeight = remember { mutableIntStateOf(Int.MIN_VALUE) }

        // Arrows display states based on list state
        LaunchedEffect(listState) {
            snapshotFlow { listState.canScrollBackward }.collect { canScrollBackward ->
                leftArrowDisplayState.value = canScrollBackward
            }
        }
        LaunchedEffect(listState) {
            snapshotFlow { listState.canScrollForward }.collect { canScrollForward ->
                rightArrowDisplayState.value = canScrollForward
            }
        }
        // Arrows events
        val scrollBackwardEvent = {
            coroutineScope.launch {
                if (listState.firstVisibleItemIndex > 0)
                    listState.animateScrollToItem(listState.firstVisibleItemIndex - 1)
                else
                    listState.animateScrollToItem(0)
            }
        }
        val scrollForwardEvent = {
            coroutineScope.launch {
                listState.animateScrollToItem(listState.firstVisibleItemIndex + 1)
            }
        }
        // Minimum Card Height Event (SubComposeCarousel calls this event while measuring items)
        val minCardHeightUpdateEvent: (Int) -> Unit = { newMinHeight ->
            if (cardMinHeight.intValue < newMinHeight) cardMinHeight.intValue = newMinHeight
        }
        // Event to fire when user clicks on any carousel item.
        val cardClickEvent: (SourceComment, Int) -> Unit = { comment, position ->
            comment.url?.let { commentIdUrl ->
                val miscellanyPrefix = if (comment.video != null) {
                    Measurement.FTS_BOTTOM_VIDEO_
                } else {
                    Measurement.FTS_BOTTOM_LINK_
                }
                articlesInteractionHelper.onEventFired(
                    ArticleInteractionEvent.FromTheSourceTapEvent(
                        commentIdUrl,
                        miscellanyPrefix + (position + 1),
                        comment.author?.name
                    )
                )
            }
        }

        // Initialize container and carousel paddings.
        InitDimensions(item, isCompactWidth, parentViewWidth)

        Column {
            Column(
                modifier = Modifier
                    .padding(horizontal = containerHorizontalPadding),
            ) {
                // Top Divider
                HorizontalDivider(
                    color = colorResource(com.washingtonpost.android.recirculation.R.color.carousel_divider_color),
                    thickness = 1.dp
                )
                Spacer(Modifier.height(8.dp))
                // Label
                Label(item.label)
                Spacer(Modifier.height(22.dp))
            }
            Box {
                SubComposeCarousel(
                    item.sourceComments,
                    parentViewWidth
                ) { minCardHeightUpdateEvent.invoke(it) }
                // Carousel
                Carousel(
                    item.sourceComments,
                    cardMinHeight.intValue,
                    listState,
                    parentViewWidth,
                    cardClickEvent
                )
                // Arrow Buttons. Display on non COMPACT class sizes
                // and carousel has more items than the container size (check InitDimensions composable).
                if (!isCompactWidth && canDisplayArrows) {
                    LeftArrowButton(leftArrowDisplayState) { scrollBackwardEvent.invoke() }
                    RightArrowButton(rightArrowDisplayState) { scrollForwardEvent.invoke() }
                }
            }
        }
    }

    @Composable
    private fun InitDimensions(
        item: FtsCarousel,
        isCompactWidth: Boolean,
        parentWidth: MutableIntState
    ) {
        val deviceDensity = itemView.resources.displayMetrics.density
        // Calculate article margin.
        // isCompactWidth is true for phones portrait mode and false for all other sizes.
        val articleMargin = if (isCompactWidth)
            dimensionResource(id = com.washingtonpost.android.articles.R.dimen.articles_medium_margin)
        else
            AppContextUtils.calculateArticleMargin().toDp(deviceDensity).dp
        // Calculate card width based on the article margin and available width of the window/device.
        val defaultCardWidth = dimensionResource(com.washingtonpost.android.recirculation.R.dimen.carousel_article_style_card_width)
        val defaultCardSpacing = dimensionResource(com.washingtonpost.android.recirculation.R.dimen.carousel_article_style_horizontal_space)
        val containerWidth =
            if (composeView.measuredWidth == 0) AppContextUtils.getDeviceWidthInDp() else parentWidth.intValue / deviceDensity
        // Check cards can fit into the container with their fixed size. If they can fit, make them span to the full container width.
        val containerWidthExcludingMargins = containerWidth.dp - (articleMargin * 2)
        val requiredWidthForCards = item.sourceComments.size.times(defaultCardWidth.value)
        val requiredWidthForSpacingBetweenCards =
            (item.sourceComments.size - 1).times(defaultCardSpacing.value)
        cardWidth =
            if (requiredWidthForCards + requiredWidthForSpacingBetweenCards <= containerWidthExcludingMargins.value) {
                canDisplayArrows = false
                ((containerWidthExcludingMargins.value - requiredWidthForSpacingBetweenCards) / item.sourceComments.size).dp
            } else {
                canDisplayArrows = true
                defaultCardWidth
            }
        // Calculate container and carousel paddings
        if (isCompactWidth) {
            // container padding is applicable only to non carousel items.
            containerHorizontalPadding = articleMargin
            if (item.sourceComments.size == 1) {
                carouselHorizontalPadding = articleMargin
                carouselContentPadding = 0.dp
            } else {
                carouselHorizontalPadding = 0.dp
                carouselContentPadding = articleMargin
            }
        } else {
            val arrowsOffsetFromSideMargin = 40.dp
            arrowsOffset = articleMargin - arrowsOffsetFromSideMargin
            containerHorizontalPadding = articleMargin
            carouselHorizontalPadding = articleMargin
            carouselContentPadding = 0.dp
        }
    }

    @Composable
    private fun Label(label: String?) {
        if (label.isNullOrEmpty()) return
        Text(
            text = label,
            color = wpdsColors.gray20,
            fontSize = 18.sp,
            letterSpacing = 0.0.sp,
            fontFamily = FontFamily(Font(com.wpds.wpds.R.font.franklinitcstd_bold)),
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()

        )
    }

    @Composable
    private fun SubComposeCarousel(
        sourceComments: List<SourceComment>,
        parentWidth: MutableIntState,
        minHeight: (Int) -> Unit
    ) {
        if (sourceComments.isEmpty()) return

        sourceComments.forEachIndexed { index, comment ->
            SubComposeCarouselCard(
                minHeight = { minCardHeight -> minHeight.invoke(minCardHeight) },
                content = {
                    CarouselCard(
                        comment = comment,
                        positionInCarousel = index,
                        parentWidth = parentWidth,
                        onItemClicked = { comment, index -> {} },
                        carouselHasVideos = sourceComments.any { it.video != null },
                        isSubComposingLayout = true
                    )
                }
            )
        }
    }

    @Composable
    private fun SubComposeCarouselCard(
        minHeight: (Int) -> Unit,
        content: @Composable () -> Unit,
    ) {
        SubcomposeLayout { constraints ->
            val placeable = subcompose(1, content).firstOrNull()?.measure(constraints)
            minHeight.invoke(placeable?.height ?: 0)
            layout(0, 0) {}
        }
    }

    @Composable
    private fun Carousel(
        sourceComments: List<SourceComment>,
        cardMinHeight: Int,
        listState: LazyListState,
        parentWidth: MutableIntState,
        onItemClicked: (SourceComment, Int) -> Unit
    ) {
        LazyRow(
            modifier = Modifier
                .padding(horizontal = carouselHorizontalPadding),
            contentPadding = PaddingValues(horizontal = carouselContentPadding),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(com.washingtonpost.android.recirculation.R.dimen.carousel_article_style_horizontal_space)),
            verticalAlignment = Alignment.CenterVertically,
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
        ) {
            items(count = sourceComments.size) { index ->
                CarouselCard(sourceComments[index], index, cardMinHeight, parentWidth, onItemClicked, sourceComments.any { it.video != null })
            }
        }
    }

    @Composable
    private fun CarouselCard(
        comment: SourceComment,
        positionInCarousel: Int,
        cardMinHeight: Int = -1,
        parentWidth: MutableIntState,
        onItemClicked: (SourceComment, Int) -> Unit,
        carouselHasVideos: Boolean,
        isSubComposingLayout: Boolean = false
    ) {
        val cardHeightInDp = if (isSubComposingLayout) {
            Dp.Unspecified
        } else {
            (cardMinHeight / AppContextUtils.getDeviceDensity()).dp
        }

        /* Consume the parent width value so that recomposition is triggered when the parent view
           changes (orientation change, etc.). */
        parentWidth.intValue.dp

        Box(
            modifier = Modifier
                .clickable(
                    onClick = {
                        onItemClicked.invoke(comment, positionInCarousel)
                    }
                )
        ) {
            Card(
                modifier = Modifier
                    .width(cardWidth)
                    .heightIn(min = cardHeightInDp)
                    .border(
                        width = 1.dp,
                        color = wpdsColors.outline,
                        shape = RoundedCornerShape(8.dp)
                    ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(containerColor = wpdsColors.gridCardBg)
            ) {
                Column { // Content
                    Box(modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp)) {
                        if (comment.video != null) {
                            VideoItem(comment.video)
                        } else if (comment.body != null) {
                            Comment(comment.body, carouselHasVideos)
                        }
                    }
                    Author(comment.author)
                }
            }
        }
    }

    @Composable
    private fun Comment(comment: String?, carouselHasVideos: Boolean) {
        if (comment.isNullOrEmpty()) return
        val annotatedString = AnnotatedString.fromHtml(comment)
        Text(
            text = annotatedString,
            color = wpdsColors.gray20,
            fontSize = 16.sp,
            letterSpacing = 0.0.sp,
            fontFamily = FontFamily(Font(com.wpds.wpds.R.font.franklinitcstd_light)),
            lineHeight = 20.sp,
            minLines = 3,
            maxLines = if (carouselHasVideos) 5 else 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }

    @OptIn(ExperimentalGlideComposeApi::class)
    @Composable
    private fun VideoItem(video: Video) {
        Row {
            PromoImage(video)
            if (video.transcript != null) {
                Spacer(modifier = Modifier.width(12.dp))
                Transcript(video.transcript)
            }
        }
    }

    @Composable
    private fun PromoImage(video: Video) {
        val sizeModifier = if (video.transcript != null) {
            Modifier.height(100.dp).width(100.dp)
        } else {
            Modifier.height(100.dp).fillMaxWidth()
        }
        Box(
            modifier = sizeModifier
        ) {
            VideoThumbnail(sizeModifier, video.promoImage?.url)
            Column { // Play icon and timestamp
                Spacer(modifier = Modifier.weight(1f))
                Row(modifier = Modifier.padding(12.dp)) {
                    Icon(
                        painter = painterResource(id = com.wapo.view.R.drawable.play_icon_btn),
                        contentDescription = stringResource(R.string.play_button),
                        tint = wpdsColorsLight.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    secondsToDuration(video.duration)?.let {
                        Text(
                            text = it,
                            color = wpdsColorsLight.onPrimary,
                            fontSize = 14.sp,
                            letterSpacing = 0.0.sp,
                            fontFamily = FontFamily(Font(com.wpds.wpds.R.font.franklinitcstd_light)),
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalGlideComposeApi::class)
    @Composable
    private fun VideoThumbnail(sizeModifier: Modifier, url: String?) {
        Box(
            modifier = sizeModifier
                .background(
                    shape = RoundedCornerShape(8.dp),
                    color = wpdsColors.alpha400,
                )
        ) {
            GlideImage(
                model = url,
                contentDescription = stringResource(R.string.video_thumbnail),
                contentScale = ContentScale.Crop,
                modifier = sizeModifier
                    .clip(RoundedCornerShape(8.dp))
            )
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
    }

    @Composable
    private fun Transcript(transcript: Transcript?) {
        val transcriptText = transcript?.text ?: return
        Text(
            text = transcriptText,
            color = wpdsColors.onSurface,
            fontSize = 16.sp,
            letterSpacing = 0.0.sp,
            fontFamily = FontFamily(Font(com.wpds.wpds.R.font.franklinitcstd_light)),
            lineHeight = 20.sp,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }

    @Composable
    private fun Author(author: Author?) {
        author ?: return
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
            if (!author.name.isNullOrEmpty()) {
                Text(
                    text = author.name,
                    color = wpdsColors.onSurfaceSubtle,
                    fontSize = 16.sp,
                    letterSpacing = 0.0.sp,
                    fontFamily = FontFamily(Font(com.wpds.wpds.R.font.franklinitcstd_light)),
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
                    fontFamily = FontFamily(Font(com.wpds.wpds.R.font.franklinitcstd_light)),
                    lineHeight = 17.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    @Composable
    private fun BoxScope.LeftArrowButton(
        displayState: MutableState<Boolean>,
        onClick: () -> Unit
    ) {
        if (!displayState.value) return
        OutlinedButton(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = arrowsOffset),
            colors = ButtonColors(
                containerColor = Color.Transparent,
                contentColor = wpdsColors.primary,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = wpdsColors.primary
            ),
            border = BorderStroke(0.dp, Color.Transparent),
            onClick = onClick,
            content = {
                Image(
                    painter = painterResource(com.washingtonpost.android.recirculation.R.drawable.carousel_backward_arrow),
                    contentDescription = stringResource(R.string.left_arrow_button)
                )
            }
        )
    }

    @Composable
    private fun BoxScope.RightArrowButton(
        displayState: MutableState<Boolean>,
        onClick: () -> Unit
    ) {
        if (!displayState.value) return
        OutlinedButton(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = -arrowsOffset),
            colors = ButtonColors(
                containerColor = Color.Transparent,
                contentColor = wpdsColors.primary,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = wpdsColors.primary
            ),
            border = BorderStroke(0.dp, Color.Transparent),
            onClick = onClick,
            content = {
                Image(
                    painter = painterResource(com.washingtonpost.android.recirculation.R.drawable.carousel_forward_arrow),
                    contentDescription = stringResource(R.string.right_arrow_button)
                )
            }
        )
    }

    /**
     * Returns true for phones portrait mode
     *         false for all other sizes.
     */
    private fun isWindowWidthSizeClassCompact(windowSizeClass: WindowSizeClass): Boolean {
        return windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT
    }
}

@Composable
@Preview(
    showBackground = true,
    device = "id:pixel_9",
    uiMode = Configuration.UI_MODE_TYPE_NORMAL// or Configuration.UI_MODE_NIGHT_YES
)
private fun PreviewFtsCarouselRecirculationViewHolder() {
    val comment1 = SourceComment(
        url = "https://example.com/1",
        author = Author(
            name = "Afhshin Beheshti",
            expertise = "President of the COVID-19 international Research Team",
            image = null
        ),
        body = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
        video = Video(
            duration = 84.15,
            promoImage = PromoImage(
                url = "https://d1i4t8bqe7zgj6.cloudfront.net/03-31-2022/t_a0b2f41cd476490dbabf137462159584_name_Screen_Shot_2022_03_31_at_12_12_44_PM.png",
                aspectRatio = 1.5
            ),
            transcript = Transcript(
                text = "Hi, I'm Afhshin Beheshti and I'm, the President of the COVID-19 international Research Team ..."
            )
        ),
        replies = null,
        replyCount = 0,
        created = "",
    )
    val videoNoPromo = comment1.video?.copy(promoImage = null)
    val videoNoTranscript = comment1.video?.copy(transcript = null)
    val comment2 = comment1.copy(video = videoNoTranscript)
    val comment3 = comment1.copy(video = videoNoPromo)
    val comment4 = comment1.copy(video = null)

    val dummyHelper = object : ArticlesInteractionHelper {
        override fun onEventFired(event: ArticleInteractionEvent) {}
    }
    val composeView = ComposeView(LocalContext.current).apply {
        layoutParams =
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
    AndroidClassicTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = wpdsColors.wallPrimaryBg,
        ) {
            Column {
                FtsCarouselRecirculationViewHolder(composeView, dummyHelper).CarouselContainer(
                    item = FtsCarousel(
                        label = "From the Source w/ videos",
                        type = "fts_carousel"
                    ).apply {
                        sourceComments = listOf(comment1, comment2, comment3, comment4)
                    },
                    windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass,
                    parentViewWidth = remember { mutableIntStateOf(800) }
                )
                Spacer(modifier = Modifier.height(24.dp))
                FtsCarouselRecirculationViewHolder(composeView, dummyHelper).CarouselContainer(
                    item = FtsCarousel(
                        label = "From the Source one video",
                        type = "fts_carousel"
                    ).apply {
                        sourceComments = listOf(comment2)
                    },
                    windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass,
                    parentViewWidth = remember { mutableIntStateOf(800) }
                )
                Spacer(modifier = Modifier.height(24.dp))
                FtsCarouselRecirculationViewHolder(composeView, dummyHelper).CarouselContainer(
                    item = FtsCarousel(
                        label = "From the Source no videos",
                        type = "fts_carousel"
                    ).apply {
                        sourceComments = listOf(comment4, comment4, comment4, comment4)
                    },
                    windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass,
                    parentViewWidth = remember { mutableIntStateOf(800) }
                )
            }
        }
    }
}
