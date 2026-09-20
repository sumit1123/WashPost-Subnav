package com.wapo.kmpshared.features.conversations.domain.models.response

import kotlinx.serialization.Serializable

@Serializable
data class SingleCommentResponse(
    val data: SingleDataPayload,
)

@Serializable
data class SingleDataPayload(
    val comment: CommentNode?,
)
