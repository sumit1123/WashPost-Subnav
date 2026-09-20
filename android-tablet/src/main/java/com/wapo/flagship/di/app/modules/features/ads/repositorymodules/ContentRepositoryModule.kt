// Copyright (c) 2025 The Washington Post. All rights reserved.
package com.wapo.flagship.di.app.modules.features.ads.repositorymodules

import android.content.Context
import com.wapo.flagship.features.ads.targeting.repo.ContentRepository
import com.wapo.flagship.features.ads.targeting.services.ContentService
import com.washingtonpost.android.config.domain.models.config.ContextualTargetingContent
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object ContentRepositoryModule {
    @Singleton
    @Provides
    fun provideContentRepository(
        @ApplicationContext applicationContext: Context,
        apiService: ContentService,
        config: ContextualTargetingContent?
    ): ContentRepository =
        ContentRepository(
            context = applicationContext,
            service = apiService,
            config = config
        )
}