package com.wapo.flagship.auto.ask

data class AskFeaturedQuestion(
    val id: String,
    val text: String,
)

interface AskFeaturedQuestionsGateway {
    suspend fun loadFeaturedQuestions(): List<AskFeaturedQuestion>
}
