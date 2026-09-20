package com.wapo.flagship.di.app.modules.features.purchasedarticles

import android.content.Context
import androidx.room.Room
import com.wapo.flagship.features.purchasedarticles.db.PurchasedArticleDB
import com.wapo.flagship.features.purchasedarticles.db.PurchasedArticleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object PurchasedArticleDBModule {
    @Singleton
    @Provides
    fun providePurchasedArticleDB(@ApplicationContext context: Context): PurchasedArticleDB =
        Room.databaseBuilder(
            context.applicationContext,
            PurchasedArticleDB::class.java,
            PurchasedArticleDB.Companion.DB_NAME
        )
            .build()

    @Singleton
    @Provides
    fun providesPurchasedArticleDao(purchasedArticleDB: PurchasedArticleDB): PurchasedArticleDao =
        purchasedArticleDB.purchasedArticleDao()
}