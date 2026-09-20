package com.wapo.flagship.features.comments.servicemodule

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.wapo.flagship.features.articles2.utils.getBaseUrl
import com.wapo.flagship.features.comments.service.CommentsService
import com.wapo.flagship.network.retrofit.network.CallAdapterFactory
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.CommentsConfig
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
object CommentsServiceModule {
    @Singleton
    @Provides
    internal fun provideCommentsService(
        okHttpClient: OkHttpClient,
        commentsConfig: CommentsConfig?
    ): CommentsService {
        val url: String = getBaseUrl(commentsConfig)
        return Retrofit.Builder()
            .baseUrl(url)
            .addCallAdapterFactory(CallAdapterFactory())
            .addConverterFactory(
                MoshiConverterFactory.create(
                    Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                )
            )
            .client(okHttpClient)
            .build().newBuilder().build()
            .create(CommentsService::class.java)
    }

    @Singleton
    @Provides
    fun provideCommentsRemoteConfig(): CommentsConfig {
        val config = ConfigManager.getInstance().config.commentsConfig
        return CommentsConfig(
            url = config.url
        )
    }


    private fun getDefaultBaseUrl(): String = "https://tabletapi.washingtonpost.com/"

    private fun getBaseUrl(commentsConfig: CommentsConfig?): String {
        val baseUrl = commentsConfig?.url?.let { getBaseUrl(it) }
        return if (!baseUrl.isNullOrEmpty()) baseUrl else getDefaultBaseUrl()
    }
}
