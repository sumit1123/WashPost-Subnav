package com.wapo.flagship.di.app.modules.features.watchvideo

import com.wapo.flagship.features.wpvideos.repo.WatchVideosRepository
import com.wapo.flagship.network.request.WatchApiService
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.WPVideosConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object WatchVideosRepositoryModule {
    @Singleton
    @Provides
    fun provideWatchVideosRepository(
        watchApiService: WatchApiService,
        wpVideosConfig: WPVideosConfig
    ): WatchVideosRepository {
        return WatchVideosRepository(watchApiService, wpVideosConfig)
    }

    @Singleton
    @Provides
    fun provideWPVideosConfig(): WPVideosConfig {
        return ConfigManager.getInstance().config.wpVideosConfig
    }
}