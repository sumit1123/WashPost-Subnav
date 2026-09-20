package com.wapo.flagship.features.articles3.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.wpds.wpds.R
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles3.models.ui.PodcastUiModel
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.FranklinItcStandardFontFamily
import com.wpds.theme.wpdsColors

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun PodcastView(
    uiModel: PodcastUiModel,
    articlesInteractionHelper: ArticlesInteractionHelper
) {
    val statusAnnotated = buildPodcastStatusText(uiModel)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(wpdsColors.gray500)
            .clickable { articlesInteractionHelper.onEventFired(ArticleInteractionEvent.AudioItemClicked(uiModel.rawUrl)) }
            .padding(16.dp)
    ) {
        // Series title: "SeriesName Podcast" with bold series name
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(uiModel.seriesName)
                }
                append(" Podcast")
            },
            color = wpdsColors.onSurface,
            fontSize = 14.sp,
            fontFamily = FontFamily(Font(R.font.franklinitcstd_light)),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Series image
            if (!uiModel.seriesImageUrl.isNullOrEmpty()) {
                GlideImage(
                    model = uiModel.seriesImageUrl,
                    contentDescription = "${uiModel.seriesName} podcast image",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(wpdsColors.gray400),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                // Episode title
                Text(
                    text = uiModel.episodeName,
                    color = wpdsColors.onSurface,
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.franklinitcstd_bold)),
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Play/pause controls + status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PodcastPlaybackIcon(
                        playbackState = uiModel.playbackState,
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = statusAnnotated,
                        color = wpdsColors.onSurface,
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.franklinitcstd_light)),
                    )
                }
            }
        }
    }
}

@Composable
fun InlinePodcastView(
    uiModel: PodcastUiModel,
    articlesInteractionHelper: ArticlesInteractionHelper
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .wrapContentHeight()
            .clickable { articlesInteractionHelper.onEventFired(ArticleInteractionEvent.AudioItemClicked(uiModel.rawUrl)) }
    ) {
        PodcastPlaybackIcon(
            playbackState = uiModel.playbackState,
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Status label
        val statusText = when (uiModel.playbackState) {
            AudioPlaybackState.PLAYING -> "Now playing"
            AudioPlaybackState.BUFFERING -> "Buffering..."
            else -> "Listen"
        }
        Text(
            text = statusText,
            color = wpdsColors.onSurface,
            fontSize = 14.sp,
            fontFamily = FranklinItcStandardFontFamily,
        )

        // Duration (hidden when playing/buffering)
        if (uiModel.durationText != null && uiModel.playbackState == AudioPlaybackState.READY) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = uiModel.durationText,
                color = wpdsColors.gray200,
                fontSize = 14.sp,
                fontFamily = FranklinItcStandardFontFamily,
            )
        }
    }
}

// region Internal composables

@Composable
private fun PodcastPlaybackIcon(
    playbackState: AudioPlaybackState,
) {
    Box(
        modifier = Modifier.size(32.dp),
        contentAlignment = Alignment.Center
    ) {
        when (playbackState) {
            AudioPlaybackState.PLAYING -> {
                Image(
                    painter = painterResource(id = R.drawable.pause),
                    contentDescription = "Pause",
                    modifier = Modifier.size(32.dp)
                )
            }
            AudioPlaybackState.BUFFERING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size((32 - 4).dp),
                    strokeWidth = 2.dp,
                    color = wpdsColors.primary
                )
            }
            else -> {
                Image(
                    painter = painterResource(id = R.drawable.play),
                    contentDescription = "Play",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

private fun buildPodcastStatusText(uiModel: PodcastUiModel): String {
    val label = when (uiModel.playbackState) {
        AudioPlaybackState.PLAYING -> "Now Playing"
        AudioPlaybackState.BUFFERING -> "Buffering"
        else -> "Listen"
    }
    return if (uiModel.durationText != null && uiModel.playbackState == AudioPlaybackState.READY) {
        "$label - ${uiModel.durationText}"
    } else {
        label
    }
}

// endregion

enum class PodcastUiStyle {
    DEFAULT,
    INLINE
}

// region Previews

@Preview(showBackground = true)
@Composable
private fun PodcastViewReadyPreview() {
    AndroidClassicTheme {
        PodcastView(
            uiModel = PodcastUiModel(
                rawUrl = "podcast-123",
                seriesName = "Post Reports",
                episodeName = "Why the economy is sending mixed signals",
                seriesImageUrl = "https://s3.amazonaws.com/arc-authors/washpost/054223d1-918c-4d39-965f-e1877701e420.png",
            ),
            dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PodcastViewPlayingPreview() {
    AndroidClassicTheme {
        PodcastView(
            uiModel = PodcastUiModel(
                rawUrl = "podcast-123",
                seriesName = "Post Reports",
                episodeName = "Why the economy is sending mixed signals",
                seriesImageUrl = "https://s3.amazonaws.com/arc-authors/washpost/054223d1-918c-4d39-965f-e1877701e420.png",
            ),
            dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PodcastViewBufferingPreview() {
    AndroidClassicTheme {
        PodcastView(
            uiModel = PodcastUiModel(
                rawUrl = "podcast-123",
                seriesName = "Post Reports",
                episodeName = "Why the economy is sending mixed signals",
                seriesImageUrl = "https://s3.amazonaws.com/arc-authors/washpost/054223d1-918c-4d39-965f-e1877701e420.png",
            ),
            dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PodcastViewNoImagePreview() {
    AndroidClassicTheme {
        PodcastView(
            uiModel = PodcastUiModel(
                rawUrl = "podcast-456",
                seriesName = "Can He Do That?",
                episodeName = "The limits of executive power in the modern era",
            ),
            dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InlinePodcastViewReadyPreview() {
    AndroidClassicTheme {
        InlinePodcastView(
            uiModel = PodcastUiModel(
                rawUrl = "podcast-789",
                seriesName = "Can He Do That?",
                episodeName = "The limits of executive power in the modern era",
            ),
            dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InlinePodcastViewPlayingPreview() {
    AndroidClassicTheme {
        InlinePodcastView(
            uiModel = PodcastUiModel(
                rawUrl = "podcast-789",
                seriesName = "Can He Do That?",
                episodeName = "The limits of executive power in the modern era",
            ),
            dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InlinePodcastViewBufferingPreview() {
    AndroidClassicTheme {
        InlinePodcastView(
            uiModel = PodcastUiModel(
                rawUrl = "podcast-789",
                seriesName = "Can He Do That?",
                episodeName = "The limits of executive power in the modern era",
            ),
            dummyArticlesInteractionHelper
        )
    }
}

// endregion

