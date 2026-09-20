package com.washingtonpost.foryou.cache

import android.content.Context
import com.washingtonpost.foryou.data.ForYouResponse
import com.squareup.moshi.Moshi
import com.washingtonpost.foryou.data.RemoteConfig

/**
 * A JSON-based cache implementation for For-You service.
 * It requires a moshi json parser for [ForYouResponse] class.
 * Can be configured for widget use with different TTL and storage location.
 */
class WidgetCache(
    context: Context,
    moshi: Moshi,
    remoteConfig: RemoteConfig
): Cache(
    context,
    moshi,
    remoteConfig,
    cacheType = "widget"
)