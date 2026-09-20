package com.wapo.flagship.features.personalizedpodcasts.model

import com.wapo.flagship.features.feedback.models.FeedbackProvider
import com.wapo.flagship.features.feedback.models.PersonalizedPodcastMetadata

class PersonalizedPodcastFeedbackProviderImpl(
    private val contentId: String,
    private val position: Float
) : FeedbackProvider {
    override fun type() = PERSONALIZED_PODCAST
    override fun contentId() = contentId
    override fun metadata() : PersonalizedPodcastMetadata =
        PersonalizedPodcastMetadata(position)
    override fun description() = DESCRIPTION
    override fun scale() = SCALE
}

private const val PERSONALIZED_PODCAST = "personalized_podcast"
private const val DESCRIPTION = "Was this episode useful?"
private const val SCALE = 2