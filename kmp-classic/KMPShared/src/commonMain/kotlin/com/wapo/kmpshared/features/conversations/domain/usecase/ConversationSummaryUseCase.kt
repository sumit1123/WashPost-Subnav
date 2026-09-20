package com.wapo.kmpshared.features.conversations.domain.usecase

import com.wapo.kmpshared.core.KMPResult
import com.wapo.kmpshared.features.conversations.data.CommentsRepository
import org.koin.core.annotation.Factory

@Factory
class ConversationSummaryUseCase(
    private val repo: CommentsRepository,
) {
    suspend operator fun invoke(storyID: String): KMPResult<String> =
        when (val result = repo.fetchConversationSummary(storyID)) {
            is KMPResult.Success -> {
                KMPResult.Success(result.data.answer)
            }

            is KMPResult.Error -> {
                KMPResult.Error(result.message)
            }
        }
}
