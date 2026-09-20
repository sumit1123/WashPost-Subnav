package com.washingtonpost.foryou.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.wapo.android.commons.util.ViewUtil.findActivityOfType
import com.wapo.flagship.features.grid.GridEnvironment
import com.wapo.flagship.features.posttv.PostTvPlayer2Manager
import com.washingtonpost.foryou.ForYouActivity
import com.washingtonpost.foryou.R
import com.washingtonpost.foryou.data.ForYouContentType
import com.washingtonpost.foryou.data.RecommendationsItem
import com.washingtonpost.foryou.repo.ForYouFeedRepositoryImpl
import com.washingtonpost.foryou.viewmodel.ForYouViewModel
import com.washingtonpost.userhistory.models.RecommendationsHelperItem
import com.washingtonpost.userhistory.viewmodel.UserHistoryViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.math.max
import kotlin.math.min

private val franklinFont = FontFamily(Font(com.wapo.view.R.font.wp_franklinitcstd_font_family))

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ForYouScreen(
    items: List<RecommendationsItem>,
    forYouViewModel: ForYouViewModel,
    userHistoryViewModel: UserHistoryViewModel,
    onItemClicked: ((Int, RecommendationsItem) -> Unit)?,
    onEllipsisClicked: ((Int, RecommendationsItem) -> Unit)?,
    onAudioClicked: ((RecommendationsItem, (Boolean) -> Unit) -> Unit)?,
    onCommentClicked: ((RecommendationsItem) -> Unit)?,
    onSummaryClicked: ((RecommendationsItem) -> Unit)?,
    scrollToTopTrigger: StateFlow<Int>,
    sectionTitle: String?,
    player2Manager: PostTvPlayer2Manager?,
    gridEnvironment: GridEnvironment,
) {
    val context = LocalContext.current
    val activity = context.findActivityOfType<ForYouActivity>()

    val listState = rememberLazyListState()
    val uiState by forYouViewModel.uiState.collectAsState()
    val columns = getColumnCount()

    val uniqueItems = items.distinctBy { it.articleId }
    val chunkedItems = uniqueItems.chunked(columns)
    var savedVisibleItemId by rememberSaveable { mutableStateOf<String?>(null) }

    var savedFlatIndex by rememberSaveable { mutableStateOf(-1) }
    var isRestoringScroll by rememberSaveable { mutableStateOf(false) }

    val scrollToIndex = if (savedFlatIndex >= 0) savedFlatIndex / columns else -1

    val configuration = LocalConfiguration.current
    val orientation = configuration.orientation
    var lastOrientation by rememberSaveable { mutableStateOf(orientation) }

    val trigger by scrollToTopTrigger.collectAsState()

    val cardSizePixels =
        remember { context.resources.getDimensionPixelSize(R.dimen.for_you_card_size) }

    val spacingDp = if (columns == 1) {
        dimensionResource(R.dimen.for_you_card_separator)
    } else {
        dimensionResource(R.dimen.for_you_card_separator_grid)
    }

    val screenWidthDp = configuration.screenWidthDp.dp
    val totalSpacing = spacingDp * (columns - 1)
    val cardWidthDp = (screenWidthDp - totalSpacing) / columns

    val screenWidthPx =
        with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val cardWidthPx = screenWidthPx / columns
    val cardHeightPx = cardWidthPx * 16f / 9f

    val minVisibilityToTakeOver = 0.6f
    val visibleGlobalIndices = remember { mutableStateListOf<Int>() }

    val isTabActive by forYouViewModel.isTabActive.collectAsState()
    val nestedScrollInterop = rememberNestedScrollInteropConnection()

    fun updateForYouViewedEvents(visibility: Boolean) = run {
        // Create events only when ForYou screen is active and visibility is true.
        if (forYouViewModel.isTabActive.value && visibility) {
            val uiStateValue = uiState
            if (uiStateValue is ForYouUiState.Feed) {
                val recommendationsHelperItems = uiStateValue.items.map {
                    RecommendationsHelperItem(
                        it.articleId ?: it.video?.contentId,
                        it.recReason,
                        uiStateValue.requestId,
                        uiStateValue.recipeId,
                        uiStateValue.testId,
                        it.contentType
                    )
                }

                userHistoryViewModel.startOrStopForYouViewedTimers(
                    recommendationsItems = recommendationsHelperItems,
                    visibleItemIndices = visibleGlobalIndices,
                    surface = ForYouFeedRepositoryImpl.SURFACE_FEED
                )
            }
        } else {
            userHistoryViewModel.stopAllForYouViewedTimers()
        }
    }

    // saves the most visible item on the screen when scrolling
    LaunchedEffect(Unit) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                if (!isRestoringScroll) {
                    val mostVisibleRow = visibleItems.maxByOrNull { item ->
                        val top = item.offset
                        val bottom = item.offset + item.size
                        val visibleTop = top.coerceAtLeast(0)
                        val visibleBottom =
                            bottom.coerceAtMost(listState.layoutInfo.viewportEndOffset)
                        (visibleBottom - visibleTop).coerceAtLeast(0)
                    }

                    mostVisibleRow?.index?.let { rowIndex ->
                        val flatIndex = rowIndex * columns
                        val item = items.getOrNull(flatIndex)
                        if (item != null) {
                            savedVisibleItemId = item.articleId
                            savedFlatIndex = flatIndex
                        }
                    }
                }
            }
    }

    // restores scroll position when the orientation changes
    LaunchedEffect(orientation, savedVisibleItemId) {
        if (savedVisibleItemId != null && orientation != lastOrientation) {
            isRestoringScroll = true

            val flatIndex = items.indexOfFirst { it.articleId == savedVisibleItemId }
            if (flatIndex != -1) {
                listState.scrollToItem(scrollToIndex)
                lastOrientation = orientation
                isRestoringScroll = false
            }
        }
    }

    LaunchedEffect(trigger) {
        listState.animateScrollToItem(0)
    }

    var isRefreshing by remember { mutableStateOf(false) }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            forYouViewModel.refresh()
            isRefreshing = false
        }
    )

    // load more recommendations when near the end of the list
    LaunchedEffect(forYouViewModel.canLoadMore, chunkedItems.size) {
        snapshotFlow { listState.layoutInfo }
            .map { layoutInfo ->
                val lastVisibleRowIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                val totalRowCount = layoutInfo.totalItemsCount
                lastVisibleRowIndex to totalRowCount
            }
            .distinctUntilChanged()
            .collect { (lastVisibleRowIndex, totalRowCount) ->
                val hasLoadedInitialItems = totalRowCount > 1
                val nearEnd = lastVisibleRowIndex >= totalRowCount - 5
                val unableToScroll = !listState.canScrollForward

                if (hasLoadedInitialItems && (nearEnd || unableToScroll) && forYouViewModel.canLoadMore) {
                    forYouViewModel.getMoreRecommendations(
                        contentType = listOf(
                            ForYouContentType.ARTICLE.type,
                            ForYouContentType.VIDEO.type
                        )
                    )
                }
            }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _: LifecycleOwner, event: Lifecycle.Event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    player2Manager?.pauseMedia()
                    updateForYouViewedEvents(false)
                }

                Lifecycle.Event.ON_RESUME -> {
                    updateForYouViewedEvents(true)
                }

                Lifecycle.Event.ON_STOP -> {
                    player2Manager?.releasePlayerFrame()
                }

                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player2Manager?.pauseMedia()
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                activity?.toggleNavBars(
                    scrollY = consumed.y,
                    hasSectionTitle = sectionTitle != null
                )
                return super.onPostScroll(consumed, available, source)
            }
        }
    }

    val visibleVideoRatios = mutableMapOf<Int, Float>()
    var activeVideoId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(listState, isTabActive, items, isRefreshing, columns) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->

                visibleVideoRatios.clear()

                val viewportStart = listState.layoutInfo.viewportStartOffset
                val viewportEnd = listState.layoutInfo.viewportEndOffset

                visibleGlobalIndices.clear()

                visibleItems.forEach { itemInfo ->
                    val rowIndex = itemInfo.index
                    val row = chunkedItems.getOrNull(rowIndex) ?: return@forEach

                    val rowTop = itemInfo.offset
                    val rowBottom = itemInfo.offset + cardHeightPx

                    val visibleTop = max(rowTop, viewportStart)
                    val visibleBottom = min(rowBottom, viewportEnd.toFloat())
                    val visibleHeight = (visibleBottom - visibleTop).coerceAtLeast(0f)

                    if (visibleHeight <= 0f) return@forEach

                    row.forEachIndexed { columnIndex, content ->
                        val globalIndex = rowIndex * columns + columnIndex

                        visibleGlobalIndices.add(globalIndex)

                        if (content.contentType != ForYouContentType.VIDEO.type || content.articleId == null)
                            return@forEachIndexed

                        val ratio = visibleHeight / cardHeightPx
                        visibleVideoRatios[globalIndex] = ratio
                    }
                }

                // Then map columns to max visibility as before
                val columnVideoMap = mutableMapOf<Int, Pair<Int, Float>>()
                visibleVideoRatios.forEach { (globalIndex, ratio) ->
                    val col = globalIndex % columns
                    val existing = columnVideoMap[col]
                    if (existing == null || ratio > existing.second) {
                        columnVideoMap[col] = globalIndex to ratio
                    }
                }

                // Pick most visible video across columns
                val maxEntry = columnVideoMap.values.maxByOrNull { it.second }
                val mostVisibleIndex =
                    maxEntry?.takeIf { it.second >= minVisibilityToTakeOver }?.first
                val mostVisibleVideoId = mostVisibleIndex?.let {
                    val rowIdx = it / columns
                    val colIdx = it % columns
                    chunkedItems.getOrNull(rowIdx)?.getOrNull(colIdx)?.articleId
                }

                if (activeVideoId != mostVisibleVideoId) {
                    activeVideoId = mostVisibleVideoId
                }

                updateForYouViewedEvents(true)
            }
    }

    Box(
        Modifier
            .nestedScroll(nestedScrollInterop)
            .nestedScroll(nestedScrollConnection)
            .pullRefresh(pullRefreshState)
    ) {
        if (uiState is ForYouUiState.Loading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            LazyColumn(state = listState) {
                itemsIndexed(
                    items = chunkedItems,
                    key = { index, row -> row.getOrNull(0)?.articleId ?: index }
                ) { rowIndex, rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(spacingDp),
                    ) {
                        rowItems.forEachIndexed { columnIndex, item ->
                            ForYouView(
                                index = rowIndex * columns + columnIndex,
                                recommendationsItem = item,
                                forYouViewModel = forYouViewModel,
                                userHistoryViewModel = userHistoryViewModel,
                                onItemClicked = onItemClicked,
                                onEllipsisClicked = onEllipsisClicked,
                                onAudioClicked = onAudioClicked,
                                onCommentClicked = onCommentClicked,
                                onSummaryClicked = onSummaryClicked,
                                cardSizePixels = cardSizePixels,
                                modifier = Modifier
                                    .width(cardWidthDp)
                                    .fillMaxHeight(),
                                player2Manager = if (item.contentType == ForYouContentType.VIDEO.type) player2Manager else null,
                                gridEnvironment = gridEnvironment,
                                isActive = activeVideoId == item.articleId
                            )
                        }
                    }
                }
                if (forYouViewModel.canLoadMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }

        // match flicker in screen when reloading
        if (isRefreshing) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(colorResource(id = R.color.foryou_fragment_bg).copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {}
        }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (uiState is ForYouUiState.Error) {
                Text(
                    text = stringResource(R.string.for_you_unable_to_load),
                    color = Color(0xFF666666),
                    style = MaterialTheme.typography.body1,
                    fontFamily = franklinFont,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

@Composable
fun getColumnCount(): Int {
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(LocalDensity.current) { configuration.screenWidthDp.dp.toPx() }
    val cardSizePx = LocalContext.current.resources.getDimensionPixelSize(R.dimen.for_you_card_size)
    return max(1, (screenWidthPx / cardSizePx).toInt())
}