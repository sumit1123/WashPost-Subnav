package com.wapo.flagship.auto.ask

import kotlinx.coroutines.flow.Flow

interface AskThePostGateway {
    val events: Flow<AskThePostEvent>

    fun tryAcquireSession(): Boolean

    fun startConversation(question: String)

    fun continueConversation(
        conversationId: String,
        question: String,
    )

    suspend fun endConversation()
}

sealed interface AskThePostEvent {
    data class ConversationStarted(
        val conversationId: String,
    ) : AskThePostEvent

    data class AudioChunk(
        val base64Audio: String,
    ) : AskThePostEvent

    data object Closed : AskThePostEvent

    data object Error : AskThePostEvent
}
