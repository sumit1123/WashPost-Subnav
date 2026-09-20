package com.wapo.kmpshared.testutils.network

import com.wapo.kmpshared.core.network.NetworkSession
import com.wapo.kmpshared.core.network.PlatformCredentials
import com.wapo.kmpshared.core.network.PlatformNetworkProvider
import com.wapo.kmpshared.core.network.PlatformSession

expect fun createMockPlatformSession(): PlatformSession

class MockPlatformNetworkProvider : PlatformNetworkProvider {
    override fun getHeaders(): Map<String, String> =
        mapOf(
            "User-Agent" to "Test-Agent",
            "Client-App" to "Test-App",
            "Client-App-Version" to "1.0",
            "Platform-Name" to "Test-Platform",
            "Device-Name" to "Test-Device",
            "OS-Version" to "1.0",
            "deviceId" to "test-id",
        )

    override fun getCredentials(): PlatformCredentials? = null

    override fun getSessions(): NetworkSession =
        NetworkSession(
            default = createMockPlatformSession(),
            logger = createMockPlatformSession(),
        )

    override suspend fun refreshCredentials(): PlatformCredentials? = null
}
