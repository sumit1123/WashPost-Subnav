// Copyright (c) 2026 The Washington Post. All rights reserved.

package com.wapo.kmpshared.features.feedback.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FeedbackRequestDto(
    @SerialName("content_id") val contentId: String? = null,
    @SerialName("rating") val rating: Int? = null,
    @SerialName("feedback") val feedback: String? = null,
    @SerialName("scale") val scale: Int? = null,
    @SerialName("metadata") val metadata: FeedbackMetadataDto? = null,
)

@Serializable
sealed class FeedbackMetadataDto

@Serializable
@SerialName("PersonalizedPodcastMetadataDto")
data class PersonalizedPodcastMetadataDto(
    val position: Double,
) : FeedbackMetadataDto()
