package com.washingtonpost.android.follow.network

import com.google.gson.GsonBuilder
import com.wapo.android.commons.retrofit.DefaultHeadersInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.POST

/**
 * A repository for follow author sender flow.
 * Follow services contains 2 services {follow & un-follow} Services
 */
class FollowAuthorRepo private constructor() {
    private var followAuthorService: FollowAuthorService? = null
    private val shouldEnableNetworkDebugging =
        false //enable to verify each call with headers and response code
    private val interceptor: HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        this.level = HttpLoggingInterceptor.Level.BODY
    }
    private val client: OkHttpClient = OkHttpClient.Builder().apply {
        addInterceptor(DefaultHeadersInterceptor())
        if (shouldEnableNetworkDebugging) this.addInterceptor(interceptor)
    }.cache(null).build()

    fun geAuthorServiceNetwork(baseUrl: String): FollowAuthorService {
        if (followAuthorService == null) {
            val converterFactory = GsonConverterFactory.create(
                GsonBuilder()
                    .setDateFormat(PAGE_DATE_FORMAT).create()
            )
            followAuthorService = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(converterFactory)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build()
                .create<FollowAuthorService>(FollowAuthorService::class.java)
        }
        return followAuthorService!!
    }

// Need to change the services once the prod end points are ready
    interface FollowAuthorService {
        @POST("/preferenceapi/v1/current/followable/AUTHORS/follow")
        fun followAuthor(
            @HeaderMap headers: HashMap<String, String>,
            @Body followAuthorSenderRequestBody: FollowAuthor
        ): Call<FollowAuthor>

        @POST("/preferenceapi/v1/current/followable/AUTHORS/unfollow")
        fun unFollowAuthor(
            @HeaderMap headers: HashMap<String, String>,
            @Body followAuthorSenderRequestBody: FollowAuthor
        ): Call<FollowAuthor>

        @GET("/followservice/v1/current/authors/list")
        fun syncAuthorMetaFromRemote(
            @HeaderMap headers: HashMap<String, String>
        ) : Call<AuthorMetaDataFromRemote>
    }

    companion object {
        @Volatile
        private var INSTANCE: FollowAuthorRepo? = null
        private val TAG: String = FollowAuthorRepo::class.java.simpleName
        private const val PAGE_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

        @JvmStatic
        fun getInstance(): FollowAuthorRepo =
            INSTANCE ?: synchronized(this) {
                INSTANCE
                    ?: FollowAuthorRepo().also {
                        INSTANCE = it
                    }
            }
    }
}