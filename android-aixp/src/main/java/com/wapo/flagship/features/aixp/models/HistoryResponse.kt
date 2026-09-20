package com.wapo.flagship.features.aixp.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HistoryResponse(
    @Json(name = "conversation_id")
    val conversationId: String? = null,
    @Json(name = "conversation_title")
    val conversationTitle: String? = null,
    @Json(name = "created_date")
    val createdDate: String? = null,
    @Json(name = "last_updated_date")
    val lastUpdatedDate: String? = null,
    @Json(name = "display_chat")
    val displayChat: List<ConversationTurn>?,
)
