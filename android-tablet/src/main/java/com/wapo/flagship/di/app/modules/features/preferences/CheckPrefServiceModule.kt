/* Copyright (c) 2024 The Washington Post. All rights reserved. */

package com.wapo.flagship.di.app.modules.features.preferences

import com.wapo.flagship.features.preferencesapi.services.CheckPrefService
import com.washingtonpost.android.config.domain.manager.ConfigManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.CallAdapter
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object CheckPrefServiceModule {
    @Singleton
    @Provides
    internal fun provideCheckPrefServiceModule(
        okHttpClient: OkHttpClient,
        callAdapterFactory: CallAdapter.Factory
    ): CheckPrefService {
        val url: String = ConfigManager.getInstance().config.preferencesApiConfig.checkPrefBaseUrl

        return Retrofit.Builder().baseUrl(url)
            .addCallAdapterFactory(callAdapterFactory)
            .addConverterFactory(MoshiConverterFactory.create())
            .client(okHttpClient).build().create(CheckPrefService::class.java)
    }
}
