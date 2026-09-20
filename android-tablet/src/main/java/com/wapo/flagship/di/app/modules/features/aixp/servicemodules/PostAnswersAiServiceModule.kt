package com.wapo.flagship.di.app.modules.features.aixp.servicemodules

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.wapo.flagship.features.aixp.services.PostAnswersAiService
import com.washingtonpost.android.config.domain.manager.ConfigManager
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
object PostAnswersAiServiceModule {
    @Singleton
    @Provides
    fun providePostAnswersAiService(okHttpClient: OkHttpClient): PostAnswersAiService {
        val baseUrl = ConfigManager.getInstance().config.search2Config.askThePost.converseBaseUrl

        return Retrofit
            .Builder()
            .baseUrl(baseUrl)
            .addCallAdapterFactory(com.wapo.flagship.features.aixp.network.CallAdapterFactory())
            .addConverterFactory(
                MoshiConverterFactory.create(
                    Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build(),
                ),
            ).client(okHttpClient)
            .build()
            .newBuilder()
            .build()
            .create(PostAnswersAiService::class.java)
    }
}