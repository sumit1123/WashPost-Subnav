package com.wapo.flagship.di.app.modules.features.preferences

import com.wapo.flagship.features.preferencesapi.services.PreferencesApiService
import com.wapo.networkutils.interceptors.AuthenticationInterceptor
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

/**
 * This module provides service for the preferences API
 */
@InstallIn(SingletonComponent::class)
@Module
object PreferenceServiceModule {
    @Singleton
    @Provides
    internal fun providePreferenceServiceModule(
        okHttpClient: OkHttpClient,
        callAdapterFactory: CallAdapter.Factory,
    ): PreferencesApiService {
        val httpClient = okHttpClient
            .newBuilder()
            .addInterceptor(AuthenticationInterceptor(PreferencesApiService::class.java.simpleName, listOf("config/my-post/content-pack.json")))
            .build()

        val url: String = ConfigManager.getInstance().config.paywallConfig.subsBaseUrl

        return Retrofit
            .Builder()
            .baseUrl(url)
            .addCallAdapterFactory(callAdapterFactory)
            .addConverterFactory(MoshiConverterFactory.create())
            .client(httpClient)
            .build()
            .newBuilder()
            .build()
            .create(PreferencesApiService::class.java)
    }
}
