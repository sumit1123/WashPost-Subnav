package com.wapo.flagship.features.ask.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TalkToThePostResponse(
    @Json(name = "answer_audio")
    val answerAudio: String?,

    @Json(name = "answer_text")
    val answerText: String?,

    @Json(name = "related_articles")
    val relatedArticles: List<RelatedArticle>?
)

@JsonClass(generateAdapter = true)
data class RelatedArticle(
    @Json(name = "title")
    val title: String?,

    @Json(name = "url")
    val url: String?
)
