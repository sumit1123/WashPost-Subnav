package com.wapo.kmpshared.features.conversations.domain.usecase

import com.wapo.kmpshared.core.KMPResult
import com.wapo.kmpshared.features.conversations.data.CommentsRepository
import com.wapo.kmpshared.features.conversations.domain.GetSingleCommentRequest
import com.wapo.kmpshared.features.conversations.domain.models.CommentItem
import com.wapo.kmpshared.features.conversations.domain.models.response.ActionPresence
import org.koin.core.annotation.Factory

@Factory
class GetSingleCommentUseCase(
    private val repo: CommentsRepository,
) {
    suspend operator fun invoke(
        commentId: String,
        viewerActionLookup: Map<String, ActionPresence>,
    ): KMPResult<CommentItem?> {
        val request = GetSingleCommentRequest(commentId)
        return repo.getComment(request, viewerActionLookup)
    }
}
