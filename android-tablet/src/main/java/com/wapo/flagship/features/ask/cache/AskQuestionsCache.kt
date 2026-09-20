// Copyright (c) 2024 The Washington Post. All rights reserved.
package com.wapo.flagship.features.ask.cache

import android.content.Context
import com.wapo.android.commons.util.Logger
import com.squareup.moshi.JsonAdapter
import com.wapo.flagship.features.ask.models.AskQuestionsResponse
import java.io.File
import java.util.Date

private const val SHARED_PREFS_NAME = "ask-questions-prefs"
private const val TTL_KEY = "TTL"

/**
 * A JSON-based cache implementation for AskQuestions service.
 * It requires a moshi json parser for [AskQuestionsResponse] class.
 */
class AskQuestionsCache(
    private val context: Context,
    private val jsonAdapter: JsonAdapter<AskQuestionsResponse>,
    private val ttls: Int,
) {
    private val cacheDir = File(context.filesDir, "ask-questions-cache/")
    private val cacheFile = File(cacheDir, "ask-questions-cache.json")
    val ttlsMs = ttls * 1000

    /**
     * Re-write the existing cache with provided data
     */
    suspend fun saveQuestionsList(data: AskQuestionsResponse) {
        if (data.questions == null) return
        try {
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val string = jsonAdapter.toJson(data)
            cacheFile.writeText(string)
            updateTTL()
        } catch (t: Throwable) {
            Logger.e("Cache", "Could not save ask-the-post response", t)
        }
    }

    /**
     * Get previously saved [AskQuestionsResponse] or null if cache in invalid/empty
     */
    suspend fun getQuestionsList(): AskQuestionsResponse? {
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
    fun isCacheValid(): Boolean {
        val ttl =
            context
                .getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(TTL_KEY, 0)
        return ttl > Date().time
    }

    /**
     * Delete everything in cache
     */
    suspend fun clear() {
        context
            .getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(TTL_KEY)
            .apply()
        cacheFile.delete()
    }

    private fun updateTTL() {
        val ttl = Date().time + ttlsMs
        val sharedPreferences =
            context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences
            .edit()
            .putLong(TTL_KEY, ttl)
            .apply()
    }
}
