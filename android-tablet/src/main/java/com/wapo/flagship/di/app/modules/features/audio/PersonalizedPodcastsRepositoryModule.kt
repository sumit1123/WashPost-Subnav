package com.wapo.flagship.di.app.modules.features.audio

import com.wapo.flagship.features.personalizedpodcasts.repo.PersonalizedPodcastRepository
import com.wapo.flagship.features.personalizedpodcasts.repo.PersonalizedPodcastRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
abstract class PersonalizedPodcastsRepositoryModule {
    @Binds
    abstract fun providePersonalizedPodcastsRepository(
        personalizedPodcastRepositoryImpl: PersonalizedPodcastRepositoryImpl
    ): PersonalizedPodcastRepository
}