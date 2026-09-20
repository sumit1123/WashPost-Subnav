/* Copyright (c) 2024 The Washington Post. All rights reserved. */

package com.wapo.flagship.di.app.modules.features.foryou

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.wapo.android.commons.retrofit.DefaultHeadersInterceptor
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.userhistory.network.CallAdapterFactory
import com.washingtonpost.userhistory.remote.UserHistoryService
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
object UserHistoryServiceModule {

    @Singleton
    @Provides
    fun provideUserHistoryService(
        okHttpClient: OkHttpClient
    ): UserHistoryService {
        val url = getUrl()
        val moshi = getMoshi()
        val httpClient = okHttpClient
            .newBuilder()
            .addInterceptor(DefaultHeadersInterceptor())
            .build()
        return Retrofit.Builder().baseUrl(url)
            .addCallAdapterFactory(CallAdapterFactory())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(httpClient).build().create(UserHistoryService::class.java)
    }

    private fun getMoshi(): Moshi {
        return Moshi.Builder()
            .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private fun getUrl(): String {
        return ConfigManager.getInstance().config.userHistoryServiceConfig.url
    }
}
