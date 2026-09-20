package com.wapo.flagship.features.search2.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PostAnswerFeedback(
    @Json(name = "type")
    override val type: String?,
    @Json(name = "endpoint")
    val endpoint: String?,
    @Json(name = "response_id")
    val responseId: String?,
) : PostAnswerItem(
        type = type,
    )
