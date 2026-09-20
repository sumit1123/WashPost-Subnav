// Copyright (c) 2026 The Washington Post. All rights reserved.

package com.wapo.kmpshared.core.network

data class PlatformCredentials(
    val loginID: String,
    val secureLoginID: String,
    val accessToken: String,
    val clientID: String,
    val commentsToken: String?,
)

interface PlatformNetworkProvider {
    fun getHeaders(): Map<String, String>

    fun getSessions(): NetworkSession

    fun getCredentials(): PlatformCredentials?

    suspend fun refreshCredentials(): PlatformCredentials?
}
