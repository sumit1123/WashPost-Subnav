/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.di.app.modules.features.tts

import android.content.Context
import com.wapo.android.commons.di.CoroutineScopeCommonsModule
import com.wapo.android.domain.repository.RemoteLogRepo
import com.wapo.flagship.data.repository.ProvideExternalTtsRepoImpl
import com.wapo.flagship.features.articles2.services.Articles2Service
import com.wapo.flagship.features.tts.TtsNotificationActionImpl
import com.wapo.flagship.features.tts.data.TtsEngineSource
import com.wapo.flagship.features.tts.data.TtsRepositoryImpl
import com.wapo.flagship.features.tts.domain.ProvideExternalTtsRepo
import com.wapo.flagship.features.tts.domain.TtsNotificationAction
import com.wapo.flagship.features.tts.domain.TtsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TtsModule {

    @Singleton
    @Provides
    fun providesExternalTtsRepo(
        @CoroutineScopeCommonsModule.IoDispatcher dispatcher: CoroutineDispatcher,
        articles2Service: Articles2Service
    ): ProvideExternalTtsRepo = ProvideExternalTtsRepoImpl(
        dispatcher,
        articles2Service
    )

    @Singleton
    @Provides
    fun providesTtsEngineSource(
        @ApplicationContext applicationContext: Context,
    ): TtsEngineSource = TtsEngineSource(applicationContext)

    @Singleton
    @Provides
    fun providesTtsRepository(
        @ApplicationContext applicationContext: Context,
        ttsEngineSource: TtsEngineSource,
        remoteLogRepo: RemoteLogRepo
    ): TtsRepository = TtsRepositoryImpl(applicationContext, ttsEngineSource, remoteLogRepo)

    @Singleton
    @Provides
    fun providesTtsNotificationAction(): TtsNotificationAction = TtsNotificationActionImpl()
}
