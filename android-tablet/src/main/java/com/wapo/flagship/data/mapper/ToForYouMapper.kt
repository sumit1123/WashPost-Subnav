/* Copyright (c) 2024 The Washington Post. All rights reserved. */
import com.wapo.flagship.features.grid.domain.model.SectionHabitTile
import com.wapo.flagship.features.personalizedpodcasts.model.ArticleSource
import com.wapo.flagship.features.personalizedpodcasts.model.PersonalizedPodcast

/**
 * Function to map Data Tile model to UI Tile model
 */
fun SectionHabitTile.toWapoViewsTile(): com.wapo.view.habittiles.Tile {
    return com.wapo.view.habittiles.Tile(
        score = score,
        tileCategory = tileCategory,
        imageUrl = imageUrl,
        contextLabel = contextLabel,
        contextIndicator = contextIndicator,
        tileLabel = tileLabel,
        tileLink = tileLink,
        tileLabelBehavior = tileLabelBehavior,
        tileCategoryDetail = tileCategoryDetail,
        position = position,
        persoPodcastMetadata = persoPodcastMetadata.toHabitTilePodcastMetadata()
    )
}

fun PersonalizedPodcast?.toHabitTilePodcastMetadata(): com.wapo.view.habittiles.PersonalizedPodcast? {
    if (this == null) return null
    return com.wapo.view.habittiles.PersonalizedPodcast(
        id = id,
        mp3Url = mp3Url, // temporarily used till backend makes updates
        itemType = itemType,
        kicker = kicker,
        transcript = transcript,
        title = title,
        summary = summary,
        audioFilePath = audioFilePath,
        audioDuration = audioDuration,
        totalCharacters = totalCharacters,
        image = image,
        articlesUsed = articlesUsed.toHabitTileArticleSource(),
        createdAt = createdAt
    )
}

private fun List<ArticleSource>?.toHabitTileArticleSource(): List<com.wapo.view.habittiles.ArticleSource>? {
    return this?.map {
        com.wapo.view.habittiles.ArticleSource(
            headline = it.headline,
            canonicalUrl = it.canonicalUrl,
            publishDate = it.displayDate,
            text = it.text,
            imageUrl = it.imageUrl
        )
    }
}
