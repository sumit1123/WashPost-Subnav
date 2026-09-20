package com.wapo.flagship.di.app.modules.features.foryou

import android.content.Context
import com.wapo.android.commons.data.repository.DeviceUtilRepoImpl
import com.wapo.android.commons.di.CoroutineScopeCommonsModule
import com.wapo.android.domain.repository.RemoteLogRepo
import com.wapo.flagship.features.foryou.ForYouMetaProviderImpl
import com.wapo.flagship.features.foryou.ReadListProviderImpl
import com.wapo.flagship.features.habittiles.HabitTilesApiDataListener
import com.wapo.flagship.util.coroutines.CoroutineScopeProvider
import com.washingtonpost.android.save.SavedArticleManager
import com.washingtonpost.foryou.cache.Cache
import com.washingtonpost.foryou.domain.HabitTilesCache
import com.washingtonpost.foryou.cache.WidgetCache
import com.washingtonpost.foryou.di.ForYouCoroutineModule
import com.washingtonpost.foryou.domain.ForYouFeedRepository
import com.washingtonpost.foryou.domain.HabitTilesRepository
import com.washingtonpost.foryou.remote.ForYouService
import com.washingtonpost.foryou.repo.ForYouFeedRepositoryImpl
import com.washingtonpost.foryou.repo.HabitTilesRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object ForYouRepositoryModule {


    @Singleton
    @Provides
    fun provideForYouRepository(
        @ApplicationContext applicationContext: Context,
        @ForYouCoroutineModule.ForYouCoroutineScope scope: CoroutineScope,
        @CoroutineScopeCommonsModule.IoDispatcher dispatcher: CoroutineDispatcher,
        forYouService: ForYouService,
        savedArticleManager: SavedArticleManager,
        cache: Cache,
        widgetCache: WidgetCache,
        deviceUtilRepoImpl: DeviceUtilRepoImpl
    ): ForYouFeedRepository =
        ForYouFeedRepositoryImpl(
            context = applicationContext,
            scope = scope,
            ioDispatcher = dispatcher,
            forYouService = forYouService,
            consumedListProvider = ReadListProviderImpl(savedArticleManager),
            cache = cache,
            widgetCache = widgetCache,
            forYouMetaProvider = ForYouMetaProviderImpl(applicationContext),
            deviceUtilRepo = deviceUtilRepoImpl
        )

    @Singleton
    @Provides
    fun provideHabitTilesRepository(
        @ApplicationContext applicationContext: Context,
        @CoroutineScopeCommonsModule.IoDispatcher dispatcher: CoroutineDispatcher,
        forYouService: ForYouService,
        savedArticleManager: SavedArticleManager,
        cache: HabitTilesCache,
        coroutineScopeProvider: CoroutineScopeProvider,
        habitTilesApiDataListener: HabitTilesApiDataListener,
        remoteLogRepo: RemoteLogRepo,
        deviceUtilRepoImpl: DeviceUtilRepoImpl
    ): HabitTilesRepository =
        HabitTilesRepositoryImpl(
            ioDispatcher = dispatcher,
            coroutineScope = coroutineScopeProvider.sync,
            forYouService = forYouService,
            consumedListProvider = ReadListProviderImpl(savedArticleManager),
            cache = cache,
            forYouMetaProvider = ForYouMetaProviderImpl(applicationContext),
            apiDataListener = habitTilesApiDataListener,
            deviceUtilRepo = deviceUtilRepoImpl,
            remoteLog = remoteLogRepo
        )
}
