package com.wapo.kmpshared.features.conversations.domain.models.response

import kotlinx.serialization.Serializable

@Serializable
data class CommentRepliesResponse(
    val data: CommentRepliesData,
)

@Serializable
data class CommentRepliesData(
    val comment: ParentComment,
)

@Serializable
data class ParentComment(
    val id: String,
    val displayDepth: Int,
    val replies: CommentConnection,
)
