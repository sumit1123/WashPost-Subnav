// Copyright (c) 2026 The Washington Post. All rights reserved.
package com.wapo.flagship.di.app.modules.sdk.iterable

import android.content.Context
import com.wapo.android.commons.config.sec.helper.WapoSecDataProvider
import com.wapo.flagship.SecureAppData
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object SecureAppDataModule {
    @Singleton
    @Provides
    fun provideSecureAppDataProvider(
        @ApplicationContext context: Context,
    ): WapoSecDataProvider {
        return WapoSecDataProvider.also { provider ->
            SecureAppData.loadLibrary(context)
            SecureAppData.loadAppSecureData(provider)
        }
    }
}
