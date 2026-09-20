package com.wapo.flagship.features.conversations.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wapo.flagship.features.conversations.model.CommentAction
import com.wapo.kmpshared.features.conversations.domain.models.CommentItem
import com.wpds.theme.wpdsColors
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * This composable displays a list of all replies to a given parent comment.
 * It includes a header with back and close buttons, the parent comment itself,
 * and a scrollable list of replies. It also supports loading more replies as
 * the user scrolls to the end of the list.
 */
@Composable
fun AllRepliesView(
    parentComment: CommentItem,
    replies: List<CommentItem>,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onAction: (CommentAction) -> Unit,
    onLoadMoreReplies: ((CommentItem) -> Unit)? = null,
    commentsDisabled: Boolean = false,
    isUserLoggedIn: Boolean = true,
    isSubscriber: Boolean = false,
) {
    val listState = rememberLazyListState()

    BackHandler(onBack = onBack)

    LaunchedEffect(listState, replies.size) {
        snapshotFlow {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleIndex >= replies.size - 4 && replies.isNotEmpty()
        }.distinctUntilChanged().collect { nearEnd ->
            if (nearEnd) {
                onLoadMoreReplies?.invoke(replies.last())
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Action bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                colors = IconButtonDefaults.iconButtonColors(contentColor = wpdsColors.onSurface)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = "Replies",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = wpdsColors.onSurface,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            IconButton(
                onClick = onClose,
                colors = IconButtonDefaults.iconButtonColors(contentColor = wpdsColors.onSurface)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close"
                )
            }
        }

        HorizontalDivider()

        // All replies (with parent comment as first item so everything scrolls)
        Box(Modifier.weight(1f)) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                // Parent comment at top (scrollable)
                item(key = "parent_comment") {
                    CommentItem(
                        comment = parentComment,
                        isRepliesExpanded = false,
                        commentsDisabled = commentsDisabled,
                        isUserLoggedIn = isUserLoggedIn,
                        isSubscriber = isSubscriber,
                        onAction = onAction
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = wpdsColors.outline.copy(alpha = 0.3f)
                    )
                }

                items(items = replies, key = { it.id }) { reply ->
                    ReplyWithChildren(
                        reply = reply,
                        commentsDisabled = commentsDisabled,
                        isUserLoggedIn = isUserLoggedIn,
                        isSubscriber = isSubscriber,
                        onAction = onAction
                    )
                }
            }

            if (!commentsDisabled && isSubscriber) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    CommentButton(
                        onClick = { onAction(CommentAction.Reply(parentComment)) },
                        isScrolling = listState.isScrollInProgress,
                        text = "Reply"
                    )
                }
            }
        }
    }
}

/**
 * Displays a single reply with a vertical lane on the left.
 * Recursively renders child replies with their own vertical lanes.
 * No horizontal divider — the lane visually groups all replies together.
 */
@Composable
private fun ReplyWithChildren(
    reply: CommentItem,
    commentsDisabled: Boolean = false,
    isUserLoggedIn: Boolean = true,
    isSubscriber: Boolean = false,
    onAction: (CommentAction) -> Unit
) {
    val laneColor = wpdsColors.outline.copy(alpha = 0.3f)
    val laneWidthDp = 1.dp
    val laneStartPaddingDp = 28.dp  // aligns with avatar centre

    Row(modifier = Modifier
        .fillMaxWidth()
        .height(IntrinsicSize.Min)
    ) {
        // ── Vertical lane ──
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

        // ── Reply content only — child replies open in their own screen ──
        CommentItem(
            comment = reply,
            isRepliesExpanded = false,
            commentsDisabled = commentsDisabled,
            isUserLoggedIn = isUserLoggedIn,
            isSubscriber = isSubscriber,
            onAction = onAction
        )
    }
}