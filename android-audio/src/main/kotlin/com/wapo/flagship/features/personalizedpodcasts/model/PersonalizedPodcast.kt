package com.wapo.flagship.features.personalizedpodcasts.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PersonalizedPodcasts(
    @Json(name = "podcasts")
    val personalizedPodcasts: List<PersonalizedPodcast>?
)

@JsonClass(generateAdapter = true)
data class PersonalizedPodcast(
    @Json(name = "id")
    val id: String?,
    @Json(name = "mp3_url")
    val mp3Url: String?,
    @Json(name = "item_type")
    val itemType: String?,
    @Json(name = "kicker")
    val kicker: String?,
    @Json(name = "transcript_url")
    val transcript: String?,
    @Json(name = "title")
    val title: String?,
    @Json(name = "summary")
    val summary: String?,
    @Json(name = "audio_file_path")
    val audioFilePath: String?,
    @Json(name = "audio_duration")
    val audioDuration: Float?,
    @Json(name = "total_characters")
    val totalCharacters: Float?,
    @Json(name = "image")
    val image: String?,
    @Json(name = "articles_used")
    val articlesUsed: List<ArticleSource>?,
    @Json(name = "created_at")
    val createdAt: String?
)

@JsonClass(generateAdapter = true)
data class ArticleSource(
    @Json(name = "headline")
    val headline: String?,
    @Json(name = "canonical_url")
    val canonicalUrl: String?,
    @Json(name = "display_date")
    val displayDate: String?,
    @Json(name = "text")
    val text: String?,
    @Json(name = "image")
    val imageUrl: String?
)