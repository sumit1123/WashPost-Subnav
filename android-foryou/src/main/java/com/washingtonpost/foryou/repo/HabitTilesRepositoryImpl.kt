/* Copyright (c) 2024 The Washington Post. All rights reserved. */
package com.washingtonpost.foryou.repo

import com.wapo.android.commons.di.CoroutineScopeCommonsModule
import com.wapo.android.commons.domain.DeviceUtilRepo
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.Logger
import com.wapo.android.domain.repository.RemoteLogRepo
import com.washingtonpost.foryou.BuildConfig
import com.washingtonpost.foryou.ConsumedListProvider
import com.washingtonpost.foryou.domain.HabitTilesCache
import com.washingtonpost.foryou.data.ApiDataListener
import com.washingtonpost.foryou.data.ConsumedArticles
import com.washingtonpost.foryou.data.HabitTilesOverrides
import com.washingtonpost.foryou.data.HabitTilesRequestBody
import com.washingtonpost.foryou.data.HabitTilesResponse
import com.washingtonpost.foryou.domain.HabitTilesRepository
import com.washingtonpost.foryou.network.APIResult
import com.washingtonpost.foryou.remote.ForYouService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject

private const val TAG = "HabitTilesRepository"

class HabitTilesRepositoryImpl @Inject constructor(
    @CoroutineScopeCommonsModule.IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    coroutineScope: CoroutineScope,
    private val forYouService: ForYouService,
    private val consumedListProvider: ConsumedListProvider,
    private val cache: HabitTilesCache,
    private val forYouMetaProvider: ForYouMetaProvider,
    private val apiDataListener: ApiDataListener,
    private val deviceUtilRepo: DeviceUtilRepo,
    private val remoteLog: RemoteLogRepo
): HabitTilesRepository {
    private val checkReadList = cache.checkReadList()
    private val timeout = if (BuildConfig.DEBUG) 20_000 else 5_000
    private var currentReadList = listOf<ConsumedArticles>()

    init {
        coroutineScope.launch(ioDispatcher) {
            currentReadList = consumedListProvider.getReadList()
            Logger.d(TAG, "init HabitTiles repo: ${cache.checkReadList()}\n ${cache.getTtlsMsValue()}")
        }
    }

    override suspend fun getHabitTilesFeed(skipCache: Boolean, surface: String): APIResult<HabitTilesResponse> {
        val readingListChanged = validateReadList()
        Logger.d(TAG, "getHabitTilesFeed()")
        if (cache.isCacheValid() && !readingListChanged && !skipCache) {
            val cacheData = cache.getTilesList()
            if (cacheData != null) {
                Logger.d(TAG, "HabitTilesRepo: cache was used")
                return APIResult.Success(cacheData)
            }
        }

        Logger.d(TAG, "HabitTilesRepo: cache is not valid, using network")
        val forYouMeta = withContext(ioDispatcher) {
            forYouMetaProvider.getForYouMeta()
        }
        val consumedList = when {
            !forYouMeta.privacyConsentGiven -> emptyList()
            else -> consumedListProvider.getReadList()
        }
        val userTimeZone = when {
            !forYouMeta.privacyConsentGiven -> null
            else -> TimeZone.getDefault().id
        }

        val requestId = UUID.randomUUID().toString()

        val apiResult = forYouService.getHabitTiles(
            timeout,
            forYouMeta.clientId,
            HabitTilesRequestBody(
                requestId = requestId,
                jucId = forYouMeta.sessionId,
                wapoLoginId = forYouMeta.loginId,
                surface = surface,
                surfaceVariant = getSurfaceVariant(),
                readList = consumedList,
                userTimeZone = userTimeZone,
                overrides = if (AppContextUtils.isBetaBuild() || AppContextUtils.isDebugBuild()) HabitTilesOverrides(aiPodcast = true) else null
            )
        )

        Logger.d(TAG, "HabitTilesRepo: ${apiResult}")
        val additionalFields = hashMapOf<String, String>().apply {
            this["request_id"] = requestId
            this["jucid"] = (!forYouMeta.sessionId.isNullOrEmpty()).toString()
            this["loginId"] = (!forYouMeta.loginId.isNullOrEmpty()).toString()
        }

        when (apiResult) {
            is APIResult.Success -> {
                val data = apiResult.data
                if (data != null) {
                    cache.saveTilesList(apiResult.data)
                    apiDataListener.setABTestGroup(apiResult.data.testGroup)
                }
                additionalFields["test_group"] = data?.testGroup.toString()
                additionalFields["data_size"] = data?.tiles?.size.toString()
                additionalFields["tiles"] =
                    data?.tiles?.joinToString("|") { "${it?.tileCategory}:${it?.tileLabel}" }
                    .toString()
            }

            is APIResult.Failure -> {
                Logger.d(TAG, "HabitTiles: ERROR ${apiResult.rawResponse}")
                additionalFields["cache_size"] = cache.getTilesList()?.tiles?.size.toString()
                remoteLog("HabitTiles Failure", apiResult.rawResponse ?: "Failure", additionalFields)
                val cacheData = cache.getTilesList()
                if (cacheData != null) {
                    return APIResult.Success(cacheData, true)
                }
            }

            is APIResult.NetworkError -> {
                Logger.d(TAG, "HabitTiles: NETWORK ERROR: ${apiResult.error}")
                additionalFields["cache_size"] = cache.getTilesList()?.tiles?.size.toString()
                val cacheData = cache.getTilesList()
                if (cacheData != null) {
                    return APIResult.Success(cacheData, true)
                }
            }
        }

        return apiResult
    }

    /**
     * Force refresh the feed
     */
    override suspend fun refresh(): APIResult<HabitTilesResponse> {
        return getHabitTilesFeed(true)
    }

    /**
     * Always succeeds but may return null data if no cache is available
     */
    override suspend fun getCache(): APIResult<HabitTilesResponse> {
        Logger.d(TAG, "HabitTiles: cache only")
        val cacheData = cache.getTilesList()
        return APIResult.Success(cacheData)
    }

    private fun remoteLog(
        message: String,
        errorMessage: String?,
        additionalFields: HashMap<String, String>,
    ) {
        EventLog.Builder().apply {
            setMessage(message)
            setModule(LogModules.FOR_YOU)
            setForceUpload()
            if (!errorMessage.isNullOrEmpty()) {
                setErrorMessage(errorMessage)
            }
            additionalFields.forEach {
                set(it.key, it.value)
            }
        }.run {
            if (errorMessage.isNullOrEmpty()) {
                remoteLog.d(build())
            } else {
                remoteLog.e(build())
            }
        }
    }

    private suspend fun validateReadList() : Boolean {
        if (!checkReadList) {
            Logger.d(TAG, "checkReadList=false, skipping")
            return false
        }
        val readListChanged = consumedListProvider.getReadList() != currentReadList
        Logger.d(TAG, "validateReadList: Reading list changed = $readListChanged and check reading history = $checkReadList")
        currentReadList = consumedListProvider.getReadList()
        return readListChanged
    }

    private fun getSurfaceVariant(): String {
        return if (deviceUtilRepo.isTablet()) {
            "tablet"
        } else {
            "phone"
        }
    }

    override fun canRequestPersonalizedData(): Boolean {
        val meta = forYouMetaProvider.getForYouMeta()
        return !meta.sessionId.isNullOrEmpty() || !meta.loginId.isNullOrEmpty()
    }

    override suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            cache.clear()
        }
    }
}