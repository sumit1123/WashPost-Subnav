package com.wapo.kmpshared.features.conversations.domain.usecase

import com.wapo.kmpshared.core.KMPResult
import com.wapo.kmpshared.features.conversations.data.CommentsRepository
import com.wapo.kmpshared.features.conversations.domain.FlagRequest
import com.wapo.kmpshared.features.conversations.domain.models.CommentFlagReason
import com.wapo.kmpshared.features.conversations.domain.models.CommentItem
import org.koin.core.annotation.Factory
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Factory
class FlagCommentUseCase(
    private val repo: CommentsRepository,
) {
    suspend operator fun invoke(
        comment: CommentItem,
        reason: CommentFlagReason,
    ): KMPResult<String> {
        val mutationId = "flag-${getUniqueId()}"

        val request =
            FlagRequest(
                commentID = comment.id,
                commentRevisionID = comment.revisionId ?: "",
                reason = reason,
                additionalDetails = null,
                clientMutationID = mutationId,
            )

        val result = repo.flagComment(request)

        return when (result) {
            is KMPResult.Success -> {
                KMPResult.Success("We've received your report and will review this comment.")
            }

            is KMPResult.Error -> {
                KMPResult.Error(result.message)
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun getUniqueId(): String = Uuid.random().toString()
}
