package com.wapo.flagship.features.wpvideos.ui

import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.formatTimeSecondsVideoDuration
import com.wapo.android.commons.util.truncatedString
import com.wapo.flagship.features.posttv.PostTvPlayer2Manager
import com.wapo.flagship.features.posttv.VideoTracker2
import com.wapo.flagship.features.posttv.model.Video
import com.washingtonpost.android.R
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color as ComposeColor

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun WatchVideoView(
    modifier: Modifier = Modifier,
    player2Manager: PostTvPlayer2Manager?,
    isPlaying: Boolean,
    videoItem: Video,
    activeIndex: Int,
    rows: Int,
    onClick: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val orientation = configuration.orientation

    // Adjust rows or height based on orientation
    val gridPadding = 7.dp * (rows + 1) + 60.dp
    val isLandscape = orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val aspectRatio = 1.10f
    val aspectRatioTablet = 2.4f
    val aspectRatioPhoneLandscape = 2.2f

    val videoHeight = if (AppContextUtils.isTablet() && isLandscape) {
        screenWidth / aspectRatioTablet
    } else if (isLandscape) {
        screenWidth / aspectRatioPhoneLandscape
    } else {
        ((screenHeight - gridPadding) / rows).coerceAtMost(screenWidth / aspectRatio)
    }
    val imageUrl = videoItem.mediaUrl
    val duration = videoItem.duration
    val fadeInAlpha = remember { Animatable(0f) }
    val remainingTime = remember { androidx.compose.runtime.mutableStateOf(duration ?: 0L) }

    LaunchedEffect(imageUrl) {
        fadeInAlpha.animateTo(1f, animationSpec = tween(durationMillis = 300))
    }

    LaunchedEffect(activeIndex) {
        remainingTime.value = duration ?: 0L
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        color = ComposeColor.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(videoHeight)
                .graphicsLayer { alpha = fadeInAlpha.value }
                .semantics {
                    contentDescription = videoItem.headline.toString()
                }
        ) {
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(videoHeight)

                ) {
                    AndroidView(
                        factory = { context ->
                            player2Manager?.getPlayerContainerView()?.also { view ->
                                (view.parent as? ViewGroup)?.removeView(view)
                                val playerView = view.findViewById<PlayerView>(R.id.player_view)
                                playerView?.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                playerView?.useController = false
                            } ?: View(context)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(videoHeight)
                            .background(ComposeColor.Black.copy(alpha = 0.2f))
                    ) {
                        // Bottom left: Play/Pause and Duration
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 10.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTimeSecondsVideoDuration(remainingTime.value.toLong())
                                    ?: "",
                                fontSize = 12.sp,
                                color = ComposeColor.White,
                                modifier = Modifier
                                    .background(
                                        color = ComposeColor.Black.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    LaunchedEffect(videoItem) {
                        player2Manager?.apply {
                            val tracking =
                                VideoTracker2.Tracking(
                                    progressThreshold = activeIndex + 1,
                                    VideoTracker2.VideoType.WATCH_AUTOPLAY,
                                    video = videoItem,
                                )
                            playMedia(videoItem, true, tracking)
                        }
                    }
                    LaunchedEffect(videoItem) {
                        while (remainingTime.value > 0) {
                            val currentPositionSeconds =
                                (player2Manager?.getPlaybackPosition() ?: 0L) / 1000
                            val totalDuration = duration ?: 0L
                            remainingTime.value =
                                (totalDuration - currentPositionSeconds).coerceAtLeast(0L)
                            delay(1000)
                        }
                    }
                }
            } else {
                GlideImage(
                    model = imageUrl,
                    contentDescription = "Wp Video Image",
                    modifier =
                        Modifier
                            .height(videoHeight)
                            .width(screenWidth)
                            .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            if (!isPlaying) {
                Text(
                    text = duration?.let { formatTimeSecondsVideoDuration(it) } ?: "",
                    fontSize = 12.sp,
                    color = ComposeColor.White,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 10.dp, bottom = 10.dp)
                        .background(
                            color = ComposeColor.Black.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            if ((videoItem.commentCount ?: 0) > 0) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 10.dp, bottom = 10.dp)
                        .background(
                            color = ComposeColor.Black.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = com.wpds.wpds.R.drawable.ic_comment_elipse),
                        contentDescription = "Comments",
                        tint = ComposeColor.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = videoItem.commentCount?.truncatedString().orEmpty(),
                        fontSize = 12.sp,
                        color = ComposeColor.White,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}