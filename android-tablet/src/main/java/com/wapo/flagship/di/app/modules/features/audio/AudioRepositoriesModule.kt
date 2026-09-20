package com.wapo.flagship.di.app.modules.features.audio

import com.wapo.flagship.features.audio.ads.repository.AudioAdsRepository
import com.wapo.flagship.features.audio.ads.repository.AudioAdsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioRepositoriesModule {
    @Binds
    abstract fun provideAudioAdsRepository(audioAdsRepositoryImpl: AudioAdsRepositoryImpl): AudioAdsRepository
}
