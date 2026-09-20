package com.wapo.kmpshared.core.network.di

import com.wapo.kmpshared.core.di.Qualifiers
import com.wapo.kmpshared.core.network.FeatureNetworkConfig
import com.wapo.kmpshared.core.network.NetworkClient
import com.wapo.kmpshared.core.network.PlatformNetworkProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
class NetworkModule {
    @Single
    fun provideJsonSerializer(): Json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    @Single
    fun provideNetworkClient(
        engine: HttpClientEngine,
        platformNetworkProvider: PlatformNetworkProvider,
        coder: Json,
    ): NetworkClient = NetworkClient(engine, platformNetworkProvider, coder)

    @Single
    @Named(Qualifiers.CONVERSATIONS)
    fun provideConvosClient(client: NetworkClient): HttpClient = client.forFeature(FeatureNetworkConfig.CONVERSATIONS)

    @Single
    @Named(Qualifiers.FEEDBACK)
    fun provideFeedbackClient(client: NetworkClient): HttpClient = client.forFeature(FeatureNetworkConfig.FEEDBACK)
}
