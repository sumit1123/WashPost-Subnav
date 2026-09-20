package com.wapo.kmpshared.core.network

import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import java.net.ConnectException
import java.net.MalformedURLException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

actual fun isConnectivityError(
    e: Throwable,
    isReachable: Boolean,
): Boolean {
    if (e is CancellationException) return true

    val cause = e.cause ?: e

    return when (cause) {
        is MalformedURLException,
        is IllegalArgumentException,
        -> true

        is UnknownHostException,
        is ConnectException,
        is SocketException,
        -> true

        is SocketTimeoutException,
        is HttpRequestTimeoutException,
        -> isReachable

        else -> false
    }
}
