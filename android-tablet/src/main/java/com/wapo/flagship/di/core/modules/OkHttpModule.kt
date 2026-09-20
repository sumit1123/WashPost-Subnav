package com.wapo.flagship.di.core.modules

import android.content.Context
import com.wapo.android.commons.retrofit.DefaultHeadersInterceptor
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.flagship.util.network.CacheHeadersInterceptor
import com.wapo.flagship.util.network.MockResponseInterceptor
import com.wapo.flagship.util.network.OkHttpBuilder.getOkHttpClientBuilder
import com.wapo.flagship.util.network.TimeoutInterceptor
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Singleton

/**
 * This module provides the custom [OkHttpClient] for the retrofit module.
 * Why we need custom [OkHttpClient]
 */
@InstallIn(SingletonComponent::class)
@Module
object OkHttpModule {
    @Singleton
    @Provides
    fun provideOkHttpClient(@ApplicationContext applicationContext: Context): OkHttpClient {
        if (!AppContextUtils.isInitialized) {
            AppContextUtils.init(applicationContext, Measurement.detectAppName())
        }
        val clientBuilder = getOkHttpClientBuilder()
        /**
         * [HttpLoggingInterceptor] is used to print out the logs for debugging purposes.
         */
        return clientBuilder
            .addInterceptor(DefaultHeadersInterceptor())
            .addInterceptor(TimeoutInterceptor())
            .addInterceptor(CacheHeadersInterceptor())
            .also {
                if (BuildConfig.DEBUG) {
                    it.addInterceptor(
                        HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY),
                    )
                    it.addInterceptor(MockResponseInterceptor())
                }
            }.build()
    }
}
