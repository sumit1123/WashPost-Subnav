package com.wapo.flagship.features.feedback.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UnifiedFeedbackRequest(
    @Json(name = "content_id") val contentId: String?,
    @Json(name = "rating") val rating: Int?,
    @Json(name = "feedback") val feedback: String?,
    @Json(name = "scale") val scale: Int?,
    @Json(name = "metadata") val metadata: FeedbackMetadata? = null,
)