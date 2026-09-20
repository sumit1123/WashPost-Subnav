// Copyright (c) 2026 The Washington Post. All rights reserved.

package com.wapo.kmpshared.features.feedback.domain.model

data class Feedback(
    val surface: FeedbackSurface,
    val rating: Int,
    val comment: String?,
)

sealed interface FeedbackMetadata {
    data class PersonalizedPodcastMetadata(
        val position: Double,
    ) : FeedbackMetadata
}

sealed class FeedbackSurface(
    val name: String,
) {
    abstract val contentId: String
    abstract val scale: Int
    abstract val metadata: FeedbackMetadata?

    data class PersonalizedPodcast(
        val podcastId: String,
        val position: Double,
    ) : FeedbackSurface(PERSONALIZED_PODCAST) {
        override val contentId: String = podcastId
        override val scale: Int = 2
        override val metadata: FeedbackMetadata =
            FeedbackMetadata.PersonalizedPodcastMetadata(position)
    }

    companion object {
        const val PERSONALIZED_PODCAST = "personalized_podcast"
    }
}
