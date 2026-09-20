package com.washingtonpost.android.config.utils

import com.washingtonpost.android.config.data.datasources.utils.ConfigMoshiAdapters
import com.washingtonpost.android.config.data.datasources.utils.ConfigMoshiAdapters.mapAdapter
import kotlin.collections.orEmpty

object MapUtils {
    fun mergeMaps(base: Map<String, Any?>, override: Map<String, Any?>): Map<String, Any?> {
        if (override.isEmpty()) return base

        val result = base.toMutableMap()
        for ((key, overrideValue) in override) {
            val baseValue = base[key]
            if (baseValue is Map<*, *> && overrideValue is Map<*, *>) {
                //  Recursively merge if boths are maps
                val merged = mergeMaps(
                    baseValue.toMutableMap() as MutableMap<String, Any?>,
                    overrideValue as Map<String, Any?>,
                )
                result[key] = merged
            } else {
                result[key] = overrideValue
            }
        }
        return result
    }

    fun mergeMaps(base: Map<String, Any?>, overrides: List<Map<String, Any?>>): Map<String, Any?> {
        var result = base
        for (override in overrides) {
            result = mergeMaps(result, override)
        }
        return result
    }

    inline fun <reified T> T.merge(override: T): T {
        if (this == null || override == null) return this
        val tAdapter = ConfigMoshiAdapters.moshi.adapter(T::class.java).nullSafe()
        val srcJson = tAdapter.toJson(this)
        val srcMap = mapAdapter.fromJson(srcJson).orEmpty()
        val overrideJson = tAdapter.toJson(override)
        val overrideMap = mapAdapter.fromJson(overrideJson).orEmpty()
        val mergedMap = mergeMaps(srcMap, overrideMap)
        val mergedJson = mapAdapter.toJson(mergedMap)
        return tAdapter.fromJson(mergedJson) ?: this
    }
}