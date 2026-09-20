package com.wapo.flagship.features.audio.fragments

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.wapo.flagship.features.audio.R
import com.wapo.flagship.features.audio.models.AudioPlaybackState
import com.wapo.flagship.features.personalizedpodcasts.viewmodel.PodcastGenerationState
import com.wapo.flagship.features.tts.domain.TtsState
import com.wapo.flagship.features.utils.PersonalizedPodcastHelper.PersonalizedPodcastItemType.PLACEHOLDER
import com.washingtonpost.userhistory.models.ConclusionState
import com.wpds.theme.FranklinItcStandardFontFamily
import com.wpds.theme.wpdsColors

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun PersistentMiniPlayer(
    state: AudioPlayerUiState,
    isPlayingAd: Boolean,
    onEvent: (AudioPlayerUiEvent) -> Unit
) {
    val nowPlayingItem = state.nowPlayingItem
    val generationState = state.generationState
    val playbackState = state.playbackState
    val percentage = state.percentage
    val mediaItem = nowPlayingItem?.mediaItemData ?: return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(wpdsColors.appBarBg)
            .clickable { onEvent(AudioPlayerUiEvent.Expand) }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        // Thumbnail Image
        GlideImage(
            model = mediaItem.albumArtUrl,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(wpdsColors.surface),
            contentScale = ContentScale.Crop
        )

        // Title and Subtitle Section
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (mediaItem.audioType == PLACEHOLDER) {
                    when (generationState) {
                        PodcastGenerationState.Minimizable -> stringResource(R.string.your_episode_is_processing)
                        PodcastGenerationState.Done -> stringResource(R.string.done_lets_listen)
                        PodcastGenerationState.Failure -> stringResource(R.string.could_not_generate_your_podcast)
                        else -> stringResource(R.string.creating_your_podcast)
                    }
                } else if (isPlayingAd) stringResource(R.string.now_playing_ad) else mediaItem.title,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                style = TextStyle(
                    color = wpdsColors.primary,
                    fontFamily = FranklinItcStandardFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )

            if (state.isPersonalizedPodcast) {
                Text(
                    text = stringResource(R.string.ai_generated_podcast), style = TextStyle(
                        color = wpdsColors.onSurfaceSubtle,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Light
                    ), maxLines = 1
                )
            }
        }

        // Action Buttons: Play/Pause and Close
        Box(contentAlignment = Alignment.Center) {
            if (mediaItem.audioType == PLACEHOLDER && generationState != PodcastGenerationState.Done) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(32.dp), color = wpdsColors.primary, strokeWidth = 2.dp
                )
            } else if (mediaItem.audioType == PLACEHOLDER && generationState == PodcastGenerationState.Done) {
                Icon(
                    painter = painterResource(id = R.drawable.podcast_generated_success),
                    contentDescription = "Generation Complete",
                    modifier = Modifier.size(32.dp)
                )
            } else {
                CircularProgressBar(
                    percentage = percentage,
                    audioPlaybackState = playbackState,
                    ttsState = state.ttsState,
                    onClick = { onEvent(AudioPlayerUiEvent.PauseOrPlay) })
            }
        }

        Icon(
            painter = painterResource(id = R.drawable.audio_close_icon),
            contentDescription = "Close Player",
            tint = wpdsColors.appBarIcon,
            modifier = Modifier
                .padding(start = 8.dp)
                .size(24.dp)
                .clickable {
                    val conclusion = when (playbackState) {
                        AudioPlaybackState.Playing.PlayingContent -> ConclusionState.PLAY_DISMISSED
                        AudioPlaybackState.Paused -> ConclusionState.PAUSE_DISMISSED
                        else -> ConclusionState.OTHER
                    }
                    onEvent(AudioPlayerUiEvent.Close(conclusion))
                })
    }
}

@Composable
fun CircularProgressBar(
    modifier: Modifier = Modifier,
    percentage: Float,
    audioPlaybackState: AudioPlaybackState,
    ttsState: TtsState?,
    onClick: () -> Unit,
    radius: Dp = 18.dp,
    backgroundStrokeColor: Color = colorResource(com.wapo.view.R.color.tile_border),
    strokeColor: Color = wpdsColors.appBarIcon,
    strokeWidth: Dp = 1.dp,
    animDuration: Int = 100,
    animDelay: Int = 0,
) {
    var animationPlayed by remember {
        mutableStateOf(true)
    }
    val targetPercentage = if (percentage > 1f) percentage / 100f else percentage

    val currPercentage = animateFloatAsState(
        targetValue = if (animationPlayed) targetPercentage else 0f,
        animationSpec = tween(
            durationMillis = animDuration, delayMillis = animDelay
        ),
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(radius * 2f)
            .background(color = wpdsColors.appBarBg)
    ) {
        Canvas(modifier = Modifier.size(radius * 2f)) {
            drawArc(
                color = backgroundStrokeColor,
                -90f,
                360f,
                useCenter = false,
                style = Stroke(strokeWidth.toPx(), cap = StrokeCap.Square)
            )
            drawArc(
                color = strokeColor,
                -90f,
                360 * currPercentage.value,
                useCenter = false,
                style = Stroke(strokeWidth.toPx(), cap = StrokeCap.Square)
            )
        }
        val image =
            if (audioPlaybackState is AudioPlaybackState.Playing || ttsState?.isSpeaking == true) R.drawable.audio_player_pause else R.drawable.audio_player_play
        Icon(
            tint = wpdsColors.appBarIcon,
            painter = painterResource(image),
            contentDescription = "audio-player-play",
            modifier = Modifier.clickable(enabled = true, onClick = onClick),
        )
    }
}
