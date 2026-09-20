package com.wapo.flagship.features.articles2.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class QuestionSet(
    @Json(name = "id")
    val id: String?,
    @Json(name = "questions")
    val questions: List<Question>?,
)
