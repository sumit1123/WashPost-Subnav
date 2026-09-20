package com.washingtonpost.android.follow.database.dao

import androidx.room.*
import com.washingtonpost.android.follow.database.model.AuthorEntity
import com.washingtonpost.android.follow.database.model.FollowEntity

@Dao
interface AuthorDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAuthor(vararg authorEntity: AuthorEntity)

    @Update
    suspend fun updateAuthor(vararg authorEntity: AuthorEntity)

    @Delete
    suspend fun removeAuthor(vararg authorEntity: AuthorEntity)

    @Query("""
    DELETE FROM AuthorEntity
    WHERE author_id NOT IN (
        SELECT author_id FROM FollowEntity
    )
""")
    suspend fun removeAuthorForUnFollow()

    @Query("""
        DELETE FROM AuthorEntity
        WHERE author_id IN (
            SELECT author.author_id
            FROM AuthorEntity author
            LEFT JOIN FollowEntity follow USING (author_id)
            WHERE follow.author_id IS NULL
            ORDER BY author.date_added DESC
            LIMIT -1 OFFSET :limit
        )
    """)
    suspend fun enforceAuthorLimit(limit: Int)

    @Query("SELECT count(author_id) from AuthorEntity where author_id=:authorId LIMIT 1")
    suspend fun checkAuthorExist(authorId:String?) : Int

    @Update
    fun updateSyncedStatus(vararg followEntity: FollowEntity)

    @Query(
        """
        DELETE FROM AuthorEntity
        WHERE author_id IN (
            SELECT author.author_id
            FROM AuthorEntity author
            LEFT JOIN FollowEntity follow USING(author_id)
            where follow.author_id IS NULL
        )
    """
    )
    fun clearMetaData()
}