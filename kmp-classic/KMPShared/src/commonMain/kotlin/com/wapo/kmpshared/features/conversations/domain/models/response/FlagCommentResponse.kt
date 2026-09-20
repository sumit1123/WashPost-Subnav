package com.wapo.kmpshared.features.conversations.domain.models.response

import kotlinx.serialization.Serializable

@Serializable
data class FlagCommentResponse(
    val data: FlagData,
)

@Serializable
data class FlagData(
    val createCommentFlag: CreateCommentFlagPayload,
)

@Serializable
data class CreateCommentFlagPayload(
    val comment: FlaggedComment,
    val clientMutationId: String,
)

@Serializable
data class FlaggedComment(
    val id: String,
    val viewerActionPresence: ViewerActionPresence,
)

@Serializable
data class ViewerActionPresence(
    val flag: Boolean,
)
