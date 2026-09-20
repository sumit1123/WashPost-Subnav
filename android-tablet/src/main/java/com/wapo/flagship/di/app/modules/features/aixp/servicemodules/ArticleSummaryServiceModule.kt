// Copyright (c) 2024 The Washington Post. All rights reserved.
package com.wapo.flagship.di.app.modules.features.aixp.servicemodules

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.wapo.flagship.features.aixp.models.SummaryRemoteConfig
import com.wapo.flagship.features.aixp.network.CallAdapterFactory
import com.wapo.flagship.features.aixp.services.ArticleFeedbackService
import com.wapo.flagship.features.aixp.services.ArticleSummaryService
import com.wapo.flagship.features.articles2.utils.getBaseUrl
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.SummariesConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object ArticleSummaryServiceModule {

    @Singleton
    @Provides
    internal fun provideArticleSummaryService(
        okHttpClient: OkHttpClient,
        summariesConfig: SummariesConfig?
    ): ArticleSummaryService {
        val url: String = getBaseSummaryUrl(summariesConfig)
        return Retrofit
            .Builder()
            .baseUrl(url)
            .addCallAdapterFactory(CallAdapterFactory())
            .addConverterFactory(
                MoshiConverterFactory.create(
                    Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build(),
                ),
            ).client(okHttpClient)
            .build()
            .newBuilder()
            .build()
            .create(ArticleSummaryService::class.java)
    }

    @Singleton
    @Provides
    internal fun provideArticleFeedbackService(
        okHttpClient: OkHttpClient,
        summariesConfig: SummariesConfig?
    ): ArticleFeedbackService {
        val url: String = getBaseFeedbackUrl(summariesConfig)
        return Retrofit
            .Builder()
            .baseUrl(url)
            .addCallAdapterFactory(CallAdapterFactory())
            .addConverterFactory(
                MoshiConverterFactory.create(
                    Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build(),
                ),
            ).client(okHttpClient)
            .build()
            .newBuilder()
            .build()
            .create(ArticleFeedbackService::class.java)
    }

    @Singleton
    @Provides
    fun provideSummariesConfig(): SummariesConfig {
        return ConfigManager.getInstance().config.summariesConfig
    }

    @Singleton
    @Provides
    fun provideSummariesRemoteConfig(): SummaryRemoteConfig {
        val config = ConfigManager.getInstance().config.summariesConfig
        return SummaryRemoteConfig(
            config.realtimeSummary.url,
            config.realtimeFeedback.url
        )
    }

    private fun getDefaultBaseUrl(): String = "https://searchapi.washingtonpost.com/"

    private fun getBaseSummaryUrl(summariesConfig: SummariesConfig?): String {
        val baseUrl = summariesConfig?.realtimeSummary?.url?.let { getBaseUrl(it) }
        return if (!baseUrl.isNullOrEmpty()) baseUrl else getDefaultBaseUrl()
    }

    private fun getBaseFeedbackUrl(summariesConfig: SummariesConfig?): String {
        val baseUrl = summariesConfig?.realtimeFeedback?.url?.let { getBaseUrl(it) }
        return if (!baseUrl.isNullOrEmpty()) baseUrl else getDefaultBaseUrl()
    }
}
