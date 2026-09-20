package com.wapo.kmpshared.testutils.network

import com.wapo.kmpshared.core.network.PlatformSession
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response

actual fun createMockPlatformSession(): PlatformSession =
    OkHttpClient
        .Builder()
        .addInterceptor { chain ->
            Response
                .Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(500)
                .message("Mock response")
                .body("".toResponseBody())
                .build()
        }.build()