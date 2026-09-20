package com.wapo.flagship.features.audio.fragments

import com.wapo.flagship.features.audio.models.MediaItemData
import com.wapo.flagship.features.personalizedpodcasts.viewmodel.PodcastGenerationState
import com.wapo.flagship.features.audio.config2.NowPlayingAudioItem
import com.wapo.flagship.features.personalizedpodcasts.model.DialogueSegment
import com.wapo.flagship.features.audio.models.AudioPlaybackState
import com.wapo.flagship.features.tts.domain.TtsState

/**
 * Groups all UI state properties for the Audio Player.
 */
data class AudioPlayerUiState(
    val mediaItemData: MediaItemData? = null,
    val nowPlayingItem: NowPlayingAudioItem? = null,
    val isPlaylistVisible: Boolean = false,
    val isTranscriptVisible: Boolean = false,
    val transcript: List<DialogueSegment>? = null,
    val generationState: PodcastGenerationState? = null,
    val playbackSpeedText: String = "1.0x",
    val upcomingItems: List<MediaItemData> = emptyList(),
    val lockedHeightPx: Float? = null,
    val percentage: Float = 0f,
    val playbackState: AudioPlaybackState = AudioPlaybackState.None,
    val isPersonalizedPodcast: Boolean = false,
    val adUiState: AudioAdUiState = AudioAdUiState(),
    val ttsState: TtsState? = null,
    val isFeedbackEnabled: Boolean = false,
)
