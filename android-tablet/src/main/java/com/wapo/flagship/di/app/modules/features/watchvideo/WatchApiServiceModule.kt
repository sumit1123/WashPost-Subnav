package com.wapo.flagship.di.app.modules.features.watchvideo

import com.wapo.android.commons.retrofit.DefaultHeadersInterceptor
import com.wapo.flagship.network.request.WatchApiService
import com.washingtonpost.android.config.domain.manager.ConfigManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object WatchApiServiceModule {
    @Singleton
    @Provides
    fun provideWatchApiService(okHttpClient: OkHttpClient): WatchApiService {
        val httpClient =
            okHttpClient
                .newBuilder()
                .addInterceptor(DefaultHeadersInterceptor())
                .build()
        return Retrofit.Builder()
            .baseUrl(ConfigManager.getInstance().config.wpVideosConfig.baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()
            .create(WatchApiService::class.java)
    }
}