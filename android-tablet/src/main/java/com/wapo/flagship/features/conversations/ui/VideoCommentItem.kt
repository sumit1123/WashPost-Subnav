package com.wapo.flagship.features.conversations.ui

import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.wapo.android.commons.util.commentsTimeAgo
import com.wapo.flagship.features.conversations.model.CommentAction
import com.wapo.kmpshared.features.conversations.domain.models.CommentItem
import com.washingtonpost.android.R
import com.wpds.theme.wpdsColors

/**
 * A composable that displays a single video comment item.
 *
 * @param comment The comment to display.
 * @param isRepliesExpanded Whether the replies to this comment are expanded.
 * @param onAction A callback to be invoked when the user performs an action on the comment.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoCommentItem(
    comment: CommentItem,
    isRepliesExpanded: Boolean = false,
    commentsDisabled: Boolean = false,
    isUserLoggedIn: Boolean = true,
    isSubscriber: Boolean = false,
    onAction: (CommentAction) -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(true) }

    // CommentItem.videoItem has url directly (resolved from CommentNode.extra in KMP-Shared mapping)
    val streamUrl = remember(comment.id) {
        comment.videoItem?.url
    }

    val exoPlayer = remember(streamUrl) {
        streamUrl?.let {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(it.toString()))
                volume = if (isMuted) 0f else 1f
                repeatMode = Player.REPEAT_MODE_ONE
                prepare()
                playWhenReady = false
            }
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingParam: Boolean) {
                isPlaying = isPlayingParam
            }
        }
        exoPlayer?.addListener(listener)
        onDispose {
            exoPlayer?.removeListener(listener)
            exoPlayer?.release()
        }
    }

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
                    verticalAlignment = Alignment.Top,
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

        Spacer(Modifier.height(8.dp))

        // ── Video Player ──
        if (streamUrl != null && exoPlayer != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = {
                        PlayerView(it).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                        }
                )

                // Floating Mute/Unmute Button (Top Right)
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .clickable {
                            isMuted = !isMuted
                            exoPlayer.volume = if (isMuted) 0f else 1f
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            if (isMuted) R.drawable.ic_media_mute else R.drawable.ic_media_unmute
                        ),
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Center Play Button (only shown when not playing)
                if (!isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                            .clickable {
                                isPlaying = true
                                exoPlayer.play()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(com.wpds.wpds.R.drawable.play),
                            contentDescription = "Play",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        }

        // ── Video Body / Link ──
        val bodyText = comment.bodyNodes.toRawHtml().takeIf { it.isNotEmpty() }
        bodyText?.let { body ->
            val url = extractUrlFromBody(body)
            val linkContext = LocalContext.current
            Spacer(Modifier.height(6.dp))
            if (url != null) {
                Text(
                    text = url,
                    color = wpdsColors.blue100,
                    fontSize = 14.sp,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            addCategory(Intent.CATEGORY_BROWSABLE)
                        }
                        linkContext.startActivity(intent)
                    }
                )
            } else {
                Text(
                    text = body,
                    color = wpdsColors.onSurface,
                    fontSize = 14.sp
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

/**
 * Extracts a URL from a string of HTML.
 *
 * @param body The HTML string.
 * @return The extracted URL, or null if no URL is found.
 */
fun extractUrlFromBody(body: String?): String? {
    if (body == null) return null
    return Regex("""href="([^"]+)"""").find(body)?.groupValues?.get(1)
}
