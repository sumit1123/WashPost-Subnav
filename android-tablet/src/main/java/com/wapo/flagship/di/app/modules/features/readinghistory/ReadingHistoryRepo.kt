package com.wapo.flagship.di.app.modules.features.readinghistory

import com.wapo.android.commons.constants.AUTHORIZATION
import com.wapo.android.commons.constants.CLIENT_ID
import com.wapo.android.commons.constants.REQUEST_ID
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.ContentType
import com.wapo.android.commons.util.Logger
import com.washingtonpost.userhistory.network.APIResult
import com.washingtonpost.userhistory.repo.UserHistoryMetaProvider
import java.util.UUID
import com.washingtonpost.android.save.network.MetadataRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.wapo.android.commons.util.convertIsoToMillis
import com.wapo.android.commons.util.getCanonicalUrl
import com.wapo.android.remotelog.logger.RemoteLog
import com.washingtonpost.android.save.database.ReadingHistoryDBHelper
import com.washingtonpost.android.save.database.model.MetadataModel
import com.washingtonpost.android.save.database.model.ReadingHistoryAndMetadata
import com.washingtonpost.android.save.database.model.ReadingHistoryModel
import com.washingtonpost.android.save.network.SavedRetrofit
import java.util.concurrent.TimeUnit

private const val MAX_AGE_DAYS = 60

class ReadingHistoryRepo(
    private val readingHistoryService: ReadingHistoryService,
    val userHistoryMetaProvider: UserHistoryMetaProvider,
    private val readingHistoryDBHelper: ReadingHistoryDBHelper,
    private val metadataNetwork: SavedRetrofit.MetadataNetwork,
    ) {
    suspend fun getReadingHistory(): List<Data> {
        val meta = userHistoryMetaProvider.getUserHistoryMeta()

        if (meta.loginId == null) {
            Logger.d(TAG, "User is not signed in. Skipping reading history request.")
            return emptyList()
        }

        if (!meta.privacyConsentGiven) {
            Logger.d(TAG, "Privacy consent was not given. Skipping reading history request.")
            return emptyList()
        }

        return try {
            val apiResult = readingHistoryService.getReadingHistory(getHeaders(), "article,podcast")
            when (apiResult) {
                is APIResult.Success -> {
                    val response = apiResult.data
                    if (response?.status != "SUCCESS") {
                        Logger.d(
                            TAG,
                            "Reading history API failure: state=${response?.state} status=${response?.status}"
                        )
                        val eventLog = EventLog.Builder()
                            .setMessage("Reading history API failure")
                            .set("state", response?.state)
                            .set("status", response?.status)
                            .setModule(LogModules.READING_HISTORY)
                            .build()
                        RemoteLog.e(AppContextUtils.appContext, eventLog)
                        emptyList()
                    } else {
                        response.readingHistory?.data.orEmpty()
                    }
                }

                is APIResult.Failure -> {
                    Logger.e(TAG, "Reading history API failure: code=${apiResult.statusCode}")
                    val eventLog = EventLog.Builder()
                        .setMessage("Reading history API failure: ${apiResult.getMessage()}")
                        .set("status", apiResult.statusCode)
                        .setModule(LogModules.READING_HISTORY)
                        .build()
                    RemoteLog.e(AppContextUtils.appContext, eventLog)
                    emptyList()
                }

                is APIResult.NetworkError -> {
                    Logger.e(TAG, "Reading history network error", apiResult.error)
                    emptyList()
                }
            }
        } catch (t: Throwable) {
            Logger.e(TAG, "Exception occurred", t)
            val eventLog = EventLog.Builder()
                .setErrorMessage(t.message)
                .set("cause", t.cause)
                .setModule(LogModules.READING_HISTORY)
                .build()
            RemoteLog.e(AppContextUtils.appContext, eventLog)
            emptyList()
        }
    }

    private fun getHeaders(): HashMap<String, String> {
        return hashMapOf(
            Pair(CLIENT_ID, userHistoryMetaProvider.getUserHistoryMeta().clientId.toString()),
            Pair(REQUEST_ID, UUID.randomUUID().toString()),
            Pair(
                AUTHORIZATION,
                "Bearer " +
                        userHistoryMetaProvider.getAccessToken().accessToken
            ),
        )
    }

    suspend fun mergeReadingHistory(
    ): List<ReadingHistoryAndMetadata> {
        val now = System.currentTimeMillis()
        val maxAgeMs = TimeUnit.DAYS.toMillis(MAX_AGE_DAYS.toLong())
        val articleMap = LinkedHashMap<String, ReadingHistoryAndMetadata>()

        val local = getArticles(limit = 100)
        val remoteArticles = getReadingHistory()
        val remote = buildReadingHistoryAndMetadata(remoteArticles)

        for (item in local) {
            val url = item.contentUrl
            val lmt = item.lmt
            val key = item.canonicalURL ?: getCanonicalUrl(url)

            if (key == null) {
                Logger.d(TAG, "Missing canonicalURL for url=${item.contentUrl}")
                val eventLog = EventLog.Builder()
                    .setMessage("Missing canonicalURL for url=${item.contentUrl}")
                    .setModule(LogModules.READING_HISTORY)
                    .build()
                RemoteLog.d(AppContextUtils.appContext, eventLog)
                continue
            }
            // only add local articles less than 60 days old
            if (lmt != null && now - lmt < maxAgeMs){
                articleMap[key] = item
            }
        }

        // merge remote articles into the map unless they are older than local
        for (r in remote) {
            val key = if (r.contentType == ContentType.ARTICLE) {
                r.canonicalURL ?: getCanonicalUrl(r.contentUrl)
            } else {
                r.mediaId
            }

            val remoteTime = r.lmt ?: continue

            if (key == null) {
                Logger.d(TAG, "Missing canonicalURL for url=${r.contentUrl}")
                val eventLog = EventLog.Builder()
                    .setMessage("Missing canonicalURL for url=${r.contentUrl}")
                    .setModule(LogModules.READING_HISTORY)
                    .build()
                RemoteLog.e(AppContextUtils.appContext, eventLog)
                continue
            } else if (r.displayDate == null) {
                Logger.d(TAG, "Missing display date for url=${r.contentUrl}")
                val eventLog = EventLog.Builder()
                    .setMessage("Missing display date for url=${r.contentUrl}")
                    .setModule(LogModules.READING_HISTORY)
                    .build()
                RemoteLog.e(AppContextUtils.appContext, eventLog)
                continue
            }
            val inLocal = articleMap[key]
            if (inLocal == null) {
                articleMap[key] = r
            } else {
                val localTime = inLocal.lmt ?: Long.MIN_VALUE
                if (remoteTime >= localTime) {
                    articleMap[key] = r
                } else {
                    // always ensure to take remote listenDepthSec and percentConsumed as source of truth
                    inLocal.listenDepthSec = r.listenDepthSec
                    inLocal.percentConsumed = r.percentConsumed
                    inLocal.deepestScrollId = r.deepestScrollId
                    inLocal.contentType = r.contentType
                }
            }
        }

        // sort articles from newest to oldest by display date or last modified time
        val merged = articleMap.values
            .sortedByDescending {
                convertIsoToMillis(it.displayDate.toString()) ?: it.lmt ?: Long.MIN_VALUE
            }
            .take(100)

        // save new merged reading history back to local reading history db but write only articles to DB
        val articlesOnly = merged.filter { it.contentType == ContentType.ARTICLE }
        val historyRows = articlesOnly.map { historyToReadingHistoryModel(it) }
        val metadataRows = articlesOnly.map { historyToMetadataModel(it) }

        // write the articles to the database only
        replaceAll(historyRows, metadataRows)

        return merged
    }

    private fun convertHistoryDataToArticleOrPodcastType(
        historyData: Data,
        metadata: MetadataModel?
    ): ReadingHistoryAndMetadata {

        return ReadingHistoryAndMetadata().apply {
            contentType = if (historyData.contentType == ContentType.ARTICLE.type) ContentType.ARTICLE else ContentType.PODCAST

            contentId = historyData.contentId
            percentConsumed = historyData.percentConsumed
            displayDate = historyData.displayDate
            readingTimeMilli = historyData.readingTimeMilli
            listenDepthSec = historyData.listenDepthSec
            deepestScrollId = historyData.deepestScrollId
            hidden = historyData.hidden
            canonicalURL = historyData.canonicalUrl
            contentUrl = historyData.linkUrl
            lmt = convertIsoToMillis(historyData.displayDate.toString())

            //additional fields for Podcast Type
            mediaId = historyData.mediaId
            headline = historyData.title
            label = historyData.label
            streamUrl = historyData.streamUrl
            imageURL = historyData.imageUrl

            if (historyData.contentType == ContentType.ARTICLE.type) {
                headline = metadata?.headline
                byline = metadata?.byline
                blurb = metadata?.blurb
                imageURL = metadata?.imageURL
                publishedTime = metadata?.publishedTime
                lastUpdated = metadata?.lastUpdated
                secondaryText = metadata?.secondaryText
                displayLabel = metadata?.displayLabel
                displayTransparency = metadata?.displayTransparency
                trackingString = metadata?.trackingString
                headlinePrefix = metadata?.headlinePrefix
                isListened = false
            }
        }
    }

    private fun historyToReadingHistoryModel(item: ReadingHistoryAndMetadata): ReadingHistoryModel {
        return ReadingHistoryModel(
            contentId = item.contentId,
            contentUrl = item.contentUrl.toString(),
            lmt = item.lmt ?: System.currentTimeMillis(),
        ).apply {
            canonicalURL = item.canonicalURL
                ?: item.contentUrl?.let { getCanonicalUrl(it) } ?: ""
            isListened = item.isListened
        }
    }

    private fun historyToMetadataModel(item: ReadingHistoryAndMetadata): MetadataModel {
        return MetadataModel(
            contentURL = item.contentUrl ?: "",
            syncLmt = item.lmt ?: System.currentTimeMillis()
        ).apply {
            headline = item.headline
            byline = item.byline
            blurb = item.blurb
            imageURL = item.imageURL
            publishedTime = item.publishedTime
            lastUpdated = item.lastUpdated
            secondaryText = item.secondaryText
            displayLabel = item.displayLabel
            displayTransparency = item.displayTransparency
            trackingString = item.trackingString
            headlinePrefix = item.headlinePrefix
        }
    }

    // fetch metadata for list of urls
    private suspend fun fetchMetadataForUrls(urls: List<String>): Map<String, MetadataModel> {
        if (urls.isEmpty()) {
            return emptyMap()
        }

        val response = withContext(Dispatchers.IO) {
            getMetadataNetwork()
                .getArticleMetadata(MetadataRequest(urls.distinct()))
                .execute()
        }

        if (!response.isSuccessful) {
            return emptyMap()
        }

        val body = response.body() ?: return emptyMap()
        val syncLmt = System.currentTimeMillis()

        val metadataList = body.metadata.orEmpty()
            .filter { entry ->
                entry.error == null && entry.url.isNotBlank()
            }
            .map { entry ->
                MetadataModel(entry.url, syncLmt).apply {
                    headline = entry.headline
                    blurb = entry.description
                    byline = entry.byLine
                    imageURL = entry.socialImageUrl
                    lastUpdated = entry.lastUpdated.toLongOrNull()
                    publishedTime = entry.displayDate.toLongOrNull()
                }
            }

        return metadataList.associateBy { it.contentURL }
    }

    // builds list of reading history items with their metadata
    private suspend fun buildReadingHistoryAndMetadata(data: List<Data>): List<ReadingHistoryAndMetadata> {
//    private suspend fun buildReadingHistoryAndMetadata(data: List<Article>): List<ReadingHistoryAndMetadata> {
        val urls = data.mapNotNull { it.linkUrl }
        val urlMetadata = fetchMetadataForUrls(urls)

        return data.map { item ->
            val url = item.linkUrl
            val metadata = if (url != null) urlMetadata[url] else null
            convertHistoryDataToArticleOrPodcastType(item, metadata)
        }
    }

    suspend fun addArticle(readingHistory: ReadingHistoryModel, metadataModel: MetadataModel) =
        withContext(Dispatchers.IO) {
            try {
                Logger.d(TAG, "Adding Reading History article url=${readingHistory.contentUrl}")
                readingHistoryDBHelper.beginTransaction()
                val result = readingHistoryDBHelper.getArticleByUrl(readingHistory.contentUrl)
                if (result == null) {
                    readingHistoryDBHelper.addAllArticles(listOf(readingHistory))
                    readingHistoryDBHelper.addAllMetadata(listOf(metadataModel))
                    readingHistoryDBHelper.setTransactionSuccessful()
                }
            } catch (e: java.lang.Exception) {
                Logger.d(TAG, "Error saving article", e)
            } finally {
                readingHistoryDBHelper.endTransaction()
            }
        }

    suspend fun getArticles(limit: Int): List<ReadingHistoryAndMetadata> {
        return readingHistoryDBHelper.getArticlesToList(limit = limit)
    }

    fun getMetadataNetwork(): SavedRetrofit.MetadataNetwork {
        return metadataNetwork
    }

    suspend fun replaceAll(
        readingHistoryList: List<ReadingHistoryModel>,
        metadataList: List<MetadataModel>
    ) {
        readingHistoryDBHelper.replaceAll(readingHistoryList, metadataList)
    }

    suspend fun getCount(): Int {
        return readingHistoryDBHelper.getCount();
    }

    companion object {
        private val TAG = ReadingHistoryRepo::class.java.simpleName
    }
}