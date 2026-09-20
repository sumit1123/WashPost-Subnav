package com.wapo.flagship.features.ask.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TalkToThePostRequest(
    @Json(name = "question_text")
    val questionText: String
)
