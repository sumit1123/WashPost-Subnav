package com.wapo.kmpshared.features.conversations.domain.models

import com.wapo.kmpshared.features.conversations.domain.models.response.CommentReactionType
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentSentimentType

data class CommentViewerActionState(
    val sentiment: CommentSentimentType? = null,
    val reaction: CommentReactionType? = null,
    val flagged: Boolean = false,
)
