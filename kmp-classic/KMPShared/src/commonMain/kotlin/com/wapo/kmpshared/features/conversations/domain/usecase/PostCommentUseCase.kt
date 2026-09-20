package com.wapo.kmpshared.features.conversations.domain.usecase

import com.wapo.kmpshared.core.KMPResult
import com.wapo.kmpshared.features.conversations.data.CommentsRepository
import com.wapo.kmpshared.features.conversations.data.mapper.PostCommentResponse
import com.wapo.kmpshared.features.conversations.domain.CreateCommentRequest
import com.wapo.kmpshared.features.conversations.domain.CreateReplyRequest
import com.wapo.kmpshared.features.conversations.domain.models.CommentItem
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentStatus
import org.koin.core.annotation.Factory
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Factory
class PostCommentUseCase(
    private val repo: CommentsRepository,
) {
    suspend operator fun invoke(
        body: String,
        storyID: String,
        // Optional parameters for replies
        parentId: String? = null,
        parentRevision: String? = null,
        parentDepth: Int = 0,
    ): KMPResult<CommentItem> {
        val mutationId = "${if (parentId == null) "m" else "r"}-${getUniqueId()}"

        val result: KMPResult<PostCommentResponse> =
            if (parentId == null) {
                val request = CreateCommentRequest(storyID, body, mutationId)
                repo.postComment(request)
            } else {
                val request =
                    CreateReplyRequest(
                        storyID = storyID,
                        parentID = parentId,
                        parentRevision = parentRevision ?: "",
                        body = body,
                        clientMutationID = mutationId,
                    )
                repo.postReply(request, parentDepth)
            }

        return when (result) {
            is KMPResult.Success -> {
                handlePostSuccess(
                    response = result.data,
                )
            }

            is KMPResult.Error -> {
                KMPResult.Error(result.message)
            }
        }
    }

    private fun handlePostSuccess(response: PostCommentResponse): KMPResult<CommentItem> {
        val newComment = response.comment ?: return KMPResult.Error("EMPTY_CONTENT_ERROR")

        // Validation: If rejected/withdrawn, return early with error  and do not merge into the list.
        if (newComment.status == CommentStatus.REJECTED || newComment.status == CommentStatus.WITHDRAWN) {
            return KMPResult.Error("MODERATION_REJECTED")
        }

        return KMPResult.Success(newComment)
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun getUniqueId(): String = Uuid.random().toString()
}
