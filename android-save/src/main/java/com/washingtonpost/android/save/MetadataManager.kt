package com.washingtonpost.android.save

import com.wapo.android.commons.util.Logger
import com.washingtonpost.android.save.database.model.MetadataModel
import com.washingtonpost.android.save.database.model.ModifiedMetadata
import com.washingtonpost.android.save.network.Metadata
import com.washingtonpost.android.save.network.MetadataEntry
import com.washingtonpost.android.save.network.MetadataRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class MetadataManager private constructor(private val savedArticleManager: SavedArticleManager) {
    fun syncMetadata(callback: ((type: SavedArticleManager.CallbackType) -> Unit) = {}, scope: CoroutineScope) {
        val lmt = System.currentTimeMillis() - FIVE_MINUTES_IN_MILLISECONDS
        val total = savedArticleManager.getTotalLastModifiedMetadata(lmt)
        val limit = MAX_ARTICLES_PER_REQUEST
        Logger.d(TAG, "Syncing metadata total=$total lmt=$lmt")
        val numPages = Math.ceil(total.toDouble() / limit).toInt()
        for (i in 0 until numPages) {
            if (scope.isActive) {
                try {
                    val articles = savedArticleManager.getLastModifiedMetadata(lmt, limit)
                    val request = MetadataRequest(articles.distinctBy { it.contentURL }.mapNotNull { it.contentURL })
                    val response = savedArticleManager.getMetadataNetwork().getArticleMetadata(request).execute()
                    val responseMs = response.raw().receivedResponseAtMillis - response.raw().sentRequestAtMillis
                    Logger.d(TAG, "Received response in " + responseMs + "ms")
                    processResponse(response, callback, articles)
                    if (i == 0) {
                        callback(SavedArticleManager.CallbackType.ON_INITIAL_METADATA_SYNC)
                    }
                } catch (e: Exception) {
                    Logger.d(TAG, "An error occurred processing metadata", e)
                    callback(SavedArticleManager.CallbackType.ON_METADATA_SYNC_ERROR)
                    savedArticleManager.getSaveProvider().logMetadataSyncException(e)
                }
            } else {
                break
            }
        }
    }

    private fun processResponse(response: Response<Metadata>, callback: (type: SavedArticleManager.CallbackType) -> Unit = {}, articles: List<ModifiedMetadata>) {
        val entries = mutableListOf<MetadataModel>()
        val body = response.body()
        if (response.isSuccessful && body is Metadata) {
            val syncLmt = System.currentTimeMillis()
            body.metadata?.forEach {
                if (it.error == null) {
                    val matchingArticles = articles.filter { article -> article.contentURL == it.url }
                    matchingArticles.forEach { article ->
                        article.let { type ->
                            entries.add(getModel(it, syncLmt))
                        }
                    }
                } else {
                    Logger.d(TAG, "Failed to retrieve metadata ${it.error} for ${it.url}")
                    savedArticleManager.updateSyncLmt(it.url, syncLmt)
                }
            }
            if (entries.isNotEmpty()) {
                savedArticleManager.addAllMetadata(entries)
            }
        } else {
            Logger.d(TAG, "Metadata sync failed, response code: ${response.code()}")
            callback(SavedArticleManager.CallbackType.ON_METADATA_SYNC_ERROR)
            savedArticleManager.getSaveProvider().logMetadataErrorResponse(response.code(), response.errorBody()?.string() ?: "error body empty")
        }
    }

    private fun getModel(entry: MetadataEntry, syncLmt: Long): MetadataModel {
        val model = MetadataModel(entry.url, syncLmt)
        model.headline = entry.headline
        model.blurb = entry.description
        model.byline = entry.byLine
        model.imageURL = entry.socialImageUrl
        model.lastUpdated = parseDateString(entry.lastUpdated)
        model.publishedTime = parseDateString(entry.displayDate)
        return model
    }

    /**
     * Parses RFC Standard date format to milliseconds in the table to make it easier to use a different date format in the UI.
     *
     * This method allows for two different date formats because of the differences in the way that the content api provides the data.
    // Both are compliant with ANS schema which calls for the particular RFC standard that allows for either format.
     */
    private fun parseDateString(dateString : String?) : Long? {

        if (dateString == null) {
            return null
        }

        val dateFormatSeconds = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val dateFormatMilliseconds = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        try {
            return dateFormatSeconds.parse(dateString)?.time
        } catch (e : Exception) {
        }

        try {
            return dateFormatMilliseconds.parse(dateString)?.time
        } catch (e : Exception) {
        }

        val dateParsingException = Exception("Unable to parse RFC dates from $dateString")
        Logger.e(TAG, "parseDateString failed", dateParsingException)
        savedArticleManager.getSaveProvider().logException(dateParsingException)
        return null
    }

    fun cleanMetadata() {
        val affectedRows = savedArticleManager.cleanMetadata()
        Logger.d(TAG, "Metadata cleanup, removed $affectedRows rows.")
    }

    companion object : SingletonHolder<MetadataManager, SavedArticleManager>(::MetadataManager) {
        private val TAG: String = MetadataManager::class.java.simpleName
        private const val MAX_ARTICLES_PER_REQUEST = 100
        private const val FIVE_MINUTES_IN_MILLISECONDS = 5 * 60 * 1000
    }
}
