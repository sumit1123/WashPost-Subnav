/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.washingtonpost.foryou.domain

import com.washingtonpost.foryou.data.HabitTilesResponse

interface HabitTilesCache {

    fun saveTilesList(data: HabitTilesResponse)

    fun getTilesList(): HabitTilesResponse?

    fun isCacheValid(): Boolean

    fun clear()

    fun checkReadList(): Boolean

    fun getTtlsMsValue(): Long
}
