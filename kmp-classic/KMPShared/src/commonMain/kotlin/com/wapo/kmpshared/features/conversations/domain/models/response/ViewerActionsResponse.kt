package com.wapo.kmpshared.features.conversations.domain.models.response

import kotlinx.serialization.Serializable

@Serializable
data class ViewerActionsResponse(
    val data: ViewerData,
)

@Serializable
data class ViewerData(
    val viewer: Viewer,
    val viewerStoryActions: ViewerStoryActions?,
)

@Serializable
data class Viewer(
    val id: String,
    val username: String,
    val createdAt: String,
)

@Serializable
data class ViewerStoryActions(
    val actions: List<CommentAction>,
    val hasMore: Boolean,
)

@Serializable
data class CommentAction(
    val commentID: String,
    val actionPresence: ActionPresence,
)

@Serializable
data class ActionPresence(
    val sentimentHelpful: Boolean = false,
    val sentimentCare: Boolean = false,
    val sentimentSurprising: Boolean = false,
    val sentimentFunny: Boolean = false,
    val sentimentFrustrating: Boolean = false,
    val sentimentUpVote: Boolean = false,
    val sentimentDownVote: Boolean = false,
    val flag: Boolean = false,
) {
    // Return the specific sentiment type if any is true
    fun toSentiment(): CommentSentimentType? =
        when {
            sentimentUpVote -> CommentSentimentType.UPVOTE
            sentimentDownVote -> CommentSentimentType.DOWNVOTE
            else -> null
        }

    // Return the specific reaction type if any is true
    fun toReaction(): CommentReactionType? =
        when {
            sentimentCare -> CommentReactionType.CARE
            sentimentHelpful -> CommentReactionType.HELPFUL
            sentimentSurprising -> CommentReactionType.SURPRISING
            sentimentFunny -> CommentReactionType.FUNNY
            sentimentFrustrating -> CommentReactionType.FRUSTRATING
            else -> null
        }
}
