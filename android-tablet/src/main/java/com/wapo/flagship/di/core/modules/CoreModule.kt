package com.wapo.flagship.di.core.modules

import android.content.Context
import com.captechconsulting.captechbuzz.model.images.BitmapLruImageCache
import com.wapo.android.commons.util.DeviceUtils
import com.wapo.android.commons.util.Logger
import com.wapo.android.domain.repository.RemoteLogRepo
import com.wapo.flagship.content.ContentManager
import com.wapo.flagship.content.ContentUpdateRulesManager
import com.wapo.flagship.content.ContentUpdateRulesManagerImpl
import com.wapo.flagship.content.PageImageLoader
import com.wapo.flagship.data.CacheManager
import com.wapo.flagship.features.articles2.repo.Articles2Repository
import com.wapo.flagship.features.ask.repo.AskQuestionsRepo
import com.wapo.flagship.features.ask.repo.AskQuestionsRepoImpl
import com.wapo.flagship.network.HurlStackDispatcher
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.volley.RequestQueue
import com.washingtonpost.android.volley.VolleyError
import com.washingtonpost.android.volley.toolbox.AnimatedImageLoader
import com.washingtonpost.android.volley.toolbox.BasicNetwork
import com.washingtonpost.android.volley.toolbox.GlobalImageListener
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.math.max

@InstallIn(SingletonComponent::class)
@Module
object CoreModule {

    @Provides
    @Singleton
    fun provideRequestQueue(cacheManager: CacheManager): RequestQueue {
        val threadPoolSize = max(4, DeviceUtils.getNumberOfCores())
        return RequestQueue(
            cacheManager,
            BasicNetwork(HurlStackDispatcher()),
            threadPoolSize,
        ).apply {
            start()
        }
    }

    @Provides
    @Singleton
    fun provideAnimatedImageLoader(requestQueue: RequestQueue): AnimatedImageLoader {
        val maxMemory: Int = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8
        val bitmapImageCache = BitmapLruImageCache(cacheSize)
        val globalImageListener: GlobalImageListener =
            object : GlobalImageListener {
                override fun onResponse(
                    data: Any?,
                    requestUrl: String?,
                    isImmediate: Boolean,
                ) {
                }

                override fun onErrorResponse(
                    requestUrl: String,
                    error: VolleyError,
                ) {
                    Logger.e("provideAnimatedImageLoader", "Failed to download the image for URL $requestUrl", error)
                }
            }

        return AnimatedImageLoader(requestQueue, bitmapImageCache, globalImageListener)
    }

    @Provides
    @Singleton
    fun provideContentUpdateRulesManager(
        @ApplicationContext applicationContext: Context
    ): ContentUpdateRulesManager =
        ContentUpdateRulesManagerImpl(
            applicationContext,
            ConfigManager.getInstance().config.contentUpdateRulesConfig,
        )

    @Provides
    @Singleton
    fun provideContentManager(
        @ApplicationContext applicationContext: Context,
        cacheManager: CacheManager,
        requestQueue: RequestQueue,
        animatedImageLoader: AnimatedImageLoader,
        contentUpdateRulesManager: ContentUpdateRulesManager,
        articles2Repository: Articles2Repository,
        askQuestionsRepo: AskQuestionsRepo,
        remoteLogRepo: RemoteLogRepo
    ): ContentManager =
        ContentManager(
            applicationContext,
            cacheManager,
            requestQueue,
            PageImageLoader(animatedImageLoader),
            contentUpdateRulesManager,
            articles2Repository,
            askQuestionsRepo,
            remoteLogRepo
        )
}
