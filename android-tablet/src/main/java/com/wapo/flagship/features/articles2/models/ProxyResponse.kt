package com.wapo.flagship.features.articles2.models

import com.squareup.moshi.JsonClass

/**
 * Response model from the proxy API endpoint containing encrypted article data.
 *
 * This data class represents the JSON response returned by the proxy API
 * after fetching and encrypting an article. All fields are hex-encoded strings
 * that need to be converted to byte arrays for decryption.
 * JSON Format:
 * {
 *   "iv": "hex-encoded-initialization-vector",
 *   "authTag": "hex-encoded-authentication-tag",
 *   "encrypted": "hex-encoded-encrypted-article-content"
 * }
 */
@JsonClass(generateAdapter = true)
data class ProxyResponse(
    val iv: String,
    val authTag: String,
    val encrypted: String,
)