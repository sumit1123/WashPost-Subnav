/* Copyright (c) 2019 The Washington Post. All rights reserved. */

package com.washingtonpost.android.save

import com.wapo.android.commons.util.Logger
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.paging.PagedList
import androidx.paging.toLiveData
import androidx.sqlite.db.SupportSQLiteDatabase
import com.washingtonpost.android.save.database.SavedArticleDBHelper
import com.washingtonpost.android.save.database.model.*
import com.washingtonpost.android.save.misc.ArticleListQueueType
import com.washingtonpost.android.save.network.*
import com.washingtonpost.android.save.views.ArticleListViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import java.lang.IllegalStateException
import java.text.SimpleDateFormat
import java.lang.Exception
import java.util.*

class SavedArticleManager private constructor(params: Params) {
    data class Params(
        val saveProvider: SaveProvider,
        val coroutineScope: CoroutineScope = GlobalScope,
        val savedArticleDBHelper: SavedArticleDBHelper = SavedArticleDBHelper(saveProvider)
    )

    private val saveProvider: SaveProvider = params.saveProvider
    private val coroutineScope: CoroutineScope = params.coroutineScope
    private val savedArticleDBHelper: SavedArticleDBHelper = params.savedArticleDBHelper
    private val dateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val preferenceNetwork: SavedRetrofit.PreferenceNetwork
    get() = SavedRetrofit.getInstance().getPreferenceNetwork(saveProvider.getPreferenceBaseURL())
    private val metadataNetwork: SavedRetrofit.MetadataNetwork = SavedRetrofit.getInstance().getMetadataNetwork(saveProvider.getMetadataBaseUrl())
    private val metadataManager: MetadataManager = MetadataManager.getInstance(this)
    private val readingListMutex: Mutex = Mutex()
    private var metadataSyncJob: Job? = null

    companion object : SingletonHolder<SavedArticleManager, Params>(::SavedArticleManager) {
        private val TAG: String = SavedArticleManager::class.java.simpleName
        const val ADAPTER_PAGED_LIST_SIZE = 10
        const val DOWNLOAD_ARTICLE_LIMIT = 25
    }

    fun synchronize(callback: ((type: CallbackType) -> Unit) = {}) {
        coroutineScope.launch {
            if (saveProvider.isConnected() && readingListMutex.tryLock()) {
                try {
                    if (saveProvider.isLoggedInUser()) {
                        syncArticles()
                    }
                } finally {
                    readingListMutex.unlock()
                    callback(CallbackType.ON_SAVED_SYNC_COMPLETE)
                }
                handleMetadata()
                if (metadataSyncJob?.isCancelled == false) {
                    callback(CallbackType.ON_METADATA_SYNC_COMPLETE)
                    saveProvider.updateArticlesIfNeeded(savedArticleDBHelper.getArticlesToList(limit = DOWNLOAD_ARTICLE_LIMIT))
                }
            } else {
                callback(CallbackType.ON_SAVED_SYNC_IN_PROGRESS)
            }
            callback(CallbackType.ON_SYNC_METHOD_COMPLETE)
        }
    }

    private suspend fun handleMetadata(callback: ((type: CallbackType) -> Unit) = {}) {
        if (metadataSyncJob?.isActive == true) {
            Logger.d(TAG, "Cancelling existing metadata sync")
            metadataSyncJob?.cancelAndJoin()
        }
        Logger.d(TAG, "Starting metadata sync")
        metadataSyncJob = syncMetadataAsync(callback)
        metadataSyncJob?.join()
    }

    private suspend fun syncArticles() {
        val articleListQueue = savedArticleDBHelper.getArticleListQueue()
        try {
            if (articleListQueue.isNotEmpty()) {
                val saveQueue = articleListQueue.filter { it.articleListQueueType == ArticleListQueueType.ADD_ARTICLE }
                if (saveQueue.isNotEmpty()) {
                    makeArticleSyncRequest(saveQueue, ArticleListQueueType.ADD_ARTICLE)
                }

                val deleteQueue = articleListQueue.filter { it.articleListQueueType == ArticleListQueueType.DELETE_ARTICLE }
                if (deleteQueue.isNotEmpty()) {
                    makeArticleSyncRequest(deleteQueue, ArticleListQueueType.DELETE_ARTICLE)
                }
            }

            // Fetch and sync complete list of saved stories after pending transactions have completed
            makeArticleSyncRequest(listOf(), null)
        } catch (e: Exception) {
            saveProvider.logPreferenceSyncException(e)
        }
    }
    fun getArticleByUrl(url: String): ArticleAndMetadata? {
        return savedArticleDBHelper.getArticleByUrl(url)
    }

    private suspend fun makeArticleSyncRequest(articleListQueue: List<ArticleListQueue>, articleListQueueType: ArticleListQueueType?) {
        val response = when (articleListQueueType) {
            ArticleListQueueType.ADD_ARTICLE -> {
                preferenceNetwork.saveArticles(saveProvider.getPreferencesRequestHeaders(false), constructSyncRequestBody(articleListQueue)).execute()
            }
            ArticleListQueueType.DELETE_ARTICLE -> {
                preferenceNetwork.deleteArticles(saveProvider.getPreferencesRequestHeaders(false), constructSyncRequestBody(articleListQueue)).execute()
            }
            else -> {
                preferenceNetwork.getSavedArticlesList(saveProvider.getPreferencesRequestHeaders(false), constructSyncRequestBody(articleListQueue)).execute()
            }
        }

        val responseMs = response.raw().receivedResponseAtMillis - response.raw().sentRequestAtMillis
        saveProvider.logExtras("save sync response time: $responseMs")
        Logger.d(TAG, "Saved response: " + response.body()?.toString())
        if (response.code() == 200) {
            processSavedArticlesResponseAsync(response.body(), articleListQueue).await()
        }  else if (response.code() == 202) {
            saveProvider.logPreferenceErrorResponse(response.code(), "Error processing request: " + (response.errorBody()?.string() ?: "error body empty"))
        } else {
            Logger.d(TAG, "Invalid response code=" + response.code())
            saveProvider.logPreferenceErrorResponse(response.code(), response.errorBody()?.string() ?: "error body empty")
        }
    }

    private fun constructSyncRequestBody(articleListQueue: List<ArticleListQueue>): SavedStoriesRequest {
        val transactionList = mutableListOf<UrisRequestValue>()
        articleListQueue.forEach {
            transactionList.add(UrisRequestValue(it.contentURL, it.lmt))
        }
        saveProvider.logExtras("Queueing ${transactionList.size} articles for sync")
        return SavedStoriesRequest(transactionList)
    }

    fun tryWithLock(callback: () -> Unit) {
        if (readingListMutex.tryLock()) {
            try {
                callback()
            } finally {
                readingListMutex.unlock()
            }
        } else {
            Logger.d(TAG, "Failed to obtain lock")
        }
    }

    fun cleanup() {
        coroutineScope.launch {
            metadataManager.cleanMetadata()
        }
    }

    fun onLogout() {
        synchronize {
            coroutineScope.launch {
                if (it == CallbackType.ON_METADATA_SYNC_STARTED
                        || it == CallbackType.ON_SYNC_METHOD_COMPLETE) {
                    metadataSyncJob?.cancelAndJoin()
                    savedArticleDBHelper.enforceArticlesLimit(0)
                    savedArticleDBHelper.cleanMetadata()
                }
            }
        }
    }

    fun getViewModel(owner: LifecycleOwner): ArticleListViewModel {
        val factory = ArticleListViewModel.Factory(this)
        return when (owner) {
            is Fragment -> ViewModelProvider(owner, factory)[ArticleListViewModel::class.java]
            is FragmentActivity -> ViewModelProvider(owner, factory)[ArticleListViewModel::class.java]
            else -> throw IllegalStateException("Must pass valid lifecycle owner")
        }
    }

    fun addArticle(savedArticle: SavedArticleModel, metadataModel: MetadataModel) {
        coroutineScope.launch {
            try {
                Logger.d(TAG, "Adding article url=${savedArticle.contentURL}")
                savedArticleDBHelper.beginTransaction()
                val result = savedArticleDBHelper.getArticleByUrl(savedArticle.contentURL)
                if (result == null) {
                    savedArticleDBHelper.addAllArticles(listOf(savedArticle))
                    savedArticleDBHelper.addAllMetadata(listOf(metadataModel))
                    queueIfNeeded(ArticleListQueueType.ADD_ARTICLE, savedArticle.contentURL)
                    savedArticleDBHelper.setTransactionSuccessful()
                } else {
                        val ex = IllegalStateException("Trying to save article that is saved newUrl=${savedArticle.contentURL} existingUrl=${result.contentURL}")
                        Logger.d(TAG, "This should not be allowed by the UI", ex)
                        saveProvider.logException(ex)
                }
            } catch (e: Exception) {
                Logger.d(TAG, "Error saving article", e)
            } finally {
                savedArticleDBHelper.endTransaction()
            }
        }
    }

    fun removeArticles(articles: List<ArticleAndMetadata>) {
        coroutineScope.launch {
            try {
                Logger.d(TAG, "Removing articles urls=[${articles.joinToString { it.contentURL }}]")
                savedArticleDBHelper.beginTransaction()
                savedArticleDBHelper.deleteArticlesByUrl(articles.map {
                    queueIfNeeded(ArticleListQueueType.DELETE_ARTICLE, it.contentURL)
                    return@map it.contentURL
                })
                savedArticleDBHelper.setTransactionSuccessful()
            } catch (e: Exception) {
                saveProvider.logException(e)
                Logger.d(TAG, "Error removing articles", e)
            } finally {
                savedArticleDBHelper.endTransaction()
            }
        }
    }

    fun updateSyncLmt(contentUrl: String, syncLmt: Long) {
        savedArticleDBHelper.updateSyncLmt(contentUrl, syncLmt)
    }

    fun getPagedArticles(): LiveData<PagedList<ArticleAndMetadata>> {
        return savedArticleDBHelper.getPagedArticles()
                .toLiveData(pageSize = ADAPTER_PAGED_LIST_SIZE)
    }

    fun getArticles(limit: Int): List<ArticleAndMetadata> {
        return savedArticleDBHelper.getArticlesToList(limit = limit)
    }

    fun getTotalArticles(): Long {
        return savedArticleDBHelper.getTotalArticles()
    }

    fun getLiveArticleByUrl(url: String): LiveData<ArticleAndMetadata?> {
        return savedArticleDBHelper.getLiveArticleByUrl(url)
    }

    fun addAllMetadata(metadataList: List<MetadataModel>) {
        savedArticleDBHelper.addAllMetadata(metadataList)
    }

    fun cleanMetadata(): Int {
        return savedArticleDBHelper.cleanMetadata()
    }

    fun getTotalLastModifiedMetadata(lmt: Long): Int {
        return savedArticleDBHelper.getTotalLastModifiedMetadata(lmt)
    }

    fun getLastModifiedMetadata(lmt: Long, limit: Int, offset: Int = 0): List<ModifiedMetadata> {
        return savedArticleDBHelper.getLastModifiedMetadata(lmt, limit, offset)
    }

    fun getMetadataNetwork(): SavedRetrofit.MetadataNetwork {
        return metadataNetwork
    }

    fun getWritableDatabase(): SupportSQLiteDatabase {
        return savedArticleDBHelper.getWritableDatabase()
    }

    fun getSaveProvider(): SaveProvider {
        return saveProvider
    }

    enum class CallbackType {
        ON_SYNC_ERROR,
        ON_SAVED_SYNC_IN_PROGRESS,
        ON_SAVED_SYNC_COMPLETE,
        ON_INITIAL_METADATA_SYNC,
        ON_METADATA_SYNC_COMPLETE,
        ON_SYNC_METHOD_COMPLETE,
        ON_METADATA_SYNC_STARTED,
        ON_METADATA_SYNC_ERROR,
    }

    private fun queueIfNeeded(articleListQueueType: ArticleListQueueType, contentUrl: String) {
        Logger.d(TAG, "Queueing article url=$contentUrl queueType=$articleListQueueType")
        saveProvider.logExtras("Queueing article url=$contentUrl queueType=$articleListQueueType")
        savedArticleDBHelper.queueArticle(ArticleListQueue(contentUrl, System.currentTimeMillis(), articleListQueueType))
    }


    private fun processSavedArticlesResponseAsync(savedArticlesResponse: SavedStoriesResponse?,
                                                  articleListQueue: List<ArticleListQueue>) = GlobalScope.async {
        try {
            savedArticleDBHelper.beginTransaction()
            savedArticlesResponse?.saved?.let { saved ->
                val articleList = mutableListOf<SavedArticleModel>()
                saveProvider.logExtras("saving ${saved.size} articles after sync")
                saved.forEach { savedStory ->
                    if (savedStory.location == null) {
                        Logger.e(TAG, "Saved story location is null")
                    } else if (savedStory.userUpdated == null) {
                        Logger.e(TAG, "Saved story user updated time is null")
                    } else {
                        articleList.add(SavedArticleModel(savedStory.location, savedStory.userUpdated))
                    }
                }
                savedArticleDBHelper.syncArticles(articleList)
            }
            savedArticlesResponse?.uris?.let { uris ->
                uris.forEach { uri ->
                    if (uri.status == "FAILED") {
                        Logger.d(TAG, "Failed to process saved story: " + uri.location)
                    }
                }
            }
            val affectedRows = savedArticleDBHelper.removeQueuedArticles(articleListQueue)
            if (affectedRows == articleListQueue.size) {
                savedArticleDBHelper.setTransactionSuccessful()
            } else {
                Logger.d(TAG, "Article queue size changed during sync - a new sync will start")
            }
        } catch (e: Exception) {
            Logger.d(TAG, "An error occurred while processing saved stories", e)
            saveProvider.logPreferenceSyncException(e)
        } finally {
            savedArticleDBHelper.endTransaction()
        }
    }

    private fun syncMetadataAsync(callback: ((type: CallbackType) -> Unit) = {}): Job = GlobalScope.async {
        metadataSyncJob = launch {
            try {
                callback(CallbackType.ON_METADATA_SYNC_STARTED)
                metadataManager.syncMetadata(callback, this)
            } catch (e: Exception) {
                Logger.d(TAG, "Error syncing metadata", e)
            }
        }
    }
}
