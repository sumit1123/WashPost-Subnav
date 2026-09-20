package com.wapo.flagship.di.app.modules.features.audio.roomdbmodules

import android.content.Context
import androidx.room.Room
import com.wapo.flagship.features.audio.playlist.PlaylistDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * This module provides the [PlaylistDatabase] required by room in order to carry out all the database related operations.
 */
@InstallIn(SingletonComponent::class)
@Module
object PlaylistRoomDatabaseModule {
    @Singleton
    @Provides
    fun providePlaylistRoomDatabase(@ApplicationContext applicationContext: Context): PlaylistDatabase =
        Room
            .databaseBuilder(
                applicationContext,
                PlaylistDatabase::class.java,
                "playlist-db",
            ).addMigrations(PlaylistDatabase.MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()
}
