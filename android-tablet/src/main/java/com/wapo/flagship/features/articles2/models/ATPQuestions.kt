package com.wapo.flagship.features.articles2.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ATPQuestions(
    @Json(name = "question_set")
    val questionSet: List<QuestionSet>? = null,
    @Json(name = "related_questions")
    val relatedQuestions: List<Question>? = null,
)
