/* Copyright (c) 2024 The Washington Post. All rights reserved. */
package com.washingtonpost.foryou.cache

import android.content.Context
import com.wapo.android.commons.util.Logger
import com.squareup.moshi.JsonAdapter
import com.washingtonpost.foryou.data.HabitTilesResponse
import com.washingtonpost.foryou.data.RemoteConfig
import java.io.File
import java.util.Date
import androidx.core.content.edit
import com.washingtonpost.foryou.domain.HabitTilesCache

private const val SHARED_PREFS_NAME = "habit-tiles-prefs"
private const val TTL_KEY = "TTL"

/**
 * A JSON-based cache implementation for Habit Tiles service.
 * It requires a moshi json parser for [HabitTilesResponse] class.
 */
class HabitTilesCacheImpl(
    private val context: Context,
    private val jsonAdapter: JsonAdapter<HabitTilesResponse>,
    remoteConfig: RemoteConfig
): HabitTilesCache {
    private val cacheDir = File(context.filesDir, "habit-tiles-cache/")
    private val cacheFile = File(cacheDir, "habit-tiles-cache.json")
    val ttlsMs = remoteConfig.ttls * 1000
    val checkReadList = remoteConfig.checkReadList

    /**
     * Re-write the existing cache with provided data
     */
    override fun saveTilesList(data: HabitTilesResponse) {
        if (data.tiles == null) return
        try {
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val string = jsonAdapter.toJson(data)
            cacheFile.writeText(string)
            updateTTL()
        } catch (t: Throwable) {
            Logger.e("Cache", "Could not save habit tiles response", t)
        }
    }

    /**
     * Get previously saved [HabitTilesResponse] or null if cache in invalid/empty
     */
    override fun getTilesList(): HabitTilesResponse? {
        try {
            if (cacheFile.exists()) {
                val string = cacheFile.readText()
                return jsonAdapter.fromJson(string)
            }
            return null
        } catch (t: Throwable) {
            return null
        }
    }

    /**
     * Check if cached version is expired by TTL
     */
    override fun isCacheValid(): Boolean {
        val ttl = context
            .getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(TTL_KEY, 0)
        return ttl > Date().time
    }

    /**
     * Delete everything in cache
     */
    override fun clear() {
        context
            .getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                remove(TTL_KEY)
            }
        cacheFile.delete()
    }

    override fun checkReadList(): Boolean {
        return checkReadList
    }

    override fun getTtlsMsValue(): Long {
        return ttlsMs
    }

    private fun updateTTL() {
        val ttl = Date().time + ttlsMs
        val sharedPreferences =
            context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences
            .edit {
                putLong(TTL_KEY, ttl)
            }
    }
}