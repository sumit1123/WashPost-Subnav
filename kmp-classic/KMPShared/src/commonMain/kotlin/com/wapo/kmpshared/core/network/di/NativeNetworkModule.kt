package com.wapo.kmpshared.core.network.di

import com.wapo.kmpshared.core.network.PlatformNetworkProvider
import io.ktor.client.engine.HttpClientEngine
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
internal expect class NativeNetworkModule() {
    @Single
    fun provideNativeNetworkEngine(provider: PlatformNetworkProvider): HttpClientEngine
}
