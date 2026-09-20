/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wapo.flagship.features.articles2.models.Article2
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

/**
 * DAO for room that represents the [Article2] model in the database and helper methods to manipulate DB
 */
@Dao
abstract class Article2Dao {

    /**
     * Retrieve [Article2] model by a giver [contentUrl] and continuously listen to all updates of the article
     */
    @Query("SELECT * FROM articles WHERE contenturl LIKE :contentUrl")
    abstract fun getArticleUpdates(contentUrl: String): Flow<Article2>

    @Query("SELECT * FROM articles WHERE contenturl LIKE :contentUrl")
    protected abstract suspend fun getArticleInternal(contentUrl: String): Article2?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun insertArticleInternal(vararg article: Article2)

    @Query("UPDATE articles SET ttl=:ttl WHERE contenturl=:contentUrl")
    abstract suspend fun updateTtl(contentUrl: String, ttl: Long = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(12))

    @Query("DELETE FROM articles WHERE ttl < :ttl ")
    abstract fun cleanUp(ttl: Long)

    /**
     * Retrieve a single [Article2] model
     */
    open suspend fun getArticle(url: String): Article2? {
        updateTtl(url)
        return getArticleInternal(url)
    }

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
                    ttl = now + TimeUnit.HOURS.toMillis(12))
            }
            .toTypedArray()
            .run { insertArticleInternal(*this) }
    }

}