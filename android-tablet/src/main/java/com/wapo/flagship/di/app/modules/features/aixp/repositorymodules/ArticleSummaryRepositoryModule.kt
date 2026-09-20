// Copyright (c) 2024 The Washington Post. All rights reserved.
package com.wapo.flagship.di.app.modules.features.aixp.repositorymodules

import com.wapo.flagship.features.aixp.models.SummaryRemoteConfig
import com.wapo.flagship.features.aixp.repo.ArticleSummaryRepository
import com.wapo.flagship.features.aixp.services.ArticleFeedbackService
import com.wapo.flagship.features.aixp.services.ArticleSummaryService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object ArticleSummaryRepositoryModule {

    @Singleton
    @Provides
    fun provideArticleSummaryRepository(
        summaryService: ArticleSummaryService,
        feedbackService: ArticleFeedbackService,
        summaryRemoteConfig: SummaryRemoteConfig
    ): ArticleSummaryRepository =
        ArticleSummaryRepository(summaryService, feedbackService, summaryRemoteConfig)
}
