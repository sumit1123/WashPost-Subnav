package com.wapo.flagship.features.aixp.domain

import com.wapo.flagship.features.aixp.models.AskThePostShare
import com.wapo.flagship.features.aixp.models.AskThePostShareTurnRequest
import com.wapo.flagship.features.aixp.models.ConversationTurn
import com.wapo.flagship.features.aixp.models.HistoryResponse
import com.wapo.flagship.features.aixp.models.PostAnswersFeedbackResponse
import com.wapo.flagship.features.aixp.network.APIResult
import retrofit2.Call

interface PostAnswersAIRepo {

    suspend fun getSessionHistory(uuid: String?): APIResult<List<HistoryResponse>>

    suspend fun getConversationHistory(
        conversationId: String,
        uuid: String?
    ): APIResult<List<ConversationTurn>>

    suspend fun deleteConversation(
        conversationId: String,
        uuid: String?
    ): APIResult<PostAnswersFeedbackResponse>

    suspend fun shareChat(
        conversationId: String,
        shareTurnRequest: AskThePostShareTurnRequest?
    ): APIResult<String?>

    suspend fun getSharedConversation(shareId: String): APIResult<HistoryResponse>

    suspend fun getShares(): APIResult<List<AskThePostShare>>

    fun deleteShareLink(
        shareId: String,
    ): Call<Void>

    fun deleteAllShareLinks(): Call<Void>
}