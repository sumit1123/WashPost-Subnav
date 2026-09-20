package com.wapo.flagship.di.app.modules.features.audio

import android.content.Context
import com.wapo.flagship.features.audio.AudioRecommendationsProvider
import com.wapo.flagship.features.audio.ClassicAudioManager2
import com.wapo.flagship.features.audio.ads.repository.AudioAdsRepository
import com.wapo.flagship.features.audio.ads.ui.AudioAdsController
import com.wapo.flagship.features.audio.config.AudioProvider
import com.wapo.flagship.features.audio.podcast.PodcastMetadataResolver
import com.wapo.flagship.features.audio.service2.common.MusicServiceConnection
import com.washingtonpost.userhistory.domain.UserHistoryManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object AudioManagerModule {
    @Singleton
    @Provides
    internal fun provideAudioManager2(
        @ApplicationContext applicationContext: Context,
        musicServiceConnection: MusicServiceConnection,
        audioProvider: AudioProvider,
        audioRecommendationsProvider: AudioRecommendationsProvider,
        userHistoryManager: UserHistoryManager,
        adsController: AudioAdsController,
        audioAdsRepository: AudioAdsRepository,
        podcastMetadataResolver: PodcastMetadataResolver,
    ): ClassicAudioManager2 =
        ClassicAudioManager2(
            applicationContext,
            musicServiceConnection,
            audioProvider,
            audioRecommendationsProvider,
            userHistoryManager,
            adsController,
            audioAdsRepository,
            podcastMetadataResolver,
        )
}
