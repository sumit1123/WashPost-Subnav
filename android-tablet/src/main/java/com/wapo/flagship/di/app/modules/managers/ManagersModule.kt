package com.wapo.flagship.di.app.modules.managers

import android.content.Context
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.config.ConfigUtils
import com.wapo.flagship.content.WapoConfigManager
import com.wapo.flagship.data.CacheManager
import com.wapo.flagship.data.CacheManagerImpl
import com.wapo.flagship.kmp.core.DependencyRegistry
import com.washingtonpost.android.config.domain.manager.ConfigManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object ManagersModule {
    @Singleton
    @Provides
    fun provideConfigManager(): WapoConfigManager =
        FlagshipApplication.getInstance().contentManager.wapoConfigManager

    @Singleton
    @Provides
    fun provideCacheManager(@ApplicationContext applicationContext: Context): CacheManager = CacheManagerImpl(applicationContext)

    @Singleton
    @Provides
    fun provideConfigManager2(
        @ApplicationContext applicationContext: Context,
        kmpDependencyRegistry: DependencyRegistry,
    ): ConfigManager {
        if (!ConfigManager.isInitialized) {
            ConfigUtils.initConfig(applicationContext, kmpDependencyRegistry)
        }
        return ConfigManager.getInstance()
    }
}
