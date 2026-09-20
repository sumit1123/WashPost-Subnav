// Copyright (c) 2026 The Washington Post. All rights reserved.

package com.wapo.kmpshared.features.feedback.data.mapper

import com.wapo.kmpshared.features.feedback.data.remote.FeedbackRequestDto
import com.wapo.kmpshared.features.feedback.data.remote.PersonalizedPodcastMetadataDto
import com.wapo.kmpshared.features.feedback.domain.model.Feedback
import com.wapo.kmpshared.features.feedback.domain.model.FeedbackMetadata

fun Feedback.toRequestDto(): FeedbackRequestDto =
    FeedbackRequestDto(
        contentId = this.surface.contentId,
        rating = this.rating,
        feedback = this.comment,
        scale = this.surface.scale,
        metadata =
            when (val m = this.surface.metadata) {
                is FeedbackMetadata.PersonalizedPodcastMetadata -> {
                    PersonalizedPodcastMetadataDto(
                        position = m.position,
                    )
                }

                else -> {
                    null
                }
            },
    )
