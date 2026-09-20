package com.washingtonpost.foryou.ui

import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.wapo.android.commons.util.formatTimeSecondsVideoDuration
import com.wapo.flagship.features.grid.GridEnvironment
import com.wapo.flagship.features.posttv.VideoTracker2
import com.wapo.flagship.features.posttv.model.Video
import com.washingtonpost.foryou.R
import com.washingtonpost.foryou.viewmodel.ForYouViewModel
import com.washingtonpost.userhistory.models.RecommendationsHelperItem
import com.washingtonpost.userhistory.viewmodel.UserHistoryViewModel
import com.wpds.theme.AndroidClassicTheme
import kotlinx.coroutines.delay

private val franklinFont = FontFamily(Font(com.wapo.view.R.font.wp_franklinitcstd_font_family))

@OptIn(ExperimentalGlideComposeApi::class)
@UnstableApi
@Composable
fun ForYouVideoView(
    video: Video,
    onEllipsisClicked: () -> Unit,
    gridEnvironment: GridEnvironment,
    userHistoryViewModel: UserHistoryViewModel,
    recommendationsHelperItem: RecommendationsHelperItem,
    index: Int,
    forYouViewModel: ForYouViewModel,
    isActive: Boolean,
) {

    val context = LocalContext.current
    val remainingTime = remember { mutableLongStateOf(video.duration ?: 0L) }

    val showImage = remember { mutableStateOf(false) }
    val isTabActive by forYouViewModel.isTabActive.collectAsState()
    val player2Manager = forYouViewModel.playerManager.value

    val startTimeForAutoplay = remember { mutableLongStateOf(0L) }

    AndroidClassicTheme {
        Card (
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio((9.0 / 16.0).toFloat(), true)
                .semantics {
                    contentDescription = video.altText ?: ""
                },
            shape = RoundedCornerShape(6.dp),
            elevation = 4.dp
        ) {
            Column (
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    key(isTabActive) {
                        if (isActive && !showImage.value) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                GlideImage( // blurred thumbnail
                                    model = video.mediaUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize()
                                )
                                AndroidView(
                                    factory = {
                                        player2Manager?.getPlayerContainerView()?.also { view ->
                                            (view.parent as? ViewGroup)?.removeView(view)
                                            val playerView = view.findViewById<PlayerView>(com.wapo.flagship.features.posttv.R.id.player_view)
                                            playerView?.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                            playerView?.useController = false
                                        } ?: View(context)
                                    },
                                    modifier = Modifier
                                        .fillMaxSize(),
                                )
                            }
                            LaunchedEffect(video.id, isActive) {
                                val tracking =
                                    VideoTracker2.Tracking(
                                        progressThreshold = 1,
                                        VideoTracker2.VideoType.STANDARD_AUTOPLAY,
                                        video = video
                                    )
                                player2Manager?.playMedia(video, true, tracking)
                            }

                            LaunchedEffect(video, isActive) {
                                if (isActive) {
                                    while (remainingTime.longValue > 0) {
                                        val currentPositionSeconds =
                                            (player2Manager?.getPlaybackPosition() ?: 0L) / 1000
                                        val totalDuration = video.duration ?: 0L
                                        remainingTime.longValue =
                                            (totalDuration - currentPositionSeconds).coerceAtLeast(0L)
                                        startTimeForAutoplay.longValue+=1000L
                                        if (remainingTime.longValue != 0L) {
                                            delay(1000)
                                        } else {
                                            showImage.value = true
                                        }
                                    }
                                    remainingTime.longValue = video.duration
                                }
                            }
                        } else {
                            GlideImage(
                                model = video.mediaUrl,
                                contentDescription = "Wp Video Image",
                                modifier =
                                    Modifier
                                        .aspectRatio((9.0/16.0).toFloat())
                                        .clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop,
                            )
                            userHistoryViewModel.updateAutoPlayDuration(
                                recommendationsHelperItem,
                                player2Manager?.getPlaybackPosition() ?: 0L,
                                player2Manager?.getDuration() ?: 0L
                            )
                        }
                    }

                    LaunchedEffect(isActive, isTabActive) {
                        if (!isActive) {
                            showImage.value = true
                            player2Manager?.stopTracking()
                        } else if (isTabActive && isActive) {
                            showImage.value = false
                            val tracking =
                                VideoTracker2.Tracking(
                                    progressThreshold = 1,
                                    VideoTracker2.VideoType.STANDARD_AUTOPLAY,
                                    video = video
                                )
                            player2Manager?.playMedia(video, true, tracking)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.00f to Color.Transparent,
                                        0.50f to Color.Transparent,
                                        0.7f to Color.Black.copy(alpha = 0.4f),
                                        1f to Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                    ) {

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            // Bottom left: Duration
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(start = 10.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatTimeSecondsVideoDuration(remainingTime.longValue)
                                        ?: "",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    modifier = Modifier
                                        .background(
                                            color = Color.Black.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Divider(
                            color = Color(0x66FFFFFF),
                            thickness = 1.dp,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 5.dp)
                        )

                        CompositionLocalProvider(
                            LocalDensity provides Density(
                                density = LocalDensity.current.density,
                                fontScale = 1f
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 15.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                ) {
                                    video.videoCategory?.let {
                                        Text(
                                            text = it,
                                            color = Color.White,
                                            fontFamily = franklinFont,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.weight(1.5f),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(color = Color(0x26FFFFFF))
                                            .clickable {
                                                onEllipsisClicked()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_ellipsis_img),
                                            contentDescription = "More",
                                            tint = Color(0xFFF0F0F0),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                        }
                    }
                }
            }
        }
    }
}

private fun Dp.toPx(current: Density): Float {
    return this.value * current.density
}
