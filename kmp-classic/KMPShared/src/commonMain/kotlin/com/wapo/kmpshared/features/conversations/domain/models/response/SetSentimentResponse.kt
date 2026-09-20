package com.wapo.kmpshared.features.conversations.domain.models.response

import kotlinx.serialization.Serializable

@Serializable
data class SetSentimentResponse(
    val data: SentimentData,
)

@Serializable
data class SentimentData(
    val setSentiment: SetSentimentResult,
)

@Serializable
data class SetSentimentResult(
    val comment: SentimentComment,
    val clientMutationId: String,
)

@Serializable
data class SentimentComment(
    val id: String,
    val viewerActionPresence: ActionPresence,
    val actionCounts: CommentActionCounts,
)
