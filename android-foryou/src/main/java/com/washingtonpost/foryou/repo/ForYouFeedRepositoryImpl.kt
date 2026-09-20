package com.washingtonpost.foryou.repo

import android.content.Context
import com.wapo.android.commons.di.CoroutineScopeCommonsModule
import com.wapo.android.commons.domain.DeviceUtilRepo
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.Logger
import com.wapo.android.remotelog.logger.RemoteLog
import com.washingtonpost.foryou.BuildConfig
import com.washingtonpost.foryou.ConsumedListProvider
import com.washingtonpost.foryou.cache.Cache
import com.washingtonpost.foryou.cache.WidgetCache
import com.washingtonpost.foryou.data.ConsumedArticles
import com.washingtonpost.foryou.data.ForYouContentType
import com.washingtonpost.foryou.data.ForYouRequestBody
import com.washingtonpost.foryou.data.ForYouResponse
import com.washingtonpost.foryou.data.RecommendationsItem
import com.washingtonpost.foryou.domain.ForYouFeedRepository
import com.washingtonpost.foryou.network.APIResult
import com.washingtonpost.foryou.remote.ForYouService
import com.washingtonpost.foryou.repo.ForYouFeedRepositoryImpl.Companion.SURFACE_RECIRC_SOFTWALL
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ForYouFeedRepository"

class ForYouFeedRepositoryImpl
@Inject
constructor(
    @ApplicationContext val context: Context,
    scope: CoroutineScope,
    @CoroutineScopeCommonsModule.IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val forYouService: ForYouService,
    private val consumedListProvider: ConsumedListProvider,
    private val cache: Cache,
    private val widgetCache: WidgetCache,
    private val forYouMetaProvider: ForYouMetaProvider,
    private val deviceUtilRepo: DeviceUtilRepo
): ForYouFeedRepository {
    private val checkReadList = cache.checkReadList
    private val timeout = if (BuildConfig.DEBUG) 20_000 else 5_000
    private var currentReadList = listOf<ConsumedArticles>()

    init {
        scope.launch (ioDispatcher) {
            currentReadList = consumedListProvider.getReadList()
            Logger.d(TAG, "init 4U repo: ${cache.checkReadList}\n ${cache.ttlsMs}")
        }
    }

    override suspend fun getForYouRecommendations(
        skipReadingFromCache: Boolean,
        excludeList: List<String>,
        surface: String,
        limit: Int?,
        currentUrl: String?,
        contentType: List<String>?
    ): APIResult<ForYouResponse> {
        Logger.d(TAG, "getForYouFeed(), surface=$surface")
        if (cache.isCacheValid(surface) && !validateReadList() && !skipReadingFromCache) {
            val cacheData = cache.getRecommendationList(surface)
            if (cacheData != null) {
                Logger.d(TAG, "4URepo: cache was used")
                return APIResult.Success(cacheData)
            }
        }

        val forYouMeta = forYouMetaProvider.getForYouMeta()
        val consumedList =
            when {
                !forYouMeta.privacyConsentGiven -> emptyList()
                else -> consumedListProvider.getReadList()
            }

        val apiResult =
            forYouService.getFeed(
                timeout,
                forYouMeta.clientId,
                ForYouRequestBody(
                    consumedList,
                    excludeList,
                    jucId = forYouMeta.sessionId,
                    wapoLoginId = forYouMeta.loginId,
                    surface = surface,
                    surfaceVariant = getSurfaceVariant(),
                    limit = limit,
                    currentUrl = currentUrl,
                    contentType = contentType
                ),
            )

        when (apiResult) {
            is APIResult.Success -> {
                Logger.d(TAG, "getForYouFeed: SUCCESS $apiResult")
                val data = apiResult.data
                if (data != null) {
                    if (surface == SURFACE_RECIRC_SOFTWALL) {
                        cache.addRecommendationList(apiResult.data, surface)
                    } else {
                        cache.saveRecommendationList(apiResult.data, surface)
                    }
                }
            }

            is APIResult.Failure -> {
                Logger.d(TAG, "getForYouFeed: ERROR ${apiResult.rawResponse}")
                remoteLog("ForYou Failure", apiResult.rawResponse ?: "Failure")
                val cacheData = cache.getRecommendationList(surface)
                if (cacheData != null) {
                    return APIResult.Success(cacheData, true)
                }
            }

            is APIResult.NetworkError -> {
                Logger.d(TAG, "getForYouFeed: NETWORK ERROR: ${apiResult.error}")
                val cacheData = cache.getRecommendationList(surface)
                if (cacheData != null) {
                    return APIResult.Success(cacheData, true)
                }
            }
        }

        return apiResult
    }

    // PTR clears items in shared pref and makes a call to getForYouFeed
    override suspend fun refresh(): APIResult<ForYouResponse> = getForYouRecommendations(
        true,
        surface = SURFACE_FEED,
        contentType = listOf(ForYouContentType.ARTICLE.type, ForYouContentType.VIDEO.type)
    )

    /** Retrieves more recommendations for the [surface], merges them with that surface's cached recs,
     * and returns a response with the new merged recommendations list
     * @param surface The surface this request is being made for
     * @param limit How many recommendations to retrieve from the FY Flex service
     * @param currentUrl The url of the article being viewed. For [SURFACE_RECIRC_SOFTWALL] this is
     * the currently recommended article in the MAP wall
     * @return An [ForYouResponse] with the recommendations being a merged list of cached recommendations
     * for the surface and the items that were retrieved (wrapped in an [APIResult])
     */
    override suspend fun getMoreRecommendations(
        surface: String,
        limit: Int?,
        currentUrl: String?,
        contentType: List<String>?
    ): APIResult<ForYouResponse> {
        Logger.d(TAG, "getMoreRecommendations(), surface=$surface")
        val cachedForYouResponse = cache.getRecommendationList(surface)
        val cachedRecommendations = cachedForYouResponse?.recommendations
        val excludeList = cachedRecommendations?.mapNotNull { it.url } ?: emptyList()
        val forYouMeta = forYouMetaProvider.getForYouMeta()
        val readList =
            if (forYouMeta.privacyConsentGiven) consumedListProvider.getReadList() else emptyList()
        if (!cache.recommendationsAreAtLimit(surface) || surface == SURFACE_RECIRC_SOFTWALL) {
            val apiResult =
                forYouService.getFeed(
                    timeout,
                    forYouMeta.clientId,
                    ForYouRequestBody(
                        readList,
                        excludeList,
                        jucId = forYouMeta.sessionId,
                        wapoLoginId = forYouMeta.loginId,
                        surface = surface,
                        surfaceVariant = getSurfaceVariant(),
                        limit = limit,
                        currentUrl = currentUrl,
                        contentType = contentType
                    ),
                )

            when (apiResult) {
                is APIResult.Success -> {
                    val data = apiResult.data
                    if (data != null) {
                        val mergedResult = cache.addRecommendationList(data, surface)
                        return APIResult.Success(mergedResult)
                    }
                }

                is APIResult.Failure -> {
                    Logger.d(TAG, "getForYouFeed: ERROR ${apiResult.rawResponse}")
                    remoteLog("ForYou Failure", apiResult.rawResponse ?: "Failure")
                }

                is APIResult.NetworkError -> {
                    Logger.d(TAG, "getForYouFeed: NETWORK ERROR: ${apiResult.error}")
                }
            }

            return apiResult
        }
        Logger.d(TAG, "getMoreRecommendations: LIMIT REACHED")
        return APIResult.Success(cachedForYouResponse)
    }

    override fun maxLimit(): Int = cache.maxLimit

    /**
     * Always succeeds but may return null data if no cache is available
     */
    override suspend fun getCache(surface: String): APIResult<ForYouResponse> {
        Logger.d(TAG, "getForYouFeed: cache only")
        val cacheData = cache.getRecommendationList(surface)
        return APIResult.Success(cacheData)
    }

    private fun remoteLog(
        message: String,
        errorMessage: String,
        widgetOriginated: Boolean = false
    ) {
        EventLog
            .Builder()
            .apply {
                setMessage(message)
                if (!widgetOriginated) setModule(LogModules.FOR_YOU)
                else setModule(LogModules.WIDGET)
                setErrorMessage(errorMessage)
            }.run {
                RemoteLog.e(context, build())
            }
    }

    private suspend fun validateReadList(): Boolean {
        if (!checkReadList) {
            Logger.d(TAG, "checkReadList=false, skipping")
            return false
        }
        val readListChanged = consumedListProvider.getReadList() != currentReadList
        Logger.d(
            TAG,
            "validateReadList: Reading list changed = $readListChanged and check reading history = $checkReadList"
        )
        currentReadList = consumedListProvider.getReadList()
        return readListChanged
    }

    /**
     * Articles in the Widget Cache are returned in pages
     * @param page The current page number being displayed by the Widget
     * @param pageSize The size of the page to be fetched
     */
    override fun getCachedArticlesPage(page: Int): List<RecommendationsItem> {
        val pageSize: Int = widgetCache.pageSize ?: 8
        return try {
            val cachedData = widgetCache.getRecommendationList(SURFACE_FEED)
            val recommendations = cachedData?.recommendations ?: emptyList()

            val startIndex = page * pageSize
            val endIndex = startIndex + pageSize

            // Return the slice for this page
            if (startIndex < recommendations.size) {
                recommendations.subList(startIndex, minOf(endIndex, recommendations.size))
            } else {
                emptyList() // No more articles available
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to get cached articles page $page", e)
            emptyList()
        }
    }

    /**
     * 1. Get recommendations for the Widget from the Flex API
     * 2. Clears existing WidgetCache
     * 3. Writes fetched recommendations to the WidgetCache
     */
    override suspend fun makeWidgetCall(
        forYouMeta: ForYouMetaData?
    ) {
        val meta = forYouMeta ?: forYouMetaProvider.getForYouMeta()

        try {

            val consumedList =
                when {
                    !meta.privacyConsentGiven -> emptyList()
                    else -> consumedListProvider.getReadList()
                }

            val widgetApiResult = forYouService.getFeed(
                timeout,
                meta.clientId,
                ForYouRequestBody(
                    readList = consumedList,
                    emptyList(),
                    jucId = meta.sessionId,
                    wapoLoginId = meta.loginId,
                    surface = SURFACE_FEED,
                    surfaceVariant = getSurfaceVariant(),
                    limit = widgetCache.maxLimit,
                    currentUrl = null,
                ),
            )

            when (widgetApiResult) {
                is APIResult.Success -> {
                    val widgetData = widgetApiResult.data
                    if (widgetData != null) {
                        widgetCache.clear(SURFACE_FEED)
                        widgetCache.saveRecommendationList(widgetData, SURFACE_FEED)
                        Logger.d(
                            TAG,
                            "Widget cache updated with ${widgetData.recommendations?.size ?: 0} articles"
                        )
                    }
                }

                is APIResult.Failure -> {
                    Logger.e(TAG, "Widget API call failed: ${widgetApiResult.rawResponse}")
                    remoteLog("ForYou Widget API Failure", "${widgetApiResult.rawResponse}")
                }

                is APIResult.NetworkError -> {
                    Logger.e(TAG, "Widget API network error: ${widgetApiResult.error}")
                }
            }
        } catch (e: Exception) {
            remoteLog("ForYou Widget Exception", "$e")
        }
    }

    private fun getSurfaceVariant(): String =
        if (deviceUtilRepo.isTablet()) {
            "tablet"
        } else {
            "phone"
        }

    companion object {
        const val WP_TIMEOUT = "WP_TIMEOUT"
        const val CLIENT_ID = "client_generated_id"
        const val SURFACE_AWAKEN = "awaken"
        const val SURFACE_FEED = "feed"
        const val SURFACE_RECIRC = "recirc"
        const val SURFACE_CAR = "car"

        /** MAP Wall */
        const val SURFACE_RECIRC_SOFTWALL = "recirc-softwall"
        const val SURFACE_NEXTAUDIO = "nextaudio"
        const val SURFACE_HOME = "home"
        const val SURFACE_HOMEPAGE = "homepage"

        const val TABLET_VARIANT = "tablet"
        const val PHONE_VARIANT = "phone"
    }
}
