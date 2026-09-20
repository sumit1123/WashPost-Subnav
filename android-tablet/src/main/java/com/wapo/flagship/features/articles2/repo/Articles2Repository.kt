package com.wapo.flagship.features.articles2.repo

import com.wapo.android.commons.util.Logger
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.base.BaseRepo
import com.wapo.flagship.features.articles2.utils.ArticleDecryptionHelper
import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.articles2.models.deserialized.Audio
import com.wapo.flagship.features.articles2.services.Articles2Service
import com.wapo.flagship.features.articles2.utils.getUrlWithoutParameters
import com.wapo.flagship.model.Status
import com.wapo.flagship.network.retrofit.network.APIResult
import com.wapo.flagship.querypolicies.Query
import com.wapo.flagship.roomdb.AppDatabase
import com.wapo.flagship.util.coroutines.CoroutineScopeProvider
import com.washingtonpost.android.config.domain.manager.ConfigManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URISyntaxException
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

const val TAG = "[d][articlesRepo]"

/**
 * A repository responsible for fetching article content given the article URL.
 */
open class Articles2Repository
    @Inject
    constructor(
        private val articles2Service: Articles2Service,
        private val appDatabase: AppDatabase,
        private val coroutineScopeProvider: CoroutineScopeProvider,
    ) : BaseRepo<Article2> {
        override fun fetchData(
            query: Query<Article2>,
            viewModelScope: CoroutineScope?,
            coroutineContext: CoroutineContext?,
        ): LiveData<Status<out Article2>> {
            val url = getUrl(query)
            val liveData = MutableLiveData<Status<out Article2>>()
            val scope = viewModelScope ?: coroutineScopeProvider.sync
            val context = coroutineContext ?: Dispatchers.IO
            scope.launch(context) {
                val status = fetchArticleStatus(query)
                liveData.postValue(status)
            }
            return liveData
        }

        suspend fun fetchArticleStatus(query: Query<Article2>): Status<out Article2> {
            val url = getUrl(query)
            val articleCache = appDatabase.articlesDao().getArticle(url)
            return if (!query.queryPolicy.needUpdate(articleCache)) {
                Logger.d(TAG, "Article Status No Need Update")
                if (articleCache != null) {
                    // cache is good but we still want to update the TTL
                    appDatabase.articlesDao().updateTtl(url)
                    Status.Cache(articleCache)
                } else {
                    Logger.d(TAG, "Article Status Error")
                    Status.Error("cache is missing")
                }
            } else {
                Logger.d(TAG, "Article Status Need Update")
                val data =
                    articles2Service.getArticleContent(
                        timeoutMs = query.queryPolicy.timeOut(),
                        cache = articleCache,
                        url = url,
                    )
                val status = query.queryPolicy.onResponse(data)
                if (data is APIResult.Success && data.data != null) {
                    logMissingAudioIfNeeded(url, data.data)
                    appDatabase.articlesDao().insertArticle(data.data)
                } else if (status is Status.Cache) {
                    // cache is good but we still want to update the TTL
                    appDatabase.articlesDao().updateTtl(url)
                } else if (status is Status.Error && (data is APIResult.Failure)) {
                    EventLog
                        .Builder()
                        .apply {
                            setMessage("Article Load Error")
                            setModule(LogModules.ARTICLES)
                            setContentUrl(url)
                            if (data is APIResult.Failure) {
                                setErrorCode(data.statusCode)
                                setErrorMessage(data.rawResponse)
                            } else if (data is APIResult.NetworkError) {
                                setErrorMessage(data.error.message)
                            }
                        }.run {
                            if (AppContextUtils.isConnectingOrConnected()) {
                                RemoteLog.e(FlagshipApplication.getInstance(), build())
                            }
                        }
                }

                status
            }
        }

        fun cleanUp() {
            appDatabase.articlesDao().cleanUp(System.currentTimeMillis())
        }

        fun prefetchArticle(queries: List<Query<Article2>>) {
            coroutineScopeProvider.sync.launch {
                for (query in queries) {
                    try {
                        val url = getUrl(query)
                        val articleCache = appDatabase.articlesDao().getArticle(url)
                        if (!query.queryPolicy.needUpdate(articleCache)) {
                            // cache is good but we still want to update the TTL
                            appDatabase.articlesDao().updateTtl(url)
                        } else {
                            val data =
                                articles2Service.getArticleContent(
                                    timeoutMs = query.queryPolicy.timeOut(),
                                    cache = articleCache,
                                    url = url,
                                )
                            if (data is APIResult.Success && data.data != null) {
                                logMissingAudioIfNeeded(url, data.data)
                            }
                            when (val response = query.queryPolicy.onResponse(data)) {
                                is Status.Network ->
                                    appDatabase
                                        .articlesDao()
                                        .insertArticle(response.data)
                                is Status.Cache ->
                                    appDatabase
                                        .articlesDao()
                                        .insertArticle(response.data)
                                is Status.Error -> {
                                    appDatabase.articlesDao().updateTtl(url)
                                    EventLog
                                        .Builder()
                                        .apply {
                                            setMessage("Article Parse Error")
                                            setModule(LogModules.ARTICLES)
                                            setContentUrl(url)
                                            setErrorMessage(response.message)
                                        }.run {
                                            RemoteLog.e(FlagshipApplication.getInstance(), build())
                                        }
                                }
                                is Status.Error415 -> {
                                /*
                                    no action is required for 415
                                 */
                                }
                            }
                        }
                    } catch (t: Throwable) {
                        Logger.d("Articles2Repository", "unable to prefetch article $query ${t.message}")
                    }
                }
            }
        }

        private fun getUrl(query: Query<Article2>): String =
            try {
                getUrlWithoutParameters(query.url)
            } catch (ex: URISyntaxException) {
                query.url
            }

        private fun logMissingAudioIfNeeded(
            url: String,
            article: Article2,
        ) {
            val hasTopLevelAudio = article.audio != null
            val hasAudioItem = article.items?.any { it is Audio } == true
            if (hasTopLevelAudio && hasAudioItem) return

            EventLog
                .Builder()
                .apply {
                    setMessage("Article API response missing audio")
                    setModule(LogModules.ARTICLES)
                    setContentUrl(url)
                    set("has_top_level_audio", hasTopLevelAudio)
                    set("has_audio_item", hasAudioItem)
                }.run {
                    RemoteLog.w(FlagshipApplication.getInstance(), build())
                }
        }

    /**
     * Fetches and decrypts an article from the proxy API.
     * Receives an encrypted response containing:
     *    - iv: Initialization Vector (hex string)
     *    - authTag: Authentication tag for GCM mode (hex string)
     *    - encrypted: Encrypted article data (hex string)
     * Decrypts the response using ArticleDecryptionHelper.decrypt() with AES-256-GCM algorithm
     * If decryption succeeds, logs the decrypted JSON to Splunk for debugging/monitoring
     */
    suspend fun fetchDecryptedArticleFromProxy(query: Query<Article2>): String? {
        val url = getUrl(query)
        val startTimeMs = System.currentTimeMillis()
        return try {
            val proxyConfig = ConfigManager.getInstance().config.proxyApiConfig
            val proxyUrl = proxyConfig.baseUrl
            val proxyResponse =
                articles2Service.fetchArticleFromProxy(proxyUrl, url)
            val decrypted = ArticleDecryptionHelper.decrypt(
                proxyResponse.iv,
                proxyResponse.authTag,
                proxyResponse.encrypted
            )
            decrypted?.let { json ->
                val elapsedMs = System.currentTimeMillis() - startTimeMs
                val responseSize = json.length
                remoteLog(url, elapsedMs, responseSize)
            }
            decrypted
        } catch (t: Throwable) {
            Logger.d(TAG, "fetchDecryptedArticleFromProxy failed for $url ${t.message}")
            logException(url, t)
            null
        }
    }

    /**
     * Logs the decrypted article JSON to Splunk for monitoring and debugging purposes.
     */
    private fun remoteLog(url: String, responseTimeMs: Long, responseSize: Int) {
        EventLog
            .Builder()
            .apply {
                setMessage("Proxy decrypted article")
                setModule(LogModules.ARTICLES)
                setContentUrl(url)
                set("proxy_response_time_ms", String.format("%.2f", responseTimeMs.toDouble()))
                set("proxy_response_size_bytes", responseSize.toString())
                setForceUpload()
            }.run {
                if (AppContextUtils.isConnectingOrConnected()) {
                    RemoteLog.d(
                        FlagshipApplication.getInstance(),
                        build(),
                        RemoteLog.ARTICLE_RENDER
                    )
                }
            }
    }

    private fun logException(url: String, t: Throwable) {
        EventLog
            .Builder()
            .apply {
                setMessage("Secure Proxy decrypted article failure")
                setModule(LogModules.ARTICLES)
                setContentUrl(url)
                setErrorMessage(t.message)
            }.run {
                if (AppContextUtils.isConnectingOrConnected()) {
                    RemoteLog.e(
                        FlagshipApplication.getInstance(),
                        build()
                    )
                }
            }
    }
    }
