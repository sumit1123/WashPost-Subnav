// Copyright (c) 2024 The Washington Post. All rights reserved.
package com.wapo.flagship.di.app.modules.features.aixp.servicemodules

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.wapo.android.commons.retrofit.DefaultHeadersInterceptor
import com.wapo.flagship.features.aixp.network.CallAdapterFactory
import com.wapo.flagship.features.ask.cache.AskQuestionsCache
import com.wapo.flagship.features.ask.models.AskQuestionsResponse
import com.wapo.flagship.features.ask.services.AskQuestionsService
import com.washingtonpost.android.config.domain.manager.ConfigManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.Date
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object AskServiceModule {
    @Singleton
    @Provides
    fun provideAskQuestionsService(okHttpClient: OkHttpClient): AskQuestionsService {
        val url = getUrl()
        val moshi = getMoshi()
        val httpClient =
            okHttpClient
                .newBuilder()
                .addInterceptor(DefaultHeadersInterceptor())
                .build()
        return Retrofit
            .Builder()
            .baseUrl(url)
            .addCallAdapterFactory(CallAdapterFactory())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(httpClient)
            .build()
            .create(AskQuestionsService::class.java)
    }

    @Singleton
    @Provides
    fun provideAskQuestionsCache(@ApplicationContext applicationContext: Context): AskQuestionsCache {
        val moshi = getMoshi()
        val jsonAdapter = moshi.adapter(AskQuestionsResponse::class.java)
        return AskQuestionsCache(
            applicationContext,
            jsonAdapter,
            5,
        )
    }

    // We use moshi to both network and cache
    private fun getMoshi(): Moshi =
        Moshi
            .Builder()
            .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
            .add(KotlinJsonAdapterFactory())
            .build()

    private fun getUrl(): String =
        ConfigManager
            .getInstance()
            .config
            .search2Config.askThePost
            .questionsBaseUrl
}
