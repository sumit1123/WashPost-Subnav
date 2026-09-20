// Copyright (c) 2026 The Washington Post. All rights reserved.

package com.wapo.kmpshared.core.network

import platform.Foundation.NSURLSession

actual typealias PlatformSession = NSURLSession

actual class NetworkSession actual constructor(
    actual val default: PlatformSession,
    actual val logger: PlatformSession,
)
