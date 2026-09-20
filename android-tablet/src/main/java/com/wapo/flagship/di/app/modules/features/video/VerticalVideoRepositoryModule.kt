package com.wapo.flagship.di.app.modules.features.video

import android.content.Context
import com.wapo.flagship.features.video.repo.VerticalVideoRepository
import com.wapo.flagship.features.video.service.VerticalVideoService
import com.washingtonpost.android.config.domain.models.config.NextVideoConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class VerticalVideoRepositoryModule {

    @Singleton
    @Provides
    fun provideVerticalVideoRepository(
        @ApplicationContext applicationContext: Context,
        service: VerticalVideoService,
        nextVideoConfig: NextVideoConfig
    ): VerticalVideoRepository =
        VerticalVideoRepository(applicationContext, service, nextVideoConfig)
}