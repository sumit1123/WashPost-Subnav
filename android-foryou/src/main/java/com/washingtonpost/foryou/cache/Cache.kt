package com.washingtonpost.foryou.cache

import android.content.Context
import com.wapo.android.commons.util.Logger
import com.squareup.moshi.Moshi
import com.washingtonpost.foryou.data.ForYouResponse
import com.washingtonpost.foryou.data.RecommendationsItem
import com.washingtonpost.foryou.data.RemoteConfig
import com.washingtonpost.foryou.repo.ForYouFeedRepositoryImpl
import java.io.File
import java.util.Date
import androidx.core.content.edit

/**
 * A JSON-based cache implementation for For-You service.
 * It requires a moshi json parser for [ForYouResponse] class.
 */
open class Cache(
    private val context: Context,
    private val moshi: Moshi,
    remoteConfig: RemoteConfig,
    val cacheType: String = "",
) {
    private val cachePrefix = if (cacheType.isNotEmpty()) "$cacheType-" else ""
    private val cacheDir = File(context.filesDir, "${cachePrefix}for-you-cache/")
    private val jsonAdapter by lazy { moshi.adapter(ForYouResponse::class.java) }
    val ttlsMs = remoteConfig.ttls * 1000
    val checkReadList = remoteConfig.checkReadList
    val maxLimit = remoteConfig.maxSize
    val pageSize = remoteConfig.pageSize

    private fun getSharedPrefsKey(surface: String): String = "${cachePrefix}for-you-$surface-prefs"
    private fun getCacheFileName(surface: String): String =
        "${cachePrefix}for-you-$surface-cache.json"

    private fun getCacheFile(surface: String): File = File(cacheDir, getCacheFileName(surface))

    init {
        cleanUpIfNeeded()
    }

    /**
     * Re-write the existing cache with provided data
     */
    suspend fun saveRecommendationList(
        data: ForYouResponse,
        surface: String,
    ) {
        if (data.recommendations == null) return
        val dataToCache =
            if (surface == ForYouFeedRepositoryImpl.SURFACE_RECIRC_SOFTWALL) {
                val maxLimitItems = trimRecommendations(data.recommendations)
                ForYouResponse(
                    data.requestId,
                    data.recipeId,
                    data.testId,
                    maxLimitItems,
                )
            } else {
                data
            }

        try {
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val string = jsonAdapter.toJson(dataToCache)
            getCacheFile(surface).writeText(string)
            updateTTL(surface)

            Logger.d(TAG, "Cache saved: cacheType=$cacheType, surface=$surface, recommendations=${dataToCache.recommendations?.size ?: 0} recommendations")
        } catch (t: Throwable) {
            Logger.e(TAG, "Could not save for-you response: cacheType=$cacheType, surface=$surface", t)
        }
    }

    /**
     * Add the provided data to the existing cache.
     * @return A merged version of [ForYouResponse] with cached and new items
     */
    suspend fun addRecommendationList(
        data: ForYouResponse,
        surface: String,
    ): ForYouResponse {
        val cachedItems = getRecommendationList(surface)?.recommendations
        val newItems = data.recommendations
        val mergedItems = mutableListOf<RecommendationsItem>()
        mergedItems.addAll(cachedItems.orEmpty())
        mergedItems.addAll(newItems.orEmpty())
        val result = ForYouResponse(data.requestId, data.recipeId, data.testId, mergedItems)
        if (mergedItems.isNotEmpty()) {
            saveRecommendationList(result, surface)
        }
        return result
    }

    private fun trimRecommendations(recommendations: List<RecommendationsItem>): List<RecommendationsItem> {
        val maxLimitItems =
            if (recommendations.size > maxLimit) {
                val difference = recommendations.size - maxLimit
                recommendations.subList(difference, recommendations.size)
            } else {
                recommendations
            }
        return maxLimitItems
    }

    /**
     * Get previously saved [ForYouResponse] or null if cache in invalid/empty
     */
    fun getRecommendationList(surface: String): ForYouResponse? {
        try {
            val cacheFile = getCacheFile(surface)
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
    fun isCacheValid(surface: String): Boolean {
        val ttl = context
            .getSharedPreferences(getSharedPrefsKey(surface), Context.MODE_PRIVATE)
            .getLong(TTL_KEY, 0)
        return ttl > Date().time
    }

    /**
     * Check if max size recommendations have been hit
     */
    suspend fun recommendationsAreAtLimit(surface: String): Boolean =
        (getRecommendationList(surface)?.recommendations?.size ?: 0) >= maxLimit

    /**
     * Delete everything in cache
     */
    suspend fun clear(surface: String) {
        context
            .getSharedPreferences(getSharedPrefsKey(surface), Context.MODE_PRIVATE)
            .edit {
                remove(TTL_KEY)
            }
        getCacheFile(surface).delete()
        Logger.d(TAG, "Cache cleared: cacheType=$cacheType, surface=$surface")
    }

    private fun updateTTL(surface: String) {
        val ttl = Date().time + ttlsMs
        context
            .getSharedPreferences(getSharedPrefsKey(surface), Context.MODE_PRIVATE)
            .edit {
                putLong(TTL_KEY, ttl)
            }
    }

    private fun cleanUpIfNeeded() {
        try {
            val prefs = context
                .getSharedPreferences("ForYouPreferences", Context.MODE_PRIVATE)
            val versionKey = "$cachePrefix$CACHE_VERSION_KEY"
            val currentVersion = prefs.getInt(versionKey, 1)
            if (currentVersion < CACHE_VERSION) {
                cacheDir.listFiles()?.forEach { it.deleteRecursively() }
                prefs.edit {
                    putInt(versionKey, CACHE_VERSION)
                }
            }
        } catch (t: Throwable) {
            Logger.e(TAG, "runMigrationIfNeeded failed", t)
        }
    }

    companion object {
        private const val TAG = "Cache"
        private const val TTL_KEY = "TTL"
        private const val CACHE_VERSION = 2
        private const val CACHE_VERSION_KEY = "ForYouCacheVersion"
    }
}