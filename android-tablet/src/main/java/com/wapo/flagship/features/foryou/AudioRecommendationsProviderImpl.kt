package com.wapo.flagship.features.foryou

import android.net.Uri
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.common.getArticlesAdTargetingValues
import com.wapo.flagship.features.articles2.models.deserialized.Date
import com.wapo.flagship.features.articles2.repo.Articles2Repository
import com.wapo.flagship.features.articles2.tracking.AudioTrackerImpl
import com.wapo.flagship.features.articles2.utils.getUrlWithoutParameters
import com.wapo.flagship.features.articles2.utils.toTrackingInfo
import com.wapo.flagship.features.audio.AudioRecommendationsProvider
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.grid.AudioArticleVoiceType
import com.wapo.flagship.model.Status
import com.wapo.flagship.querypolicies.Query
import com.wapo.android.commons.util.Logger
import com.wapo.android.commons.util.getCanonicalUrl
import com.wapo.flagship.di.app.modules.features.readinghistory.ReadingHistoryRepo
import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.articles2.models.deserialized.Audio
import com.wapo.flagship.features.audio.playlist.toAudioAdConfig
import com.wapo.flagship.features.audio.playlist.toAudioMediaConfig
import com.wapo.flagship.util.AudioUtil
import com.wapo.flagship.util.coroutines.DispatcherProvider
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.ImageServiceConfig
import com.washingtonpost.android.save.database.model.MetadataModel
import com.washingtonpost.android.save.database.model.ReadingHistoryModel
import com.washingtonpost.foryou.data.RecommendationsItem
import com.washingtonpost.foryou.data.getURL
import com.washingtonpost.foryou.domain.ForYouFeedRepository
import com.washingtonpost.foryou.network.APIResult
import com.washingtonpost.foryou.repo.ForYouFeedRepositoryImpl
import kotlinx.coroutines.withContext
import java.net.MalformedURLException
import java.util.LinkedList
import java.util.Queue
import javax.inject.Inject

/**
 * Implementation of [AudioRecommendationsProvider]
 * [forYouFeedRepository] - required to get the next set of recommendations to be played
 * [article2Repository] - required to get the audio meta data for the next audio that is to be played
 */
class AudioRecommendationsProviderImpl
    @Inject
    constructor(
        private val forYouFeedRepository: ForYouFeedRepository,
        private val article2Repository: Articles2Repository,
        val dispatcherProvider: DispatcherProvider,
        private val readingHistoryRepository: ReadingHistoryRepo
    ) : AudioRecommendationsProvider {
        /**
         * Keep a queue of recommendations to be played. This queue will change each time for you is called
         * but if for you fails, next item in queue will be used
         */
        private var nextAudioQueue: Queue<RecommendationsItem> = LinkedList()

        /**
         * Cached audio config for next audio to be played
         */
        private var nextAudioConfigCached: AudioMediaConfig? = null

        /**
         * has next audio played as a result of rollthrough
         * - previous audio completed -> true
         * - user clicked next -> false
         */
        private var isRollthrough = false

        override fun getNextAudioRecommendation(): AudioMediaConfig? = nextAudioConfigCached

        override fun isRecommendedRollthrough(): Boolean = isRollthrough

        override fun setRollthroughNextAudio(isRoll: Boolean) {
            this.isRollthrough = isRoll
        }

        /**
         * This function will load the next audio recommendation
         * - 1. fetch recommendations from for you and populate queue
         * - 2. Iterate through queue and fetch article until article has audio object.
         * - 3. Cache the audio config for next audio.
         */
        override suspend fun loadNextAudioRecommendation(excludeList: List<String>) {
            withContext(dispatcherProvider.io) {
                // Get for you feed
                when (
                    val result =
                        forYouFeedRepository.getForYouRecommendations(
                            skipReadingFromCache = true,
                            excludeList = excludeList.mapNotNull { trimToPath(it) },
                            surface = ForYouFeedRepositoryImpl.SURFACE_NEXTAUDIO
                        )
                ) {
                    is APIResult.Success -> {
                        // If returned result is cached data and nextAudioQueue is not empty, use existing queue
                        if (!result.isCached || nextAudioQueue.isEmpty()) {
                            // Update queue if result is a successful network response

                            result.data
                                ?.recommendations
                                ?.filter { it.additionalProperties?.audioArticle?.enabled == true }
                                ?.let { recommendations ->
                                    nextAudioQueue.clear()
                                    nextAudioQueue.addAll(recommendations)
                                }
                        }
                    }

                    else -> {
                        // no-op
                    }
                }

                // Flag to check if next audio has been cached
                var hasNextAudio = false

                // Reset cached audio config
                nextAudioConfigCached = null

                // Loop through queue until we find valid audio or queue is empty
                while (nextAudioQueue.isNotEmpty() && !hasNextAudio) {
                    // Get the next recommendation in the queue
                    val nextAudioItem = nextAudioQueue.poll()

                    // If next recommendation is in exclude list continue
                    if (excludeList.contains(nextAudioItem?.url ?: "")) {
                        continue
                    }

                    // Get next item in audio queue
                    val article =
                        nextAudioItem?.let {
                            it.getURL().let { url ->
                                when (val status = article2Repository.fetchArticleStatus(Query(url))) {
                                    is Status.Cache -> status.data
                                    is Status.Network -> status.data
                                    else -> null
                                }
                            }
                        }

                    // Cache the next Audio Config. If config is generated, [hasArticleAudio] is set to true
                    article?.audio?.let { audio ->
                        nextAudioConfigCached =
                            AudioMediaConfig(
                                mediaId = audio.mediaId,
                                manifestUrl = audio.manifestUrl,
                                humanAdsUrl = if (audio.subtype == AudioArticleVoiceType.HUMAN.name.lowercase()) audio.adsUrl else null,
                                humanRawUrl = if (audio.subtype == AudioArticleVoiceType.HUMAN.name.lowercase()) audio.rawUrl else null,
                                adsUrl = if (audio.subtype != AudioArticleVoiceType.HUMAN.name.lowercase()) audio.adsUrl else null,
                                rawUrl = if (audio.subtype != AudioArticleVoiceType.HUMAN.name.lowercase()) audio.rawUrl else null,
                                titlePrefix = audio.title?.prefix,
                                titleSeparator = audio.title?.separator,
                                title = audio.title?.content ?: article.title,
                                primaryLabel = audio.label?.displayLabel,
                                secondaryLabel = audio.label?.displayTransparency,
                                date = (article.items?.firstOrNull { item -> item is Date } as? Date)?.content,
                                imageUrl = audio.image?.imageURL,
                                imageCaption = audio.image?.fullCaption,
                                contentUrl = getUrlWithoutParameters(article.contenturl),
                                sectionName = article.section,
                                caption = audio.caption,
                                voices = null,
                                arcId = article.arcId,
                                audioTracking =
                                    AudioTrackerImpl(
                                        trackingInfo = article.omniture?.toTrackingInfo(),
                                        isRecommended = true,
                                    ),
                                adsCustomTargeting = getArticlesAdTargetingValues(article, null, null, article.contenturl),
                                labelStyle = audio.label?.style,
                                adConfig = audio.adConfig?.toAudioAdConfig(),
                            )
                        hasNextAudio = true
                    }
                }
            }
        }

        /**
         * We save the heard audio to save history so it can be used in for you request.
         */
        override suspend fun addAudioToHistory(audioMediaConfig: AudioMediaConfig) =
            withContext(dispatcherProvider.io) {
                val url = audioMediaConfig.contentUrl ?: return@withContext

                val readingHistoryModel =
                    ReadingHistoryModel(
                        url,
                        getCanonicalUrl(url),
                        System.currentTimeMillis(),
                        null,
                        isListened = true,
                    )
                val meta =
                    MetadataModel(
                        contentURL = url,
                        syncLmt = System.currentTimeMillis()
                    ).apply {
                        imageURL = audioMediaConfig.imageUrl
                        headline = audioMediaConfig.title
                    }
                readingHistoryRepository.addArticle(
                    readingHistoryModel,
                    meta,
                )
            }

        private fun trimToPath(url: String): String? {
            val uri = Uri.parse(url)
            return uri.path
        }

    override suspend fun getAudioRecommendationList(excludeList: List<String>?): List<AudioMediaConfig> {
        return withContext(dispatcherProvider.io) {
            val historyExclusions = readingHistoryRepository.getArticles(limit = Int.MAX_VALUE)
                .mapNotNull { it.contentUrl }
                .mapNotNull { trimToPath(it) }
            val currentListExclusions = (excludeList ?: emptyList()).mapNotNull { trimToPath(it) }
            val exclusions = historyExclusions + currentListExclusions

            // how many new items are still needed to fill out the list the user sees
            val needed = (MIN_FOR_YOU_ITEMS - currentListExclusions.size).coerceAtLeast(1)

            val fromCache = fetchAudioRecommendations(exclusions, skipReadingFromCache = false)
            if (fromCache.size >= needed) {
                return@withContext fromCache
            }

            // the cached feed is shared with the phone and is not filtered by the exclusion list,
            // so after filtering it can come back short. top it up with a network request, which
            // applies the exclusions server side.
            val fromNetwork = fetchAudioRecommendations(exclusions, skipReadingFromCache = true)
            mergeDistinct(fromCache, fromNetwork)
        }
    }

    private fun mergeDistinct(vararg lists: List<AudioMediaConfig>): List<AudioMediaConfig> {
        val seen = mutableSetOf<String>()
        return lists.asSequence().flatten().filter { item ->
            val key = item.contentUrl ?: item.id ?: return@filter true
            seen.add(key)
        }.toList()
    }

    private suspend fun fetchAudioRecommendations(
        exclusions: List<String>,
        skipReadingFromCache: Boolean,
    ): List<AudioMediaConfig> {
        val excluded = exclusions.toSet()
        return when (
            val result =
                forYouFeedRepository.getForYouRecommendations(
                    skipReadingFromCache = skipReadingFromCache,
                    excludeList = exclusions,
                    surface = ForYouFeedRepositoryImpl.SURFACE_FEED
                )
        ) {
                is APIResult.Success -> {
                    val recommendations = result.data
                        ?.recommendations
                        ?.filter { it.additionalProperties?.audioArticle?.enabled == true }
                        .orEmpty()
                    // when taking audio from cache, make sure the added items are not duplicated with items the user currently sees
                    val newRecommendations = recommendations.filter { rec ->
                        val path = rec.url?.let { trimToPath(it) }
                        rec.pinned == true || path == null || path !in excluded
                    }
                    newRecommendations
                        .mapNotNull { rec ->
                            val article = rec.getURL()?.let { url ->
                                when (val status = article2Repository.fetchArticleStatus(Query(url))) {
                                    is Status.Cache -> status.data
                                    is Status.Network -> status.data
                                    else -> null
                                }
                            }
                            article?.audio?.let { audio ->
                                getAudioMediaConfig(audio, article, rec)
                            }
                        }
                }
                else -> {
                    EventLog.Builder().apply {
                        setMessage("Failed to load audio recommendation list")
                        setModule(LogModules.AUTO)
                        setErrorMessage(result.getMessage())
                    }.run {
                            RemoteLog.e(AppContextUtils.appContext, build())
                    }
                    emptyList()
                }
        }
    }

    private fun getAudioMediaConfig(audio: Audio, article: Article2, rec: RecommendationsItem): AudioMediaConfig{
        var title = audio.title?.content ?: article.title
        val tracker = AudioTrackerImpl(
            trackingInfo = article.omniture?.toTrackingInfo(),
            isRecommended = true,
            tabName = "For You",
            appSection = "carplay_for_you",
            isAuto = true,
            duration = AudioUtil.calculateTotalDuration(audio) ?: 0L
        )
        // if luf, assemble audio media config with children and transitions
        if (rec.pinned == true) {
            // todo remove once car app template can be pushed to production (add pin icon)
            title = LUF_PREFIX + title

            val containerConfig = audio.toAudioMediaConfig(
                article = article,
                children = null,
                transitions = null,
                audioTracker = tracker
            )

            val childConfigs = audio.children.orEmpty().filter { !it.url.isNullOrBlank() }.map { childAudio ->
                childAudio.toAudioMediaConfig(
                    article = article,
                    children = null,
                    transitions = null
                )
            }

            val transitionConfigs = audio.transitions.orEmpty().filter { !it.url.isNullOrBlank() }.map { transitionAudio ->
                transitionAudio.toAudioMediaConfig(
                    article = article,
                    children = null,
                    transitions = null
                )
            }

            return containerConfig.copy(
                imageUrl = getImageResizerUrl(ImageServiceConfig(512, 512), audio.image?.imageURL),
                children = childConfigs.toMutableList(),
                transitions = transitionConfigs.toMutableList(),
                pinned = true,
                title = title,
            )
        }
        else{
            return AudioMediaConfig(
                mediaId = audio.mediaId,
                manifestUrl = audio.manifestUrl,
                humanAdsUrl = if (audio.subtype == AudioArticleVoiceType.HUMAN.name.lowercase()) audio.adsUrl else null,
                humanRawUrl = if (audio.subtype == AudioArticleVoiceType.HUMAN.name.lowercase()) audio.rawUrl else null,
                adsUrl = if (audio.subtype != AudioArticleVoiceType.HUMAN.name.lowercase()) audio.adsUrl else null,
                rawUrl = if (audio.subtype != AudioArticleVoiceType.HUMAN.name.lowercase()) audio.rawUrl else null,
                titlePrefix = audio.title?.prefix,
                titleSeparator = audio.title?.separator,
                title = title,
                primaryLabel = audio.label?.displayLabel,
                secondaryLabel = audio.label?.displayTransparency,
                date = (article.items?.firstOrNull { item -> item is Date } as? Date)?.content,
                imageUrl = getImageResizerUrl(ImageServiceConfig(512, 512), audio.image?.imageURL),
                imageCaption = audio.image?.fullCaption,
                contentUrl = getUrlWithoutParameters(article.contenturl),
                sectionName = article.section,
                caption = audio.caption,
                voices = null,
                arcId = article.arcId,
                audioTracking = tracker,
                adsCustomTargeting = getArticlesAdTargetingValues(article, null, null, article.contenturl),
                labelStyle = audio.label?.style,
                duration = audio.duration,
                pinned = rec.pinned,
                adConfig = audio.adConfig?.toAudioAdConfig(),
            )
        }
    }

    private fun getImageResizerUrl(
        imageServiceConfig: ImageServiceConfig,
        url: String?,
    ): String? {
        try {
            if (url == null) return null
            val finalUrl: String =
                ConfigManager.getInstance().config.createImageRequestUrlForAuto(
                    url,
                    imageServiceConfig,
                )
            Logger.d("ImageService", "Original URL: $url, Final URL: $finalUrl")
            return finalUrl
        } catch (e: MalformedURLException) {
            Logger.e("ImageService", "Error imageURL $url", e)
        }
        return null
    }
}

const val LUF_PREFIX = "Live Updates: "

private const val MIN_FOR_YOU_ITEMS = 4