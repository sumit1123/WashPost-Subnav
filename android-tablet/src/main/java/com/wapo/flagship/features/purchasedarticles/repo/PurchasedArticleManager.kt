package com.wapo.flagship.features.purchasedarticles.repo

import android.content.Context
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.features.purchasedarticles.db.PurchasedArticleDao
import com.wapo.flagship.features.purchasedarticles.model.MetadataPurchasedArticleModel
import com.wapo.flagship.features.purchasedarticles.model.PurchasedArticleUrl
import com.wapo.flagship.features.purchasedarticles.service.PurchasedArticlesService
import com.wapo.flagship.network.retrofit.network.APIResult
import com.wapo.flagship.util.coroutines.CoroutineScopeProvider
import com.washingtonpost.android.save.network.MetadataEntry
import com.washingtonpost.android.save.network.MetadataRequest
import com.washingtonpost.android.save.network.SavedRetrofit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

open class PurchasedArticleManager @Inject constructor(
    @ApplicationContext val context: Context,
    private val service: PurchasedArticlesService,
    private val metadataService: SavedRetrofit.MetadataNetwork,
    private val purchasedArticleDao: PurchasedArticleDao,
    private val coroutineScopeProvider: CoroutineScopeProvider
) {

    fun getPurchasedArticlesFromRemote(headerMap: HashMap<String, String>) {
        coroutineScopeProvider.sync.launch(Dispatchers.IO) {
            val response =
                service.getPurchasedArticles(
                    headerMap,
                    true.toString(),
                    true.toString(),
                    1.toString(),
                    20.toString()
                )
            when (response) {
                is APIResult.Failure -> {
                    EventLog.Builder().apply {
                        setMessage("Fetching purchased articles failed")
                        setModule(LogModules.PURCHASED_ARTICLES)
                        setErrorMessage(response.getMessage())
                    }.run {
                        RemoteLog.e(context, build())
                    }
                }

                is APIResult.NetworkError -> Unit

                is APIResult.Success -> {
                    response.data?.purchasedArticles?.let {
                        getArticleMetadataFromRemote(it)
                    }
                }
            }
        }
    }

    private fun getArticleMetadataFromRemote(urls: List<PurchasedArticleUrl>) {
        try {
            val response = metadataService.getArticleMetadata(
                MetadataRequest(urls.mapNotNull { it.canonicalUrl })
            ).execute()
            if (response.isSuccessful) {
                response.body()?.metadata?.let {
                    coroutineScopeProvider.sync.launch {
                        saveMetaData(urls, it)
                    }

                } ?: run {
                    EventLog.Builder().apply {
                        setMessage("Fetching purchased articles meta data is empty")
                        setModule(LogModules.PURCHASED_ARTICLES)
                    }.run {
                        RemoteLog.e(context, build())
                    }
                }
            } else {
                EventLog.Builder().apply {
                    setMessage("Fetching purchased articles meta data failed")
                    setModule(LogModules.PURCHASED_ARTICLES)
                    setErrorMessage(response.errorBody()?.string())
                }.run {
                    RemoteLog.e(context, build())
                }
            }
        } catch (e: Exception) {
            EventLog.Builder().apply {
                setMessage("Time exception fetching purchased articles meta data")
                setModule(LogModules.PURCHASED_ARTICLES)
                setErrorMessage(e.message)
            }.run {
                RemoteLog.e(context, build())
            }
        }
    }

    private fun saveMetaData(
        purchasedArticles: List<PurchasedArticleUrl>,
        metaDataEntries: List<MetadataEntry>
    ) {
        metaDataEntries.forEach { metadata ->
            val purchasedArticleMeta = metadata.toPurchasedArticleMetaData(purchasedArticles)
            coroutineScopeProvider.sync.launch(Dispatchers.IO) {
                purchasedArticleDao.insert(purchasedArticleMeta)
            }
        }
    }

    fun getPurchasedArticle(): Flow<List<MetadataPurchasedArticleModel>> =
        purchasedArticleDao.getAllPurchasesArticles()

    fun MetadataEntry.toPurchasedArticleMetaData(purchasedArticleUrls: List<PurchasedArticleUrl>): MetadataPurchasedArticleModel =
        MetadataPurchasedArticleModel(
            this.url,
            this.canonicalUrl,
            this.headline,
            this.byLine,
            this.displayDate,
            this.socialImageUrl,
            this.description,
            this.label,
            this.lastUpdated,
            this.contentRestrictionCode,
            this.shouldOpenWebView,
            purchasedArticleUrls.find { it.canonicalUrl == this.canonicalUrl }?.purchaseDate
                ?: 0L
        )

    suspend fun clearCache() {
        purchasedArticleDao.clearAllDataFromTable()
    }
}
