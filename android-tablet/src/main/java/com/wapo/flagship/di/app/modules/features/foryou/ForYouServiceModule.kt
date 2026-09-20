package com.wapo.flagship.di.app.modules.features.foryou

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.wapo.android.commons.retrofit.DefaultHeadersInterceptor
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.foryou.cache.Cache
import com.washingtonpost.foryou.cache.HabitTilesCacheImpl
import com.washingtonpost.foryou.domain.HabitTilesCache
import com.washingtonpost.foryou.cache.WidgetCache
import com.washingtonpost.foryou.data.HabitTilesResponse
import com.washingtonpost.foryou.data.RemoteConfig
import com.washingtonpost.foryou.network.CallAdapterFactory
import com.washingtonpost.foryou.network.ForYouFlexApiHeaders
import com.washingtonpost.foryou.remote.ForYouService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.Date
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object ForYouServiceModule {
    private val forYouFlexConfig get() = ConfigManager.getInstance().config.forYouFlexConfig
    private val forYouWidgetConfig get() = ConfigManager.getInstance().config.forYouWidgetConfig

    @Singleton
    @Provides
    fun provideForYouService(okHttpClient: OkHttpClient): ForYouService {
        val url = getUrl()
        val moshi = getMoshi()
        val httpClient =
            okHttpClient
                .newBuilder()
                .addInterceptor(DefaultHeadersInterceptor())
                .addInterceptor(ForYouFlexApiHeaders())
                .build()
        return Retrofit
            .Builder()
            .baseUrl(url)
            .addCallAdapterFactory(CallAdapterFactory())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(httpClient)
            .build()
            .create(ForYouService::class.java)
    }

    @Singleton
    @Provides
    fun provideForYouCache(@ApplicationContext applicationContext: Context): Cache {
        val moshi = getMoshi()
        val ttl = forYouFlexConfig.ttl
        val checkReadList = forYouFlexConfig.checkReadList
        val maxSize = forYouFlexConfig.maxSize
        return Cache(applicationContext, moshi, RemoteConfig(ttl, checkReadList, maxSize))
    }

    @Singleton
    @Provides
    fun provideForYouWidgetCache(@ApplicationContext applicationContext: Context): WidgetCache {
        val moshi = getMoshi()
        val ttls = forYouWidgetConfig.ttls
        val maxSize = forYouWidgetConfig.maxSize
        val pageSize = forYouWidgetConfig.pageSize
        return WidgetCache(
            applicationContext,
            moshi,
            RemoteConfig(ttls, false, maxSize, pageSize)
        )
    }

    @Singleton
    @Provides
    fun provideHabitTilesCache(@ApplicationContext applicationContext: Context): HabitTilesCache {
        val moshi = getMoshi()
        val ttl = forYouFlexConfig.ttl
        val checkReadList = forYouFlexConfig.checkReadList
        val maxSize = forYouFlexConfig.maxSize
        val jsonAdapter = moshi.adapter(HabitTilesResponse::class.java)
        return HabitTilesCacheImpl(
            applicationContext,
            jsonAdapter,
            RemoteConfig(ttl, checkReadList, maxSize),
        )
    }

    // we use moshi to both network and cache
    private fun getMoshi(): Moshi =
        Moshi
            .Builder()
            .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
            .add(KotlinJsonAdapterFactory())
            .build()

    private fun getUrl(): String = forYouFlexConfig.url
}
