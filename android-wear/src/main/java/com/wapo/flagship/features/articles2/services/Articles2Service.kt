/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.services

import com.wapo.flagship.base.Cacheable
import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.network.APIResult
import com.wapo.flagship.utils.network.TimeoutInterceptor
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.http.Tag

/**
 * This service is mainly (more functionality can be added as needed) used for
 * fetching [Article2] content from the network using Retrofit2.
 *
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
        @Tag cache: Cacheable? = null): APIResult<Article2>

}