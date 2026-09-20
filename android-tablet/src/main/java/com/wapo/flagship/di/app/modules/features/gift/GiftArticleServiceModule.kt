package com.wapo.flagship.di.app.modules.features.gift

import com.wapo.flagship.features.gifting.services.GiftArticleService
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
 * This module provides service for gift article feature
 * As of now we are using only [GiftArticleService]
 */
@InstallIn(SingletonComponent::class)
@Module
object GiftArticleServiceModule {
    @Singleton
    @Provides
    internal fun provideGiftArticleService(
        okHttpClient: OkHttpClient,
        callAdapterFactory: CallAdapter.Factory,
    ): GiftArticleService {
        val url: String = ConfigManager.getInstance().config.paywallConfig.subsBaseUrl

        return Retrofit
            .Builder()
            .baseUrl(url)
            .addCallAdapterFactory(callAdapterFactory)
            .addConverterFactory(MoshiConverterFactory.create())
            .client(okHttpClient)
            .build()
            .newBuilder()
            .build()
            .create(
                GiftArticleService::class.java,
            )
    }
}
