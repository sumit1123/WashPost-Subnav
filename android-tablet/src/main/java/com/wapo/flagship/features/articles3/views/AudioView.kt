package com.wapo.flagship.features.articles3.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles3.models.ui.AudioUiModel.AudioStatusText
import com.wapo.flagship.features.articles3.models.ui.AudioUiModel
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.ArticleTextStyles
import com.wpds.theme.wpdsColors
import com.washingtonpost.android.R

@Composable
fun AudioView(
    uiModel: AudioUiModel,
    articlesInteractionHelper: ArticlesInteractionHelper,
) {
    when (uiModel.uiStyle) {
        AudioUiStyle.STANDALONE -> StandaloneAudioView(uiModel, articlesInteractionHelper)
        AudioUiStyle.DEFAULT -> DefaultAudioView(uiModel, articlesInteractionHelper)
    }
}

@Composable
private fun DefaultAudioView(
    uiModel: AudioUiModel,
    articlesInteractionHelper: ArticlesInteractionHelper,
) {
    val style = getAudioStyle(uiModel.uiStyle)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.wrapContentHeight()
    ) {
        // Audio Pill Container
        Box(
            modifier = Modifier
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
                        ArticleInteractionEvent.AudioItemClicked(uiModel.rawUrl, true)
                    )
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.wrapContentHeight()
            ) {
                // Play/Pause/Buffering Icon
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (uiModel.playbackState) {
                        AudioPlaybackState.PLAYING -> {
                            Icon(
                                painter = painterResource(id = com.wpds.wpds.R.drawable.pause),
                                contentDescription = "Pause",
                                modifier = Modifier.size(16.dp),
                                tint = wpdsColors.primary
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
                                modifier = Modifier.size(16.dp),
                                tint = wpdsColors.primary
                            )
                        }
                    }
                }

                // Duration Text
                Text(
                    text = uiModel.durationText,
                    style = style,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun StandaloneAudioView(
    uiModel: AudioUiModel,
    articlesInteractionHelper: ArticlesInteractionHelper,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                articlesInteractionHelper.onEventFired(
                    ArticleInteractionEvent.AudioItemClicked(uiModel.rawUrl)
                )
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.wrapContentHeight()
        ) {
            // Play/Pause/Buffering Icon (34dp to match item_standalone_audio.xml minHeight)
            Box(
                modifier = Modifier.size(34.dp),
                contentAlignment = Alignment.Center
            ) {
                when (uiModel.playbackState) {
                    AudioPlaybackState.PLAYING -> {
                        Image(
                            painter = painterResource(id = R.drawable.ic_standalone_audio_pause),
                            contentDescription = "Pause",
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    AudioPlaybackState.BUFFERING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = wpdsColors.primary
                        )
                    }
                    else -> {
                        Image(
                            painter = painterResource(id = R.drawable.ic_standalone_audio_play),
                            contentDescription = "Play",
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }

            // Status label ("Listen" or "Now Playing")
            val statusLabel = when (uiModel.playbackState) {
                AudioPlaybackState.PLAYING -> AudioStatusText.NOW_PLAYING.value
                else -> AudioStatusText.LISTEN.value
            }
            Text(
                text = statusLabel,
                style = ArticleTextStyles.AUDIO_STATUS.style,
                modifier = Modifier.padding(start = 8.dp)
            )

            // Duration (hidden when playing)
            if (uiModel.playbackState != AudioPlaybackState.PLAYING && uiModel.durationText?.isNotEmpty() == true) {
                Text(
                    text = uiModel.durationText,
                    style = ArticleTextStyles.AUDIO_CAPTION.style,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        // Caption / title (shown below the controls row when not empty)
        if (!uiModel.title.isNullOrEmpty()) {
            Text(
                text = uiModel.title,
                style = ArticleTextStyles.AUDIO_CAPTION.style,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun getAudioStyle(uiStyle: AudioUiStyle): TextStyle {
    return when (uiStyle) {
        AudioUiStyle.DEFAULT -> ArticleTextStyles.AUDIO_DURATION.style
        AudioUiStyle.STANDALONE -> ArticleTextStyles.AUDIO_DURATION.style
    }
}

@Composable
private fun getStatusTextStyle(): TextStyle {
    return ArticleTextStyles.AUDIO_DURATION.style
}

enum class AudioUiStyle {
    DEFAULT,
    STANDALONE
}

enum class AudioPlaybackState {
    READY,
    PLAYING,
    BUFFERING
}

@Preview(showBackground = true)
@Composable
private fun AudioViewPlayingPreview() {
    AndroidClassicTheme {
        AudioView(
            uiModel = AudioUiModel(
                rawUrl = "audio-123",
                title = "Sample Audio",
                durationText = "2:30",
                uiStyle = AudioUiStyle.DEFAULT
            ).apply {
                playbackState = AudioPlaybackState.PLAYING
            },
            dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AudioViewReadyPreview() {
    AndroidClassicTheme {
        AudioView(
            uiModel = AudioUiModel(
                rawUrl = "audio-123",
                title = "Sample Audio",
                durationText = "2:30",
                uiStyle = AudioUiStyle.DEFAULT
            ).apply {
                playbackState = AudioPlaybackState.READY
            },
            dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AudioViewBufferingPreview() {
    AndroidClassicTheme {
        AudioView(
            uiModel = AudioUiModel(
                rawUrl = "audio-123",
                title = "Sample Audio",
                durationText = "2:30",
                uiStyle = AudioUiStyle.DEFAULT
            ).apply {
                statusText = "Buffering..."
                playbackState = AudioPlaybackState.BUFFERING
            },
            dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AudioViewWithStatusTextPreview() {
    AndroidClassicTheme {
        AudioView(
            uiModel = AudioUiModel(
                rawUrl = "audio-123",
                title = "Sample Audio",
                durationText = "2:30",
                uiStyle = AudioUiStyle.DEFAULT
            ).apply {
                statusText = AudioStatusText.NOW_PLAYING.value
                playbackState = AudioPlaybackState.PLAYING
            },
            dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StandaloneAudioViewReadyPreview() {
    AndroidClassicTheme {
        AudioView(
            uiModel = AudioUiModel(
                rawUrl = "audio-123",
                title = "How the economy is affecting everyday Americans",
                durationText = "4:12",
                uiStyle = AudioUiStyle.STANDALONE
            ).apply {
                playbackState = AudioPlaybackState.READY
            },
            dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StandaloneAudioViewPlayingPreview() {
    AndroidClassicTheme {
        AudioView(
            uiModel = AudioUiModel(
                rawUrl = "audio-123",
                title = "How the economy is affecting everyday Americans",
                durationText = "4:12",
                uiStyle = AudioUiStyle.STANDALONE
            ).apply {
                playbackState = AudioPlaybackState.PLAYING
            },
            dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StandaloneAudioViewBufferingPreview() {
    AndroidClassicTheme {
        AudioView(
            uiModel = AudioUiModel(
                rawUrl = "audio-123",
                title = "How the economy is affecting everyday Americans",
                durationText = "4:12",
                uiStyle = AudioUiStyle.STANDALONE
            ).apply {
                playbackState = AudioPlaybackState.BUFFERING
            },
            dummyArticlesInteractionHelper
        )
    }
}
