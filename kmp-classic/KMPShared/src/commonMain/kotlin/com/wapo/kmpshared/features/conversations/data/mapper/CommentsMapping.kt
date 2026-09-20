package com.wapo.kmpshared.features.conversations.data.mapper

import com.wapo.kmpshared.features.conversations.domain.models.CommentItem
import com.wapo.kmpshared.features.conversations.domain.models.CommentViewerActionState
import com.wapo.kmpshared.features.conversations.domain.models.response.ActionPresence
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentActionCounts
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentDomainActionCounts
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentNode
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentRepliesResponse
import com.wapo.kmpshared.features.conversations.domain.models.response.CreateCommentResponse
import com.wapo.kmpshared.features.conversations.domain.models.response.CreateReplyResponse
import com.wapo.kmpshared.features.conversations.domain.models.response.PageInfo
import com.wapo.kmpshared.features.conversations.domain.models.response.TopLevelCommentsResponse
import com.wapo.kmpshared.util.createKMPDateFromISO8601

fun CommentNode.toDomain(
    depth: Int,
    parentId: String?,
    viewerActionLookup: Map<String, ActionPresence>?,
): CommentItem? {
    // Resolve the viewer action for this specific ID
    val presence = viewerActionLookup?.get(this.id)
    val viewerState =
        CommentViewerActionState(
            sentiment = presence?.toSentiment(),
            reaction = presence?.toReaction(),
            flagged = presence?.flag ?: false,
        )

    return createKMPDateFromISO8601(createdAt)?.let {
        CommentItem(
            id = this.id,
            parentId = parentId,
            revisionId = this.revision?.id,
            depth = depth,
            createdAt = it,
            author = this.author,
            bodyNodes = this.bodyNodes,
            videoItem = this.hasVideo(),
            status = this.status,
            tags = this.tags,
            actionCounts = this.actionCounts.toDomain(),
            viewerAction = viewerState,
            replyCount = replyCount,
        )
    }
}

/**
 * Normalizes both TopLevel and Reply responses into a single Domain object
 */
data class NormalizedCommentsPage(
    val items: List<CommentItem>,
    val pageInfo: PageInfo,
    val parentId: String? = null,
)

// Extension for TopLevel
fun TopLevelCommentsResponse.toDomain(viewerLookup: Map<String, ActionPresence>?): NormalizedCommentsPage {
    val connection = this.data.stream.comments
    return NormalizedCommentsPage(
        items = connection.nodes.mapNotNull { it.toDomain(0, null, viewerLookup) },
        pageInfo = connection.pageInfo,
        parentId = null,
    )
}

// Extension for Replies
fun CommentRepliesResponse.toDomain(viewerLookup: Map<String, ActionPresence>): NormalizedCommentsPage {
    val parent = this.data.comment
    val connection = parent.replies
    return NormalizedCommentsPage(
        items = connection.nodes.mapNotNull { it.toDomain(parent.displayDepth + 1, parent.id, viewerLookup) },
        pageInfo = connection.pageInfo,
        parentId = parent.id,
    )
}

data class PostCommentResponse(
    val comment: CommentItem?,
)

// Extension for CreateComment
fun CreateCommentResponse.toDomain(): PostCommentResponse {
    val node = this.data.createComment.edge.node
    val comment = node.toDomain(0, null, null)
    return PostCommentResponse(comment)
}

// Extension for CreateReply
fun CreateReplyResponse.toDomain(
    parentId: String,
    parentDepth: Int,
): PostCommentResponse {
    val node = this.data.createCommentReply.edge.node
    val comment = node.toDomain(parentDepth, parentId, null)
    return PostCommentResponse(comment)
}

fun CommentActionCounts.toDomain(): CommentDomainActionCounts =
    CommentDomainActionCounts(
        helpful = this.sentimentHelpful.total,
        care = this.sentimentCare.total,
        surprising = this.sentimentSurprising.total,
        funny = this.sentimentFunny.total,
        frustrating = this.sentimentFrustrating.total,
        upVotes = this.sentimentUpVote.total,
        downVotes = this.sentimentDownVote.total,
    )
