package com.wapo.flagship.features.conversations.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.wapo.flagship.features.conversations.model.CommentAction
import com.wapo.kmpshared.features.conversations.domain.models.CommentItem
import com.washingtonpost.android.R
import com.wpds.theme.wpdsColors

/**
 * This composable displays a bar with actions that can be performed on a comment,
 * such as reacting, replying, and reporting. It also includes a toggle to show
 * or hide replies.
 */
@Composable
fun CommentActionsBar(
    comment: CommentItem,
    isRepliesExpanded: Boolean = false,
    commentsDisabled: Boolean = false,
    isUserLoggedIn: Boolean = true,
    isSubscriber: Boolean = false,
    onAction: (CommentAction) -> Unit,
    onFlag: (CommentItem) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val isFlagged = comment.viewerAction.flagged

    val commentId = comment.id
    val replyCount = comment.replyCount

    Spacer(Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ReactionsRow(
            comment = comment,
            isUserLoggedIn = isUserLoggedIn,
            onAction = onAction
        )

        Spacer(Modifier.width(16.dp))

        // When there are existing replies, open the AllRepliesView.
        // When count is 0 and the user can reply, go directly to compose to avoid an
        // extra back-press step through an empty replies screen.
        // We only allow replying if the conversation is not disabled and the user is a subscriber.
        val canReply = !commentsDisabled && isSubscriber
        val showReplyAction = replyCount > 0 || canReply

        if (showReplyAction) {
            val onClick: () -> Unit = if (replyCount > 0) {
                { onAction(CommentAction.ShowReplies(commentId, comment)) }
            } else {
                { onAction(CommentAction.Reply(comment)) }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    onClick()
                }
            ) {
                Icon(
                    painter = painterResource(com.wpds.wpds.R.drawable.comment),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = wpdsColors.gray100
                )
                if (replyCount > 0) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "$replyCount",
                        color = wpdsColors.gray100
                    )
                }
                if (canReply) {
                    Spacer(Modifier.width(4.dp))
                    Text("Reply", color = wpdsColors.gray100)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        if (isUserLoggedIn) {
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    colors = IconButtonDefaults.iconButtonColors(contentColor = wpdsColors.onSurface)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_horizontal),
                        contentDescription = null
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    containerColor = wpdsColors.surface
                ) {
                    DropdownMenuItem(
                        text = { Text(if (isFlagged) "Flagged" else "Report", color = wpdsColors.onSurface) },
                        onClick = {
                            showMenu = false
                            if (!isFlagged) {
                                onFlag(comment)
                            }
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = wpdsColors.onSurface,
                            leadingIconColor = wpdsColors.onSurface,
                            trailingIconColor = wpdsColors.onSurface,
                            disabledTextColor = wpdsColors.onSurfaceSubtle,
                            disabledLeadingIconColor = wpdsColors.onSurfaceSubtle,
                            disabledTrailingIconColor = wpdsColors.onSurfaceSubtle
                        )
                    )
                }
            }
        }
    }
}