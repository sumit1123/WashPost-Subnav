package com.washingtonpost.android.save.database

import androidx.sqlite.db.SupportSQLiteDatabase
import com.washingtonpost.android.save.database.model.MetadataModel
import com.washingtonpost.android.save.database.model.MetadataUpdateType
import com.washingtonpost.android.save.database.model.ReadingHistoryAndMetadata
import com.washingtonpost.android.save.database.model.ReadingHistoryModel
import javax.inject.Inject

class ReadingHistoryDBHelper @Inject constructor(
    private val readingHistoryDB: ReadingHistoryDB
) {

    fun addAllMetadata(
        metadataList: List<MetadataModel>,
        metadataUpdateType: MetadataUpdateType = MetadataUpdateType.DEFAULT
    ) {
        readingHistoryDB.readingHistoryItemModel()
            .upsertMetadata(metadataUpdateType, *metadataList.toTypedArray())
    }

    fun addAllArticles(readingHistoryModel: List<ReadingHistoryModel>) {
        readingHistoryDB.readingHistoryItemModel().addArticle(*readingHistoryModel.toTypedArray())
    }

    suspend fun replaceAll(
        readingHistoryList: List<ReadingHistoryModel>,
        metadataList: List<MetadataModel>
    ) {
        readingHistoryDB.readingHistoryItemModel()
            .replaceAll(readingHistoryList, metadataList)
    }

    suspend fun getArticlesToList(limit: Int): List<ReadingHistoryAndMetadata> {
        return readingHistoryDB.readingHistoryItemModel().getArticlesToList(limit)
    }

    fun observeReadingHistory() {
        readingHistoryDB.readingHistoryItemModel().observeReadingHistory()
    }

    @Deprecated("Use runInTransaction")
    fun beginTransaction() {
        readingHistoryDB.beginTransaction()
    }

    @Deprecated("Use runInTransaction")
    fun endTransaction() {
        readingHistoryDB.endTransaction()
    }

    @Deprecated("Use runInTransaction")
    fun setTransactionSuccessful() {
        readingHistoryDB.setTransactionSuccessful()
    }

    fun getArticleByUrl(contentUrl: String): ReadingHistoryAndMetadata? {
        return readingHistoryDB.readingHistoryItemModel().getArticleByUrl(contentUrl)
    }

    fun getWritableDatabase(): SupportSQLiteDatabase {
        return readingHistoryDB.openHelper.writableDatabase
    }

    suspend fun getCount(): Int {
        return readingHistoryDB.readingHistoryItemModel().getCount();
    }
}