package com.wapo.flagship.features.search2.model

import com.squareup.moshi.Json

data class AskThePostRequest(
    val message: String,
    @Json(name = "conversation_id")
    val conversationId: String? = null,
    val uuid: String?,
    val anonymous: Boolean,
    @Json(name = "voice_response")
    val voiceResponse: String? = null,
    val timestamp: Float? = null,
    @Json(name = "transcript_url")
    val transcriptUrl: String? = null,
    @Json(name = "share_id")
    val shareId: String? = null
)
