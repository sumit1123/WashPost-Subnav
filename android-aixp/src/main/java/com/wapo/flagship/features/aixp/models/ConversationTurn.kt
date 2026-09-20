package com.wapo.flagship.features.aixp.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ConversationTurn(
    @Json(name = "turn_id")
    val turnId: Int,

    @Json(name = "timestamp")
    val timestamp: String,

    @Json(name = "in_response_to")
    val inResponseTo: Int? = null,

    @Json(name = "role")
    val role: String,

    @Json(name = "message")
    val message: String,

    @Json(name = "sources")
    val sources: List<ArticleSource>? = null,

    @Json(name = "search_results")
    val searchResults: List<String>? = null,

    @Json(name = "system_message")
    val systemMessage: String? = null
)

@JsonClass(generateAdapter = true)
data class Source(
    @Json(name = "headline")
    val headline: String,

    @Json(name = "canonical_url")
    val canonicalUrl: String,

    @Json(name = "publish_date")
    val publishDate: String,

    @Json(name = "text")
    val text: String,

    @Json(name = "image")
    val image: String
)
