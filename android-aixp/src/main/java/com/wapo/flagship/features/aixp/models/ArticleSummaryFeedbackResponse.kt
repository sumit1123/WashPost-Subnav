/* Copyright (c) 2024 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.aixp.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.lang.StringBuilder

@JsonClass(generateAdapter = true)
data class ArticleSummaryFeedbackResponse(
    @Json(name = "detail") val detail: FeedbackSubmissionDetail?,
    @Json(name = "message") val message: String? = null,
) {
    override fun toString(): String {
        return StringBuilder().apply {
            if (!message.isNullOrEmpty())
                append(message)
            if (!detail?.message.isNullOrEmpty())
                append("\n${detail?.message}")
            if (!detail?.error.isNullOrEmpty())
                append("\n${detail?.error}")
        }.toString()
    }
}

@JsonClass(generateAdapter = true)
data class FeedbackSubmissionDetail(
    @Json(name = "message") val message: String?,
    @Json(name = "error") val error: String?,
)