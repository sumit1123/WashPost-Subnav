package com.wapo.flagship.di.app.modules.features.purchasedarticles

import com.google.gson.GsonBuilder
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.wapo.flagship.features.purchasedarticles.service.PurchasedArticlesService
import com.wapo.flagship.network.retrofit.network.CallAdapterFactory
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.PurchasedArticleConfig
import com.washingtonpost.android.save.network.SavedRetrofit
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object PurchasedArticlesServiceModule {

    private const val PAGE_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

    @Singleton
    @Provides
    fun providePurchasedArticlesService(
        okHttpClient: OkHttpClient,
        payPerArticleConfig: PurchasedArticleConfig
    ): PurchasedArticlesService {
        return Retrofit
            .Builder()
            .baseUrl(payPerArticleConfig.baseUrl)
            .addCallAdapterFactory(CallAdapterFactory())
            .addConverterFactory(MoshiConverterFactory.create(
                Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            ))
            .client(okHttpClient)
            .build().newBuilder().build()
            .create(PurchasedArticlesService::class.java)
    }

    @Singleton
    @Provides
    fun getMetadataNetwork(
        okHttpClient: OkHttpClient,
        payPerArticleConfig: PurchasedArticleConfig
    ): SavedRetrofit.MetadataNetwork {
        val converterFactory = GsonConverterFactory.create(
            GsonBuilder()
                .setDateFormat(PAGE_DATE_FORMAT).create()
        )
        return Retrofit.Builder()
            .baseUrl(payPerArticleConfig.metadataServiceBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(converterFactory)
            .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
            .build()
            .create(SavedRetrofit.MetadataNetwork::class.java)
    }

    @Singleton
    @Provides
    fun providePurchasedArticleConfigConfig(): PurchasedArticleConfig {
        return ConfigManager.getInstance().config.purchasedArticleConfig
    }
}