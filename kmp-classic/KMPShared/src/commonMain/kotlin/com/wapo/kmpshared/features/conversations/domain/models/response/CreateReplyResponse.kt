package com.wapo.kmpshared.features.conversations.domain.models.response
import kotlinx.serialization.Serializable

@Serializable
data class CreateReplyResponse(
    val data: CreateReplyData,
)

@Serializable
data class CreateReplyData(
    val createCommentReply: CreateReplyPayload,
)

@Serializable
data class CreateReplyPayload(
    val edge: CommentEdge,
    val clientMutationId: String? = null,
)
