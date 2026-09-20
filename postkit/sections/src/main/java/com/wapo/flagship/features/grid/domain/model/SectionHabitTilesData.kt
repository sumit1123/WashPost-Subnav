// Copyright (c) 2026 The Washington Post. All rights reserved.
package com.wapo.flagship.features.grid.domain.model

import com.wapo.flagship.features.personalizedpodcasts.model.PersonalizedPodcast

data class SectionHabitTilesData(
    var tiles: List<SectionHabitTile>? = null,
    var requestId: String? = null,
    var testGroup: String? = null
)

data class SectionHabitTile(
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
    val persoPodcastMetadata: PersonalizedPodcast?
)
