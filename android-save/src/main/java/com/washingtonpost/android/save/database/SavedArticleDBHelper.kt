/* Copyright (c) 2019 The Washington Post. All rights reserved. */

package com.washingtonpost.android.save.database

import androidx.paging.DataSource
import androidx.lifecycle.LiveData
import androidx.sqlite.db.SupportSQLiteDatabase
import com.washingtonpost.android.save.SaveProvider
import com.washingtonpost.android.save.database.model.*

class SavedArticleDBHelper(
    saveProvider: SaveProvider,
    private val savedArticleDB: SavedArticleDB = SavedArticleDB.getInstance(saveProvider.getAppContext())
) {

    fun addAllMetadata(metadataList: List<MetadataModel>, metadataUpdateType: MetadataUpdateType = MetadataUpdateType.DEFAULT) {
       savedArticleDB.articleItemModel().upsertMetadata(metadataUpdateType, *metadataList.toTypedArray())
    }

    fun syncArticles(articleList: List<SavedArticleModel>) {
        savedArticleDB.articleItemModel().removeAllArticles()
        savedArticleDB.articleItemModel().addArticle(*articleList.toTypedArray())
    }

    fun addAllArticles(savedArticleModel: List<SavedArticleModel>) {
        savedArticleDB.articleItemModel().addArticle(*savedArticleModel.toTypedArray())
    }

    fun deleteArticlesByUrl(contentUrls: List<String>) {
        savedArticleDB.articleItemModel().deleteArticlesByUrl(contentUrls)
    }

    fun queueArticle(articleListQueue: ArticleListQueue) {
        savedArticleDB.articleItemModel().queueArticles(articleListQueue)
    }

    fun getTotalLastModifiedMetadata(lmt: Long): Int {
        return savedArticleDB.articleItemModel().getTotalLastModifiedMetadata(lmt)
    }

    fun getLastModifiedMetadata(lmt: Long, limit: Int, offset: Int): List<ModifiedMetadata> {
        return savedArticleDB.articleItemModel().getLastModifiedMetadata(lmt, limit, offset)
    }

    fun getPagedArticles(): DataSource.Factory<Int, ArticleAndMetadata> {
        return savedArticleDB.articleItemModel().getPagedArticles()
    }

    fun getArticlesToList(limit: Int) : List<ArticleAndMetadata> {
        return savedArticleDB.articleItemModel().getArticlesToList(limit)
    }

    fun getArticleListQueue() : List<ArticleListQueue> {
        return savedArticleDB.articleItemModel().getArticleListQueue()
    }

    fun removeQueuedArticles(articleListQueue: List<ArticleListQueue>): Int {
        return savedArticleDB.articleItemModel().removeQueuedArticles(*articleListQueue.toTypedArray())
    }

    fun getTotalArticles(): Long {
        return savedArticleDB.articleItemModel().getTotalArticles()
    }

    fun getLiveArticleByUrl(contentUrl : String) : LiveData<ArticleAndMetadata?> {
        return savedArticleDB.articleItemModel().getLiveArticleByUrl(contentUrl)
    }

    fun getArticleByUrl(contentUrl: String): ArticleAndMetadata? {
        return savedArticleDB.articleItemModel().getArticleByUrl(contentUrl)
    }

    fun updateSyncLmt(contentUrl: String, lmt: Long) {
        savedArticleDB.articleItemModel().updateSyncLmt(contentUrl, lmt)
    }

    @Deprecated("Use runInTransaction")
    fun beginTransaction() {
        savedArticleDB.beginTransaction()
    }

    @Deprecated("Use runInTransaction")
    fun endTransaction() {
        savedArticleDB.endTransaction()
    }

    @Deprecated("Use runInTransaction")
    fun setTransactionSuccessful() {
        savedArticleDB.setTransactionSuccessful()
    }

    fun enforceArticlesLimit(maxArticles: Int): Int {
        return savedArticleDB.articleItemModel().enforceArticlesLimit(maxArticles)
    }

    fun cleanMetadata(): Int {
        return savedArticleDB.articleItemModel().cleanMetadata()
    }

    fun getWritableDatabase(): SupportSQLiteDatabase {
        return savedArticleDB.openHelper.writableDatabase
    }
}