package com.wapo.flagship.features.audio.fragments

import com.wapo.flagship.features.audio.models.MediaItemData
import com.washingtonpost.userhistory.models.ConclusionState

/**
 * Groups all UI events for the Audio Player.
 */
sealed class AudioPlayerUiEvent {
    data object PlaylistToggle : AudioPlayerUiEvent()
    data object TranscriptToggle : AudioPlayerUiEvent()
    data class ItemClicked(val mediaItemData: MediaItemData) : AudioPlayerUiEvent()
    data class EllipsisClicked(val mediaItemData: MediaItemData) : AudioPlayerUiEvent()
    data object CreateClicked : AudioPlayerUiEvent()
    data object SpeedClicked : AudioPlayerUiEvent()
    data object AskSamClicked : AudioPlayerUiEvent()
    data class PodcastMenuAction(val event: PodcastMenuUIEvent) : AudioPlayerUiEvent()
    data object TrackChanged : AudioPlayerUiEvent()
    data class HeightMeasured(val height: Float) : AudioPlayerUiEvent()
    data class TitleClicked(val mediaItemData: MediaItemData) : AudioPlayerUiEvent()
    data object PauseOrPlay : AudioPlayerUiEvent()
    data object Expand : AudioPlayerUiEvent()
    data class Close(val conclusionState: ConclusionState) : AudioPlayerUiEvent()
    data object SkipAdClicked : AudioPlayerUiEvent()
}

/**
 * Groups all UI events for the Audio Player Podcast Menu.
 */
sealed class PodcastMenuUIEvent {
    data object MenuOpened : PodcastMenuUIEvent()
    data object Share : PodcastMenuUIEvent()
    data object UpNext : PodcastMenuUIEvent()
    data object Sources : PodcastMenuUIEvent()
    data object Transcript : PodcastMenuUIEvent()
    data object Feedback : PodcastMenuUIEvent()
}

