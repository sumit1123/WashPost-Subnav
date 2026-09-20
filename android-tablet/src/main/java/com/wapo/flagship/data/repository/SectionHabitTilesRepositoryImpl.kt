// Copyright (c) 2026 The Washington Post. All rights reserved.
package com.wapo.flagship.data.repository

import com.wapo.flagship.features.grid.domain.model.SectionHabitTile
import com.wapo.flagship.features.grid.domain.model.SectionHabitTilesData
import com.wapo.flagship.features.grid.domain.repository.SectionHabitTilesRepository
import com.washingtonpost.foryou.data.Tile
import com.washingtonpost.foryou.domain.HabitTilesRepository
import com.washingtonpost.foryou.network.APIResult
import javax.inject.Inject

class SectionHabitTilesRepositoryImpl @Inject constructor(
    private val habitTilesRepository: HabitTilesRepository
): SectionHabitTilesRepository {

    override suspend fun getHabitTilesFeed(skipCache: Boolean): SectionHabitTilesData? {
        val result = habitTilesRepository.getHabitTilesFeed(skipCache)
        return when (result) {
            is APIResult.Success -> {
                SectionHabitTilesData(
                    tiles = result.data?.tiles?.filterNotNull()?.map {
                        mapToSectionHabitTilesData(it)
                    },
                    requestId = result.data?.requestId,
                    testGroup = result.data?.testGroup
                )
            }

            else -> {
                null
            }
        }
    }

    private fun mapToSectionHabitTilesData(tile: Tile): SectionHabitTile {
        return SectionHabitTile(
            score = tile.score,
            tileCategory = tile.tileCategory,
            imageUrl = tile.imageUrl,
            contextLabel = tile.contextLabel,
            contextIndicator = tile.contextIndicator,
            tileLabel = tile.tileLabel,
            tileLink = tile.tileLink,
            tileLabelBehavior = tile.tileLabelBehavior,
            tileCategoryDetail = tile.tileCategoryDetail,
            position = tile.position,
            persoPodcastMetadata = tile.persoPodcastMetadata
        )
    }

    override fun canRequestPersonalizedData(): Boolean =
        habitTilesRepository.canRequestPersonalizedData()
}
