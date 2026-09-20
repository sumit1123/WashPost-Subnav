package com.wapo.flagship.features.personalizedpodcasts.viewmodel

import com.wapo.flagship.features.personalizedpodcasts.model.DialogueSegment
import com.wapo.flagship.features.personalizedpodcasts.model.PersoPodTrackingInfo
import com.wapo.flagship.features.personalizedpodcasts.model.PodcastConfigResponse

data class PersonalizedPodcastUiState(
    val persoPodTrackingInfo: Pair<PersoPodTrackingInfo, PersoPodTrackingInfo?>? = null,
    val configMetadata: PodcastConfigResponse? = null,
    val personalizedPodcasts: List<com.wapo.flagship.features.personalizedpodcasts.model.PersonalizedPodcast> = emptyList(),
    val generationState: PodcastGenerationState = PodcastGenerationState.Hidden,
    val transcript: List<DialogueSegment> = emptyList(),
    val pausedPodcast: Boolean = false
)

sealed class PersonalizedPodcastEvents {
    data object PlaySound : PersonalizedPodcastEvents()
    data object RestorePreviousPlayback : PersonalizedPodcastEvents()
}
