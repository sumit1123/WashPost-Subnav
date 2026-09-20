package com.washingtonpost.android.save.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.washingtonpost.android.save.database.model.DefaultMetadataUpdate
import com.washingtonpost.android.save.database.model.MetadataModel
import com.washingtonpost.android.save.database.model.MetadataUpdateType
import com.washingtonpost.android.save.database.model.ReadingHistoryAndMetadata
import com.washingtonpost.android.save.database.model.ReadingHistoryModel
import com.washingtonpost.android.save.database.model.toDefault
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingHistoryDao {
    @Query(
        """
        SELECT rhm.contentId, rhm.contentUrl, mm.headline, mm.byline, mm.blurb, mm.imageURL, mm.publishedTime, mm.lastUpdated, mm.canonicalURL, mm.secondaryText, mm.displayLabel, mm.displayTransparency, mm.trackingString, mm.headlinePrefix, rhm.lmt, rhm.isListened
        FROM ReadingHistoryModel rhm
        JOIN MetadataModel mm ON rhm.contentUrl = mm.contentURL OR rhm.contentUrl = mm.canonicalURL
        WHERE (rhm.contentUrl = :contentUrl OR mm.canonicalURL = :contentUrl)
        ORDER BY rhm.lmt DESC, rhm.contentId DESC
        LIMIT 1
    """
    )
    fun getArticleByUrl(
        contentUrl: String
    ): ReadingHistoryAndMetadata?

    @Query("SELECT COUNT(*) FROM ReadingHistoryModel") suspend fun getCount(): Int

    @Query(
        """
        SELECT rhm.contentId, rhm.contentUrl, mm.headline, mm.byline, mm.blurb, mm.imageURL, mm.publishedTime, mm.lastUpdated, mm.canonicalURL, mm.secondaryText, mm.displayLabel, mm.displayTransparency, mm.trackingString, mm.headlinePrefix, rhm.lmt, rhm.isListened
        FROM ReadingHistoryModel rhm
        JOIN MetadataModel mm ON rhm.contentUrl = mm.contentURL
        ORDER BY rhm.lmt DESC, rhm.contentId DESC
        LIMIT :limit
    """
    )
    suspend fun getArticlesToList(
        limit: Int
    ): List<ReadingHistoryAndMetadata>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addArticle(vararg readingHistoryModel: ReadingHistoryModel)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun addMetadata(vararg metadataModel: MetadataModel): List<Long>

    @Update(entity = MetadataModel::class)
    fun updateMetadata(vararg defaultMetadataUpdate: DefaultMetadataUpdate)

    @Query("DELETE FROM MetadataModel")
    suspend fun clearMetadata()

    fun upsertMetadata(
        metadataUpdateType: MetadataUpdateType,
        vararg metadataModel: MetadataModel
    ) {
        val insertResult: List<Long> = addMetadata(*metadataModel)
        val updateList: MutableList<MetadataModel> = ArrayList()
        for (i in insertResult.indices) {
            if (insertResult[i] == -1L) {
                updateList.add(metadataModel[i])
            }
        }
        if (updateList.isNotEmpty()) {
            when (metadataUpdateType) {
                MetadataUpdateType.DEFAULT -> updateMetadata(*updateList.map { it.toDefault() }
                    .toTypedArray())
            }
        }
    }

    @Query("DELETE FROM ReadingHistoryModel")
    fun clearReadingHistory(): Int

    @Query("SELECT * FROM ReadingHistoryModel rhm ORDER BY rhm.lmt DESC")
    fun observeReadingHistory(): Flow<List<ReadingHistoryAndMetadata>>

    @Transaction
    suspend fun replaceAll(
        readingHistoryList: List<ReadingHistoryModel>,
        metadataList: List<MetadataModel>
    ) {
        clearReadingHistory()
        clearMetadata()
        addMetadata(*metadataList.toTypedArray())
        addArticle(*readingHistoryList.toTypedArray())
    }
}