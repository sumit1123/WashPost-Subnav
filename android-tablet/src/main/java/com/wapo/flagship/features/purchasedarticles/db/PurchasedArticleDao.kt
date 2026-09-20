package com.wapo.flagship.features.purchasedarticles.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wapo.flagship.features.purchasedarticles.model.MetadataPurchasedArticleModel
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchasedArticleDao {

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertAll(metadataPurchasedArticles: List<MetadataPurchasedArticleModel>)

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(metadataPurchasedArticle: MetadataPurchasedArticleModel)

    @Query("SELECT * from metadataPurchasedArticles")
    fun getAllPurchasesArticles(): Flow<List<MetadataPurchasedArticleModel>>

    @Query("DELETE FROM metadataPurchasedArticles")
    suspend fun clearAllDataFromTable()

}