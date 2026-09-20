// Copyright (c) 2026 The Washington Post. All rights reserved.

package com.wapo.kmpshared.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.cookie
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.core.Closeable
import kotlinx.serialization.json.Json

enum class FeatureNetworkConfig(
    val expectSuccess: Boolean,
    val requireCredentials: Boolean,
    val contentNegotiation: Boolean,
) {
    CONVERSATIONS(expectSuccess = true, requireCredentials = true, contentNegotiation = true),
    FEEDBACK(expectSuccess = true, requireCredentials = true, contentNegotiation = false),
}

class NetworkClient(
    engine: HttpClientEngine,
    private val platformNetworkProvider: PlatformNetworkProvider,
    private val coder: Json,
) : Closeable {
    private val baseClient =
        HttpClient(engine) {
            defaultRequest {
                platformNetworkProvider.getHeaders().forEach { (k, v) -> header(k, v) }
            }
        }

    fun forFeature(clientConfig: FeatureNetworkConfig): HttpClient =
        baseClient.config {
            expectSuccess = clientConfig.expectSuccess
            if (clientConfig.contentNegotiation) {
                install(ContentNegotiation) { json(coder) }
            }
            defaultRequest {
                if (clientConfig.requireCredentials) {
                    platformNetworkProvider.getCredentials()?.let { creds ->
                        cookie("wapo_login_id", creds.loginID)
                        cookie("wapo_secure_login_id", creds.secureLoginID)
                    }
                }
            }
        }

    override fun close() {
        baseClient.close()
    }

    companion object {
        const val TAG = "NetworkClient"
    }
}
