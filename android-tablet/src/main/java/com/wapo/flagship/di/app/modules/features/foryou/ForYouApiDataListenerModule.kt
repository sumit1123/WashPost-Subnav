package com.wapo.flagship.di.app.modules.features.foryou

import com.wapo.flagship.features.habittiles.HabitTilesApiDataListener
import com.wapo.flagship.util.coroutines.CoroutineScopeProvider
import com.washingtonpost.foryou.domain.HabitTilesCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object ForYouApiDataListenerModule {
    @Singleton
    @Provides
    fun provideHabitTilesApiDataListener(
        coroutineScopeProvider: CoroutineScopeProvider,
        cache: HabitTilesCache,
    ): HabitTilesApiDataListener = HabitTilesApiDataListener(coroutineScopeProvider, cache)
}
