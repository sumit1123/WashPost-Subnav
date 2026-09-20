/* Copyright (c) 2024 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.aixp.models

import com.squareup.moshi.Json

data class ArticleSummaryFeedbackRequest(
    @Json(name = "content_id") val contentId: String?,
    @Json(name = "url") val url: String?,
    @Json(name = "user_id") val userId: String?,
    @Json(name = "rating") val rating: Int?,
    @Json(name = "feedback") val feedback: String?,
    @Json(name = "platform") val platform: String? = "app"
)
