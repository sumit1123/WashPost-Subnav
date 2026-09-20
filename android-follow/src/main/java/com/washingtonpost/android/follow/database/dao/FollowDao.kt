package com.washingtonpost.android.follow.database.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.*
import com.washingtonpost.android.follow.database.model.AuthorEntity
import com.washingtonpost.android.follow.database.model.FollowEntity

@Dao
interface FollowDao {
    @Query("SELECT * FROM FollowEntity WHERE author_id = :authorId  LIMIT 1")
    fun isFollowing(authorId: String?): LiveData<FollowEntity?>

    @Query("SELECT * from FollowEntity where author_id = :authorId LIMIT 1")
    fun isFollowedAuthor(authorId: String?): FollowEntity?

    @Insert
    suspend fun setFollowing(vararg followEntity: FollowEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun syncFollowAuthor(vararg followEntity: FollowEntity)

    @Delete
    suspend fun removeFollowing(vararg followEntity: FollowEntity)

    @Update
    suspend fun updateFollowing(vararg followEntity: FollowEntity)

    @Query("SELECT COUNT(author_id) FROM FollowEntity")
    fun getNumFollowing(): LiveData<Int>

    @Query("SELECT COUNT(author_id) FROM FollowEntity")
    suspend fun getNumFollowingSync(): Int

    @Query("""SELECT * FROM FollowEntity""")
    suspend fun getFollowedAuthors(): List<FollowEntity>?

    @Query("""
        SELECT *
        FROM followentity where isAuthorMetaDataAvailable =:notExist
    """)
    suspend fun getFollowedAuthorsList(notExist: Int): List<FollowEntity>?

    @Query(""" SELECT * from FollowEntity where isFollowing =:following AND  isSynced= 0 """)
    suspend fun getFollowUnFollowAuthors(following: Boolean): List<FollowEntity>

    @Query("""
        SELECT author.*
        FROM AuthorEntity author
        JOIN FollowEntity follow USING(author_id)
        ORDER BY author.lmt DESC, author.name ASC
    """)
    fun followingByLmt(): DataSource.Factory<Int, AuthorEntity>

    @Query("""
        SELECT author.*
        FROM AuthorEntity author
        ORDER BY author.date_added DESC, author.name ASC
    """)
    fun followingAllByLmtLiveData(): LiveData<List<AuthorEntity>?>

    @Query("""
        SELECT (SELECT COUNT(author_id)
                FROM AuthorEntity author2
                JOIN FollowEntity USING(author_id)
               WHERE author2.lmt > author.lmt OR (author2.lmt = author.lmt AND author2.name < author.name)) AS `row_number`
        FROM AuthorEntity author
        JOIN FollowEntity follow USING(author_id)
        WHERE author_id = :authorId
        LIMIT 1
    """)
    suspend fun getFollowPosition(authorId: String): Int

    @Query("""
        DELETE FROM FollowEntity
        WHERE author_id IN (
            SELECT author_id FROM FollowEntity
            JOIN AuthorEntity USING (author_id)
            ORDER BY date_added DESC
            LIMIT -1 OFFSET :limit
        )
    """)
    suspend fun enforceFollowLimit(limit: Int)

    @Update
    fun updateSyncedStatus(vararg followEntity: FollowEntity)
    @Query("""
        DELETE FROM FollowEntity
        WHERE author_id IN (
            SELECT follow.author_id
            FROM FollowEntity follow
            LEFT JOIN AuthorEntity author USING(author_id)
        )
    """)
    fun clearMetaData()
}