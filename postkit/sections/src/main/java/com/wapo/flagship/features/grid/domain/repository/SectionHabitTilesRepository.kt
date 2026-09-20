// Copyright (c) 2026 The Washington Post. All rights reserved.
package com.wapo.flagship.features.grid.domain.repository

import com.wapo.flagship.features.grid.domain.model.SectionHabitTilesData

interface SectionHabitTilesRepository {

    suspend fun getHabitTilesFeed(skipCache: Boolean = false): SectionHabitTilesData?

    fun canRequestPersonalizedData(): Boolean
}
