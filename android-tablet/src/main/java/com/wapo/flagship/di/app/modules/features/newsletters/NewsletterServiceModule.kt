package com.wapo.flagship.di.app.modules.features.newsletters

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.wapo.flagship.features.newsletter.repo.remote.NewslettersService
import com.wapo.flagship.features.newsletter.repo.remote.adapters.NewslettersKeyAdapter
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
 * This module provides service for newsletters feature
 * As of now we are using only [NewslettersService]
 */
@InstallIn(SingletonComponent::class)
@Module
object NewsletterServiceModule {
    @Singleton
    @Provides
    internal fun provideNewsletterService(
        okHttpClient: OkHttpClient,
        callAdapterFactory: CallAdapter.Factory,
    ): NewslettersService {
        val url: String = ConfigManager.getInstance().config.paywallConfig.newsLettersBaseUrl

        val moshi =
            Moshi
                .Builder()
                .add(NewslettersKeyAdapter())
                .add(KotlinJsonAdapterFactory())
                .build()

        return Retrofit
            .Builder()
            .baseUrl(url)
            .addCallAdapterFactory(callAdapterFactory)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()
            .create(NewslettersService::class.java)
    }
}
