package com.wapo.kmpshared.core.network

import io.ktor.client.engine.darwin.DarwinHttpRequestException
import kotlinx.coroutines.CancellationException
import platform.Foundation.NSURLErrorBadURL
import platform.Foundation.NSURLErrorCallIsActive
import platform.Foundation.NSURLErrorCancelled
import platform.Foundation.NSURLErrorDataNotAllowed
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLErrorInternationalRoamingOff
import platform.Foundation.NSURLErrorNetworkConnectionLost
import platform.Foundation.NSURLErrorNotConnectedToInternet
import platform.Foundation.NSURLErrorTimedOut
import platform.Foundation.NSURLErrorUnknown
import platform.Foundation.NSURLErrorUnsupportedURL

actual fun isConnectivityError(
    e: Throwable,
    isReachable: Boolean,
): Boolean {
    if (e is CancellationException) return true

    val nsError = (e as? DarwinHttpRequestException)?.origin ?: return false

    if (nsError.domain != NSURLErrorDomain) return false

    return when (nsError.code) {
        NSURLErrorUnknown, // -1
        NSURLErrorCancelled, // -999
        NSURLErrorBadURL, // -1000
        NSURLErrorUnsupportedURL, // -1002
        NSURLErrorNotConnectedToInternet, // -1009
        NSURLErrorInternationalRoamingOff, // -1018
        NSURLErrorCallIsActive, // -1019
        NSURLErrorDataNotAllowed, // -1020
        -> true

        NSURLErrorTimedOut, // -1001
        NSURLErrorNetworkConnectionLost, // -1005
        -> isReachable

        else -> false
    }
}
