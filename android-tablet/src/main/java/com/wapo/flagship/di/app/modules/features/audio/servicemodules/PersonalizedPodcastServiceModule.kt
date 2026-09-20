package com.wapo.flagship.di.app.modules.features.audio.servicemodules

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.wapo.android.commons.retrofit.DefaultHeadersInterceptor
import com.wapo.flagship.features.personalizedpodcasts.service.PersonalizedPodcastService
import com.wapo.flagship.features.utils.MockResponseInterceptor
import com.washingtonpost.android.BuildConfig
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.PersonalizedPodcastConfig
import com.washingtonpost.userhistory.network.CallAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object PersonalizedPodcastServiceModule {

    private val client : OkHttpClient = OkHttpClient.Builder().apply {
        connectTimeout(45, TimeUnit.SECONDS)
        writeTimeout(45, TimeUnit.SECONDS)
        readTimeout(45, TimeUnit.SECONDS)
        addInterceptor(DefaultHeadersInterceptor())
            .also {
                if (BuildConfig.DEBUG) {
                    it.addInterceptor(
                        HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY),
                    )
                    it.addInterceptor(MockResponseInterceptor())
                }
            }
    }.build()

    @Singleton
    @Provides
    fun providePersonalizedPodcastService(
        personalizedPodcastConfig: PersonalizedPodcastConfig
    ): PersonalizedPodcastService {
        return Retrofit.Builder().baseUrl(personalizedPodcastConfig.baseUrl)
            .addCallAdapterFactory(CallAdapterFactory())
            .addConverterFactory(
                MoshiConverterFactory.create(
                    Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build(),
                ),
            ).client(client)
            .build()
            .create(PersonalizedPodcastService::class.java)
    }

    @Singleton
    @Provides
    fun providePersonalizedPodcastConfig(): PersonalizedPodcastConfig {
        return ConfigManager.getInstance().config.personalizedPodcastConfig
    }

    private fun getMoshi(): Moshi {
        return Moshi.Builder()
            .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
            .add(KotlinJsonAdapterFactory())
            .build()
    }
}