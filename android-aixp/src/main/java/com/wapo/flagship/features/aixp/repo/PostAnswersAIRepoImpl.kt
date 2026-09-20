package com.wapo.flagship.features.aixp.repo

import com.wapo.flagship.features.aixp.domain.PostAnswersAIRepo
import com.wapo.flagship.features.aixp.models.AskThePostShare
import com.wapo.flagship.features.aixp.models.AskThePostShareTurnRequest
import com.wapo.flagship.features.aixp.models.ConversationTurn
import com.wapo.flagship.features.aixp.models.HistoryResponse
import com.wapo.flagship.features.aixp.models.PostAnswersFeedbackResponse
import com.wapo.flagship.features.aixp.network.APIResult
import com.wapo.flagship.features.aixp.services.PostAnswersAiService
import retrofit2.Call
import javax.inject.Inject

class PostAnswersAIRepoImpl @Inject constructor(private val postAnswersAiService: PostAnswersAiService) :
    PostAnswersAIRepo {

    override suspend fun getSessionHistory(uuid: String?): APIResult<List<HistoryResponse>> {
        return postAnswersAiService.getSessionHistory(PostAnswersAiService.getHeaders(), uuid)
    }

    override suspend fun getConversationHistory(
        conversationId: String,
        uuid: String?
    ): APIResult<List<ConversationTurn>> {
        return postAnswersAiService.getConversationHistory(
            conversationId,
            PostAnswersAiService.getHeaders(),
            uuid
        )
    }

    override suspend fun deleteConversation(
        conversationId: String,
        uuid: String?
    ): APIResult<PostAnswersFeedbackResponse> {
        return postAnswersAiService.deleteConversation(
            conversationId,
            PostAnswersAiService.getHeaders(),
            uuid
        )
    }

    override suspend fun shareChat(
        conversationId: String,
        shareTurnRequest: AskThePostShareTurnRequest?
    ): APIResult<String?> {
        return postAnswersAiService.shareChat(
            conversationId,
            PostAnswersAiService.getHeaders(),
            shareTurnRequest
        )
    }

    override suspend fun getSharedConversation(shareId: String): APIResult<HistoryResponse> {
        return postAnswersAiService.getSharedConversation(
            shareId,
            PostAnswersAiService.getHeaders()
        )
    }

    override suspend fun getShares(): APIResult<List<AskThePostShare>> {
        return postAnswersAiService.getShares(PostAnswersAiService.getHeaders())
    }

    override fun deleteShareLink(
        shareId: String,
    ): Call<Void> {
        return postAnswersAiService.deleteShareLink(shareId, PostAnswersAiService.getHeaders())
    }

    override fun deleteAllShareLinks(): Call<Void> {
        return postAnswersAiService.deleteAllShareLinks(PostAnswersAiService.getHeaders())
    }


}