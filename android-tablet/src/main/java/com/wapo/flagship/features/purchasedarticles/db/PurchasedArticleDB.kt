package com.wapo.flagship.features.purchasedarticles.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.wapo.flagship.features.purchasedarticles.model.MetadataPurchasedArticleModel

@Database(
    entities = [MetadataPurchasedArticleModel::class],
    version = PurchasedArticleDB.DB_VERSION
)
@TypeConverters(
    MetadataLabelTypeConverter::class
)
abstract class PurchasedArticleDB : RoomDatabase() {

    abstract fun purchasedArticleDao(): PurchasedArticleDao

    companion object {
        const val DB_NAME = "purchased_articles_db"
        const val DB_VERSION = 1
    }
}