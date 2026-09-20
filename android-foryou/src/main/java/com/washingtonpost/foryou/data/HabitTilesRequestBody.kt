/* Copyright (c) 2024 The Washington Post. All rights reserved. */
package com.washingtonpost.foryou.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.TimeZone

/**
 * Habit Tiles API Request Body
 */
@JsonClass(generateAdapter = true)
data class HabitTilesRequestBody(
    @Json(name = "request_id") val requestId: String?,
    @Json(name = "j_ucid") val jucId: String?,
    @Json(name = "wapo_login_id") val wapoLoginId: String?,
    @Json(name = "interface") val platform: String = "android",
    @Json(name = "surface") val surface: String?,
    @Json(name = "surface_variant") val surfaceVariant: String?,
    @Json(name = "readlist") val readList: List<ConsumedArticles?>? = emptyList(),
    @Json(name = "user_timezone") val userTimeZone: String? = null,
    @Json(name = "overrides") val overrides: HabitTilesOverrides? = null, // Testing purpose. overrides should be null in prod
)

// Testing purpose.
data class HabitTilesOverrides(
    @Json(name = "authors") val authors: List<String> = listOf("abutaleby", "jacobss"),
    @Json(name = "all_tiles") val allTiles: Boolean = false,
    @Json(name = "ai_podcast") val aiPodcast: Boolean = false,
    @Json(name = "tile_count") val tileCount: Int = 4,
    @Json(name = "games") val games: List<String> = listOf(
        "games-comics - games-keyword",
        "games-keyword",
        "/crosswords/daily",
        "/crosswords/mini-meta",
        "games-comics - quiz"
    ),
    @Json(name = "the_seven") val theSeven: Boolean = false,
    @Json(name = "lufs") val lufs: Boolean = false,
    @Json(name = "subsections") val subsections: List<String> = listOf(
        "D.C. Politics",
        "Inspired Life",
        "Post Reports"
    ),
    @Json(name = "print_edition") val printEdition: Boolean = false,
)
