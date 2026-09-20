package com.wapo.flagship.di.app.modules

import android.content.Context
import androidx.room.Room
import com.wapo.flagship.roomdb.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * This module provides the [AppDatabase] required by room in order to carry out all the database related operations.
 */
@InstallIn(SingletonComponent::class)
@Module
object RoomDatabaseModule {
    @Singleton
    @Provides
    fun provideRoomDatabase(@ApplicationContext applicationContext: Context): AppDatabase =
        Room
            .databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "articles-db",
            ).fallbackToDestructiveMigration()
            .build()
}
