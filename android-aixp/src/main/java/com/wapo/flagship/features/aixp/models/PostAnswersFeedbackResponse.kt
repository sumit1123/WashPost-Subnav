package com.wapo.flagship.features.aixp.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PostAnswersFeedbackResponse(
    @Json(name = "detail")
    val detail: String? = null
)
