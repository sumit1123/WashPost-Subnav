/* Copyright (c) 2024 The Washington Post. All rights reserved. */
package com.wapo.view.habittiles

/**
 * UI Tile model
 */
data class Tile(
    val score: Double?,
    val tileCategory: String?,
    val imageUrl: String?,
    val contextLabel: String?,
    val contextIndicator: String?,
    val tileLabel: String?,
    val tileLink: String?,
    val tileLabelBehavior: String?,
    val tileCategoryDetail: String?,
    val position: Int?,
    val persoPodcastMetadata: PersonalizedPodcast? = null
)

data class TilesCta(
    val text: String?,
    val url: String?
)

data class PersonalizedPodcast(
    val id: String?,
    val mp3Url: String?,
    val itemType: String?,
    val kicker: String?,
    val transcript: String?,
    val title: String?,
    val summary: String?,
    val audioFilePath: String?,
    val audioDuration: Float?,
    val totalCharacters: Float?,
    val image: String?,
    val articlesUsed: List<ArticleSource>?,
    val createdAt: String?
)

data class ArticleSource(
    val headline: String?,
    val canonicalUrl: String?,
    val publishDate: String?,
    val text: String?,
    val imageUrl: String?
)