package com.wapo.flagship.di.app.modules.features.video

import com.wapo.flagship.features.video.service.VerticalVideoService
import com.wapo.flagship.network.retrofit.network.CallAdapterFactory
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.NextVideoConfig
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
object VerticalVideoServiceModule {
    @Singleton
    @Provides
    fun provideVerticalVideoService(
        okHttpClient: OkHttpClient,
        nextVideoConfig: NextVideoConfig
    ): VerticalVideoService {

        return Retrofit
            .Builder()
            .baseUrl(nextVideoConfig.url)
            .addCallAdapterFactory(CallAdapterFactory())
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(VerticalVideoService::class.java)
    }

    @Singleton
    @Provides
    fun provideNextVideoConfig(): NextVideoConfig {
        return ConfigManager.getInstance().config.nextVideoConfig
    }
}