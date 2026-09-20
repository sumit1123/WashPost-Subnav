package com.wapo.kmpshared.features.conversations.domain.models.response

import kotlinx.serialization.Serializable

@Serializable
data class CommentSummaryResponse(
    val answer: String,
)
