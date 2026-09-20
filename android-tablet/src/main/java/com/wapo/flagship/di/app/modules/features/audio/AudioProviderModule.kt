package com.wapo.flagship.di.app.modules.features.audio

import com.wapo.flagship.di.app.modules.features.readinghistory.ReadingHistoryRepo
import com.wapo.flagship.features.articles2.repo.Articles2Repository
import com.wapo.flagship.features.audio.AudioRecommendationsProvider
import com.wapo.flagship.features.audio.config.AudioProvider
import com.wapo.flagship.features.foryou.AudioRecommendationsProviderImpl
import com.wapo.flagship.features.podcast.AudioProviderImpl
import com.wapo.flagship.util.coroutines.DispatcherProvider
import com.washingtonpost.foryou.domain.ForYouFeedRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object AudioProviderModule {
    @Singleton
    @Provides
    internal fun provideAudioProvider(): AudioProvider = AudioProviderImpl()

    @Singleton
    @Provides
    internal fun provideAudioRecommendations(
        forYouFeedRepository: ForYouFeedRepository,
        articles2Repository: Articles2Repository,
        dispatcherProvider: DispatcherProvider,
        readingHistoryRepo: ReadingHistoryRepo
    ): AudioRecommendationsProvider =
        AudioRecommendationsProviderImpl(
            forYouFeedRepository,
            articles2Repository,
            dispatcherProvider,
            readingHistoryRepo
        )
}
