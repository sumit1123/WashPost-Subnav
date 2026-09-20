package com.wapo.flagship.features.articles3.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles3.models.ui.HumanAudioUiModel
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.FranklinItcStandardFontFamily
import com.wpds.theme.wpdsColors

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun HumanAudioView(
    uiModel: HumanAudioUiModel,
    articlesInteractionHelper: ArticlesInteractionHelper
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .wrapContentHeight()
            .background(
                color = wpdsColors.surface,
                shape = RoundedCornerShape(50.dp)
            )
            .border(
                width = 1.dp,
                color = wpdsColors.gray300,
                shape = RoundedCornerShape(50.dp)
            )
            .clickable {
                articlesInteractionHelper.onEventFired(
                    ArticleInteractionEvent.AudioItemClicked(uiModel.url, isActionAudio = true)
                )
            }
            .padding(horizontal = 12.dp)
            .height(48.dp)
    ) {
        // Author image or narration icon
        if (uiModel.authorImageUrl != null) {
            GlideImage(
                model = uiModel.authorImageUrl,
                contentDescription = uiModel.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
            )
        } else {
            Icon(
                painter = painterResource(id = com.wpds.wpds.R.drawable.soundwave),
                contentDescription = null,
                tint = wpdsColors.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Label (e.g. "Author narrated")
        Text(
            text = uiModel.label,
            color = wpdsColors.onSurface,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontFamily = FranklinItcStandardFontFamily,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Vertical divider
        VerticalDivider(
            modifier = Modifier
                .height(16.dp)
                .width(1.dp),
            color = Color(0xFFD4D4D4)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Play/pause/buffering icon
        when (uiModel.playbackState) {
            AudioPlaybackState.PLAYING -> {
                Icon(
                    painter = painterResource(id = com.wpds.wpds.R.drawable.pause),
                    contentDescription = "Pause",
                    tint = wpdsColors.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            AudioPlaybackState.BUFFERING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = wpdsColors.primary
                )
            }
            else -> {
                Icon(
                    painter = painterResource(id = com.wpds.wpds.R.drawable.play),
                    contentDescription = "Play",
                    tint = wpdsColors.onSurface,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(5.dp))

        // Listen label
        val listenText = when (uiModel.playbackState) {
            AudioPlaybackState.PLAYING -> "Now playing"
            AudioPlaybackState.BUFFERING -> "Loading..."
            else -> "Listen"
        }
        Text(
            text = listenText,
            color = wpdsColors.onSurface,
            fontSize = 16.sp,
            fontFamily = FranklinItcStandardFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Duration (hidden when playing or buffering)
        if (uiModel.durationText != null && uiModel.playbackState == AudioPlaybackState.READY) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = uiModel.durationText,
                color = Color(0xFF666666),
                fontSize = 14.sp,
                fontFamily = FranklinItcStandardFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))
    }
}

enum class HumanAudioUiStyle {
    DEFAULT
}

@Preview(showBackground = true)
@Composable
private fun HumanAudioViewReadyPreview() {
    AndroidClassicTheme {
        HumanAudioView(
            uiModel = HumanAudioUiModel(
                url = "",
                label = "Author narrated",
                durationText = "8 min",
                authorImageUrl = "https://s3.amazonaws.com/arc-authors/washpost/054223d1-918c-4d39-965f-e1877701e420.png"
            ).apply { playbackState = AudioPlaybackState.READY },
            articlesInteractionHelper = dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HumanAudioViewPlayingPreview() {
    AndroidClassicTheme {
        HumanAudioView(
            uiModel = HumanAudioUiModel(
                url = "",
                label = "Author narrated",
                durationText = "8 min",
                authorImageUrl = "https://s3.amazonaws.com/arc-authors/washpost/054223d1-918c-4d39-965f-e1877701e420.png"
            ).apply { playbackState = AudioPlaybackState.PLAYING },
            articlesInteractionHelper = dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HumanAudioViewBufferingPreview() {
    AndroidClassicTheme {
        HumanAudioView(
            uiModel = HumanAudioUiModel(
                url = "",
                label = "Author narrated",
                durationText = "8 min",
                authorImageUrl = null
            ).apply { playbackState = AudioPlaybackState.BUFFERING },
            articlesInteractionHelper = dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HumanAudioViewNoImagePreview() {
    AndroidClassicTheme {
        HumanAudioView(
            uiModel = HumanAudioUiModel(
                url = "",
                label = "Author narrated",
                durationText = "12 min",
                authorImageUrl = null
            ).apply { playbackState = AudioPlaybackState.READY },
            articlesInteractionHelper = dummyArticlesInteractionHelper
        )
    }
}

