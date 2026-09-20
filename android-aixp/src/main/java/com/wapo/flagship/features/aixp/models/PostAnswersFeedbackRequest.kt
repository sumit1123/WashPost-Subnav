package com.wapo.flagship.features.aixp.models

import com.squareup.moshi.Json

data class PostAnswersFeedbackRequest(
    @Json(name = "uuid") val uuid: String?,
    @Json(name = "response_id") val responseId: String?,
    @Json(name = "rating") val rating: Int,
    @Json(name = "feedback") val feedback: String?,
    @Json(name = "turn_id") val turnId: String? = null,
    @Json(name = "conversation_id") val conversationId: String? = null
)
