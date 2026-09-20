package com.wapo.flagship.di.app.modules

import android.content.Context
import com.wapo.android.commons.data.repository.BuildProviderRepoImpl
import com.wapo.android.commons.data.repository.DeviceUtilRepoImpl
import com.wapo.android.commons.data.repository.UtilsRepoImpl
import com.wapo.android.commons.domain.BuildConfigProvider
import com.wapo.android.commons.domain.BuildProviderRepo
import com.wapo.android.commons.domain.DeviceUtilRepo
import com.wapo.android.commons.domain.UtilsRepo
import com.wapo.android.commons.util.agerestriction.AgeRestrictionValidator
import com.wapo.android.commons.util.agerestriction.AgeRestrictionValidatorProvider
import com.wapo.android.domain.repository.EventTimerLogRepo
import com.wapo.android.domain.repository.LoadRenderMetrics
import com.wapo.android.remotelog.logger.EventTimerLogRepoImpl
import com.wapo.android.remotelog.logger.LoadRenderMetricsImpl
import com.wapo.flagship.IntentHelper
import com.wapo.flagship.features.grid.model.PageConfig
import com.wapo.flagship.util.BuildConfigProviderImpl
import com.washingtonpost.android.config.domain.manager.ConfigManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object UtilsModule {

    @Provides
    fun provideAgeRestrictionValidator(@ApplicationContext context: Context): AgeRestrictionValidator =
        AgeRestrictionValidatorProvider.provideAgeRestrictionValidator(context)

    @Provides
    fun providePageConfig(): PageConfig =
        PageConfig.build(ConfigManager.getInstance().config)

    @Provides
    @Singleton
    fun provideIntentHelper(): IntentHelper =
        IntentHelper()

    @Provides
    @Singleton
    fun provideBuildProviderRepo(): BuildProviderRepo =
        BuildProviderRepoImpl()

    @Provides
    @Singleton
    fun provideBuildConfigProvider(): BuildConfigProvider =
        BuildConfigProviderImpl()

    @Provides
    @Singleton
    fun provideUtilRepo(
        @ApplicationContext context: Context,
        buildProviderRepo: BuildProviderRepo,
        buildConfigProvider: BuildConfigProvider
    ): UtilsRepo =
        UtilsRepoImpl(context, buildProviderRepo, buildConfigProvider)

    @Provides
    @Singleton
    fun provideDeviceUtilRepo(
        @ApplicationContext context: Context,
        buildProviderRepo: BuildProviderRepo
    ): DeviceUtilRepo =
        DeviceUtilRepoImpl(context, buildProviderRepo)

    @Provides
    @Singleton
    fun provideEventTimerLogRepo(
        @ApplicationContext context: Context
    ): EventTimerLogRepo =
        EventTimerLogRepoImpl(context)

    @Provides
    @Singleton
    fun provideLoadRenderMetrics(
        eventTimerLogRepo: EventTimerLogRepo
    ): LoadRenderMetrics =
        LoadRenderMetricsImpl(eventTimerLogRepo)
}
