package com.wapo.flagship.features.conversations.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.wapo.android.commons.util.commentsTimeAgo
import com.wapo.flagship.features.conversations.model.CommentAction
import com.wapo.kmpshared.features.conversations.domain.models.CommentItem
import com.washingtonpost.android.R
import com.washingtonpost.android.paywall.util.rememberHtmlAnnotatedString
import com.wpds.theme.wpdsColors

/**
 * This composable is responsible for displaying a single comment item in the conversation.
 * It shows the author's avatar and username, the time the comment was posted, and the
 * comment body. It also includes an action bar with options to react, reply, and more.
 */
@Composable
fun CommentItem(
    comment: CommentItem,
    isRepliesExpanded: Boolean = false,
    commentsDisabled: Boolean = false,
    isUserLoggedIn: Boolean = true,
    isSubscriber: Boolean = false,
    onAction: (CommentAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // ── Author Row ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            val hasStaffTag = comment.isStaff()
            val isSource = comment.isSource()
            val hasAvatar = !comment.author?.avatar.isNullOrBlank() || isSource
            if (hasAvatar) {
                Box(modifier = Modifier.size(36.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(wpdsColors.gray400),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSource) {
                            Image(
                                painter = painterResource(R.drawable.avatar_fromthesource),
                                contentDescription = null,
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                            )
                        } else {
                            AsyncImage(
                                model = comment.author?.avatar,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp).clip(CircleShape),
                                placeholder = painterResource(com.wpds.wpds.R.drawable.profile)
                            )
                        }
                    }
                    // Tag badge - bottom right with overlap
                    if (hasStaffTag) {
                        Image(
                            painter = painterResource(R.drawable.commenter_verification_squiggle_logo_false),
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(20.dp)
                                .offset(x = 4.dp, y = 4.dp)
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
            }

            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        comment.author?.username ?: "",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = wpdsColors.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        commentsTimeAgo(comment.createdAt.time),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Normal,
                        color = wpdsColors.onSurfaceSubtle,
                        fontSize = 13.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                comment.roleText?.let { role ->
                    Text(
                        text = role,
                        style = MaterialTheme.typography.labelSmall,
                        color = wpdsColors.onSurfaceSubtle,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // ── Comment Body ──
        var expanded by rememberSaveable { mutableStateOf(false) }
        var hasOverflow by remember { mutableStateOf(false) }
        val defaultCommentBodylineLimit = 2
        val rawHtml = remember(comment.bodyNodes) { comment.bodyNodes.toRawHtml() }
        val annotatedBody = rememberHtmlAnnotatedString(rawHtml)

        Column(
            Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = annotatedBody,
                style = MaterialTheme.typography.bodyMedium,
                color = wpdsColors.onSurface,
                maxLines = if (expanded) Int.MAX_VALUE else defaultCommentBodylineLimit,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = {
                    hasOverflow = !expanded && (it.hasVisualOverflow || it.lineCount > defaultCommentBodylineLimit)
                },
                fontSize = 16.sp
            )

            if (hasOverflow || expanded) {
                Text(
                    text = if (expanded) "Show less" else "Show more",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = wpdsColors.onSurfaceSubtle,
                        fontWeight = FontWeight.Normal,
                        textDecoration = TextDecoration.Underline,
                        fontSize = 14.sp
                    ),
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clickable { expanded = !expanded }
                )
            }
        }

        // ── Actions Bar (includes reactions, reply, more menu, replies toggle) ──
        CommentActionsBar(
            comment = comment,
            isRepliesExpanded = isRepliesExpanded,
            commentsDisabled = commentsDisabled,
            isUserLoggedIn = isUserLoggedIn,
            isSubscriber = isSubscriber,
            onAction = onAction,
            onFlag = { onAction(CommentAction.Flag(it)) }
        )
    }
}
