package com.washingtonpost.android.save.network

import com.google.gson.GsonBuilder
import com.wapo.android.commons.retrofit.DefaultHeadersInterceptor
import com.wapo.networkutils.interceptors.AuthenticationInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

class SavedRetrofit private constructor() {
    private var metadataNetwork: MetadataNetwork? = null
    private var preferenceNetwork: PreferenceNetwork? = null
    private val shouldEnableNetworkDebugging = false //enable to verify each call with headers and response code
    private val interceptor : HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        this.level = HttpLoggingInterceptor.Level.BODY
    }
    private val client : OkHttpClient = OkHttpClient.Builder().apply {
        addInterceptor(DefaultHeadersInterceptor())
        addInterceptor(AuthenticationInterceptor(TAG))
        if (shouldEnableNetworkDebugging) this.addInterceptor(interceptor)
    }.cache(null).build()

    fun getMetadataNetwork(baseUrl: String): MetadataNetwork {
        if (metadataNetwork == null) {
            val converterFactory = GsonConverterFactory.create(GsonBuilder()
                    .setDateFormat(PAGE_DATE_FORMAT).create())
            metadataNetwork = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(converterFactory)
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .build()
                    .create<MetadataNetwork>(MetadataNetwork::class.java)
        }
        return metadataNetwork!!
    }

    fun getPreferenceNetwork(baseUrl: String): PreferenceNetwork {
        if (preferenceNetwork == null) {
            val converterFactory = GsonConverterFactory.create(GsonBuilder().create())
            preferenceNetwork = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(converterFactory)
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .build()
                    .create<PreferenceNetwork>(PreferenceNetwork::class.java)
        }
        return preferenceNetwork!!
    }

    interface MetadataNetwork {
        @POST("collection")
        fun getArticleMetadata(@Body metadataRequest: MetadataRequest): Call<Metadata>
    }

    interface PreferenceNetwork {
        @POST("list")
        fun getSavedArticlesList(@HeaderMap headerMap: HashMap<String, String>, @Body savedStoriesRequest: SavedStoriesRequest): Call<SavedStoriesResponse>
        @POST("save")
        fun saveArticles(@HeaderMap headerMap: HashMap<String, String>, @Body savedStoriesRequest: SavedStoriesRequest): Call<SavedStoriesResponse>
        @POST("delete")
        fun deleteArticles(@HeaderMap headerMap: HashMap<String, String>, @Body savedStoriesRequest: SavedStoriesRequest): Call<SavedStoriesResponse>
    }

    companion object {
        @Volatile
        private var INSTANCE: SavedRetrofit? = null
        private val TAG: String = SavedRetrofit::class.java.simpleName
        private const val PAGE_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

        @JvmStatic
        fun getInstance(): SavedRetrofit =
                INSTANCE ?: synchronized(this) {
                    INSTANCE
                            ?: SavedRetrofit().also {
                        INSTANCE = it
                    }
                }
    }
}