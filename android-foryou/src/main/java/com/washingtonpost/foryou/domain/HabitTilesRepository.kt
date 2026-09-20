/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.washingtonpost.foryou.domain

import com.washingtonpost.foryou.data.HabitTilesResponse
import com.washingtonpost.foryou.network.APIResult
import com.washingtonpost.foryou.repo.ForYouFeedRepositoryImpl

interface HabitTilesRepository {

    suspend fun getHabitTilesFeed(
        skipCache: Boolean = false,
        surface: String = ForYouFeedRepositoryImpl.SURFACE_HOME,
    ): APIResult<HabitTilesResponse>

    suspend fun refresh(): APIResult<HabitTilesResponse>

    suspend fun getCache(): APIResult<HabitTilesResponse>

    fun canRequestPersonalizedData(): Boolean

    suspend fun clearCache()
}
