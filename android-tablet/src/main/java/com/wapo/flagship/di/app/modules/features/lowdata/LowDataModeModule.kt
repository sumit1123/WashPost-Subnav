package com.wapo.flagship.di.app.modules.features.lowdata

import android.content.Context
import com.wapo.flagship.features.lowdata.LowDataModeDataStore
import com.wapo.flagship.features.lowdata.LowDataModeDataStoreImpl
import com.wapo.flagship.features.lowdata.LowDataModeNotificationImpl
import com.wapo.flagship.features.lowdata.LowDataModeRepo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@InstallIn(SingletonComponent::class)
@Module
object LowDataModeModule {

    @Singleton
    @Provides
    fun provideLowDataModeRepo(
        lowDataModeDataStore: LowDataModeDataStore,
    ): LowDataModeRepo =
        LowDataModeNotificationImpl(lowDataModeDataStore)

    @Singleton
    @Provides
    fun provideLowDataModeDataStore(@ApplicationContext context: Context): LowDataModeDataStore =
        LowDataModeDataStoreImpl(context)
}