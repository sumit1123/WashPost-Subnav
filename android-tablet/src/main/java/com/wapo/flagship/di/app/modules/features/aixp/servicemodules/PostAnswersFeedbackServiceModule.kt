/* Copyright (c) 2024 The Washington Post. All rights reserved. */

package com.wapo.flagship.di.app.modules.features.aixp.servicemodules

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.wapo.flagship.features.aixp.network.CallAdapterFactory
import com.wapo.flagship.features.aixp.services.PostAnswersFeedbackService
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
object PostAnswersFeedbackServiceModule {
    @Singleton
    @Provides
    internal fun provideArticleSummaryService(
        okHttpClient: OkHttpClient
    ): PostAnswersFeedbackService {
        return Retrofit.Builder()
            // The endpoint comes from the post answers service, so use localhost as a stub and
            // pass the full url to the service call function at runtime.
            .baseUrl("http://localhost/")
            .addCallAdapterFactory(CallAdapterFactory())
            .addConverterFactory(
                MoshiConverterFactory.create(
                    Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                )
            )
            .client(okHttpClient).build().newBuilder().build()
            .create(PostAnswersFeedbackService::class.java)
    }
}
