package com.wapo.flagship.features.articles2.models

import com.squareup.moshi.JsonClass

/**
 * Request model for the proxy API endpoint.
 *
 * This data class represents the JSON request body sent to the proxy API
 * when fetching an encrypted article. It contains the article URL that
 * the proxy should fetch and encrypt.
 * JSON Format:
 * {
 *   "url": "https://www.washingtonpost.com/article-url"
 * }
 */

@JsonClass(generateAdapter = true)
data class ProxyRequest(
    val url: String,
)