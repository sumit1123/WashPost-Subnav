package com.wapo.flagship.features.ask.repo

import com.wapo.flagship.features.aixp.domain.PostAnswersAIRepo
import com.wapo.flagship.features.aixp.models.AskThePostShare
import com.wapo.flagship.features.aixp.models.AskThePostShareTurnRequest
import com.wapo.flagship.features.aixp.models.ConversationTurn
import com.wapo.flagship.features.aixp.models.HistoryResponse
import com.wapo.flagship.features.aixp.models.PostAnswersFeedbackResponse
import com.wapo.flagship.features.aixp.network.APIResult
import retrofit2.Call
import javax.inject.Inject

class AskThePostRepoImpl @Inject constructor(private val postAnswersAIRepo: PostAnswersAIRepo) :
    AskThePostRepo {

    override suspend fun getSessionHistory(uuid: String?): APIResult<List<HistoryResponse>> {
        return postAnswersAIRepo.getSessionHistory(uuid)
    }

    override suspend fun getConversationHistory(
        conversationId: String,
        uuid: String?
    ): APIResult<List<ConversationTurn>> {
        return postAnswersAIRepo.getConversationHistory(
            conversationId,
            uuid
        )
    }

    override suspend fun deleteConversation(
        conversationId: String,
        uuid: String?
    ): APIResult<PostAnswersFeedbackResponse> {
        return postAnswersAIRepo.deleteConversation(
            conversationId,
            uuid
        )
    }

    override suspend fun shareChat(
        conversationId: String,
        shareTurnRequest: AskThePostShareTurnRequest?
    ): APIResult<String?> {
        return postAnswersAIRepo.shareChat(
            conversationId,
            shareTurnRequest
        )
    }

    override suspend fun getSharedConversation(shareId: String): APIResult<HistoryResponse> {
        return postAnswersAIRepo.getSharedConversation(
            shareId
        )
    }

    override suspend fun getShares(): APIResult<List<AskThePostShare>> {
        return postAnswersAIRepo.getShares()
    }

    override fun deleteShareLink(shareId: String): Call<Void> {
        return postAnswersAIRepo.deleteShareLink(shareId)
    }

    override fun deleteAllShareLinks(): Call<Void> {
        return postAnswersAIRepo.deleteAllShareLinks()
    }
}
