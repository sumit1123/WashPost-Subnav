// Copyright (c) 2025 The Washington Post. All rights reserved.
package com.wapo.flagship.di.app.modules.sdk.iterable

import android.content.Context
import com.squareup.moshi.Moshi
import com.wapo.android.commons.config.sec.helper.WapoSecDataProvider
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor
import com.wapo.flagship.sdk.iterable.IterableSdk
import com.wapo.flagship.util.coroutines.CoroutineScopeProvider
import com.washingtonpost.android.config.domain.manager.ConfigManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object IterableSdkModule {
    @Singleton
    @Provides
    fun provideIterableSdk(
        @ApplicationContext context: Context,
        coroutineScopeProvider: CoroutineScopeProvider,
        secureDataProvider: WapoSecDataProvider,
        moshi: Moshi
    ): IterableSdk {
        return IterableSdk(
            context,
            coroutineScopeProvider,
            secureDataProvider,
            ConfigManager.getInstance().config,
            DeepLinksProcessor,
            AppContextUtils,
            moshi
        )
    }
}
