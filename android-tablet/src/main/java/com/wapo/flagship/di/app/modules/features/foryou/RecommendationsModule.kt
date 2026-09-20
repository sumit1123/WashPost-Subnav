package com.wapo.flagship.di.app.modules.features.foryou

import com.wapo.flagship.data.repository.RecommendationsRepoImpl
import com.wapo.flagship.domain.repository.RecommendationsRepo
import com.washingtonpost.foryou.domain.ForYouFeedRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object RecommendationsModule {

    @Singleton
    @Provides
    fun provideRecommendationsRepo(
        forYouFeedRepository: ForYouFeedRepository
    ): RecommendationsRepo =
        RecommendationsRepoImpl(forYouFeedRepository)
}
