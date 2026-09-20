// Copyright (c) 2026 The Washington Post. All rights reserved.

package com.wapo.kmpshared.core.network

expect class PlatformSession

expect class NetworkSession(
    default: PlatformSession,
    logger: PlatformSession,
) {
    val default: PlatformSession
    val logger: PlatformSession
}
