/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.di

import com.wapo.flagship.WearAppContext
import com.wapo.flagship.features.articles2.services.Articles2Service
import com.wapo.flagship.features.articles2.typeconverters.MoshiAdapters
import com.wapo.flagship.features.section.services.FusionSectionService
import com.wapo.flagship.features.section.services.PageBuilderSectionService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.CallAdapter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiServiceModule {

    @Provides
    @Singleton
    fun provideFusionSectionService(
        callAdapterFactory: CallAdapter.Factory,
        okHttpClient: OkHttpClient
    ): FusionSectionService {
        val jsonAppBaseUrl: String = getJsonAppBaseUrl()

        return Retrofit.Builder()
            .baseUrl(jsonAppBaseUrl)
            .addCallAdapterFactory(callAdapterFactory)
            .addConverterFactory(
                GsonConverterFactory.create(
                    com.wapo.flagship.features.grid.FusionMapper.gson()
                )
            )
            .client(okHttpClient)
            .build()
            .create(FusionSectionService::class.java)
    }

    @Provides
    @Singleton
    fun providePageBuilderSectionService(
        callAdapterFactory: CallAdapter.Factory,
        okHttpClient: OkHttpClient
    ): PageBuilderSectionService {
        val baseUrl: String = getJsonAppBaseUrl()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addCallAdapterFactory(callAdapterFactory)
            .addConverterFactory(
                GsonConverterFactory.create(
                    com.wapo.flagship.features.sections.model.Mapper.gson
                )
            )
            .client(okHttpClient)
            .build()
            .create(PageBuilderSectionService::class.java)
    }

    @Provides
    @Singleton
    fun provideArticles2Service(
        callAdapterFactory: CallAdapter.Factory,
        articleMainMoshiAdapters: MoshiAdapters,
        okHttpClient: OkHttpClient
    ): Articles2Service {
        val baseUrl: String = getSingleNativeContentRainbowBaseUrl()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addCallAdapterFactory(callAdapterFactory)
            .addConverterFactory(
                MoshiConverterFactory.create(articleMainMoshiAdapters.moshi)
            )
            .client(okHttpClient)
            .build().newBuilder().build()
            .create(Articles2Service::class.java)
    }

    /**
     * Gets the URL from the currently set config.
     */
    private fun getJsonAppBaseUrl(): String {
        return WearAppContext.config().jsonAppBaseUrl!!
    }

    private fun getSingleNativeContentRainbowBaseUrl(): String {
        return WearAppContext.config().singleNativeContentRainbowBaseUrl!!
    }

    private fun getArcIoBaseUrl(): String {
        return WearAppContext.config().arcIoBaseUrl!!
    }

}