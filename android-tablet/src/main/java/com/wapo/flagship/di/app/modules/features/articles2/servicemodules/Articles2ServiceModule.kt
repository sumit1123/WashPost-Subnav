package com.wapo.flagship.di.app.modules.features.articles2.servicemodules

import com.wapo.flagship.features.articles2.services.Articles2Service
import com.wapo.flagship.features.articles2.typeconverters.MoshiAdapters
import com.wapo.flagship.features.articles2.utils.getUrlWithoutParameters
import com.washingtonpost.android.BuildConfig
import com.washingtonpost.android.config.domain.manager.ConfigManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.CallAdapter
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

/**
 * This module provides all of the required services for [Articles2ActivityModule]
 * As of now we are using only [Articles2Service]
 */
@InstallIn(SingletonComponent::class)
@Module
object Articles2ServiceModule {
    @Singleton
    @Provides
    internal fun provideArticles2Service(
        okHttpClient: OkHttpClient,
        callAdapterFactory: CallAdapter.Factory,
        articleMainMoshiAdapters: MoshiAdapters,
        configManager: ConfigManager,
    ): Articles2Service {
        val url: String = getUrl(configManager)

        return Retrofit
            .Builder()
            .baseUrl(url)
            .addCallAdapterFactory(callAdapterFactory)
            .addConverterFactory(MoshiConverterFactory.create(articleMainMoshiAdapters.moshi))
            .client(okHttpClient)
            .build()
            .newBuilder()
            .build()
            .create(Articles2Service::class.java)
    }

    /**
     * Gets the URL from the currently set config.
     */
    private fun getUrl(configManager: ConfigManager): String {
        val urlTemplate = configManager.config.getUrlTemplate(false)
        return try {
            getUrlWithoutParameters(
                urlTemplate.replace(PLACEHOLDER, ""),
            ).replace(REMOVABLE_URL_PATH, "")
        } catch (ex: Exception) {
            // Default BASE_URL is used because exception was thrown while reading the config file.
            BuildConfig.BASE_URL
        }
    }

    const val PLACEHOLDER = "%s"
    const val REMOVABLE_URL_PATH = "content-by-url.json"
}
