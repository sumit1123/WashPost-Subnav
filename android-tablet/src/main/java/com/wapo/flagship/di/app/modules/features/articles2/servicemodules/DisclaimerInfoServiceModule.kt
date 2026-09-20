package com.wapo.flagship.di.app.modules.features.articles2.servicemodules

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.wapo.android.commons.retrofit.DefaultHeadersInterceptor
import com.wapo.flagship.features.articles2.services.DisclaimerInfoService
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
object DisclaimerInfoServiceModule {

    private val disclaimerInfoUrl get() = com.washingtonpost.android.config.domain.manager.ConfigManager.getInstance().config.disclaimerBaseUrl

    @Singleton
    @Provides
    fun provideDisclaimerInfoService(
        okHttpClient: OkHttpClient,
        callAdapterFactory: CallAdapter.Factory,
    ): DisclaimerInfoService {
        val moshi = getMoshi()
        return Retrofit
            .Builder()
            .baseUrl(disclaimerInfoUrl)
            .addCallAdapterFactory(callAdapterFactory)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()
            .create(DisclaimerInfoService::class.java)
    }

    private fun getMoshi(): Moshi {
        return Moshi.Builder()
            .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
            .add(KotlinJsonAdapterFactory())
            .build()
    }
}