package com.wapo.kmpshared.features.conversations.domain.models.response
import kotlinx.serialization.Serializable

@Serializable
data class CreateCommentResponse(
    val data: CreateCommentData,
)

@Serializable
data class CreateCommentData(
    val createComment: CreateCommentPayload,
)

@Serializable
data class CreateCommentPayload(
    val edge: CommentEdge,
    val clientMutationId: String? = null,
)

@Serializable
data class CommentEdge(
    val node: CommentNode,
)
