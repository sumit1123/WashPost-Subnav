package com.washingtonpost.android.config.data.datasources.utils

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.washingtonpost.android.config.data.datasources.dto.config.RawConfig
import com.washingtonpost.android.config.data.datasources.dto.RawVersionConfig

object ConfigMoshiAdapters {
    val moshi: Moshi = Moshi.Builder()
        .add(SafeBooleanAdapter())
        .build()

    private val mapType = Types.newParameterizedType(
        Map::class.java,
        String::class.java,
        Any::class.java
    )
    val mapAdapter: JsonAdapter<Map<String, Any?>> = moshi
        .adapter<Map<String, Any?>?>(mapType)
        .nullSafe()

    private val stringListType = Types.newParameterizedType(
        List::class.java,
        String::class.java,
    )
    val stringListAdapter: JsonAdapter<List<String>> = moshi
        .adapter<List<String>?>(stringListType)
        .nullSafe()

    val rawConfigAdapter: JsonAdapter<RawConfig> = moshi
        .adapter(RawConfig::class.java)
        .nullSafe()
    val rawVersionConfigAdapter: JsonAdapter<RawVersionConfig> = moshi
        .adapter(RawVersionConfig::class.java)
        .nullSafe()
}