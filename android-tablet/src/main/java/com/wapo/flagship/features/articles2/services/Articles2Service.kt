package com.wapo.flagship.features.articles2.services

import com.wapo.flagship.base.Cacheable
import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.articles2.models.ProxyRequest
import com.wapo.flagship.features.articles2.models.ProxyResponse
import com.wapo.flagship.network.retrofit.network.APIResult
import com.wapo.flagship.util.network.TimeoutInterceptor
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Tag
import retrofit2.http.Url

/**
 * This service is mainly (more functionality can be added as needed) used for fetching [Article2] content from the network using Retrofit2.
 */
interface Articles2Service {
    /**
     * This function is used for fetching [Article2] content from the network using Retrofit2.
     * [url] is the [Article2.contenturl] which is a primary key and the unique id for any article.
     * [platform] is by default set to "iphoneclassic"
     * [followLinks] is by default set to false
     */
    @GET("content-by-url.json")
    suspend fun getArticleContent(
        @Header(TimeoutInterceptor.WP_TIMEOUT) timeoutMs: Int,
        @Query(value = "url", encoded = true) url: String,
        @Query("platform") platform: String = "iphoneclassic",
        @Query("followLinks") followLinks: Boolean = false,
        @Tag cache: Cacheable? = null,
    ): APIResult<Article2>

    /**
     * Fetches an encrypted article from the proxy API endpoint.
     */
    @Headers(
        "ngrok-skip-browser-warning: true",
    )
    @GET
    suspend fun fetchArticleFromProxy(
        @Url baseUrl: String,
        @Query(value = "url", encoded = true) url: String,
    ): ProxyResponse
}
