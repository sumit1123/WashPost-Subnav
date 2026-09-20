package com.wapo.flagship.features.conversations.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.wapo.flagship.features.conversations.model.CommentAction
import com.wapo.flagship.features.conversations.util.CommentsConstant.MAX_DEPTH
import com.wapo.flagship.features.conversations.util.CommentsConstant.MAX_VISIBLE_REPLIES
import com.wapo.kmpshared.core.KMPResult
import com.wapo.kmpshared.features.conversations.domain.models.CommentItem
import com.wpds.theme.wpdsColors
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * This composable displays a list of comments and their replies. It supports pull-to-refresh,
 * loading more comments as the user scrolls, and a "new comments" banner that appears
 * when new comments are available.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CommentsList(
    comments: List<CommentItem>,
    repliesMap: Map<String, KMPResult<List<CommentItem>>>,
    hasNextPage: Boolean = false,
    isLoadingMore: Boolean = false,
    isRefreshing: Boolean = false,
    newCommentsAvailable: Int = 0,
    scrollToTopTrigger: Int = 0,
    onLoadMore: () -> Unit = {},
    onLoadNewComments: () -> Unit = {},
    onRefresh: (() -> Unit)? = null,
    onScrollingChanged: (Boolean) -> Unit = {},
    onAtTopChanged: (Boolean) -> Unit = {},
    onAction: (CommentAction) -> Unit,
    showRepliesInline: Boolean = false,
    commentsDisabled: Boolean = false,
    isUserLoggedIn: Boolean = true,
    isSubscriber: Boolean = false,
    summaryContent: @Composable (() -> Unit)? = null,
    tabsContent: @Composable (() -> Unit)? = null,
) {
    val listState = rememberLazyListState()

    // React to scroll-to-top trigger
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            listState.scrollToItem(0)
        }
    }

    // Notify parent when scrolling starts/stops so FAB can collapse/expand
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { isScrolling -> onScrollingChanged(isScrolling) }
    }

    // Notify parent whether the list is scrolled back to the top (first item visible at offset 0)
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
            .distinctUntilChanged()
            .collect { atTop -> onAtTopChanged(atTop) }
    }

    // Trigger load more when user is near the end of the list
    LaunchedEffect(listState, hasNextPage, isLoadingMore, comments.size) {
        snapshotFlow {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleIndex >= totalItems - 2
        }
            .distinctUntilChanged()
            .collect { nearEnd ->
                if (nearEnd && hasNextPage && !isLoadingMore && comments.isNotEmpty()) {
                    onLoadMore()
                }
            }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { onRefresh?.invoke() },
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val nestedScrollConnection = rememberNestedScrollInteropConnection()
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().nestedScroll(nestedScrollConnection)
            ) {
                if (summaryContent != null) {
                    item(key = "summary_header") {
                        summaryContent()
                    }
                }

                if (tabsContent != null) {
                    stickyHeader(key = "tabs_header") {
                        tabsContent()
                    }
                }

                items(
                    items = comments,
                    key = { it.id }
                ) { comment ->
                    if (comment.videoItem != null) {
                        VideoCommentRow(
                            comment = comment,
                            depth = 0,
                            repliesMap = repliesMap,
                            showRepliesInline = showRepliesInline,
                            commentsDisabled = commentsDisabled,
                            isUserLoggedIn = isUserLoggedIn,
                            isSubscriber = isSubscriber,
                            onAction = onAction
                        )
                    } else {
                        CommentRow(
                            comment = comment,
                            depth = 0,
                            repliesMap = repliesMap,
                            showRepliesInline = showRepliesInline,
                            commentsDisabled = commentsDisabled,
                            isUserLoggedIn = isUserLoggedIn,
                            isSubscriber = isSubscriber,
                            onAction = onAction
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }

                // Pagination footer
                if (isLoadingMore) {
                    item(key = "loading_more") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                if (!hasNextPage && comments.isNotEmpty()) {
                    item(key = "end_of_list") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No more comments",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // "New comments" banner — floating at top
            if (newCommentsAvailable > 0) {
                NewCommentsBanner(
                    count = newCommentsAvailable,
                    onClick = onLoadNewComments,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                )
            }
        }
    }
}

/**
 * This composable displays a single comment row, which includes the comment item
 * and a section for its replies.
 */
@Composable
private fun CommentRow(
    comment: CommentItem,
    depth: Int,
    repliesMap: Map<String, KMPResult<List<CommentItem>>>,
    showRepliesInline: Boolean = false,
    commentsDisabled: Boolean = false,
    isUserLoggedIn: Boolean = true,
    isSubscriber: Boolean = false,
    onAction: (CommentAction) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        CommentItem(
            comment = comment,
            isRepliesExpanded = comment.id in repliesMap,
            commentsDisabled = commentsDisabled,
            isUserLoggedIn = isUserLoggedIn,
            isSubscriber = isSubscriber,
            onAction = onAction
        )
    }

    if (showRepliesInline && depth < MAX_DEPTH) {
        RepliesSection(
            nodeId = comment.id,
            replyCount = comment.replyCount,
            depth = depth,
            repliesMap = repliesMap,
            isUserLoggedIn = isUserLoggedIn,
            isSubscriber = isSubscriber,
            onAction = onAction
        )
    }
}

/**
 * This composable displays a single video comment row, which includes the video comment item
 * and a section for its replies.
 */
@Composable
private fun VideoCommentRow(
    comment: CommentItem,
    depth: Int = 0,
    repliesMap: Map<String, KMPResult<List<CommentItem>>>,
    showRepliesInline: Boolean = false,
    commentsDisabled: Boolean = false,
    isUserLoggedIn: Boolean = true,
    isSubscriber: Boolean = false,
    onAction: (CommentAction) -> Unit
) {
    VideoCommentItem(
        comment = comment,
        isRepliesExpanded = comment.id in repliesMap,
        commentsDisabled = commentsDisabled,
        isUserLoggedIn = isUserLoggedIn,
        isSubscriber = isSubscriber,
        onAction = onAction
    )
    if (showRepliesInline && depth < MAX_DEPTH) {
        RepliesSection(
            nodeId = comment.id,
            replyCount = comment.replyCount,
            depth = depth,
            repliesMap = repliesMap,
            isUserLoggedIn = isUserLoggedIn,
            isSubscriber = isSubscriber,
            onAction = onAction
        )
    }
}

/**
 * Displays the replies to a comment. All replies are grouped under a single
 * vertical lane on the left — no horizontal dividers between child replies.
 */
@Composable
private fun RepliesSection(
    nodeId: String,
    replyCount: Int,
    depth: Int,
    repliesMap: Map<String, KMPResult<List<CommentItem>>>,
    isUserLoggedIn: Boolean = true,
    isSubscriber: Boolean = false,
    onAction: (CommentAction) -> Unit
) {
    val repliesState = repliesMap[nodeId]
    val replies = (repliesState as? KMPResult.Success)?.data.orEmpty()

    if (replies.isNotEmpty()) {
        val visibleReplies = replies.take(MAX_VISIBLE_REPLIES)

        // Single left-lane row wrapping all replies
        Row(modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
        ) {

            // ── Vertical lane ──
            val laneColor = wpdsColors.outline.copy(alpha = 0.3f)
            val laneWidthDp = 1.dp
            val laneStartPaddingDp = 28.dp   // aligns with avatar centre
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(laneStartPaddingDp + laneWidthDp)
                    .drawBehind {
                        val x = laneStartPaddingDp.toPx() + laneWidthDp.toPx() / 2f
                        drawLine(
                            color = laneColor,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = laneWidthDp.toPx()
                        )
                    }
            )

            // ── Replies (no dividers between them) ──
            Column(modifier = Modifier.weight(1f)) {
                visibleReplies.forEach { reply ->
                    if (reply.videoItem != null) {
                        VideoCommentRow(
                            comment = reply,
                            depth = depth + 1,
                            repliesMap = repliesMap,
                            showRepliesInline = true,
                            isUserLoggedIn = isUserLoggedIn,
                            isSubscriber = isSubscriber,
                            onAction = onAction
                        )
                    } else {
                        CommentRow(
                            comment = reply,
                            depth = depth + 1,
                            repliesMap = repliesMap,
                            showRepliesInline = true,
                            isUserLoggedIn = isUserLoggedIn,
                            isSubscriber = isSubscriber,
                            onAction = onAction
                        )
                    }
                }

                if (replyCount > MAX_VISIBLE_REPLIES) {
                    ShowMoreReplyText(
                        maxVisibleReplies = MAX_VISIBLE_REPLIES,
                        replyCount = replyCount,
                        nodeId = nodeId,
                        onAction = onAction
                    )
                }
            }
        }
    }

    if (repliesState is KMPResult.Error) {
        ErrorView(repliesState.message)
    }
}

/**
 * This composable displays a \"Show more replies\" text button, which allows the user
 * to view more replies to a comment.
 */
@Composable
private fun ShowMoreReplyText(
    maxVisibleReplies: Int,
    replyCount: Int,
    nodeId: String,
    onAction: (CommentAction) -> Unit
) {
    Text(
        text = "Show ${replyCount - maxVisibleReplies} more replies",
        color = wpdsColors.onSurfaceSubtle,
        style = MaterialTheme.typography.labelMedium.copy(
            textDecoration = TextDecoration.Underline
        ),
        modifier = Modifier
            .padding(start = 40.dp, top = 4.dp, bottom = 8.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onAction(CommentAction.ShowMoreReplies(nodeId))
            }
    )
}

/**
 * This composable displays an error message in the center of the screen.
 */
@Composable
private fun ErrorView(message: String) {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error
        )
    }
}
