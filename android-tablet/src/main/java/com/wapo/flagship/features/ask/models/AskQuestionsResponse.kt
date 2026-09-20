// Copyright (c) 2024 The Washington Post. All rights reserved.
package com.wapo.flagship.features.ask.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AskQuestionsResponse(
    @Json(name = "questions")
    val questions: List<QuestionItem?>?,
    @Json(name = "categories")
    val categories: List<CategoryItem>?
)

@JsonClass(generateAdapter = true)
data class QuestionItem(
    @Json(name = "text")
    val text: String?,
    @Json(name = "uuid")
    val uuid: String?,
    @Json(name = "topic_id")
    val topicId: String?
)

@JsonClass(generateAdapter = true)
data class CategoryItem(
    @Json(name = "id")
    val id: String,
    @Json(name = "name")
    val name: String,
    @Json(name = "questions")
    val questions: List<QuestionItem>,
)
