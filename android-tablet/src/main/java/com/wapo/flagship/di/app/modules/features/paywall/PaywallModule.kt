package com.wapo.flagship.di.app.modules.features.paywall

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.wapo.android.commons.di.CoroutineScopeCommonsModule
import com.wapo.android.commons.retrofit.DefaultHeadersInterceptor
import com.wapo.flagship.features.utils.MockResponseInterceptor
import com.washingtonpost.android.BuildConfig
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.api.NonceApiService
import com.washingtonpost.android.paywall.api.NonceRepository
import com.washingtonpost.android.paywall.api.NonceRepositoryImpl
import com.washingtonpost.android.paywall.auth.AuthStateManager
import com.washingtonpost.android.paywall.network.retrofit.CallAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PaywallModule {
    private val client : OkHttpClient = OkHttpClient.Builder().apply {
        connectTimeout(2, TimeUnit.SECONDS)
        writeTimeout(45, TimeUnit.SECONDS)
        readTimeout(45, TimeUnit.SECONDS)
        addInterceptor(DefaultHeadersInterceptor())
            .also {
                if (BuildConfig.DEBUG) {
                    it.addInterceptor(
                        HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY),
                    )
                    it.addInterceptor(MockResponseInterceptor())
                }
            }
    }.build()

    @Singleton
    @Provides
    fun providesNonceApiService(): NonceApiService {
        return Retrofit.Builder()
            // The endpoint comes from a remote config, so use localhost as a stub and
            // pass the full url to the service call function at runtime.
            .baseUrl("http://localhost/")
            .addCallAdapterFactory(CallAdapterFactory())
            .addConverterFactory(
                MoshiConverterFactory.create(
                    Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build(),
                ),
            ).client(client)
            .build()
            .create(NonceApiService::class.java)
    }

    @Singleton
    @Provides
    fun providesNonceRepository(
        nonceApi: NonceApiService,
        @ApplicationContext appContext: Context,
        @CoroutineScopeCommonsModule.IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): NonceRepository {
        return NonceRepositoryImpl(
            nonceApi = nonceApi,
            paywallService = PaywallService.getInstance(),
            authStateManager = AuthStateManager.getInstance(appContext),
            paywallConnector = PaywallService.getConnector(),
            ioDispatcher = ioDispatcher,
        )
    }

}