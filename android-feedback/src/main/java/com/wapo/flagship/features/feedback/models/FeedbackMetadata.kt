package com.wapo.flagship.features.feedback.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true, generator = "sealed")
sealed class FeedbackMetadata

@JsonClass(generateAdapter = true)
data class PersonalizedPodcastMetadata(
    val position: Float
) : FeedbackMetadata()