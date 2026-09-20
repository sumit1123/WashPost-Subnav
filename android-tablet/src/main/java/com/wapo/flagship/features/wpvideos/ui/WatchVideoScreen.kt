package com.wapo.flagship.features.wpvideos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.flagship.features.grid.GridEnvironment
import com.wapo.flagship.features.posttv.PostTvPlayer2Manager
import com.wapo.flagship.features.wpvideos.state.WatchVideosUIState
import com.wapo.flagship.features.wpvideos.viewmodel.WatchVideosViewModel
import com.wapo.flagship.navigation.ui.BottomTab
import com.wapo.flagship.navigation.viewmodel.sectionnav.SectionNavViewModel
import com.washingtonpost.foryou.R
import com.washingtonpost.userhistory.models.VideoConclusionState
import com.washingtonpost.userhistory.viewmodel.UserHistoryViewModel
import kotlinx.coroutines.flow.StateFlow


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WatchVideoScreen(
    bundleName: String?,
    player2Manager: PostTvPlayer2Manager?,
    gridEnvironment: GridEnvironment,
    reload: () -> Unit,
    loadMoreVideos: (Int) -> Unit,
    onActiveIndexChanged: (Int) -> Unit,
    activeIndexFlow: StateFlow<Int>,
    sectionNavViewModel: SectionNavViewModel,
    sectionTitle: String?,
    scrollToTopTrigger: StateFlow<Int>,
    watchVideosViewModel: WatchVideosViewModel,
    userHistoryViewModel: UserHistoryViewModel,
    eventSection: String,
    onFullScreenView: () -> Unit
) {
    // Tracks the currently active (playing) video index in the list
    var activeIndex by remember { mutableIntStateOf(0) }

    // Collects the activeIndex from a Flow (likely updated by scroll or external triggers)
    val activeIndexState = activeIndexFlow.collectAsState()

    // Maintains scroll state for the LazyVerticalGrid (used for detecting visible items or scrolling programmatically)
    val listState = rememberLazyGridState()

    val isAtBottom by remember {
        derivedStateOf {
            !listState.canScrollForward
        }
    }

    val videosList by watchVideosViewModel.videos.collectAsStateWithLifecycle()


    // Tracks whether the pull-to-refresh action is in progress
    var isRefreshing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }


    // Tracks whether the user has scrolled to the bottom and more videos are loading
    LaunchedEffect(isAtBottom, videosList.size) {
        if (isAtBottom && !isLoading) {
            loadMoreVideos.invoke(videosList.size)
        }
    }


    // Handles pull-to-refresh logic; sets refreshing state, triggers reload, and resets after completion
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            reload()
        }
    )

    val trigger by scrollToTopTrigger.collectAsState()
    LaunchedEffect(trigger) {
        listState.animateScrollToItem(0)
    }

    LaunchedEffect(isRefreshing, isLoading) {
        watchVideosViewModel.watchVideosState.collect { state ->
            when (state) {
                is WatchVideosUIState.Error -> {
                    isRefreshing = false
                    isLoading = false
                }

                WatchVideosUIState.Loading -> {
                    isLoading = true
                }

                is WatchVideosUIState.Success -> {
                    isLoading = false
                    if (isRefreshing) {
                        player2Manager?.playMedia(
                            state.list.firstOrNull() ?: return@collect,
                            true,
                            null
                        )
                    }
                    isRefreshing = false
                }
            }
        }
    }

    //Detect when a user swipes to a new item after watching FY video
    LaunchedEffect(listState) {
        var previousFirstVisibleItemIndex = listState.firstVisibleItemIndex
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { currentFirstVisibleItemIndex ->
                if (currentFirstVisibleItemIndex > previousFirstVisibleItemIndex) {
                    videosList.getOrNull(currentFirstVisibleItemIndex)?.let {
                        userHistoryViewModel.writeVideoViewedEvent(
                            it.contentId,
                            eventSection,
                            player2Manager?.getPlaybackPosition(),
                            null,
                            player2Manager?.getDuration(),
                            VideoConclusionState.SCROLLED_THROUGH
                        )
                    }
                }
                previousFirstVisibleItemIndex = currentFirstVisibleItemIndex
            }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                sectionNavViewModel.determineTopBarState(
                    y = consumed.y,
                    hasSectionTitle = sectionTitle != null
                )
                return super.onPostScroll(consumed, available, source)
            }
        }
    }

    if (videosList.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        // Sync activeIndex with the latest value emitted from activeIndexFlow
        activeIndex = activeIndexState.value

        // Auto-play the first video when the list is first loaded
        LaunchedEffect(videosList.isNotEmpty()) {
            if (videosList.isNotEmpty() && activeIndex == 0) {
                // Force first video to start playing only if no other active video is set
                activeIndex = 0
            }
        }

        // Observes scroll changes in the LazyGrid and automatically switches to the next video
        // when the currently active video is less than 30% visible in the viewport.
        // Loops back to the first video after reaching the end.

        LaunchedEffect(listState) {
            snapshotFlow {
                listState.isScrollInProgress
            }.collect { isScrolling ->
                if (!isScrolling) {
                    val visibleItems = listState.layoutInfo.visibleItemsInfo
                    val viewportHeight = listState.layoutInfo.viewportSize.height.toFloat()
                    // Find the video that is most visible in the viewport
                    var mostVisibleIndex = activeIndex
                    var maxVisibilityRatio = 0f

                    visibleItems.forEach { itemInfo ->
                        val itemTop = itemInfo.offset.y.toFloat()
                        val itemHeight = itemInfo.size.height
                        val itemBottom = itemTop + itemHeight

                        // Calculate how much of this item is visible
                        val visibleTop = itemTop.coerceAtLeast(0f)
                        val visibleBottom = itemBottom.coerceAtMost(viewportHeight)
                        val visibleHeight = (visibleBottom - visibleTop).coerceAtLeast(0f)
                        val visibilityRatio = visibleHeight / itemHeight

                        // If this item is more visible than our current best, update it
                        if (visibilityRatio > maxVisibilityRatio) {
                            maxVisibilityRatio = visibilityRatio
                            mostVisibleIndex = itemInfo.index
                        }
                    }

                    // Only switch if the most visible video is different from current active
                    // and it's significantly visible (more than 50% visible)
                    if (mostVisibleIndex != activeIndex && maxVisibilityRatio > 0.5f) {
                        activeIndex = mostVisibleIndex
                        onActiveIndexChanged(mostVisibleIndex)
                    }
                }
            }
        }

        // Observes the lifecycle of the current component and automatically pauses or stops media playback
        // when the lifecycle owner is paused or stopped, ensuring proper resource management.

        val lifecycleOwner = LocalLifecycleOwner.current

        DisposableEffect(Unit) {
            val observer = LifecycleEventObserver { _: LifecycleOwner, event: Lifecycle.Event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> {
                        player2Manager?.pauseMedia()
                    }

                    Lifecycle.Event.ON_STOP -> {
                        //player2Manager?.stopMedia()
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

        LaunchedEffect(listState, videosList.size) {
            if (listState.firstVisibleItemIndex > 0) {
                listState.scrollToItem(
                    listState.firstVisibleItemIndex,
                    listState.firstVisibleItemScrollOffset
                )
                activeIndex = listState.firstVisibleItemIndex
                onActiveIndexChanged(listState.firstVisibleItemIndex)
            }
        }

        // Displays a pull-to-refresh enabled grid of videos, adapting column and row counts based on device type (tablet vs phone).
        // Each grid item shows a video view that plays when active, and supports click to open the detailed video player.
        Box(
            Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
                .nestedScroll(nestedScrollConnection)
        ) {
            val isTablet = AppContextUtils.isTablet()
            val columns = if (isTablet) 3 else 2
            val rows = if (isTablet) 3 else 2
            LazyVerticalGrid(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 20.dp,
                    bottom = 60.dp
                ),
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(items = videosList, key = { index, item ->
                    //Videos can potentially have the same ids, we need to generate a new id here
                    "${item.id}_$index"
                }) { index, item ->
                    WatchVideoView(
                        player2Manager = player2Manager,
                        isPlaying = index == activeIndex,
                        videoItem = videosList[index].copy(adTagUrl = null),
                        activeIndex = activeIndex,
                        rows = rows,
                        onClick = {
                            onFullScreenView()
                            //Send only 20 videos to the Vertical Video Activity. passing a large list
                            // causes a DeadObjectException
                            val minOffset = minOf(index + 20, videosList.size)
                            gridEnvironment.openWatchVideoCard(
                                videosList.subList(index, minOffset),
                                bundleName ?: "", BottomTab.Watch.trackingName, 0, minOffset
                            )
                        }
                    )
                }
            }

            // match flicker in screen when reloading
            if (isRefreshing || isLoading) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(colorResource(id = R.color.foryou_fragment_bg).copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(bottom = 80.dp)
                            .align(Alignment.BottomCenter),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}