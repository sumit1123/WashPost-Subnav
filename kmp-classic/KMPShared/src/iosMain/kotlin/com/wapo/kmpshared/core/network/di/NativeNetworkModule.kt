package com.wapo.kmpshared.core.network.di

import com.wapo.kmpshared.core.network.PlatformNetworkProvider
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.engine.darwin.KtorNSURLSessionDelegate
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import platform.Foundation.NSURLSession

@Module
internal actual class NativeNetworkModule actual constructor() {
    @Single
    actual fun provideNativeNetworkEngine(provider: PlatformNetworkProvider): HttpClientEngine {
        // 1. Get the session passed from Swift
        val swiftSession = provider.getSessions().default

        // 2. Extract its configuration copy
        val configuration = swiftSession.configuration

        // 3. Create a brand-new Ktor delegate
        val ktorDelegate = KtorNSURLSessionDelegate()

        // 4. Construct a new session, tying the delegate to it at birth
        val boundSession =
            NSURLSession.sessionWithConfiguration(
                configuration = configuration,
                delegate = ktorDelegate,
                delegateQueue = null,
            )

        // 5. Hand both over to Ktor safely
        return Darwin.create {
            usePreconfiguredSession(boundSession, ktorDelegate)
        }
    }
}
