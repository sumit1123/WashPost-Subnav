package com.wapo.flagship.di.app.modules.features.newsletters

import android.content.Context
import androidx.room.Room
import com.wapo.flagship.features.newsletter.repo.local.NewslettersDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object NewslettersRoomModule {
    @Singleton
    @Provides
    fun provideNewslettersDatabase(@ApplicationContext applicationContext: Context): NewslettersDatabase =
        Room
            .databaseBuilder(
                applicationContext,
                NewslettersDatabase::class.java,
                "newsletters-db",
            )
            .addMigrations(NewslettersDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
}
