/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.di

import com.wapo.flagship.utils.network.*
import com.washingtonpost.android.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OkHttpModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val clientBuilder = OkHttpBuilder.getOkHttpClientBuilder()
        return clientBuilder
            .addInterceptor(DefaultHeadersInterceptor())
            .addInterceptor(TimeoutInterceptor())
            .addInterceptor(CacheHeadersInterceptor())
            .also {
//                if (BuildConfig.DEBUG) {
                    it.addInterceptor(HttpLoggingInterceptor()
                        .setLevel(HttpLoggingInterceptor.Level.BODY))
//                }
            }
            .build()
    }

}