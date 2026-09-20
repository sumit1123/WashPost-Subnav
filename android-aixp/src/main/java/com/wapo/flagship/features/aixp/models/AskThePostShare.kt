package com.wapo.flagship.features.aixp.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AskThePostShare(
    @Json(name = "conversation_id")
    val conversationId: String? = null,
    @Json(name = "conversation_title")
    val conversationTitle: String? = null,
    @Json(name = "share_id")
    val shareId: String,
    @Json(name = "share_created_date")
    val shareCreatedDate: String?,
)