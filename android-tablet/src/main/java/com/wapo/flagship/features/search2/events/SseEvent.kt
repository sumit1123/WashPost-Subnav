package com.wapo.flagship.features.search2.events

import com.wapo.flagship.features.aixp.models.HistoryResponse
import com.wapo.flagship.features.aixp.models.ArticleSource
import com.wapo.flagship.features.search2.ui.PostAnswerUIItem
import okhttp3.Response

sealed class SseEvent {
    data class Data(val data: String, val id: String?, val type: String?) : SseEvent()
    data object Open : SseEvent()
    data object Closed : SseEvent()
    data class Error(val throwable: Throwable?, val response: Response?) : SseEvent()
    data object Loading: SseEvent()
    data object ForceStop: SseEvent()
}

sealed class ConversationItem {
    data class QuestionItem(val text: String, val turnId: String? = null) : ConversationItem()
    data class ResponseItem(val text: String, val turnId: String) : ConversationItem()
    data class HistoryItem(val historyResponse: HistoryResponse) : ConversationItem()
    data class Sources(val articleSources: List<ArticleSource>, val turnId: String) : ConversationItem()
    data class PostAnswerItem(val uiItems: List<PostAnswerUIItem>) : ConversationItem()
    data class ErrorItem(val message: String) : ConversationItem()
}