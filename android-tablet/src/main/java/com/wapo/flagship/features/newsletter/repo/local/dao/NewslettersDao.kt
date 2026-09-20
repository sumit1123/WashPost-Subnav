package com.wapo.flagship.features.newsletter.repo.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wapo.flagship.features.newsletter.repo.local.model.NewslettersEntity

@Dao
interface NewslettersDao {
    @Query("""SELECT * FROM NewslettersEntity where isEnrolled = :isEnrolled""")
    suspend fun getAllTopics(isEnrolled: Boolean): List<NewslettersEntity>

    @Query("""SELECT * FROM NewslettersEntity where isEnrolled = :isEnrolled AND isSynced = 0""")
    suspend fun getAllSyncTopics(isEnrolled: Boolean): List<NewslettersEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun followTopics(list: List<NewslettersEntity>)

    @Delete
    suspend fun deleteTopics(list: List<NewslettersEntity>)

    @Update
    suspend fun updateFollowing(list: List<NewslettersEntity>)

    @Query("""DELETE FROM NewslettersEntity """)
    suspend fun deleteAll()
}
