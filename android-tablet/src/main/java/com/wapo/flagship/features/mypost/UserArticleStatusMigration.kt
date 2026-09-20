package com.wapo.flagship.features.mypost

import android.database.sqlite.SQLiteDatabase
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.data.CacheManager
import com.wapo.flagship.data.FileMetaUserArticle
import com.wapo.flagship.features.articles.models.UserArticleStatus
import com.wapo.flagship.wrappers.CrashWrapper
import com.washingtonpost.android.save.SavedArticleManager
import com.washingtonpost.android.save.database.SavedArticleDB
import com.washingtonpost.android.save.database.model.ArticleListQueue
import com.washingtonpost.android.save.database.model.MetadataModel
import com.washingtonpost.android.save.database.model.SavedArticleModel
import com.washingtonpost.android.save.misc.ArticleListQueueType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class UserArticleStatusMigration private constructor(
    private val cacheManager: CacheManager,
    private val savedArticleManager: SavedArticleManager,
) {
    private val db = cacheManager.getDb()

    fun migrateIfNeeded() {
        GlobalScope.launch {
            Logger.d(TAG, "Starting user article status migration")
            savedArticleManager.tryWithLock {
                try {
                    savedArticleManager.getWritableDatabase()
                    val dbPath = cacheManager.getDatabasePath(SavedArticleDB.DB_NAME)
                    db.execSQL("ATTACH DATABASE '$dbPath' AS " + SavedArticleDB.DB_NAME)
                    try {
                        db.beginTransaction()
                        performMigration(
                            db,
                            UserArticleStatus.Type.FAVORITE.id,
                            0,
                            true,
                        )
                        performMigration(
                            db,
                            UserArticleStatus.Type.READING_HISTORY.id,
                            1,
                            false,
                        )
                        db.delete(FileMetaUserArticle.TableName, null, null)
                        db.setTransactionSuccessful()
                        Logger.d(TAG, "User article status migration complete")
                    } catch (e: Exception) {
                        CrashWrapper.sendException(e)
                        Logger.e(TAG, "An error occurred", e)
                    } finally {
                        db.endTransaction()
                    }
                    db.execSQL("DETACH " + SavedArticleDB.DB_NAME)
                } catch (e: Exception) {
                    CrashWrapper.sendException(e)
                    Logger.e(TAG, "User article status migration error", e)
                }
            }
        }
    }

    private fun performMigration(
        db: SQLiteDatabase,
        oldStatus: Long,
        newStatus: Int,
        shouldQueue: Boolean,
    ) {
        val oldStatusTable = FileMetaUserArticle.TableName
        val newStatusTable = SavedArticleDB.DB_NAME + "." + SavedArticleModel::class.java.simpleName
        val newQueueTable = SavedArticleDB.DB_NAME + "." + ArticleListQueue::class.java.simpleName
        val newMetadataTable = SavedArticleDB.DB_NAME + "." + MetadataModel::class.java.simpleName
        val articleStatusColumn = FileMetaUserArticle.ArticleStatusColumn
        val oldUrlColumn = FileMetaUserArticle.ArticleUrlColumn
        val oldLmtColumn = FileMetaUserArticle.ActivityDateColumn
        val oldHeadlineColumn = FileMetaUserArticle.HeadlineColumn
        val oldBlurbColumn = FileMetaUserArticle.SummaryColumn
        val oldBylineColumn = FileMetaUserArticle.ByLineColumn
        val addArticleType =
            ArticleListQueueType.getArticleListQueueType(
                ArticleListQueueType.ADD_ARTICLE,
            )
        val articleQuery = """
            INSERT OR REPLACE INTO $newStatusTable (contentURL, lmt, articleListType)
                SELECT REPLACE($oldUrlColumn, 'http://', 'https://'), $oldLmtColumn, $newStatus
                FROM $oldStatusTable
                WHERE $articleStatusColumn = $oldStatus
            """
        val metadataQuery = """
            INSERT OR REPLACE INTO $newMetadataTable (contentURL, syncLmt, publishedTime, headline, blurb, byline)
                SELECT REPLACE($oldUrlColumn, 'http://', 'https://'), 0, 0, $oldHeadlineColumn, $oldBlurbColumn, $oldBylineColumn
                FROM $oldStatusTable
                WHERE $articleStatusColumn = $oldStatus
        """
        val queueQuery = """
            INSERT OR REPLACE INTO $newQueueTable (contentURL, lmt, articleListType, articleListQueueType)
                SELECT REPLACE($oldUrlColumn, 'http://', 'https://'), $oldLmtColumn, $newStatus, $addArticleType
                FROM $oldStatusTable
                WHERE $articleStatusColumn = $oldStatus
            """
        db.execSQL(articleQuery)
        db.execSQL(metadataQuery)
        if (shouldQueue) {
            db.execSQL(queueQuery)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: UserArticleStatusMigration? = null
        private val TAG: String = UserArticleStatusMigration::class.java.simpleName

        @JvmStatic
        fun getInstance(
            cacheManager: CacheManager,
            savedArticleManager: SavedArticleManager,
        ): UserArticleStatusMigration =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserArticleStatusMigration(cacheManager, savedArticleManager).also {
                    INSTANCE = it
                }
            }
    }
}
