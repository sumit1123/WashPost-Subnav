package com.wapo.flagship.di.app.modules.features.articles2

import com.wapo.flagship.features.articles2.models.deserialized.video.InlineVideoPlayerEvents
import com.wapo.flagship.roomdb.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * This module provides the [AppDatabase] required by room in order to carry out all the database related operations.
 */
@InstallIn(SingletonComponent::class)
@Module
object InlineVideoPlayerEventsModule {
    @Singleton
    @Provides
    fun provideInlineVideoPlayerEvents(): InlineVideoPlayerEvents = InlineVideoPlayerEvents()
}
