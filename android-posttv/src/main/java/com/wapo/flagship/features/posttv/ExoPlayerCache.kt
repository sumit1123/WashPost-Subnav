/* Copyright (c) 2022 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.posttv

import android.content.Context
import android.net.Uri
import com.wapo.android.commons.util.Logger
import androidx.media3.common.util.Util
import androidx.media3.database.ExoDatabaseProvider
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSourceFactory
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.wapo.android.commons.util.AppContextUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

/**
 * Class to handle Cache Data Source for ExoPlayer data sources.
 * init method clears the old cache.
 */
object ExoPlayerCache {
    private var simpleCache: SimpleCache? = null
    private lateinit var leastRecentlyUsedCacheEvictor: LeastRecentlyUsedCacheEvictor
    private lateinit var exoDatabaseProvider: ExoDatabaseProvider
    private var exoPlayerCacheSize: Long = 32 * 1024 * 1024 // 32MB
    private lateinit var cacheDir: File
    private lateinit var defaultDataSourceFactory: DefaultDataSourceFactory

    fun init(appContext: Context, rootCacheDir: File, cacheSizeInBytes: Long) {
        exoPlayerCacheSize = cacheSizeInBytes
        cacheDir = File(rootCacheDir, "wp_exoplayer").apply { deleteRecursively() }
        leastRecentlyUsedCacheEvictor = LeastRecentlyUsedCacheEvictor(exoPlayerCacheSize)
        exoDatabaseProvider = ExoDatabaseProvider(appContext)
        simpleCache = SimpleCache(cacheDir, leastRecentlyUsedCacheEvictor, exoDatabaseProvider)
        defaultDataSourceFactory = DefaultDataSourceFactory(
            appContext,
            Util.getUserAgent(appContext, appContext.resources.getString(R.string.app_name))
        )
    }

    /**
     * Method to create a new default CacheDataSource.Factory object
     * Everytime this creates a new object so the caller components can still configure and update
     * the flags based on their workflows.
     */
    fun createCacheDataSourceFactory(): CacheDataSource.Factory {
        return CacheDataSource.Factory()
            .setCache(simpleCache as Cache)
            .setUpstreamDataSourceFactory(defaultDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /**
     * Helper method to set ignore flags (mainly to ignore unset length requests) on the given
     * cacheDataSource object.
     */
    fun ignoreCacheUnsetLengthRequests(cacheDataSource: CacheDataSource.Factory) {
        cacheDataSource.setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
                or CacheDataSource.FLAG_IGNORE_CACHE_FOR_UNSET_LENGTH_REQUESTS)
    }

    /**
     * Method can be called to cache the given url data
     */
    fun cacheVideo(
        videoUrl: String,
        progressListener: MediaProgressListener? = null
    ) {
        if (!AppContextUtils.isConnectingOrConnected()) {
            Logger.e(ExoPlayerCache.javaClass.simpleName, "No network connection")
            return
        }
        val videoUri = Uri.parse(videoUrl)
        val dataSpec = DataSpec(videoUri)
        val dataSource = createCacheDataSourceFactory().createDataSource()
        GlobalScope.launch(Dispatchers.IO) {
            try {
                CacheWriter(
                    dataSource,
                    dataSpec,
                    null
                ) { requestLength, bytesCached, newBytesCached ->
                    progressListener?.onProgress(requestLength, bytesCached, newBytesCached)
                }.cache()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    interface MediaProgressListener {
        fun onProgress(requestLength: Long, bytesCached: Long, newBytesCached: Long)
    }
}