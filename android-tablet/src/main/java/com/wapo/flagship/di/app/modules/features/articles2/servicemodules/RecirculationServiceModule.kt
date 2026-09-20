package com.wapo.flagship.di.app.modules.features.articles2.servicemodules

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.wapo.flagship.features.articles2.services.RecirculationService
import com.washingtonpost.android.config.domain.manager.ConfigManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.CallAdapter
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.Date
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object RecirculationServiceModule {
    @Singleton
    @Provides
    fun provideRecirculationService(
        okHttpClient: OkHttpClient,
        callAdapterFactory: CallAdapter.Factory,
        configManager: ConfigManager
    ): RecirculationService {
        val moshi = Moshi
            .Builder()
            .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
            .add(KotlinJsonAdapterFactory())
            .build()
        return Retrofit
            .Builder()
            .baseUrl(configManager.config.autoRecircConfig.baseUrl)
            .addCallAdapterFactory(callAdapterFactory)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()
            .create(RecirculationService::class.java)
    }
}