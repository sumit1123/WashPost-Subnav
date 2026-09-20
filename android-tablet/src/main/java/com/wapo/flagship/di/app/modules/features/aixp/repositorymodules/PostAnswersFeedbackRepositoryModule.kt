// Copyright (c) 2024 The Washington Post. All rights reserved.

package com.wapo.flagship.di.app.modules.features.aixp.repositorymodules

import com.wapo.flagship.features.aixp.repo.PostAnswersFeedbackRepository
import com.wapo.flagship.features.aixp.services.PostAnswersAiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object PostAnswersFeedbackRepositoryModule {
    @Singleton
    @Provides
    fun providePostAnswersFeedbackRepository(postAnswersAiService: PostAnswersAiService): PostAnswersFeedbackRepository =
        PostAnswersFeedbackRepository(postAnswersAiService)
}
