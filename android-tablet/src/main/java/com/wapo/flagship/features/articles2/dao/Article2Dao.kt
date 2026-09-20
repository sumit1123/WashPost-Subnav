package com.wapo.flagship.features.articles2.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wapo.flagship.features.articles2.models.Article2
import com.washingtonpost.android.config.domain.manager.ConfigManager

/**
 * DAO for room that represents the [Article2] model in the database and helper methods to manipulate DB
 */
@Dao
abstract class Article2Dao {
    private val cacheAgeInMillis =
        ConfigManager.getInstance().config
            .articleContentUpdateRulesConfig.contentCacheAge
            .coerceAtLeast(0)

    /**
     * Retrieve [Article2] model by a giver [contentUrl] and continuously listen to all updates of the article
     */
    @Query("Select * from articles where contenturl like :contentUrl")
    abstract fun getArticleUpdates(contentUrl: String): LiveData<Article2>

    @Query("Select * from articles where contenturl like :contentUrl")
    protected abstract suspend fun getArticleInternal(contentUrl: String): Article2?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun insertArticleInternal(vararg article: Article2)

    /**
     * This will insert an [Article2] entry in to the database. If the same entry with the existing [Article2.contenturl] is present then it will replace it
     * as per the [OnConflictStrategy.REPLACE]
     */
    open fun insertArticle(vararg article: Article2) {
        val now = System.currentTimeMillis()
        article
            .map { mappingArticle ->
                val serverDate = mappingArticle.lmt ?: now
                mappingArticle.copy(
                    createdAt = mappingArticle.createdAt.takeIf { it != null && it > 0 } ?: serverDate,
                    updatedAt = serverDate,
                    ttl = now + cacheAgeInMillis,
                )
            }.toTypedArray()
            .run { insertArticleInternal(*this) }
    }

    @Query("UPDATE articles SET ttl=:ttl WHERE contenturl=:contentUrl")
    abstract suspend fun updateTtl(
        contentUrl: String,
        ttl: Long = System.currentTimeMillis() + cacheAgeInMillis,
    )

    @Query("DELETE FROM articles WHERE ttl < :ttl ")
    abstract fun cleanUp(ttl: Long)

    /**
     * Retrieve a single [Article2] model
     */
    open suspend fun getArticle(url: String): Article2? = getArticleInternal(url)
}
