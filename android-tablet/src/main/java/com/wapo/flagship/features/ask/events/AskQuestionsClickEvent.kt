// Copyright (c) 2024 The Washington Post. All rights reserved.
package com.wapo.flagship.features.ask.events

sealed interface AskQuestionsClickEvent {
    data class QuestionClickEvent(
        val id: String,
        val question: String,
    ) : AskQuestionsClickEvent

    data object LearnMoreClickEvent : AskQuestionsClickEvent

    data class DeepLinkClickEvent(
        val link: String,
    ) : AskQuestionsClickEvent
}
