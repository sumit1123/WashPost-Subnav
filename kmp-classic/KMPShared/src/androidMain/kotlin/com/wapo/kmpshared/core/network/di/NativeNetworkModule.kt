package com.wapo.kmpshared.core.network.di

import com.wapo.kmpshared.core.network.PlatformNetworkProvider
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
internal actual class NativeNetworkModule actual constructor() {
    @Single
    actual fun provideNativeNetworkEngine(provider: PlatformNetworkProvider): HttpClientEngine =
        OkHttp.create {
            preconfigured = provider.getSessions().default
        }
}
