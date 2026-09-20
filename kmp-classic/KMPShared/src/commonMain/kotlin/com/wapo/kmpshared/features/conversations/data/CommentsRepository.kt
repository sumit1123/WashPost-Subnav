package com.wapo.kmpshared.features.conversations.data

import com.wapo.kmpshared.core.KMPResult
import com.wapo.kmpshared.core.map
import com.wapo.kmpshared.features.conversations.data.mapper.NormalizedCommentsPage
import com.wapo.kmpshared.features.conversations.data.mapper.PostCommentResponse
import com.wapo.kmpshared.features.conversations.data.mapper.toDomain
import com.wapo.kmpshared.features.conversations.data.remote.CommentsService
import com.wapo.kmpshared.features.conversations.domain.CommentsTab
import com.wapo.kmpshared.features.conversations.domain.CreateCommentRequest
import com.wapo.kmpshared.features.conversations.domain.CreateReplyRequest
import com.wapo.kmpshared.features.conversations.domain.FlagRequest
import com.wapo.kmpshared.features.conversations.domain.GetCommentRequest
import com.wapo.kmpshared.features.conversations.domain.GetReplyRequest
import com.wapo.kmpshared.features.conversations.domain.GetSingleCommentRequest
import com.wapo.kmpshared.features.conversations.domain.SentimentRequest
import com.wapo.kmpshared.features.conversations.domain.models.CommentItem
import com.wapo.kmpshared.features.conversations.domain.models.response.ActionPresence
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentSummaryResponse
import com.wapo.kmpshared.features.conversations.domain.models.response.ContextSummaryResponse
import com.wapo.kmpshared.features.conversations.domain.models.response.FlagCommentResponse
import com.wapo.kmpshared.features.conversations.domain.models.response.SetSentimentResponse
import com.wapo.kmpshared.features.conversations.domain.models.response.ViewerActionsResponse
import com.wapo.kmpshared.util.KMPURL
import org.koin.core.annotation.Single

@Single
class CommentsRepository(
    private val api: CommentsService,
) {
    suspend fun getContextSummary(storyURL: KMPURL): KMPResult<ContextSummaryResponse> = api.fetchStoryContext(storyURL)

    suspend fun fetchConversationSummary(storyID: String): KMPResult<CommentSummaryResponse> = api.fetchConversationSummary(storyID)

    suspend fun getTopLevelComments(
        request: GetCommentRequest,
        lookup: Map<String, ActionPresence>?,
    ): KMPResult<NormalizedCommentsPage> = api.fetchTopLevelComments(request).map { it.toDomain(lookup) }

    suspend fun getMyComments(
        request: GetCommentRequest,
        lookup: Map<String, ActionPresence>,
    ): KMPResult<NormalizedCommentsPage> = api.fetchMyComments(request).map { it.toDomain(lookup) }

    suspend fun getComment(
        request: GetSingleCommentRequest,
        lookup: Map<String, ActionPresence>,
    ): KMPResult<CommentItem?> =
        api.fetchComment(request).map {
            it.data.comment?.toDomain(depth = 0, parentId = null, viewerActionLookup = lookup)
        }

    suspend fun getReplies(
        request: GetReplyRequest,
        lookup: Map<String, ActionPresence>,
    ): KMPResult<NormalizedCommentsPage> = api.fetchReplies(request).map { it.toDomain(lookup) }

    suspend fun getViewerActions(storyURL: KMPURL): KMPResult<ViewerActionsResponse> = api.fetchViewerActions(storyURL)

    suspend fun postComment(request: CreateCommentRequest): KMPResult<PostCommentResponse> = api.postComment(request).map { it.toDomain() }

    suspend fun postReply(
        request: CreateReplyRequest,
        parentDepth: Int,
    ): KMPResult<PostCommentResponse> =
        api.postReply(request).map {
            it.toDomain(request.parentID, parentDepth)
        }

    suspend fun setSentiment(request: SentimentRequest): KMPResult<SetSentimentResponse> = api.postSentiment(request)

    suspend fun flagComment(request: FlagRequest): KMPResult<FlagCommentResponse> = api.flagComment(request)

    suspend fun checkForLatestId(storyURL: KMPURL): String? {
        val request = GetCommentRequest(storyURL, 1, null, CommentsTab.ALL, isInitial = true)
        val result = getTopLevelComments(request, null)

        return when (result) {
            is KMPResult.Success -> {
                result.data.items
                    .firstOrNull()
                    ?.id
            }

            is KMPResult.Error -> {
                null
            }
        }
    }
}
