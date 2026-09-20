package com.wapo.flagship.di.app.modules.features.find

import com.wapo.flagship.content.WapoConfigManager
import com.wapo.flagship.data.CacheManager
import com.wapo.flagship.domain.repository.FindRepository
import com.wapo.flagship.features.find.repo.FindRepositoryImpl
import com.wapo.flagship.util.coroutines.DispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object FindRepositoryModule {
    @Singleton
    @Provides
    fun provideFindRepository(
        configManager: WapoConfigManager,
        cacheManager: CacheManager,
        dispatcherProvider: DispatcherProvider,
    ): FindRepository =
        FindRepositoryImpl(
            configManager,
            cacheManager,
            dispatcherProvider = dispatcherProvider,
        )
}
