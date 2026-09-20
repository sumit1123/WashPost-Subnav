package com.wapo.flagship.di.app.modules.features.search2


import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.wapo.android.commons.retrofit.DefaultHeadersInterceptor
import com.wapo.flagship.features.articles2.typeconverters.MoshiAdapters
import com.wapo.flagship.features.search2.model.AskThePostRequest
import com.wapo.flagship.features.search2.remote.OkHttpSseService
import com.wapo.flagship.features.search2.remote.PostAnswersService
import com.wapo.flagship.features.search2.remote.Search2Service
import com.wapo.flagship.features.search2.remote.SearchRecipeService
import com.wapo.flagship.util.network.CacheHeadersInterceptor
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

@InstallIn(SingletonComponent::class)
@Module
object Search2ServiceModule {
    @Singleton
    @Provides
    internal fun createSearchService(
        okHttpClient: OkHttpClient,
        callAdapterFactory: CallAdapter.Factory,
    ): Search2Service {
        val url: String = getUrl()
        return Retrofit
            .Builder()
            .baseUrl(url)
            .addCallAdapterFactory(callAdapterFactory)
            .addConverterFactory(MoshiConverterFactory.create())
            .client(okHttpClient)
            .build()
            .newBuilder()
            .build()
            .create(Search2Service::class.java)
    }

    @Singleton
    @Provides
    internal fun createRecipeSearchService(
        okHttpClient: OkHttpClient,
        callAdapterFactory: CallAdapter.Factory,
    ): SearchRecipeService =
        Retrofit
            .Builder()
            // stage and prod urls are very different, so we use localhost as a stub and
            // pass the full recipe url as a param
            .baseUrl("http://localhost/")
            .addCallAdapterFactory(callAdapterFactory)
            .addConverterFactory(MoshiConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(SearchRecipeService::class.java)

    @Singleton
    @Provides
    internal fun createPostAnswersService(
        okHttpClient: OkHttpClient,
        callAdapterFactory: CallAdapter.Factory,
        moshiAdapters: MoshiAdapters,
    ): PostAnswersService {
        val url: String = getUrl()
        return Retrofit
            .Builder()
            .baseUrl(url)
            .addCallAdapterFactory(callAdapterFactory)
            .addConverterFactory(MoshiConverterFactory.create(moshiAdapters.moshi))
            .client(okHttpClient)
            .build()
            .newBuilder()
            .build()
            .create(
                PostAnswersService::class.java,
            )
    }

    @Singleton
    @Provides
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    @Singleton
    @Provides
    fun provideRequestAdapter(moshi: Moshi): JsonAdapter<AskThePostRequest> {
        return moshi.adapter(AskThePostRequest::class.java)
    }

    @Singleton
    @Provides
    fun provideOkHttpSseService(okHttpClient: OkHttpClient, requestAdapter: JsonAdapter<AskThePostRequest>): OkHttpSseService {
        //need to look into passing a custom client instead of current setup
        val okHttpClientSse = OkHttpClient.Builder()
            .readTimeout(0, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor(DefaultHeadersInterceptor())
            .addInterceptor(CacheHeadersInterceptor())
            .build()
        return OkHttpSseService(okHttpClientSse, requestAdapter)
    }

    private fun getUrl(): String = ConfigManager.getInstance().config.search2Config.baseUrl
}
