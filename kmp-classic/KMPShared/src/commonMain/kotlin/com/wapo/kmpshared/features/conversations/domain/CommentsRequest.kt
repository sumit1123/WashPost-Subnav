package com.wapo.kmpshared.features.conversations.domain

import com.wapo.kmpshared.features.conversations.domain.models.CommentFlagReason
import com.wapo.kmpshared.util.KMPURL

data class GetCommentRequest(
    val storyURL: KMPURL,
    val limit: Int,
    val cursor: String?,
    val sortBy: CommentsTab,
    val isInitial: Boolean,
)

data class GetSingleCommentRequest(
    val commentID: String,
)

data class GetReplyRequest(
    val commentID: String,
    val limit: Int,
    val cursor: String?,
    val sortBy: String,
    val isInitial: Boolean,
)

data class CreateCommentRequest(
    val storyID: String,
    val body: String,
    val clientMutationID: String,
)

data class CreateReplyRequest(
    val storyID: String,
    val parentID: String,
    val parentRevision: String,
    val body: String,
    val clientMutationID: String,
)

data class SentimentRequest(
    val commentID: String,
    val commentRevisionID: String,
    val type: String?,
    val group: String,
    val clientMutationID: String,
)

data class FlagRequest(
    val commentID: String,
    val commentRevisionID: String,
    val reason: CommentFlagReason,
    val additionalDetails: String?,
    val clientMutationID: String,
)
