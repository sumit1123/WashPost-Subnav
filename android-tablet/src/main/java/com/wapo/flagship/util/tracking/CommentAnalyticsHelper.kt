package com.wapo.flagship.util.tracking

import com.wapo.kmpshared.features.conversations.domain.models.CommentItem
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentReactionType
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentSentimentType
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentTagCode


object CommentAnalyticsHelper {

    fun getCommenterType(comment: CommentItem): String {
        if (comment.tags.any { it.code == CommentTagCode.ADMIN }) return "ADMIN"
        if (comment.tags.any { it.code == CommentTagCode.MODERATOR }) return "MODERATOR"
        if (comment.tags.any { it.code == CommentTagCode.STAFF }) return "STAFF"
        if (comment.tags.any { it.code == CommentTagCode.SOURCE_REPLY }) return "SOURCE"
        return "COMMENTER"
    }

    fun getReactionMiscellany(reactionType: CommentReactionType): String = when (reactionType) {
        CommentReactionType.HELPFUL -> "commenting-helpful"
        CommentReactionType.CARE -> "commenting-care"
        CommentReactionType.SURPRISING -> "commenting-surprising"
        CommentReactionType.FUNNY -> "commenting-remarkable"
        CommentReactionType.FRUSTRATING -> "commenting-provocative"
    }

    fun getSentimentMiscellany(sentimentType: CommentSentimentType): String = when (sentimentType) {
        CommentSentimentType.UPVOTE -> "commenting-upvote"
        CommentSentimentType.DOWNVOTE -> "commenting-downvote"
    }
}