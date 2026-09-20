package com.wapo.kmpshared.testutils.network

import com.wapo.kmpshared.core.network.PlatformSession
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration

actual fun createMockPlatformSession(): PlatformSession =
    NSURLSession.sessionWithConfiguration(
        configuration = NSURLSessionConfiguration.defaultSessionConfiguration(),
    )
