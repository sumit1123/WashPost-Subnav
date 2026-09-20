package com.washingtonpost.android.config.domain.models.config

data class Search2Config(
    val baseUrl: String,
    val postAnswersEnabled: Boolean,
    val askThePost: AskThePost,
    val learnMoreUrl: String,
    val talkToThePostBaseUrl: String,
)

data class AskThePost(
    val askThePostTimeOfDay: AskThePostTimeOfDay,
    val questionsBaseUrl: String,
    val converseBaseUrl: String,
    val questions: List<AskThePostQuestion>,
    val categories: List<AskThePostCategory>
)

data class AskThePostQuestion(
    val text: String,
    val uuid: String,
    val topicId: String?
)

data class AskThePostCategory(
    val id: String,
    val name: String,
    val questions: List<AskThePostQuestion>,
)

data class AskThePostTimeOfDay(
    val lateNight: TimeOfDay,
    val goodEvening: TimeOfDay,
    val goodAfternoon: TimeOfDay,
    val goodMorning: TimeOfDay,
)

data class TimeOfDay(
    val title: String,
    val subTitle: String,
)
