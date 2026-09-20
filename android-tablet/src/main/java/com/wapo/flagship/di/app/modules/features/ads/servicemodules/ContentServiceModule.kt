// Copyright (c) 2025 The Washington Post. All rights reserved.
package com.wapo.flagship.di.app.modules.features.ads.servicemodules

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.wapo.flagship.features.ads.targeting.services.ContentService
import com.wapo.flagship.features.aixp.network.CallAdapterFactory
import com.wapo.flagship.features.articles2.utils.getBaseUrl
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.ContextualTargetingContent
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.Date
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object ContentServiceModule {
    @Singleton
    @Provides
    fun provideContentServiceModule(
        okHttpClient: OkHttpClient,
        config: ContextualTargetingContent?
    ): ContentService {
        val url = getBaseUrl(config)
        val moshi = getMoshi()
        return Retrofit
            .Builder()
            .baseUrl(url)
            .addCallAdapterFactory(CallAdapterFactory())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()
            .create(ContentService::class.java)
    }

    @Singleton
    @Provides
    fun provideContextualTargetingContent(): ContextualTargetingContent {
        return ConfigManager.getInstance().config.adsConfig.contextualTargeting.content
    }

    private fun getMoshi(): Moshi =
        Moshi
            .Builder()
            .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
            .add(KotlinJsonAdapterFactory())
            .build()

    private fun getDefaultBaseUrl(): String = "https://www.washingtonpost.com/"

    private fun getBaseUrl(config: ContextualTargetingContent?): String {
        val baseUrl = config?.url?.let { getBaseUrl(it) }
        return if (!baseUrl.isNullOrEmpty()) baseUrl else getDefaultBaseUrl()
    }
}