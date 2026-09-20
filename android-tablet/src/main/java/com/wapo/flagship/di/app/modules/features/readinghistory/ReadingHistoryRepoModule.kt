package com.wapo.flagship.di.app.modules.features.readinghistory

import android.content.Context
import com.washingtonpost.android.save.database.ReadingHistoryDB
import com.washingtonpost.android.save.database.ReadingHistoryDBHelper
import com.washingtonpost.android.save.network.SavedRetrofit
import com.washingtonpost.userhistory.repo.UserHistoryMetaProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object ReadingHistoryRepoModule {

    @Singleton
    @Provides
    fun provideReadingHistoryRepo(
        readingHistoryService: ReadingHistoryService,
        readingHistoryDBHelper: ReadingHistoryDBHelper,
        metadataNetwork: SavedRetrofit.MetadataNetwork,
        userHistoryMetaProvider: UserHistoryMetaProvider
    ): ReadingHistoryRepo {
        return ReadingHistoryRepo(
            readingHistoryService,
            userHistoryMetaProvider,
            readingHistoryDBHelper,
            metadataNetwork
        )
    }

    @Singleton
    @Provides
    fun provideReadingHistoryDBHelper(
            @ApplicationContext applicationContext: Context
        ): ReadingHistoryDB {
            return ReadingHistoryDB.getInstance(applicationContext)
    }
}