package com.wapo.flagship.features.articles3.views

import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.wapo.android.commons.util.formatTimeMillis
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles3.models.ui.VideoUiModel
import com.wapo.flagship.features.articles3.parseHtmlContent
import com.wapo.flagship.features.posttv.VideoManager2
import com.washingtonpost.android.R
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.ArticleTextStyles
import com.wpds.theme.FranklinItcStandardFontFamily
import com.wpds.theme.wpdsColors
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun VideoView(
    uiModel: VideoUiModel,
    articlesInteractionHelper: ArticlesInteractionHelper,
    activeVideoIds: StateFlow<Set<String>>? = null,
    videoManager2: VideoManager2? = null,
    resolveVideoStreamId: ((String) -> String?)? = null,
    captionPadding: PaddingValues = PaddingValues(top = 8.dp),
) {
    val activeIds by activeVideoIds?.collectAsState()
        ?: remember { androidx.compose.runtime.mutableStateOf(emptySet<String>()) }
    val isPlaying = uiModel.id in activeIds

    // ── Autoplay visibility tracking ──
    val rootView = LocalView.current
    var isVisibleOnScreen by remember { mutableStateOf(false) }

    LaunchedEffect(isVisibleOnScreen, isPlaying) {
        if (uiModel.isAutoplayEligible) {
            if (isVisibleOnScreen && !isPlaying) {
                articlesInteractionHelper.onEventFired(
                    ArticleInteractionEvent.VideoAutoplayEvent(uiModel.id)
                )
            } else if (!isVisibleOnScreen && isPlaying) {
                articlesInteractionHelper.onEventFired(
                    ArticleInteractionEvent.VideoOffscreenEvent(uiModel.id)
                )
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (uiModel.aspectRatio < 1) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .then(
                    if (uiModel.aspectRatio < 1) {
                        Modifier.width(300.dp)
                    } else {
                        Modifier.fillMaxWidth()
                    }
                )
                .aspectRatio(uiModel.aspectRatio)
                .background(wpdsColors.gray400)
                .onGloballyPositioned { coordinates ->
                    val windowBounds = Rect(
                        0f, 0f,
                        rootView.width.toFloat(), rootView.height.toFloat()
                    )
                    val itemBounds = coordinates.boundsInWindow()
                    val visibleHeight = (
                            minOf(itemBounds.bottom, windowBounds.bottom) -
                                    maxOf(itemBounds.top, windowBounds.top)
                            ).coerceAtLeast(0f)
                    val itemHeight = itemBounds.height
                    val fractionVisible = if (itemHeight > 0f) visibleHeight / itemHeight else 0f
                    isVisibleOnScreen = fractionVisible >= 0.5f
                }
        ) {
            if (isPlaying && videoManager2 != null && resolveVideoStreamId != null) {
                // Show the native PostTv player frame via AndroidView
                val streamId = resolveVideoStreamId(uiModel.id)
                val playerFrame = streamId?.let { videoManager2.getPlayerFrameContainer(it) }
                if (streamId != null && playerFrame != null) {
                    AndroidView(
                        factory = { ctx ->
                            FrameLayout(ctx).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                )
                            }
                        },
                        update = { hostFrame ->
                            val currentPlayerFrame = videoManager2.getPlayerFrameContainer(streamId)
                            if (currentPlayerFrame != null && currentPlayerFrame.parent !== hostFrame) {
                                // Remove from previous parent (another holder or host)
                                (currentPlayerFrame.parent as? android.view.ViewGroup)?.removeView(
                                    currentPlayerFrame
                                )
                                hostFrame.removeAllViews()
                                hostFrame.addView(currentPlayerFrame)
                                currentPlayerFrame.bringToFront()
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                    DisposableEffect(streamId) {
                        onDispose {
                            // When this composable leaves, detach player frame so VideoManager2 can reuse it
                            videoManager2.removePlayerFrame(streamId)
                        }
                    }
                }
            } else {
                // Thumbnail overlay (tap to play)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            articlesInteractionHelper.onEventFired(
                                ArticleInteractionEvent.VideoClickEvent(uiModel.id)
                            )
                        }
                ) {
                    // Thumbnail image
                    GlideImage(
                        model = uiModel.thumbnailUrl,
                        contentDescription = uiModel.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Bottom gradient overlay with play button, title, and duration
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xCC000000))
                                )
                            )
                            .padding(start = 8.dp, end = 16.dp, top = 32.dp, bottom = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Play / Live button icon
                            Icon(
                                painter = painterResource(
                                    id = if (uiModel.isLive) com.washingtonpost.android.articles.R.drawable.btn_live_article else com.washingtonpost.android.articles.R.drawable.btn_play_article
                                ),
                                contentDescription = if (uiModel.isLive) "Live" else "Play",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(40.dp)
                            )

                            Column(
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .weight(1f)
                            ) {
                                // Headline (1 line max, matching legacy headline TextView)
                                if (!uiModel.title.isNullOrEmpty()) {
                                    Text(
                                        text = uiModel.title,
                                        style = getVideoHeadlineStyle(),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }

                                // Duration or LIVE label
                                if (uiModel.isLive) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(Color.Red, CircleShape)
                                        )
                                        Text(
                                            text = "LIVE",
                                            style = getVideoDurationStyle(),
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                } else {
                                    val durationText = formatTimeMillis(uiModel.durationMs)
                                    if (!durationText.isNullOrEmpty()) {
                                        Text(
                                            text = durationText,
                                            style = getVideoDurationStyle()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Caption below video
        if (!uiModel.caption.isNullOrEmpty()) {
            Text(
                text = parseHtmlContent(uiModel.caption, articlesInteractionHelper),
                style = ArticleTextStyles.IMAGE_CAPTION.style,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(captionPadding)
            )
        }
    }
}

private fun getVideoHeadlineStyle() = TextStyle(
    color = Color.White,
    fontSize = 16.sp,
    fontFamily = FranklinItcStandardFontFamily,
    fontWeight = FontWeight.Bold,
    lineHeight = 20.sp
)

private fun getVideoDurationStyle() = TextStyle(
    color = Color.White,
    fontSize = 12.sp,
    fontFamily = FranklinItcStandardFontFamily,
    fontWeight = FontWeight.Normal,
    lineHeight = 16.sp
)

enum class VideoUiStyle {
    DEFAULT
}
// region Previews

@Preview(showBackground = true)
@Composable
private fun VideoViewDefaultPreview() {
    AndroidClassicTheme {
        VideoView(
            uiModel = VideoUiModel(
                contentUrl = "https://www.washingtonpost.com/video/example/default_video.html",
                id = "1234",
                thumbnailUrl = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/V5IZ4G7Y763R54RSBQGN6M3XZE.jpg",
                title = "Watch: Officials address the press following the announcement",
                durationMs = TimeUnit.SECONDS.toMillis(245),
                isAutoplay = false,
                isLive = false,
                isLooping = false,
                caption = "Press secretary speaks at the podium. (Video by The Washington Post)",
            ),
            articlesInteractionHelper = dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VideoViewLivePreview() {
    AndroidClassicTheme {
        VideoView(
            uiModel = VideoUiModel(
                contentUrl = "https://www.washingtonpost.com/video/example/live_video.html",
                id = "1234",
                thumbnailUrl = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/OIBALHVNOAI6XAWBRFVMVFK3XE.jpg",
                title = "Live: Senate hearing on technology regulation",
                durationMs = null,
                isAutoplay = false,
                isLive = true,
                isLooping = false,
                caption = null,
            ),
            articlesInteractionHelper = dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VideoViewNoCaptionPreview() {
    AndroidClassicTheme {
        VideoView(
            uiModel = VideoUiModel(
                contentUrl = "https://www.washingtonpost.com/video/example/no_caption_video.html",
                id = "1234",
                thumbnailUrl = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/V2GTS4SWZYI6ZKAIGGL2EKYZ7I.jpg",
                title = "Full speech: President addresses the nation on the economy",
                durationMs = TimeUnit.MINUTES.toMillis(12) + TimeUnit.SECONDS.toMillis(34),
                isAutoplay = false,
                isLive = false,
                isLooping = false,
                caption = null,
            ),
            articlesInteractionHelper = dummyArticlesInteractionHelper
        )
    }
}

// endregion

