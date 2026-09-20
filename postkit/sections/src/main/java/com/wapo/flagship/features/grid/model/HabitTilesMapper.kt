/* Copyright (c) 2024 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.grid.model

import com.wapo.flagship.features.grid.HabitTilesEntity
import com.wapo.view.habittiles.TilesCta

object HabitTilesMapper {

    fun getHabitTiles(habitTilesEntity: HabitTilesEntity, baseEntity: Boolean = false): HabitTiles {
        val label =
            HomepageStoryMapper.getLabel(habitTilesEntity.label)?.apply { isStoryLabel = true }
        val cta = HomepageStoryMapper.getLabel(habitTilesEntity.cta)?.apply { isStoryLabel = true }
        return HabitTiles(habitTilesEntity.id, label, cta?.toTilesCta()).apply {
            layoutAttributes = if (habitTilesEntity.layoutAttributes != null && !baseEntity)
                PageModelMapper.getLayoutAttributes(habitTilesEntity.layoutAttributes)
            else
                PageModelMapper.createDefaultLayoutAttributes()
        }
    }

    private fun CompoundLabel.toTilesCta(): TilesCta {
        return TilesCta(
            text = text,
            url = link?.url
        )
    }
}