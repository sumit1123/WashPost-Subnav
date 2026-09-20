// Copyright (c) 2024 The Washington Post. All rights reserved.
package com.wapo.flagship.di.app.modules.features.aixp.repositorymodules

import com.wapo.android.domain.repository.RemoteLogRepo
import com.wapo.flagship.features.aixp.domain.PostAnswersAIRepo
import com.wapo.flagship.features.aixp.repo.PostAnswersAIRepoImpl
import com.wapo.flagship.features.aixp.services.PostAnswersAiService
import com.wapo.flagship.features.ask.cache.AskQuestionsCache
import com.wapo.flagship.features.ask.repo.AskQuestionsRepo
import com.wapo.flagship.features.ask.repo.AskQuestionsRepoImpl
import com.wapo.flagship.features.ask.repo.AskThePostRepo
import com.wapo.flagship.features.ask.repo.AskThePostRepoImpl
import com.wapo.flagship.features.ask.services.AskQuestionsService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object AskRepositoryModule {
    @Singleton
    @Provides
    fun provideAskQuestionsRepository(
        askQuestionsService: AskQuestionsService,
        askQuestionsCache: AskQuestionsCache,
        remoteLogRepo: RemoteLogRepo
    ): AskQuestionsRepo =
        AskQuestionsRepoImpl(
            service = askQuestionsService,
            cache = askQuestionsCache,
            remoteLogRepo
        )

    @Singleton
    @Provides
    fun provideAskThePostRepo(postAnswersAIRepo: PostAnswersAIRepo): AskThePostRepo =
        AskThePostRepoImpl(postAnswersAIRepo)

    @Singleton
    @Provides
    fun providePostAnswersAiRepo(
        postAnswersAiService: PostAnswersAiService
    ): PostAnswersAIRepo = PostAnswersAIRepoImpl(postAnswersAiService)
}
