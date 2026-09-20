package com.wapo.flagship.features.articles3.views

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wapo.adsinf.BannerAdView
import com.wapo.adsinf.models.AdsModel
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.articles2.models.Item
import com.wapo.flagship.features.articles2.models.deserialized.ExpandCollapseCard
import com.wapo.flagship.features.articles2.models.deserialized.SanitizedHtml
import com.wapo.flagship.features.articles2.states.ArticleContentState
import com.wapo.flagship.features.posttv.VideoManager2
import kotlinx.coroutines.flow.StateFlow
import com.wapo.flagship.features.articles3.models.ui.AdUiModel
import com.wapo.flagship.features.articles3.models.ui.ArticleItemUiModel
import com.wapo.flagship.features.articles3.models.ui.AudioUiModel
import com.wapo.flagship.features.articles3.models.ui.AuthorInfoUiModel
import com.wapo.flagship.features.articles3.models.ui.BlockQuoteUiModel
import com.wapo.flagship.features.articles3.models.ui.BylineUiModel
import com.wapo.flagship.features.articles3.models.ui.CarouselUiModel
import com.wapo.flagship.features.articles3.models.ui.CommentsUiModel
import com.wapo.flagship.features.articles3.models.ui.ContextBoxUiModel
import com.wapo.flagship.features.articles3.models.ui.CorrectionUiModel
import com.wapo.flagship.features.articles3.models.ui.DateUiModel
import com.wapo.flagship.features.articles3.models.ui.DeckUiModel
import com.wapo.flagship.features.articles3.models.ui.DividerUiModel
import com.wapo.flagship.features.articles3.models.ui.ElevatedBylineUiModel
import com.wapo.flagship.features.articles3.models.ui.ElementGroupUiModel
import com.wapo.flagship.features.articles3.models.ui.ExpandCollapseUiModel
import com.wapo.flagship.features.articles3.models.ui.ForYouCarouselItemUiModel
import com.wapo.flagship.features.articles3.models.ui.GalleryUiModel
import com.wapo.flagship.features.articles3.models.ui.HumanAudioUiModel
import com.wapo.flagship.features.articles3.models.ui.ImageUiModel
import com.wapo.flagship.features.articles3.models.ui.InlineAlertToggleUiModel
import com.wapo.flagship.features.articles3.models.ui.InlineMessageUiModel
import com.wapo.flagship.features.articles3.models.ui.PodcastUiModel
import com.wapo.flagship.features.articles3.models.ui.InterstitialLinkUiModel
import com.wapo.flagship.features.articles3.models.ui.KickerUiModel
import com.wapo.flagship.features.articles3.models.ui.LinkButtonUiModel
import com.wapo.flagship.features.articles3.models.ui.ListUiModel
import com.wapo.flagship.features.articles3.models.ui.LiveOutcomeUiModel
import com.wapo.flagship.features.articles3.models.ui.PdfUiModel
import com.wapo.flagship.features.articles3.models.ui.PinUiModel
import com.wapo.flagship.features.articles3.models.ui.PullQuoteUiModel
import com.wapo.flagship.features.articles3.models.ui.SanitizedHtmlUiModel
import com.wapo.flagship.features.articles3.models.ui.SourceCommentUiModel
import com.wapo.flagship.features.articles3.models.ui.TableUiModel
import com.wapo.flagship.features.articles3.models.ui.TaglineUiModel
import com.wapo.flagship.features.articles3.models.ui.TitleUiModel
import com.wapo.flagship.features.articles3.models.ui.VideoUiModel
import com.wapo.flagship.features.articles3.models.ui.WebEmbedUiModel
import com.wapo.flagship.features.comments.model.Author
import com.wapo.flagship.features.comments.model.PromoImage
import com.wapo.flagship.features.comments.model.Transcript
import com.wapo.flagship.features.comments.model.Video
import com.wapo.flagship.features.articles2.models.ArticleInlineMessage
import com.wapo.flagship.features.articles2.models.deserialized.KickerImage
import com.wapo.flagship.features.articles3.models.ui.BlockQuoteAttributionUiModel
import com.wapo.flagship.features.articles3.models.ui.MimeType
import com.wapo.flagship.features.articles3.models.ui.QuoteUiModel
import com.wapo.flagship.features.articles3.models.ui.WidthFactor
import com.wapo.flagship.features.subscribebanner.state.BannerEvent
import com.wapo.flagship.features.subscribebanner.state.BannerLifecycleEvent
import com.wapo.view.web_embeds.EmbedJSInterface
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.wpdsColors
import java.util.concurrent.TimeUnit
import com.washingtonpost.userhistory.viewmodel.UserHistoryViewModel
import com.washingtonpost.foryou.viewmodel.ForYouActivityViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.map

@Composable
fun ArticleContentView(
    uiState: ArticleContentState,
    articleInlineMessage: ArticleInlineMessage?,
    articlesInteractionHelper: ArticlesInteractionHelper,
    onPageExpandChanged: (Boolean) -> Unit = {},
    onScrollStarted: () -> Unit = {},
    onScrollStopped: (List<Int>) -> Unit = {},
    activeVideoIds: StateFlow<Set<String>>? = null,
    videoManager2: VideoManager2? = null,
    resolveVideoStreamId: ((String) -> String?)? = null,
    userHistoryViewModel: UserHistoryViewModel? = null,
    forYouActivityViewModel: ForYouActivityViewModel? = null,
    onCarouselVisibilityChanged: (Int, Boolean) -> Unit = { _, _ -> },
    onScrollProgressChanged: (Float) -> Unit = {},
    initialScrollId: String? = null,
    adsMode: AdsModel,
    isCurrentPage: Boolean,
    webEmbedSettings: WebEmbedSettings,
    onContentReady: () -> Unit = {},
) {
    if (uiState is ArticleContentState.Success) {
        val hasReportedContentReady = remember(uiState.article.contenturl) {
            mutableStateOf(false)
        }
        val expandCollapseItems = mutableMapOf<String?, Item>()
        uiState.article.items?.forEach { item ->
            when {
                item is ExpandCollapseCard -> expandCollapseItems[item.group] = item
                item is SanitizedHtml && item.truncate != null && item.state != null ->
                    expandCollapseItems["truncate-${item.content.hashCode()}"] = item
            }
        }
        Content(
            items = uiState.uiItems,
            sourceExpandCollapseItems = expandCollapseItems,
            articleInlineMessage = articleInlineMessage,
            articlesInteractionHelper = articlesInteractionHelper,
            onPageExpandChanged = onPageExpandChanged,
            onScrollStarted = onScrollStarted,
            onScrollStopped = onScrollStopped,
            activeVideoIds = activeVideoIds,
            videoManager2 = videoManager2,
            resolveVideoStreamId = resolveVideoStreamId,
            userHistoryViewModel = userHistoryViewModel,
            forYouActivityViewModel = forYouActivityViewModel,
            onCarouselVisibilityChanged = onCarouselVisibilityChanged,
            onScrollProgressChanged = onScrollProgressChanged,
            initialScrollId = initialScrollId,
            adsMode = adsMode,
            isCurrentPage = isCurrentPage,
            webEmbedSettings = webEmbedSettings,
            onContentReady = {
                if (!hasReportedContentReady.value) {
                    hasReportedContentReady.value = true
                    onContentReady()
                }
            },
        )
    }
}

@Composable
private fun Content(
    items: List<ArticleItemUiModel>,
    sourceExpandCollapseItems: Map<String?, Item>? = null,
    articleInlineMessage: ArticleInlineMessage?,
    articlesInteractionHelper: ArticlesInteractionHelper,
    onPageExpandChanged: (Boolean) -> Unit,
    onScrollStarted: () -> Unit,
    onScrollStopped: (List<Int>) -> Unit,
    activeVideoIds: StateFlow<Set<String>>? = null,
    videoManager2: VideoManager2? = null,
    resolveVideoStreamId: ((String) -> String?)? = null,
    userHistoryViewModel: UserHistoryViewModel? = null,
    forYouActivityViewModel: ForYouActivityViewModel? = null,
    onCarouselVisibilityChanged: (Int, Boolean) -> Unit = { _, _ -> },
    onScrollProgressChanged: (Float) -> Unit = {},
    initialScrollId: String? = null,
    adsMode: AdsModel,
    isCurrentPage: Boolean,
    webEmbedSettings: WebEmbedSettings,
    onContentReady: () -> Unit,
) {
    val listState = rememberLazyListState()
    val didInitialScroll = remember(initialScrollId) { mutableStateOf(false) }
    val inlineMessageIndex = items.indexOfFirst { it is InlineMessageUiModel }
    val resolvedInlineMessage = articleInlineMessage
        ?: (items.getOrNull(inlineMessageIndex) as? InlineMessageUiModel)?.articleInlineMessage

    TrackInlineMessageImpression(
        listState = listState,
        itemIndex = inlineMessageIndex,
        message = resolvedInlineMessage,
        isCurrentPage = isCurrentPage,
        onLifecycleEvent = { event ->
            articlesInteractionHelper.onEventFired(
                ArticleInteractionEvent.InlineMessageBannerEvent(
                    BannerEvent.ImpressionEvent(event)
                )
            )
        }
    )

    LaunchedEffect(items, initialScrollId) {
        if (didInitialScroll.value || initialScrollId == null) return@LaunchedEffect

        val initialScrollIndex = items.indexOfFirst { item ->
            when (item) {
                is SanitizedHtmlUiModel -> item.arcId == initialScrollId
                is ListUiModel -> item.arcId == initialScrollId
                else -> false
            }
        }

        if (initialScrollIndex >= 0) {
            if (initialScrollIndex > 0) {
                listState.scrollToItem(initialScrollIndex)
            }
            didInitialScroll.value = true
        }
    }

    val adViewCache = remember { mutableMapOf<Int, BannerAdView>() }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .map { firstVisibleItemIndex ->
                // Expand as soon as user scrolls past the header
                firstVisibleItemIndex >= 1
            }
            .distinctUntilChanged()
            .collect(onPageExpandChanged)
    }

    LaunchedEffect(listState) {
        var didStartScroll = false
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { isScrolling ->
                if (isScrolling) {
                    didStartScroll = true
                    onScrollStarted()
                } else if (didStartScroll) {
                    val layoutInfo = listState.layoutInfo
                    val fullyVisibleIndices =
                        layoutInfo.visibleItemsInfo
                            .filter { info ->
                                info.offset >= layoutInfo.viewportStartOffset &&
                                        info.offset + info.size <= layoutInfo.viewportEndOffset
                            }
                            .map { it.index }
                    onScrollStopped(fullyVisibleIndices)
                }
            }
    }

    // Track carousel visibility for fy_viewed timers
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.map { it.index } }
            .distinctUntilChanged()
            .collect { visibleIndices ->
                // Check each carousel to see if it's ≥50% visible
                items.forEachIndexed { index, item ->
                    if (item is CarouselUiModel) {
                        val visibleItemInfo = listState.layoutInfo.visibleItemsInfo
                            .firstOrNull { it.index == index }
                        if (visibleItemInfo != null) {
                            // Calculate visibility percentage of this carousel item
                            val itemHeight = visibleItemInfo.size.toFloat()
                            val visibleTop = maxOf(
                                visibleItemInfo.offset.toFloat(),
                                listState.layoutInfo.viewportStartOffset.toFloat()
                            )
                            val visibleBottom = minOf(
                                (visibleItemInfo.offset + visibleItemInfo.size).toFloat(),
                                listState.layoutInfo.viewportEndOffset.toFloat()
                            )
                            val visibleHeight = maxOf(0f, visibleBottom - visibleTop)
                            val visibilityPercentage = if (itemHeight > 0) {
                                (visibleHeight / itemHeight) * 100
                            } else {
                                0f
                            }
                            // Call callback when ≥50% visible
                            val isVisible = visibilityPercentage >= 50
                            onCarouselVisibilityChanged(index, isVisible)
                        } else {
                            // Carousel is not in visible items, so it's not visible
                            onCarouselVisibilityChanged(index, false)
                        }
                    }
                }
            }
    }

    // Treat comments as the end of the article for scroll progress. Recirculation
    // modules can be appended after comments and should not extend the article.
    val scrollProgressEndIndex = remember(items) {
        items.indexOfLast { it is CommentsUiModel }
            .takeIf { it >= 0 }
            ?: items.lastIndex
    }

    // Scroll progress
    LaunchedEffect(listState, isCurrentPage, onScrollProgressChanged, scrollProgressEndIndex) {
        if (!isCurrentPage) return@LaunchedEffect
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val firstVisibleItemIndex = listState.firstVisibleItemIndex
            val firstVisibleItem = layoutInfo.visibleItemsInfo
                .firstOrNull { it.index == firstVisibleItemIndex }
            val endItemRevealFraction = layoutInfo.visibleItemsInfo
                .firstOrNull { it.index == scrollProgressEndIndex }
                ?.let { endItem ->
                    (
                        (layoutInfo.viewportEndOffset - endItem.offset).toFloat() /
                            endItem.size.coerceAtLeast(1)
                    ).coerceIn(0f, 1f)
                }

            calculateScrollProgress(
                endItemIndex = scrollProgressEndIndex,
                endItemRevealFraction = endItemRevealFraction,
                firstVisibleItemIndex = firstVisibleItemIndex,
                firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset,
                firstVisibleItemSize = firstVisibleItem?.size ?: 0,
                canScrollBackward = listState.canScrollBackward,
                canScrollForward = listState.canScrollForward,
            )
        }
            .map { progress -> ((progress * 1000).toInt() / 1000f) }
            .distinctUntilChanged()
            .collect(onScrollProgressChanged)
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(top = 16.dp, bottom = 64.dp),
        modifier = Modifier
            .background(wpdsColors.gray700)
            .onGloballyPositioned { onContentReady() },
    ) {
        itemsIndexed(
            items = items,
            key = { index, item ->
                when (item) {
                    is WebEmbedUiModel -> {
                        val url = item.url.orEmpty()
                        val oembedHash = item.oembed?.hashCode() ?: 0
                        val subtype = item.subtype.orEmpty()
                        "embed_${index}_${url}_${oembedHash}_${subtype}"
                    }

                    else -> "${item.javaClass.simpleName}_$index"
                }
            }
        ) { index, item ->
            val config = LocalConfiguration.current
            val spacing = getItemSpacing(item, config)
            Column(modifier = Modifier.padding(horizontal = spacing.horizontal)) {
                when (item) {
                    // Keep these alphabetical
                    is AdUiModel -> {
                        if (adsMode is AdsModel.Enabled) {
                            AdView(item, index, adViewCache)
                        }
                    }

                    is AudioUiModel -> AudioView(item, articlesInteractionHelper)
                    is AuthorInfoUiModel -> AuthorInfoView(item, articlesInteractionHelper)
                    is BlockQuoteUiModel -> BlockQuoteView(item, articlesInteractionHelper)
                    is BylineUiModel -> BylineView(item, articlesInteractionHelper)
                    is CarouselUiModel -> CarouselView(
                        item,
                        articlesInteractionHelper,
                        userHistoryViewModel = userHistoryViewModel,
                        forYouActivityViewModel = forYouActivityViewModel,
                        onCarouselVisibilityChanged = { isVisible ->
                            onCarouselVisibilityChanged(index, isVisible)
                        }
                    )

                    is CommentsUiModel -> CommentsView(item, articlesInteractionHelper)
                    is ContextBoxUiModel -> ContextBoxView(item, articlesInteractionHelper)
                    is CorrectionUiModel -> CorrectionView(item, articlesInteractionHelper)
                    is DateUiModel -> DateView(item)
                    is DeckUiModel -> DeckView(item)
                    is DividerUiModel -> DividerView(item)
                    is ElevatedBylineUiModel -> ElevatedBylineView(item, articlesInteractionHelper)
                    is ElementGroupUiModel -> {
                        when(item) {
                            is ElementGroupUiModel.ElementGroupLinkBoxUiModel ->
                                ElementGroupLinkBoxView(item, articlesInteractionHelper)
                            is ElementGroupUiModel.ElementGroupBlockQuoteUiModel ->
                                BlockQuoteView(item, articlesInteractionHelper)
                        }

                    }
                    is ExpandCollapseUiModel -> {
                        val targetItem = item.group?.let { sourceExpandCollapseItems?.get(it) }
                        ExpandCollapseView(
                            uiModel = item,
                            articlesInteractionHelper = articlesInteractionHelper,
                            itemToToggle = targetItem,
                        )
                    }

                    is GalleryUiModel -> GalleryView(item, articlesInteractionHelper)
                    is HumanAudioUiModel -> HumanAudioView(item, articlesInteractionHelper)
                    is ImageUiModel -> {
                        val captionHorizontalSpacing =
                            (getDefaultHorizontalItemSpacing(config) - spacing.horizontal)
                                .coerceAtLeast(0.dp)
                        val captionPadding = PaddingValues(
                            top = 8.dp,
                            start = captionHorizontalSpacing,
                            end = captionHorizontalSpacing
                        )
                        ImageView(
                            item,
                            articlesInteractionHelper,
                            captionPadding = captionPadding
                        )
                    }
                    is InlineAlertToggleUiModel -> InlineAlertToggleView(
                        item,
                        articlesInteractionHelper
                    )

                    is InlineMessageUiModel -> {
                        val resolvedMessage = articleInlineMessage ?: item.articleInlineMessage
                        if (resolvedMessage?.isEligiblePromo == true) {
                            val visibleItem = item.copy(articleInlineMessage = resolvedMessage).apply {
                                isVisible = true
                            }
                            InlineMessageView(visibleItem, articlesInteractionHelper)
                        }
                    }

                    is InterstitialLinkUiModel -> InterstitialLinkView(
                        item,
                        articlesInteractionHelper
                    )

                    is KickerUiModel -> KickerView(item, articlesInteractionHelper)
                    is LinkButtonUiModel -> LinkButtonView(item, articlesInteractionHelper)
                    is ListUiModel -> ListView(item, articlesInteractionHelper)
                    is LiveOutcomeUiModel -> LiveOutcomeView(item)
                    is PinUiModel -> PinView(item)
                    is PodcastUiModel -> PodcastView(item, articlesInteractionHelper)
                    is PullQuoteUiModel -> PullQuoteView(item, articlesInteractionHelper)
                    is QuoteUiModel -> QuoteView(item, articlesInteractionHelper)
                    is SanitizedHtmlUiModel -> SanitizedHtmlView(item, articlesInteractionHelper)
                    is TableUiModel -> TableView(item, articlesInteractionHelper)
                    is TaglineUiModel -> TaglineView(item)
                    is TitleUiModel -> TitleView(item)
                    is VideoUiModel -> {
                        val defaultHorizontalSpacing = getDefaultHorizontalItemSpacing(config)
                        val captionPadding = PaddingValues(
                            top = 8.dp,
                            start = defaultHorizontalSpacing,
                            end = defaultHorizontalSpacing
                        )
                        VideoView(
                            item,
                            articlesInteractionHelper,
                            activeVideoIds = activeVideoIds,
                            videoManager2 = videoManager2,
                            resolveVideoStreamId = resolveVideoStreamId,
                            captionPadding = captionPadding,
                        )
                    }

                    is WebEmbedUiModel -> WebEmbedView(
                        index = index,
                        uiModel = item,
                        onArticleInteractionEvent = articlesInteractionHelper::onEventFired,
                        webEmbedSettings = webEmbedSettings,
                    )

                    is PdfUiModel -> PdfView(item)
                }
            }

            if (index < items.lastIndex) {
                Spacer(modifier = Modifier.height(spacing.bottom))
            }
        }
    }
}

@Composable
private fun TrackInlineMessageImpression(
    listState: LazyListState,
    itemIndex: Int,
    message: ArticleInlineMessage?,
    isCurrentPage: Boolean,
    onLifecycleEvent: (BannerLifecycleEvent) -> Unit
) {
    val currentOnLifecycleEvent = rememberUpdatedState(onLifecycleEvent)
    val attributionInfo = message?.attributionInfo
    val shouldTrack = itemIndex >= 0 &&
            message != null &&
            message.isEligiblePromo == true &&
            !message.title.isNullOrEmpty() &&
            !message.action.isNullOrEmpty() &&
            attributionInfo != null

    LaunchedEffect(listState, itemIndex, attributionInfo?.messageId, shouldTrack, isCurrentPage) {
        if (!shouldTrack || !isCurrentPage || attributionInfo == null) return@LaunchedEffect

        var impressionStarted = false
        try {
            snapshotFlow {
                listState.layoutInfo.visibleItemsInfo.any { it.index == itemIndex }
            }
                .distinctUntilChanged()
                .dropWhile { !it }
                .collect { isVisible ->
                    impressionStarted = isVisible
                    currentOnLifecycleEvent.value(
                        if (isVisible) {
                            BannerLifecycleEvent.StartImpression(attributionInfo)
                        } else {
                            BannerLifecycleEvent.EndImpression(attributionInfo)
                        }
                    )
                }
        } finally {
            if (impressionStarted) {
                currentOnLifecycleEvent.value(
                    BannerLifecycleEvent.EndImpression(attributionInfo)
                )
            }
        }
    }
}

internal fun calculateScrollProgress(
    endItemIndex: Int,
    endItemRevealFraction: Float?,
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    firstVisibleItemSize: Int,
    canScrollBackward: Boolean,
    canScrollForward: Boolean,
): Float {
    if (endItemIndex < 0) return 0f
    if (firstVisibleItemIndex >= endItemIndex) return 1f
    if (!canScrollForward) return 1f
    if (!canScrollBackward) return 0f

    val itemProgress = if (firstVisibleItemSize > 0) {
        firstVisibleItemScrollOffset.toFloat() / firstVisibleItemSize
    } else {
        0f
    }

    val itemBasedProgress =
        ((firstVisibleItemIndex + itemProgress) / endItemIndex.coerceAtLeast(1))
            .coerceIn(0f, 1f)
    val revealFraction = endItemRevealFraction ?: return itemBasedProgress

    return itemBasedProgress + (1f - itemBasedProgress) * revealFraction
}

private data class ArticleItemSpacing(
    val horizontal: Dp,
    val bottom: Dp
)

private fun getItemSpacing(item: ArticleItemUiModel, config: Configuration): ArticleItemSpacing {
    val horizontal = when (item) {
        is AdUiModel,
        is VideoUiModel,
        is CarouselUiModel,
        is PdfUiModel -> 0.dp

        is ImageUiModel -> when (item.widthFactor) {
            WidthFactor.FULL_BLEED, null -> 0.dp
            WidthFactor.DEFAULT -> getDefaultHorizontalItemSpacing(config)
        }

        is WebEmbedUiModel -> when (item.widthFactor) {
            WidthFactor.FULL_BLEED -> 0.dp
            WidthFactor.DEFAULT, null -> getDefaultHorizontalItemSpacing(config)
        }

        else -> getDefaultHorizontalItemSpacing(config)
    }

    val bottom = when (item) {
        is KickerUiModel,
        is TitleUiModel,
        is DeckUiModel -> 8.dp

        is BylineUiModel,
        is ElevatedBylineUiModel,
        is DateUiModel,
        is ImageUiModel,
        is VideoUiModel,
        is TableUiModel,
        is GalleryUiModel,
        is DividerUiModel,
        is CommentsUiModel,
        is AudioUiModel,
        is HumanAudioUiModel,
        is WebEmbedUiModel,
        is PdfUiModel,
        is PodcastUiModel -> 24.dp

        is SanitizedHtmlUiModel,
        is BlockQuoteUiModel,
        is PullQuoteUiModel,
        is QuoteUiModel,
        is ElementGroupUiModel.ElementGroupBlockQuoteUiModel -> 32.dp

        else -> 16.dp
    }

    return ArticleItemSpacing(horizontal = horizontal, bottom = bottom)
}

private fun getDefaultHorizontalItemSpacing(config: Configuration): Dp {
    val screenWidth = config.screenWidthDp.dp
    val maxItemWidth = 640.dp
    val defaultHorizontal = 16.dp
    return if (screenWidth >= (maxItemWidth + (defaultHorizontal * 2))) {
        (screenWidth - maxItemWidth) / 2
    } else {
        defaultHorizontal
    }
}

/**
 * Use for Composable Previews
 */
val dummyArticlesInteractionHelper = object : ArticlesInteractionHelper {
    override fun onEventFired(event: ArticleInteractionEvent) {
        // no-op for Compose preview
    }
}

val dummyWebEmbedSettings = WebEmbedSettings(
    embedJSInterface = object : EmbedJSInterface {
        override fun triggerDisplayEmbed() {
            // no-op for Compose preview
        }

        override fun trackEvent(name: String, webViewDump: String) {
            // no-op for Compose preview
        }
    }
)

@Preview(showBackground = true)
@Preview(showBackground = true, device = "id:pixel_tablet")
@Composable
private fun ContentKitchenSinkPreview() {
    AndroidClassicTheme {
        Surface(
            color = wpdsColors.secondary,
            modifier = Modifier.fillMaxSize()
        ) {
            val uiItems = listOf(
                SanitizedHtmlUiModel(
                    content = "EXCLUSIVE",
                    questionSets = null,
                    sourceAnnotations = null,
                    oEmbed = null,
                    uiStyle = SanitizedHtmlUiStyle.BRIEFS_EXCLUSIVE_LABEL
                ),
                PinUiModel(
                    content = "Pinned: Read our full coverage of the election results",
                    uiStyle = PinUiStyle.DEFAULT
                ),
                KickerUiModel(
                    displayLabel = "National Security",
                    displayTransparency = "Analysis",
                    isLive = false,
                    path = "https://www.washingtonpost.com/opinions",
                    alignment = "left",
                    image = KickerImage(""),
                    uiStyle = KickerUiStyle.DEFAULT
                ),
                KickerUiModel(
                    displayLabel = "Live Updates",
                    displayTransparency = null,
                    isLive = true,
                    path = "https://www.washingtonpost.com/opinions",
                    alignment = "left",
                    image = KickerImage(""),
                    uiStyle = KickerUiStyle.PILL_LIVE
                ),
                TitleUiModel(
                    text = "This is a TitleView with the \"h1\" style",
                    uiStyle = TitleUiStyle.H1
                ),
                DeckUiModel(
                    content = "A subheadline that provides additional context about the article's main story",
                    uiStyle = DeckUiStyle.DEFAULT
                ),
                BylineUiModel(
                    text = "By John Smith and Jane Doe",
                    subtext = null,
                    authors = listOf(
                        AuthorInfoUiModel(
                            id = "1",
                            name = "John Smith",
                            bio = "Some person named John Smith",
                            expertise = null,
                            imageUrl = null
                        ),
                        AuthorInfoUiModel(
                            id = "2",
                            name = "Jane Doe",
                            bio = "Some person named Jane Doe",
                            expertise = null,
                            imageUrl = null
                        )
                    ),
                    imageUrl = "https://s3.amazonaws.com/arc-authors/washpost/054223d1-918c-4d39-965f-e1877701e420.png",
                    bio = "John Smith is a reporter covering national security and foreign policy for The Washington Post.",
                    uiStyle = BylineUiStyle.DEFAULT
                ),
                BylineUiModel(
                    text = "By John Smith",
                    subtext = "Updated 5 minutes ago",
                    authors = listOf(
                        AuthorInfoUiModel(
                            id = "1",
                            name = "John Smith",
                            bio = "Some person named John Smith",
                            expertise = null,
                            imageUrl = null
                        )
                    ),
                    imageUrl = "https://s3.amazonaws.com/arc-authors/washpost/054223d1-918c-4d39-965f-e1877701e420.png",
                    bio = "John Smith is a reporter covering national security and foreign policy for The Washington Post.",
                    uiStyle = BylineUiStyle.LIVE_UPDATE
                ),
                DateUiModel(
                    content = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1),
                    recencyThreshold = null,
                    uiStyle = DateUiStyle.DEFAULT
                ),
                DateUiModel(
                    content = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(3),
                    recencyThreshold = 120L,
                    uiStyle = DateUiStyle.LIVE_UPDATE
                ),
                ElevatedBylineUiModel(
                    kicker = "Opinion",
                    byline = "Opinion by Margaret Chen",
                    authors = listOf(
                        AuthorInfoUiModel(
                            id = "4",
                            name = "Margaret Chen",
                            bio = "Some person named Margaret Chen",
                            imageUrl = "https://s3.amazonaws.com/arc-authors/washpost/054223d1-918c-4d39-965f-e1877701e420.png",
                            expertise = null
                        ),
                        AuthorInfoUiModel(
                            id = "5",
                            name = "James Wilson",
                            bio = "Some person named James Wilson",
                            imageUrl = "https://s3.amazonaws.com/arc-authors/washpost/054223d1-918c-4d39-965f-e1877701e420.png",
                            expertise = "Miscellany, odds and ends"
                        )
                    ),
                    uiStyle = ElevatedBylineUiStyle.OPINIONS
                ),
                AuthorInfoUiModel(
                    id = "1",
                    name = "Jamie Ross",
                    bio = "Jamie Ross is a writer for The 7, The Washington Post's morning briefing, which is available each weekday from 7 a.m. to 10 a.m. Eastern time on mobile, desktop and inboxes.",
                    expertise = "Jamie is an expert at finding the top 7 news stories for the day",
                    imageUrl = "https://s3.amazonaws.com/arc-authors/washpost/054223d1-918c-4d39-965f-e1877701e420.png",
                    showDivider = true,
                    uiStyle = AuthorInfoUiStyle.DEFAULT
                ),
                AudioUiModel(
                    rawUrl = "audio-123",
                    title = "Sample Audio",
                    durationText = "2:30",
                    uiStyle = AudioUiStyle.DEFAULT
                ),
                HumanAudioUiModel(
                    url = "",
                    label = "Author narrated",
                    durationText = "8 min",
                    authorImageUrl = "https://s3.amazonaws.com/arc-authors/washpost/054223d1-918c-4d39-965f-e1877701e420.png",
                    uiStyle = HumanAudioUiStyle.DEFAULT
                ),
                PodcastUiModel(
                    rawUrl = "podcast-123",
                    seriesName = "Post Reports",
                    episodeName = "Why the economy is sending mixed signals",
                    seriesImageUrl = "https://s3.amazonaws.com/arc-authors/washpost/054223d1-918c-4d39-965f-e1877701e420.png",
                    durationText = "6 min",
                    uiStyle = PodcastUiStyle.DEFAULT
                ),
                PodcastUiModel(
                    rawUrl = "podcast-789",
                    seriesName = "Some Podcast",
                    episodeName = "Episode 789",
                    durationText = "6 min",
                    seriesImageUrl = null,
                    uiStyle = PodcastUiStyle.INLINE,
                ),
                SanitizedHtmlUiModel(
                    content = "7",
                    questionSets = null,
                    sourceAnnotations = null,
                    oEmbed = null,
                    uiStyle = SanitizedHtmlUiStyle.SUBHEAD_BRIEFS
                ),
                SanitizedHtmlUiModel(
                    content = LoremIpsum(8).values.joinToString(),
                    questionSets = null,
                    sourceAnnotations = null,
                    oEmbed = null,
                    uiStyle = SanitizedHtmlUiStyle.PARAGRAPH_BRIEFS
                ),
                SanitizedHtmlUiModel(
                    content = LoremIpsum(50).values.joinToString(),
                    questionSets = null,
                    sourceAnnotations = null,
                    oEmbed = null,
                    uiStyle = SanitizedHtmlUiStyle.DEFAULT
                ),
                PullQuoteUiModel(
                    content = "I said something important and it is highlighted in this pull quote",
                    attribution = "— Someone Important",
                    mime = MimeType.HTML,
                    uiStyle = PullQuoteUiStyle.DEFAULT
                ),
                BlockQuoteUiModel(
                    content = "I said something important and it is highlighted in this pull quote",
                    attribution = "— Someone Important",
                    mime = MimeType.HTML,
                    uiStyle = BlockQuoteUiStyle.DEFAULT
                ),
                QuoteUiModel(
                    content = "I said something important and it is highlighted in this pull quote",
                    attribution = "— Someone Important",
                    mime = MimeType.HTML,
                    uiStyle = QuoteUiStyle.DEFAULT
                ),
                ImageUiModel(
                    imageUrl = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/V5IZ4G7Y763R54RSBQGN6M3XZE.jpg",
                    darkModeImageUrl = "https://s3.amazonaws.com/wpmobileprodipad2.0/classic_test/live-images/live-head-to-head-president%40sm-dark.png",
                    caption = "A standard image in the article. (Photo by Jane Doe/The Washington Post)",
                    imageWidth = 1200,
                    imageHeight = 800,
                    isLive = false,
                    refreshRateMs = null,
                    widthFactor = WidthFactor.DEFAULT,
                    uiStyle = ImageUiStyle.DEFAULT
                ),
                ImageUiModel(
                    imageUrl = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/OIBALHVNOAI6XAWBRFVMVFK3XE.jpg",
                    darkModeImageUrl = "https://s3.amazonaws.com/wpmobileprodipad2.0/classic_test/live-images/live-head-to-head-president%40sm-dark.png",
                    caption = null,
                    imageWidth = 1600,
                    imageHeight = 900,
                    isLive = false,
                    refreshRateMs = null,
                    widthFactor = WidthFactor.DEFAULT,
                    uiStyle = ImageUiStyle.DEFAULT
                ),
                DividerUiModel(
                    uiStyle = DividerUiStyle.DEFAULT
                ),
                ImageUiModel(
                    imageUrl = "https://dohdeick6sqa6.cloudfront.net/screenshots/staging/live-head-to-head-president@sm.png",
                    darkModeImageUrl = "https://s3.amazonaws.com/wpmobileprodipad2.0/classic_test/live-images/live-head-to-head-president%40sm-dark.png",
                    caption = "Electoral vote graph (Live updating)",
                    imageWidth = 1200,
                    imageHeight = 800,
                    isLive = true,
                    refreshRateMs = 60000L,
                    widthFactor = WidthFactor.DEFAULT,
                    uiStyle = ImageUiStyle.DEFAULT
                ),
                SanitizedHtmlUiModel(
                    content = "This is a view with an ATP chip. You can ask questions.",
                    oEmbed = null,
                    questionSets = null,
                    sourceAnnotations = null,
                    uiStyle = SanitizedHtmlUiStyle.ASK_THE_POST
                ),
                SanitizedHtmlUiModel(
                    content = "This is a view with an FTS chip. You can see what the source said.",
                    oEmbed = null,
                    questionSets = null,
                    sourceAnnotations = null,
                    uiStyle = SanitizedHtmlUiStyle.FROM_THE_SOURCE
                ),
                InlineAlertToggleUiModel(
                    topicKey = "some-topic",
                    topicDisplayName = "Some Topic",
                    isEnabled = true,
                    uiStyle = InlineAlertToggleUiStyle.DEFAULT
                ),
                InlineMessageUiModel(
                    articleInlineMessage = ArticleInlineMessage(
                        null,
                        title = "Share 3 extra accounts with friends and family - included with your subscription",
                        null,
                        "Share extra account",
                        "https://washingtonpost.com/my-post/account/extra-accounts",
                        isEligiblePromo = true
                    ),
                    uiStyle = InlineOfferUiStyle.DEFAULT
                ).apply {
                    isVisible = true
                },
                InterstitialLinkUiModel(
                    content = "Related: Read more about this story",
                    url = "https://www.washingtonpost.com/related-article",
                    uiStyle = InterstitialLinkUiStyle.DEFAULT
                ),
                LinkButtonUiModel(
                    label = "Continue reading",
                    url = "https://www.washingtonpost.com/full-article",
                    showArrow = true,
                    uiStyle = LinkButtonUiStyle.DEFAULT
                ),
                LinkButtonUiModel(
                    label = "See full results",
                    url = "https://www.washingtonpost.com/results",
                    showArrow = true,
                    uiStyle = LinkButtonUiStyle.OUTCOME
                ),
                ListUiModel(
                    items = listOf(
                        "The committee voted 12-3 to advance the legislation",
                        "Opponents argued the bill lacked sufficient oversight provisions",
                        "A final floor vote is expected by the end of the week"
                    ),
                    listType = ListType.UNORDERED,
                    uiStyle = ListUiStyle.DEFAULT
                ),
                ListUiModel(
                    items = listOf(
                        "Submit your application by March 31",
                        "Attend the required orientation session",
                        "Complete the background check process"
                    ),
                    listType = ListType.ORDERED,
                    uiStyle = ListUiStyle.DEFAULT
                ),
                LiveOutcomeUiModel(
                    headline = "Biden leads Trump in electoral votes as key swing states are called",
                    subHeadline = "270 electoral votes needed to win",
                    imageUrl = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/V5IZ4G7Y763R54RSBQGN6M3XZE.jpg",
                    uiStyle = LiveOutcomeUiStyle.DEFAULT
                ),
                LiveOutcomeUiModel(
                    headline = "Senate passes spending bill in 68-32 vote",
                    subHeadline = null,
                    imageUrl = null,
                    uiStyle = LiveOutcomeUiStyle.DEFAULT
                ),
                SanitizedHtmlUiModel(
                    content = "This is a sample Tweet",
                    questionSets = null,
                    sourceAnnotations = null,
                    oEmbed = "<blockquote class=\"twitter-tweet\"><p lang=\"en\" dir=\"ltr\">From <a href=\"https://twitter.com/LauncherWP?ref_src=twsrc%5Etfw\">@LauncherWP</a>: PlayStation reveals PS VR2, the next generation of their virtual reality headset <a href=\"https://t.co/Pv0l8CFd0o\">https://t.co/Pv0l8CFd0o</a></p>&mdash; The Washington Post (@washingtonpost) <a href=\"https://twitter.com/washingtonpost/status/1496120609750798351?ref_src=twsrc%5Etfw\">February 22, 2022</a></blockquote>\n<script async src=\"https://platform.twitter.com/widgets.js\" charset=\"utf-8\"></script>\n\n",
                    uiStyle = SanitizedHtmlUiStyle.SOCIAL_EMBED
                ),
                CorrectionUiModel(
                    correctionType = "Clarification",
                    content = "This article has been updated to clarify the role of the individual mentioned.",
                    uiStyle = CorrectionUiStyle.DEFAULT
                ),
                ElementGroupUiModel.ElementGroupLinkBoxUiModel(
                    subtype = "link_box",
                    kicker = "LIVE UPDATES",
                    title = "Follow along as the story develops",
                    subheadline = "Our reporters are tracking the latest developments",
                    displayDate = "March 10, 2026",
                    contentElements = listOf(
                        SanitizedHtmlUiModel(
                            content = "First update: Officials have announced new measures.",
                            questionSets = null,
                            sourceAnnotations = null,
                            oEmbed = null,
                            uiStyle = SanitizedHtmlUiStyle.DEFAULT
                        ),
                        SanitizedHtmlUiModel(
                            content = "Second update: The situation continues to evolve.",
                            questionSets = null,
                            sourceAnnotations = null,
                            oEmbed = null,
                            uiStyle = SanitizedHtmlUiStyle.DEFAULT
                        ),
                        SanitizedHtmlUiModel(
                            content = "Third update: Additional details have emerged.",
                            questionSets = null,
                            sourceAnnotations = null,
                            oEmbed = null,
                            uiStyle = SanitizedHtmlUiStyle.DEFAULT
                        ),
                        SanitizedHtmlUiModel(
                            content = "Fourth update: Experts weigh in on the implications.",
                            questionSets = null,
                            sourceAnnotations = null,
                            oEmbed = null,
                            uiStyle = SanitizedHtmlUiStyle.DEFAULT
                        ),
                        SanitizedHtmlUiModel(
                            content = "Fifth update: Public reaction has been mixed.",
                            questionSets = null,
                            sourceAnnotations = null,
                            oEmbed = null,
                            uiStyle = SanitizedHtmlUiStyle.DEFAULT
                        )
                    ),
                    expandCollapseUiModel = ExpandCollapseUiModel(
                        isExpanded = false,
                        expandedLabel = "Show less content",
                        truncatedLabel = "Show more content",
                        minItemsCount = null,
                        group = "123",
                        uiStyle = ExpandCollapseUiStyle.DEFAULT
                    ),
                    uiStyle = ElementGroupUiStyle.LINK_BOX
                ),
                ElementGroupUiModel.ElementGroupBlockQuoteUiModel(
                    contentElements = listOf(
                        SanitizedHtmlUiModel(
                            content = "<i>Dear Mr. Pelley:</i><br /><i>I meant what I said in my letter last week to the 60 Minutes team.</i>",
                            questionSets = null,
                            sourceAnnotations = null,
                            oEmbed = null,
                            uiStyle = SanitizedHtmlUiStyle.PARAGRAPH
                        ),
                        SanitizedHtmlUiModel(
                            content = "This is a second paragraph with <b>bold text</b>, <i>italic text</i>, and a <a href=\"https://www.washingtonpost.com/\">Washington Post link</a>.",
                            questionSets = null,
                            sourceAnnotations = null,
                            oEmbed = null,
                            uiStyle = SanitizedHtmlUiStyle.PARAGRAPH
                        ),
                        SanitizedHtmlUiModel(
                            content = "This third paragraph omits subtype because paragraph is the default. It is deliberately long enough to wrap across several lines on a phone and exercise the box width, horizontal text inset, Dynamic Type behavior, and a grouped quote that extends beyond a compact viewport.",
                            questionSets = null,
                            sourceAnnotations = null,
                            oEmbed = null,
                            uiStyle = SanitizedHtmlUiStyle.PARAGRAPH
                        )
                    ),
                    attribution = BlockQuoteAttributionUiModel(
                        content = "<b>Nick Bilton</b>, executive producer",
                        mime = MimeType.HTML,
                    ),
                    uiStyle = ElementGroupUiStyle.DEFAULT,
                ),
                GalleryUiModel(
                    images = listOf(
                        ImageUiModel(
                            imageUrl = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/V5IZ4G7Y763R54RSBQGN6M3XZE.jpg",
                            darkModeImageUrl = null,
                            caption = "First image in the gallery. (Photo by John Smith/The Washington Post)",
                            imageWidth = 1200,
                            imageHeight = 800,
                            isLive = false,
                            refreshRateMs = null,
                            widthFactor = WidthFactor.DEFAULT,
                            uiStyle = ImageUiStyle.DEFAULT
                        ),
                        ImageUiModel(
                            imageUrl = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/OIBALHVNOAI6XAWBRFVMVFK3XE.jpg",
                            darkModeImageUrl = null,
                            caption = "Second image in the gallery. (Photo by Jane Doe/The Washington Post)",
                            imageWidth = 1600,
                            imageHeight = 900,
                            isLive = false,
                            refreshRateMs = null,
                            widthFactor = WidthFactor.DEFAULT,
                            uiStyle = ImageUiStyle.DEFAULT
                        ),
                        ImageUiModel(
                            imageUrl = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/V2GTS4SWZYI6ZKAIGGL2EKYZ7I.jpg",
                            darkModeImageUrl = null,
                            caption = "Third image in the gallery.",
                            imageWidth = 1400,
                            imageHeight = 1050,
                            isLive = false,
                            refreshRateMs = null,
                            widthFactor = WidthFactor.DEFAULT,
                            uiStyle = ImageUiStyle.DEFAULT
                        ),
                        ImageUiModel(
                            imageUrl = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/T5POWRCWZYI6ZKAIGGL2EKYZ7I.jpg",
                            darkModeImageUrl = null,
                            caption = "Fourth image - only visible when expanded.",
                            imageWidth = 1200,
                            imageHeight = 800,
                            isLive = false,
                            refreshRateMs = null,
                            widthFactor = WidthFactor.DEFAULT,
                            uiStyle = ImageUiStyle.DEFAULT
                        ),
                        ImageUiModel(
                            imageUrl = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/TWLGJOCWZYI6ZKAIGGL2EKYZ7I.jpg",
                            darkModeImageUrl = null,
                            caption = "Fifth image - only visible when expanded.",
                            imageWidth = 1600,
                            imageHeight = 900,
                            isLive = false,
                            refreshRateMs = null,
                            widthFactor = WidthFactor.DEFAULT,
                            uiStyle = ImageUiStyle.DEFAULT
                        )
                    ),
                    expandCollapseUiModel = ExpandCollapseUiModel(
                        isExpanded = false,
                        minItemsCount = 3,
                        expandedLabel = "Show fewer photos",
                        truncatedLabel = "Show more photos",
                        group = "123",
                        uiStyle = ExpandCollapseUiStyle.DEFAULT
                    ),
                    uiStyle = GalleryUiStyle.DEFAULT
                ),
                CarouselUiModel(
                    label = "From the Source",
                    items = listOf(
                        SourceCommentUiModel(
                            url = "https://example.com/1",
                            author = Author(
                                name = "Dr. Sarah Chen",
                                expertise = "Chief Medical Officer, Johns Hopkins",
                                image = null
                            ),
                            body = null,
                            video = Video(
                                duration = 127.0,
                                promoImage = PromoImage(
                                    url = "https://d1i4t8bqe7zgj6.cloudfront.net/03-31-2022/t_a0b2f41cd476490dbabf137462159584_name_Screen_Shot_2022_03_31_at_12_12_44_PM.png",
                                    aspectRatio = 1.5
                                ),
                                transcript = Transcript(
                                    text = "The new treatment protocol has shown remarkable results in early trials. We're seeing a 40% improvement in patient outcomes."
                                )
                            ),
                        ),
                        SourceCommentUiModel(
                            url = "https://example.com/2",
                            author = Author(
                                name = "Rep. Michael Torres",
                                expertise = "House Committee on Science",
                                image = null
                            ),
                            body = "This bipartisan legislation represents a historic investment in American innovation. We expect to create over 50,000 new jobs in the technology sector.",
                            video = null,
                        ),
                        SourceCommentUiModel(
                            url = "https://example.com/3",
                            author = Author(
                                name = "Emily Rodriguez",
                                expertise = "Climate Policy Analyst",
                                image = null
                            ),
                            body = "The data is clear: without immediate action, we risk irreversible damage to coastal ecosystems within the next decade.",
                            video = null,
                        )
                    ),
                    uiStyle = CarouselUiStyle.FTS
                ),
                CarouselUiModel(
                    label = "More for you",
                    items = listOf(
                        ForYouCarouselItemUiModel(
                            credits = null,
                            headline = "Supreme Court ruling could reshape environmental regulations for decades",
                            imageUrl = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/Q2QB5VFJDUI6XKFHL5C53TPTMQ.jpg",
                            sourceType = null,
                            label = null,
                            recReason = "Because you read about the Supreme Court",
                            url = "https://example.com/article1",
                            authors = null
                        ),
                        ForYouCarouselItemUiModel(
                            credits = null,
                            headline = "Tech giants unveil AI partnership that could transform healthcare",
                            imageUrl = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/CBCTDYXEX4I6XCGFJ7LDQLCHZM.jpg",
                            sourceType = null,
                            label = null,
                            recReason = "Trending in Technology",
                            url = "https://example.com/article2",
                            authors = null
                        ),
                        ForYouCarouselItemUiModel(
                            credits = null,
                            headline = "Nationals advance to playoffs after dramatic walk-off victory",
                            imageUrl = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/V2GTS4SWZYI6ZKAIGGL2EKYZ7I.jpg",
                            sourceType = null,
                            label = null,
                            recReason = null,
                            url = "https://example.com/article3",
                            authors = null
                        )
                    ),
                    uiStyle = CarouselUiStyle.FOR_YOU
                ),
                TableUiModel(
                    header = listOf("Candidate", "Party", "Votes", "Pct."),
                    rows = listOf(
                        listOf("Jane Smith", "Democrat", "1,234,567", "52.3%"),
                        listOf("John Doe", "Republican", "1,123,456", "47.7%")
                    ),
                    uiStyle = TableUiStyle.DEFAULT
                ),
                VideoUiModel(
                    contentUrl = "https://www.washingtonpost.com/video/politics/biden-israel-gaza-war-will-end-after-hamas-dismantled/2023/11/15/1db3cf61-e2f9-4faf-aad3-bf4acb4c464e_video.html",
                    id = "1234",
                    thumbnailUrl = "https://d1i4t8bqe7zgj6.cloudfront.net/11-16-2023/t_2efd2aacbd124dde86031ec47f15a61d_name_biden_2.png",
                    title = "Biden: Israel-Gaza war will end after Hamas dismantled",
                    durationMs = 245_000L,
                    isAutoplay = false,
                    isLive = false,
                    isLooping = false,
                    caption = "President Biden said during a news conference on Nov. 15 he expects the Israel-Gaza war to continue until \"Hamas no longer maintains the capacity to murder.\" (The Washington Post)",
                    uiStyle = VideoUiStyle.DEFAULT
                ),
                VideoUiModel(
                    contentUrl = "https://www.washingtonpost.com/video/world/gazas-last-working-flour-mill-hit-by-strike/2023/11/14/c7f38a40-0f21-456e-8675-1a11e3e206c6_video.html",
                    id = "1234",
                    thumbnailUrl = "https://d1i4t8bqe7zgj6.cloudfront.net/11-14-2023/t_231d9a19365a48bc84e5cd086383ca53_name_TMBZINA4H2DBEDMCEUQFYLHV54.jpg",
                    title = "Gaza's last working flour mill hit by strike",
                    durationMs = 79_146L,
                    isAutoplay = false,
                    isLive = false,
                    isLooping = false,
                    caption = "Gaza's last functioning flour mill was hit by a strike on Nov. 15, leaving the strip's more than 2 million people with no local source for flour. (Obtained by Reuters)",
                    uiStyle = VideoUiStyle.DEFAULT
                ),
                TaglineUiModel(
                    uiStyle = TaglineUiStyle.DEFAULT
                ),
                CommentsUiModel(
                    uiStyle = CommentsUiStyle.DEFAULT
                )
            )
            Column {
                Spacer(Modifier.height(48.dp))
                ArticleContentView(
                    uiState = ArticleContentState.Success(
                        article = Article2(contenturl = ""),
                        uiItems = uiItems,
                        source = ArticleContentState.Source.NETWORK
                    ),
                    articleInlineMessage = null,
                    articlesInteractionHelper = dummyArticlesInteractionHelper,
                    adsMode = AdsModel.Enabled,
                    isCurrentPage = true,
                    webEmbedSettings = dummyWebEmbedSettings,
                )
            }
        }
    }
}
