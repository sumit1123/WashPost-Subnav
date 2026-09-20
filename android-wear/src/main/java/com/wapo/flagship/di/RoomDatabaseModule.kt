/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.di

import android.app.Application
import androidx.room.Room
import com.wapo.flagship.features.articles2.typeconverters.ItemListTypeConverter
import com.wapo.flagship.features.articles2.typeconverters.MoshiAdapters
import com.wapo.flagship.roomdb.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomDatabaseModule {

    @Provides
    @Singleton
    fun provideRoomDatabase(
        application: Application,
        moshiAdapters: MoshiAdapters
    ): AppDatabase {
        MoshiAdapters.INSTANCE = moshiAdapters

        return Room.databaseBuilder(
            application,
            AppDatabase::class.java, "articles-db"
        ).fallbackToDestructiveMigration().build()
    }

}