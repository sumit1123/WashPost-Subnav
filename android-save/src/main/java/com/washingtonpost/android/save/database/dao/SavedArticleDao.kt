package com.washingtonpost.android.save.database.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.*
import com.washingtonpost.android.save.database.model.*
import com.washingtonpost.android.save.misc.ArticleListQueueType

@Dao
interface SavedArticleDao {
    @Query("""
        SELECT sam.id, sam.contentURL, mm.headline, mm.byline, mm.blurb, mm.imageURL, mm.publishedTime, mm.lastUpdated, mm.canonicalURL, mm.secondaryText, mm.displayLabel, mm.displayTransparency, mm.trackingString, mm.headlinePrefix, sam.lmt, sam.isListened
        FROM SavedArticleModel sam
        JOIN MetadataModel mm ON sam.contentURL = mm.contentURL
        WHERE NOT EXISTS (
            SELECT articleListQueueType FROM (
                SELECT alq.articleListQueueType FROM ArticleListQueue alq WHERE alq.contentURL = sam.contentURL
                ORDER BY alq.lmt DESC
                LIMIT 1
            )
            WHERE articleListQueueType = :articleListQueueType
        )
        ORDER BY sam.lmt DESC, sam.id DESC
    """)
    fun getPagedArticles(
            articleListQueueType: ArticleListQueueType = ArticleListQueueType.DELETE_ARTICLE
    ): DataSource.Factory<Int, ArticleAndMetadata>

    @Query("""
        SELECT sam.id, sam.contentURL, mm.headline, mm.byline, mm.blurb, mm.imageURL, mm.publishedTime, mm.lastUpdated, mm.canonicalURL, mm.secondaryText, mm.displayLabel, mm.displayTransparency, mm.trackingString, mm.headlinePrefix, sam.lmt, sam.isListened
        FROM SavedArticleModel sam
        JOIN MetadataModel mm ON sam.contentURL = mm.contentURL OR sam.contentURL = mm.canonicalURL
        WHERE (sam.contentURL = :contentUrl OR mm.canonicalURL = :contentUrl)
            AND NOT EXISTS (
				SELECT articleListQueueType FROM (
                    SELECT alq.articleListQueueType FROM ArticleListQueue alq WHERE alq.contentURL = sam.contentURL 
                    ORDER BY alq.lmt DESC
                    LIMIT 1
				)
				WHERE articleListQueueType = :articleListQueueType
            )
        ORDER BY sam.lmt DESC, sam.id DESC
        LIMIT 1
    """)
    fun getLiveArticleByUrl(contentUrl: String, articleListQueueType: ArticleListQueueType = ArticleListQueueType.DELETE_ARTICLE): LiveData<ArticleAndMetadata?>

    @Query("""
        SELECT sam.id, sam.contentURL, mm.headline, mm.byline, mm.blurb, mm.imageURL, mm.publishedTime, mm.lastUpdated, mm.canonicalURL, mm.secondaryText, mm.displayLabel, mm.displayTransparency, mm.trackingString, mm.headlinePrefix, sam.lmt, sam.isListened
        FROM SavedArticleModel sam
        JOIN MetadataModel mm ON sam.contentURL = mm.contentURL OR sam.contentURL = mm.canonicalURL
        WHERE (sam.contentURL = :contentUrl OR mm.canonicalURL = :contentUrl) 
            AND NOT EXISTS (
				SELECT articleListQueueType FROM (
                    SELECT alq.articleListQueueType FROM ArticleListQueue alq WHERE alq.contentURL = sam.contentURL
                    ORDER BY alq.lmt DESC
                    LIMIT 1
				)
				WHERE articleListQueueType = :articleListQueueType
            )
        ORDER BY sam.lmt DESC, sam.id DESC
        LIMIT 1
    """)
    fun getArticleByUrl(contentUrl: String, articleListQueueType: ArticleListQueueType = ArticleListQueueType.DELETE_ARTICLE): ArticleAndMetadata?

    @Query("""
        SELECT sam.id, sam.contentURL, mm.headline, mm.byline, mm.blurb, mm.imageURL, mm.publishedTime, mm.lastUpdated, mm.canonicalURL, mm.secondaryText, mm.displayLabel, mm.displayTransparency, mm.trackingString, mm.headlinePrefix, sam.lmt, sam.isListened
        FROM SavedArticleModel sam
        JOIN MetadataModel mm ON sam.contentURL = mm.contentURL
        WHERE NOT EXISTS (
            SELECT articleListQueueType FROM (
                SELECT alq.articleListQueueType FROM ArticleListQueue alq WHERE alq.contentURL = sam.contentURL
                LIMIT 1
            )
            WHERE articleListQueueType = :articleListQueueType
        )
        ORDER BY sam.lmt DESC, sam.id DESC
        LIMIT :limit
    """)
    fun getArticlesToList(limit: Int, articleListQueueType: ArticleListQueueType = ArticleListQueueType.DELETE_ARTICLE): List<ArticleAndMetadata>

    @Query("""
        SELECT COUNT(sam.id)
        FROM SavedArticleModel sam
        WHERE NOT EXISTS (
            SELECT articleListQueueType FROM (
                SELECT alq.articleListQueueType FROM ArticleListQueue alq WHERE alq.contentURL = sam.contentURL
                ORDER BY alq.lmt DESC
                LIMIT 1
            )
            WHERE articleListQueueType = :articleListQueueType
        )
    """)
    fun getTotalArticles(articleListQueueType: ArticleListQueueType = ArticleListQueueType.DELETE_ARTICLE): Long

    @Query("""
        SELECT * FROM ArticleListQueue
        ORDER BY lmt ASC
    """)
    fun getArticleListQueue() : List<ArticleListQueue>

    @Delete
    fun removeQueuedArticles(vararg articleListQueue: ArticleListQueue): Int

    @Query("""
        SELECT COUNT(sam.contentURL)
        FROM SavedArticleModel sam
        LEFT JOIN MetadataModel mm ON sam.contentURL = mm.contentURL
        WHERE mm.syncLmt IS NULL OR mm.syncLmt < :lmt
    """)
    fun getTotalLastModifiedMetadata(lmt: Long): Int

    @Query("""
        SELECT sam.contentURL, mm.syncLmt FROM SavedArticleModel sam
        LEFT JOIN MetadataModel mm ON sam.contentURL = mm.contentURL
        WHERE mm.syncLmt IS NULL OR mm.syncLmt < :lmt
        ORDER BY mm.syncLmt ASC, sam.lmt DESC, sam.id ASC
        LIMIT :limit OFFSET :offset
    """)
    fun getLastModifiedMetadata(lmt: Long, limit: Int, offset: Int): List<ModifiedMetadata>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addArticle(vararg savedArticleModel: SavedArticleModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun queueArticles(vararg articleListQueue: ArticleListQueue)

    @Update
    fun updateArticle(vararg savedArticleModel: SavedArticleModel)

    @Query("""
        DELETE FROM SavedArticleModel
        WHERE contentURL IN (:urls)
    """)
    fun deleteArticlesByUrl(urls: List<String>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun addMetadata(vararg metadataModel: MetadataModel): List<Long>

    @Update(entity = MetadataModel::class)
    fun updateMetadata(vararg defaultMetadataUpdate: DefaultMetadataUpdate)

    fun upsertMetadata(metadataUpdateType: MetadataUpdateType, vararg metadataModel: MetadataModel) {
        val insertResult: List<Long> = addMetadata(*metadataModel)
        val updateList: MutableList<MetadataModel> = ArrayList()
        for (i in insertResult.indices) {
            if (insertResult[i] == -1L) {
                updateList.add(metadataModel[i])
            }
        }
        if (updateList.isNotEmpty()) {
            when (metadataUpdateType) {
                MetadataUpdateType.DEFAULT -> updateMetadata(*updateList.map { it.toDefault() }.toTypedArray())
            }
        }
    }

    @Delete
    fun deleteMetadata(metadataModel: MetadataModel)

    @Query("DELETE FROM SavedArticleModel")
    fun removeAllArticles(): Int

    @Query("""
        DELETE FROM MetadataModel
        WHERE contentURL IN (
            SELECT mm.contentURL
            FROM MetadataModel mm
            LEFT JOIN SavedArticleModel sam USING(contentURL)
            WHERE sam.contentURL IS NULL
        )
    """)
    fun cleanMetadata(): Int

    @Query("""
        DELETE FROM SavedArticleModel
        WHERE id IN (
            SELECT id FROM SavedArticleModel
            ORDER BY lmt DESC
            LIMIT -1 OFFSET :maxArticles
        )
    """)
    fun enforceArticlesLimit(maxArticles: Int): Int

    @Query("""
        UPDATE MetadataModel
        SET syncLmt = :lmt
        WHERE contentURL = :contentUrl
    """)
    fun updateSyncLmt(contentUrl: String, lmt: Long): Int
}