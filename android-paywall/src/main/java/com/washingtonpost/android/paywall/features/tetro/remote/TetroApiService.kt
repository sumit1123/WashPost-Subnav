package com.washingtonpost.android.paywall.features.tetro.remote

import com.wapo.android.commons.util.Logger
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.wapo.android.commons.retrofit.DefaultHeadersInterceptor
import com.washingtonpost.android.paywall.features.tetro.TetroResponse
import com.washingtonpost.android.paywall.features.tetro.remote.TetroApiService.TetroApi
import com.washingtonpost.android.paywall.network.retrofit.APIResult
import com.washingtonpost.android.paywall.network.retrofit.CallAdapterFactory
import com.washingtonpost.android.paywall.util.PaywallConstants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class ArticleObj(
    @SerializedName("article") val url: String,
    @SerializedName("ts") var ts: Long?,
)

data class ArticleBody(
    @SerializedName("articles") val articles: List<ArticleObj>,
)

/**
 * This class manages Retrofit instance and holds [TetroApi] retrofit interface
 */
class TetroApiService private constructor() {
    private var tetroNetwork: TetroApi? = null
    private val shouldEnableNetworkDebugging = false
    private val interceptor: HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            this.level = HttpLoggingInterceptor.Level.BODY
        }
    private val client: OkHttpClient =
        OkHttpClient
            .Builder()
            .apply {
                addInterceptor(DefaultHeadersInterceptor())
                if (shouldEnableNetworkDebugging) this.addInterceptor(interceptor)
                this
                    .readTimeout(2, TimeUnit.SECONDS)
                    .connectTimeout(2, TimeUnit.SECONDS)
            }.cache(null)
            .build()

    /**
     * Get [TetroApi] refrofit implementation instance
     */
    fun getTetroNetwork(): TetroApi {
        val baseUrl =
            if (PaywallConstants.USE_METERING_PROXY) {
                PaywallConstants.METERING_PROXY_BASE_URL
            } else {
                PaywallConstants.TETRO_API
            }
        if (tetroNetwork == null) {
            val converterFactory =
                GsonConverterFactory.create(
                    GsonBuilder()
                        .setDateFormat(PAGE_DATE_FORMAT)
                        .create(),
                )
            tetroNetwork =
                Retrofit
                    .Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addCallAdapterFactory(CallAdapterFactory())
                    .addConverterFactory(converterFactory)
                    .build()
                    .create(TetroApi::class.java)

            Logger.d("Tetro", "Base URL: $baseUrl")
        }
        return tetroNetwork!!
    }

    /**
     * DEBUG PANEL ONLY : Used to change tetro envirnonment between prod/stage. Recreates Retrofit service.
     */
    fun resetTetroNetwork(
        tetroUrl: String?,
        meteringProxyBaseUrl: String?,
    ) {
        tetroUrl?.let {
            PaywallConstants.TETRO_API = it
        }

        meteringProxyBaseUrl?.let {
            PaywallConstants.METERING_PROXY_BASE_URL = it
        }
        tetroNetwork = null
    }

    /**
     * Retrofit interface for accessing Tetro API
     */
    interface TetroApi {
        @Deprecated(
            "This is for reaching the old metering endpoint. It remains in place so" +
                    "that we can re-enable the usage of the old metering endpoint via \"useMeteringProxy\"" +
                    "in the configs if we ever want to stop using the new proxy endpoint.",
        )
        @POST("v2/engine/metering/apps/evaluate")
        suspend fun getMeterData(
            @HeaderMap headers: HashMap<String, String>,
            @Body body: ArticleBody,
        ): APIResult<TetroResponse>

        @POST("evaluate")
        suspend fun getMeterProxyData(
            @HeaderMap headers: HashMap<String, String>,
            @Body body: ArticleBody,
            @Query(APP_TESTS) abVariants: String?,
        ): APIResult<TetroResponse>
    }

    companion object {
        @Volatile
        private var INSTANCE: TetroApiService? = null
        private val TAG: String = TetroApiService::class.java.simpleName
        private const val PAGE_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        private const val APP_TESTS = "app_tests"

        @JvmStatic
        fun getInstance(): TetroApiService =
            INSTANCE ?: synchronized(this) {
                INSTANCE
                    ?: TetroApiService().also {
                        INSTANCE = it
                    }
            }
    }
}
