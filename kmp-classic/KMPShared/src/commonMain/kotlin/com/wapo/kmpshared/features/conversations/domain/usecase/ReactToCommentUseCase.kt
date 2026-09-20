package com.wapo.kmpshared.features.conversations.domain.usecase

import com.wapo.kmpshared.core.KMPResult
import com.wapo.kmpshared.features.conversations.data.CommentsRepository
import com.wapo.kmpshared.features.conversations.data.mapper.toDomain
import com.wapo.kmpshared.features.conversations.domain.SentimentRequest
import com.wapo.kmpshared.features.conversations.domain.models.CommentItem
import com.wapo.kmpshared.features.conversations.domain.models.CommentViewerActionState
import org.koin.core.annotation.Factory
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Factory
class ReactToCommentUseCase(
    private val repo: CommentsRepository,
) {
    suspend operator fun invoke(
        comment: CommentItem,
        typeValue: String?,
        groupValue: String,
    ): KMPResult<CommentItem> {
        val mutationId = "mut-${getUniqueId()}"

        val request =
            SentimentRequest(
                commentID = comment.id,
                commentRevisionID = comment.revisionId ?: "",
                type = typeValue,
                group = groupValue,
                clientMutationID = mutationId,
            )

        return when (val result = repo.setSentiment(request)) {
            is KMPResult.Success -> {
                val serverComment = result.data.data.setSentiment.comment

                val updatedComment =
                    comment.copy(
                        viewerAction =
                            CommentViewerActionState(
                                sentiment = serverComment.viewerActionPresence.toSentiment(),
                                reaction = serverComment.viewerActionPresence.toReaction(),
                                flagged = serverComment.viewerActionPresence.flag,
                            ),
                        actionCounts = serverComment.actionCounts.toDomain(),
                    )
                KMPResult.Success(updatedComment)
            }

            is KMPResult.Error -> {
                KMPResult.Error(result.message)
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun getUniqueId(): String = Uuid.random().toString()
}
