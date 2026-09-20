package com.wapo.flagship.di.app.modules.features.audio

import android.content.Context
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.audio.service2.common.MusicServiceConnection
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency Hierarchy:
 * 1. "MusicServiceConnectionModule" depends on "MusicServiceModule"
 * 2. "MusicServiceModule" depends on "AudioManagerModule"
 * 3. "AudioManagerModule" depends on "AudioProviderModule"
 */
@InstallIn(SingletonComponent::class)
@Module
object MusicServiceConnectionModule {
    @Singleton
    @Provides
    internal fun provideMusicServiceConnection(@ApplicationContext applicationContext: Context): MusicServiceConnection =
        FlagshipApplication.getInstance().musicServiceConnection
}
